import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { BookOpen, CheckCircle2, Lock, LogOut, PlayCircle, Users } from 'lucide-react';
import { Badge, Progress } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { EmptyState, ErrorState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { StudentAssignmentsTab } from '@/features/assignments/StudentAssignmentsTab';
import { AnnouncementsTab } from '@/features/community/AnnouncementsTab';
import { DiscussionsTab } from '@/features/community/DiscussionsTab';
import { LESSON_ICONS } from '@/features/content/lessonIcons';
import { useStudentCurriculum } from '@/features/content/useContent';
import { StudentQuizzesTab } from '@/features/quizzes/StudentQuizzesTab';
import { SubjectThumb } from '@/features/subjects/SubjectCard';
import { useEnroll, useLeave, useStudentSubject } from '@/features/subjects/useSubjects';
import { parseError } from '@/lib/api';
import { formatDuration, pluralize } from '@/lib/utils';

function Curriculum({ subjectId, enrolled }) {
  const q = useStudentCurriculum(subjectId);
  return (
    <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.modules.length === 0} empty={<EmptyState icon={BookOpen} title="The curriculum is being prepared" description="Lessons will show up here once the teacher publishes them." />}>
      {(c) => (
        <div className="space-y-4">
          {c.modules.map((m) => (
            <Card key={m.id} className="overflow-hidden">
              <div className="border-b bg-muted/40 px-5 py-3"><h3 className="font-semibold">{m.title}</h3>{m.description && <p className="text-xs text-muted-foreground">{m.description}</p>}</div>
              <ul className="divide-y">
                {m.lessons.map((l) => {
                  const Icon = LESSON_ICONS[l.type];
                  const row = (
                    <span className="flex items-center gap-3 px-5 py-3 text-sm">
                      {enrolled ? (l.completed ? <CheckCircle2 className="h-4 w-4 shrink-0 text-accent" aria-label="Completed" /> : <Icon className="h-4 w-4 shrink-0 text-primary" aria-hidden="true" />) : <Lock className="h-4 w-4 shrink-0 text-muted-foreground" aria-label="Locked" />}
                      <span className="min-w-0 flex-1 truncate font-medium">{l.title}</span>
                      {l.durationSeconds > 0 && <span className="text-xs text-muted-foreground">{formatDuration(l.durationSeconds)}</span>}
                    </span>
                  );
                  return <li key={l.id}>{enrolled ? <Link to={`/student/subjects/${subjectId}/learn/${l.id}`} className="block hover:bg-muted">{row}</Link> : <div className="opacity-80">{row}</div>}</li>;
                })}
              </ul>
            </Card>
          ))}
          {!enrolled && <p className="rounded-xl bg-secondary px-4 py-3 text-sm text-secondary-foreground">Lesson content, files, quizzes and discussions unlock when you enroll.</p>}
        </div>
      )}
    </QueryBoundary>
  );
}

export default function CourseDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const q = useStudentSubject(id);
  const enroll = useEnroll(id);
  const leave = useLeave(id);
  const [confirmLeave, setConfirmLeave] = useState(false);
  const tab = params.get('tab') || 'curriculum';

  if (q.isPending) return <ListSkeleton rows={3} />;
  if (q.isError) return <ErrorState error={q.error} onRetry={() => q.refetch()} />;
  const s = q.data;
  const enrolled = !!s.enrolled;
  const onEnroll = () => enroll.mutate(undefined, { onSuccess: () => toast.success(`You are enrolled in ${s.title}`), onError: (e) => toast.error(parseError(e).message) });

  return (
    <div>
      <Card className="mb-6 overflow-hidden">
        <div className="grid md:grid-cols-[18rem_1fr]">
          <SubjectThumb subject={s} className="h-48 md:h-full" />
          <div className="p-6">
            <div className="flex flex-wrap items-center gap-2">{s.category && <Badge variant="secondary">{s.category}</Badge>}{s.status === 'ARCHIVED' && <Badge variant="muted">Archived</Badge>}</div>
            <h1 className="mt-2 text-2xl font-bold tracking-tight sm:text-3xl">{s.title}</h1>
            <p className="mt-1 text-sm text-muted-foreground">Taught by <strong className="text-foreground">{s.teacherName}</strong></p>
            <div className="mt-3 flex flex-wrap gap-4 text-sm text-muted-foreground"><span className="inline-flex items-center gap-1.5"><BookOpen className="h-4 w-4" aria-hidden="true" />{pluralize(s.lessonCount, 'lesson')}</span><span className="inline-flex items-center gap-1.5"><Users className="h-4 w-4" aria-hidden="true" />{pluralize(s.enrollmentCount, 'student')}</span></div>
            {s.description && <p className="prose-user mt-4 text-muted-foreground">{s.description}</p>}
            {enrolled && <div className="mt-4 max-w-sm"><div className="mb-1 flex justify-between text-xs"><span>Your progress</span><span className="font-semibold">{Math.round(s.progressPercent || 0)}%</span></div><Progress value={s.progressPercent} label="Your progress" /></div>}
            <div className="mt-5 flex flex-wrap gap-2">
              {enrolled ? (<>
                <Button asChild size="lg"><Link to={`/student/subjects/${id}/learn`}><PlayCircle className="h-5 w-5" /> {s.progressPercent > 0 ? 'Continue learning' : 'Start learning'}</Link></Button>
                <Button variant="ghost" size="lg" onClick={() => setConfirmLeave(true)}><LogOut className="h-4 w-4" /> Leave course</Button>
              </>) : <Button size="lg" loading={enroll.isPending} onClick={onEnroll}>Enroll for free</Button>}
            </div>
          </div>
        </div>
      </Card>
      <Tabs value={tab} onValueChange={(v) => setParams({ tab: v })}>
        <TabsList><TabsTrigger value="curriculum">Curriculum</TabsTrigger><TabsTrigger value="quizzes">Quizzes</TabsTrigger><TabsTrigger value="assignments">Assignments</TabsTrigger><TabsTrigger value="announcements">Announcements</TabsTrigger><TabsTrigger value="discussions">Discussions</TabsTrigger></TabsList>
        <TabsContent value="curriculum"><Curriculum subjectId={id} enrolled={enrolled} /></TabsContent>
        {['quizzes', 'assignments', 'announcements', 'discussions'].map((t) => (
          <TabsContent key={t} value={t}>
            {!enrolled ? <EmptyState icon={Lock} title="Enroll to unlock this section" description="Quizzes, assignments, announcements and discussions are for enrolled students." action={<Button loading={enroll.isPending} onClick={onEnroll}>Enroll for free</Button>} />
              : t === 'quizzes' ? <StudentQuizzesTab subjectId={id} /> : t === 'assignments' ? <StudentAssignmentsTab subjectId={id} />
              : t === 'announcements' ? <AnnouncementsTab subjectId={id} isTeacher={false} /> : <DiscussionsTab subjectId={id} />}
          </TabsContent>
        ))}
      </Tabs>
      <ConfirmDialog open={confirmLeave} onOpenChange={setConfirmLeave} destructive confirmLabel="Leave course" title="Leave this course?" description="You will lose access to its lessons, quizzes and discussions. You can enroll again later." loading={leave.isPending}
        onConfirm={() => leave.mutate(undefined, { onSuccess: () => { toast.success('You left the course'); setConfirmLeave(false); navigate('/student/subjects'); }, onError: (e) => toast.error(parseError(e).message) })} />
    </div>
  );
}
