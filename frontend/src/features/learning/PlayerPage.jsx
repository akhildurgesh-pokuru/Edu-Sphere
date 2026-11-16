import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import * as Accordion from '@radix-ui/react-accordion';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { ArrowLeft, CheckCircle2, ChevronDown, ChevronLeft, ChevronRight, Circle, Download, ExternalLink, FileText, Lock } from 'lucide-react';
import { Progress } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Textarea } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ErrorState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { DiscussionsTab } from '@/features/community/DiscussionsTab';
import { useLesson, useStudentCurriculum } from '@/features/content/useContent';
import { LESSON_ICONS } from '@/features/content/lessonIcons';
import { api, parseError, resourceUrl } from '@/lib/api';
import { cn, formatBytes, formatDuration } from '@/lib/utils';

const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2];

function VideoPlayer({ lesson, onComplete }) {
  const ref = useRef(null);
  const lastSent = useRef(0);
  const [speed, setSpeed] = useState(() => { try { return Number(localStorage.getItem('edusphere-speed')) || 1; } catch { return 1; } });
  const [error, setError] = useState(false);
  const save = useCallback((pos) => {
    const p = Math.floor(pos);
    if (!Number.isFinite(p) || p < 0) return;
    lastSent.current = p;
    api.put(`/student/lessons/${lesson.id}/position`, { position: p }).catch(() => {});
  }, [lesson.id]);

  useEffect(() => {
    const v = ref.current;
    return () => { if (v && v.currentTime > 0) save(v.currentTime); };
  }, [save, lesson.id]);

  const onLoaded = () => {
    const v = ref.current;
    const resume = lesson.progress?.lastPositionSeconds || 0;
    v.playbackRate = speed;
    if (resume > 5 && resume < v.duration - 5) { v.currentTime = resume; toast.info(`Resuming from ${formatDuration(resume)}`); }
  };
  const changeSpeed = (e) => {
    const s = Number(e.target.value);
    setSpeed(s);
    if (ref.current) ref.current.playbackRate = s;
    try { localStorage.setItem('edusphere-speed', String(s)); } catch { /* ignore */ }
  };

  return (
    <div>
      <div className="overflow-hidden rounded-2xl bg-black shadow-card">
        <video ref={ref} key={lesson.id} className="aspect-video w-full" controls preload="metadata" controlsList="nodownload" src={resourceUrl(lesson.resource.id)} crossOrigin="use-credentials"
          onLoadedMetadata={onLoaded} onError={() => setError(true)} onPause={(e) => save(e.currentTarget.currentTime)}
          onTimeUpdate={(e) => { const t = e.currentTarget.currentTime; if (Math.abs(t - lastSent.current) >= 10) save(t); }}
          onEnded={() => { save(0); onComplete(true); }}>
          Your browser does not support HTML5 video.
        </video>
      </div>
      {error && (
        <p role="alert" className="mt-3 rounded-xl bg-destructive/10 px-4 py-3 text-sm font-medium text-destructive">
          This video could not be played in your browser. <a href={resourceUrl(lesson.resource.id, true)} className="underline">Download it instead</a>.
        </p>
      )}
      <div className="mt-3 flex items-center gap-2 text-sm">
        <label htmlFor="speed" className="text-muted-foreground">Playback speed</label>
        <select id="speed" value={speed} onChange={changeSpeed} className="h-8 rounded-lg border border-input bg-card px-2 text-sm">{SPEEDS.map((s) => <option key={s} value={s}>{s}x</option>)}</select>
      </div>
    </div>
  );
}

function NotesPanel({ lesson, subjectId }) {
  const qc = useQueryClient();
  const [text, setText] = useState(lesson.progress?.notes || '');
  const [state, setState] = useState('saved');
  const m = useMutation({ mutationFn: (notes) => api.put(`/student/lessons/${lesson.id}/notes`, { notes }), onSuccess: () => { setState('saved'); qc.invalidateQueries({ queryKey: ['lesson', String(lesson.id)] }); }, onError: (e) => { setState('error'); toast.error(parseError(e).message); } });
  useEffect(() => {
    if (text === (lesson.progress?.notes || '')) return undefined;
    setState('saving');
    const t = setTimeout(() => m.mutate(text), 900);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text]);
  return (
    <div>
      <label htmlFor="notes" className="sr-only">Your private notes</label>
      <Textarea id="notes" rows={8} value={text} maxLength={10000} onChange={(e) => setText(e.target.value)} placeholder="Write private notes for this lesson. They save automatically." />
      <p className="mt-1 text-xs text-muted-foreground" aria-live="polite">{state === 'saving' ? 'Saving…' : state === 'error' ? 'Could not save notes' : 'Notes saved'}</p>
    </div>
  );
}

function Sidebar({ curriculum, subjectId, currentId, percent }) {
  const open = curriculum.modules.filter((m) => m.lessons.some((l) => l.id === currentId)).map((m) => String(m.id));
  const [value, setValue] = useState(open);
  useEffect(() => setValue((v) => Array.from(new Set([...v, ...open]))), [currentId]); // eslint-disable-line react-hooks/exhaustive-deps
  return (
    <Card className="overflow-hidden">
      <div className="border-b p-4"><div className="mb-2 flex justify-between text-sm"><span className="font-semibold">Course progress</span><span className="font-bold text-primary">{Math.round(percent)}%</span></div><Progress value={percent} label="Course progress" /></div>
      <Accordion.Root type="multiple" value={value} onValueChange={setValue}>
        {curriculum.modules.map((m) => (
          <Accordion.Item key={m.id} value={String(m.id)} className="border-b last:border-0">
            <Accordion.Header><Accordion.Trigger className="group flex w-full items-center justify-between gap-2 px-4 py-3 text-left text-sm font-semibold hover:bg-muted"><span className="min-w-0 truncate">{m.title}</span><ChevronDown className="h-4 w-4 shrink-0 transition-transform group-data-[state=open]:rotate-180" aria-hidden="true" /></Accordion.Trigger></Accordion.Header>
            <Accordion.Content className="overflow-hidden data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down">
              <ul>{m.lessons.map((l) => {
                const Icon = LESSON_ICONS[l.type];
                return (
                  <li key={l.id}><Link to={`/student/subjects/${subjectId}/learn/${l.id}`} aria-current={l.id === currentId ? 'page' : undefined}
                    className={cn('flex items-center gap-3 px-4 py-2.5 text-sm transition-colors hover:bg-muted', l.id === currentId && 'bg-primary/10 font-semibold text-primary')}>
                    {l.completed ? <CheckCircle2 className="h-4 w-4 shrink-0 text-accent" aria-label="Completed" /> : <Circle className="h-4 w-4 shrink-0 text-muted-foreground" aria-label="Not completed" />}
                    <span className="min-w-0 flex-1 truncate">{l.title}</span><Icon className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                  </Link></li>);
              })}</ul>
            </Accordion.Content>
          </Accordion.Item>
        ))}
      </Accordion.Root>
    </Card>
  );
}

export default function PlayerPage() {
  const { id, lessonId } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const curriculum = useStudentCurriculum(id);
  const flat = useMemo(() => (curriculum.data ? curriculum.data.modules.flatMap((m) => m.lessons) : []), [curriculum.data]);

  useEffect(() => {
    if (!lessonId && flat.length) {
      const first = flat.find((l) => !l.completed) || flat[0];
      navigate(`/student/subjects/${id}/learn/${first.id}`, { replace: true });
    }
  }, [lessonId, flat, id, navigate]);

  const lesson = useLesson(lessonId);
  const complete = useMutation({
    mutationFn: (done) => (done ? api.post(`/student/lessons/${lessonId}/complete`) : api.delete(`/student/lessons/${lessonId}/complete`)),
    onSuccess: (_, done) => { qc.invalidateQueries({ queryKey: ['curriculum'] }); qc.invalidateQueries({ queryKey: ['lesson'] }); qc.invalidateQueries({ queryKey: ['student'] }); toast.success(done ? 'Lesson completed' : 'Marked as not completed'); },
    onError: (e) => toast.error(parseError(e).message),
  });

  if (curriculum.isPending) return <ListSkeleton />;
  if (curriculum.isError) return <ErrorState error={curriculum.error} onRetry={() => curriculum.refetch()} />;
  if (curriculum.data.locked) return <ErrorState error={{ kind: 'forbidden', message: 'Enroll in this course to open its lessons.' }} />;
  const idx = flat.findIndex((l) => String(l.id) === String(lessonId));
  const prev = idx > 0 ? flat[idx - 1] : null;
  const next = idx >= 0 && idx < flat.length - 1 ? flat[idx + 1] : null;
  const done = flat.filter((l) => l.completed).length;
  const percent = flat.length ? (done * 100) / flat.length : 0;
  const goto = (l) => navigate(`/student/subjects/${id}/learn/${l.id}`);

  return (
    <div>
      <Button asChild variant="ghost" size="sm" className="mb-3 -ml-2"><Link to={`/student/subjects/${id}`}><ArrowLeft className="h-4 w-4" /> Back to course</Link></Button>
      <div className="grid gap-5 xl:grid-cols-[1fr_22rem]">
        <div className="min-w-0">
          <QueryBoundary query={lesson} skeleton={<ListSkeleton rows={3} />}>
            {(l) => (
              <>
                {l.type === 'VIDEO' && l.resource && <VideoPlayer lesson={l} onComplete={(v) => !l.progress?.completed && complete.mutate(v)} />}
                {l.type === 'DOCUMENT' && l.resource && (
                  <Card className="flex flex-col items-center gap-3 p-10 text-center"><FileText className="h-14 w-14 text-primary" aria-hidden="true" /><p className="font-semibold">{l.resource.name}</p><p className="text-sm text-muted-foreground">{formatBytes(l.resource.sizeBytes)}</p>
                    <div className="flex gap-2"><Button asChild><a href={resourceUrl(l.resource.id)} target="_blank" rel="noopener noreferrer"><ExternalLink className="h-4 w-4" /> Open</a></Button><Button asChild variant="outline"><a href={resourceUrl(l.resource.id, true)}><Download className="h-4 w-4" /> Download</a></Button></div></Card>
                )}
                {l.type === 'LINK' && <Card className="flex flex-col items-center gap-3 p-10 text-center"><ExternalLink className="h-14 w-14 text-primary" aria-hidden="true" /><p className="break-all text-sm text-muted-foreground">{l.externalUrl}</p><Button asChild><a href={l.externalUrl} target="_blank" rel="noopener noreferrer">Open link</a></Button></Card>}
                {l.type === 'TEXT' && <Card className="p-6"><p className="prose-user text-base">{l.contentText}</p></Card>}
                <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
                  <h1 className="min-w-0 break-words text-xl font-bold sm:text-2xl">{l.title}</h1>
                  <Button variant={l.progress?.completed ? 'secondary' : 'accent'} loading={complete.isPending} onClick={() => complete.mutate(!l.progress?.completed)}>
                    <CheckCircle2 className="h-4 w-4" /> {l.progress?.completed ? 'Completed' : 'Mark complete'}
                  </Button>
                </div>
                <div className="mt-4 flex gap-2">
                  <Button variant="outline" disabled={!prev} onClick={() => goto(prev)}><ChevronLeft className="h-4 w-4" /> Previous</Button>
                  <Button disabled={!next} onClick={() => goto(next)}>Next <ChevronRight className="h-4 w-4" /></Button>
                </div>
                <Tabs defaultValue="overview" className="mt-6">
                  <TabsList><TabsTrigger value="overview">Overview</TabsTrigger><TabsTrigger value="notes">Notes</TabsTrigger><TabsTrigger value="resources">Resources</TabsTrigger><TabsTrigger value="discussion">Discussion</TabsTrigger></TabsList>
                  <TabsContent value="overview"><p className="prose-user">{l.type === 'TEXT' ? 'Read the lesson above, then mark it complete.' : l.contentText || 'No description for this lesson.'}</p></TabsContent>
                  <TabsContent value="notes"><NotesPanel key={l.id} lesson={l} subjectId={id} /></TabsContent>
                  <TabsContent value="resources">
                    {l.resource ? <a href={resourceUrl(l.resource.id, true)} className="flex items-center gap-3 rounded-xl border p-3 hover:bg-muted"><Download className="h-5 w-5 text-primary" aria-hidden="true" /><span className="min-w-0 flex-1 truncate text-sm font-medium">{l.resource.name}</span><span className="text-xs text-muted-foreground">{formatBytes(l.resource.sizeBytes)}</span></a>
                      : <p className="text-sm text-muted-foreground">This lesson has no downloadable files.</p>}
                  </TabsContent>
                  <TabsContent value="discussion"><DiscussionsTab subjectId={id} /></TabsContent>
                </Tabs>
              </>
            )}
          </QueryBoundary>
        </div>
        <aside aria-label="Course curriculum"><Sidebar curriculum={curriculum.data} subjectId={id} currentId={Number(lessonId)} percent={percent} /></aside>
      </div>
    </div>
  );
}
