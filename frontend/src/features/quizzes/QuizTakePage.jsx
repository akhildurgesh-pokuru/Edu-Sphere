import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { AlertTriangle, ArrowLeft, ChevronLeft, ChevronRight, Clock, Hourglass, Send, Trophy } from 'lucide-react';
import { Badge } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm';
import { Textarea } from '@/components/ui/input';
import { ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { Progress } from '@/components/ui/misc';
import { parseError } from '@/lib/api';
import { cn, formatDuration } from '@/lib/utils';
import { ReviewList } from './ReviewList';
import { useAttemptActions, useStudentAttempt } from './useQuizzes';

function ResultView({ r }) {
  return (
    <div className="mx-auto max-w-3xl">
      <Button asChild variant="ghost" size="sm" className="mb-3 -ml-2"><Link to="/student/subjects"><ArrowLeft className="h-4 w-4" /> Back to my subjects</Link></Button>
      <Card className="mb-6 overflow-hidden">
        <div className={cn('p-8 text-center text-white', r.pendingGrading ? 'bg-gradient-to-br from-amber-500 to-orange-500' : r.passed ? 'bg-gradient-to-br from-emerald-500 to-teal-600' : 'bg-gradient-to-br from-rose-500 to-orange-500')}>
          {r.pendingGrading ? <Hourglass className="mx-auto h-10 w-10" aria-hidden="true" /> : <Trophy className="mx-auto h-10 w-10" aria-hidden="true" />}
          <p className="mt-3 text-sm font-medium opacity-90">{r.quizTitle} · attempt #{r.attemptNo}</p>
          <p className="mt-1 text-5xl font-extrabold">{r.percent}%</p>
          <p className="mt-2 text-sm opacity-95">{r.score} / {r.maxScore} marks</p>
          <p className="mt-3 inline-block rounded-full bg-white/20 px-4 py-1 text-sm font-semibold">{r.pendingGrading ? 'Short answers are waiting for your teacher' : r.passed ? 'You passed!' : 'Not passed this time'}</p>
          {r.expired && <p className="mt-3 text-xs opacity-90">This attempt was submitted automatically when the time ran out.</p>}
        </div>
      </Card>
      {r.reviewAllowed ? (
        <><h2 className="mb-3 text-lg font-semibold">Review your answers</h2><ReviewList items={r.review} /></>
      ) : (
        <p className="rounded-xl bg-muted px-4 py-3 text-sm text-muted-foreground">Your teacher has turned answer review off for this quiz.</p>
      )}
    </div>
  );
}

function Taker({ attempt }) {
  const qc = useQueryClient();
  const { save, submit } = useAttemptActions();
  const total = attempt.questions.length;
  const offset = useRef(new Date(attempt.serverNow).getTime() - Date.now());
  const deadline = useMemo(() => new Date(attempt.deadlineAt).getTime(), [attempt.deadlineAt]);
  const [answers, setAnswers] = useState(() => Object.fromEntries(attempt.questions.map((q) => [q.id, { optionIds: q.selectedOptionIds || [], text: q.textAnswer || '' }])));
  const [index, setIndex] = useState(0);
  const [remaining, setRemaining] = useState(() => Math.max(0, Math.round((deadline - (Date.now() + offset.current)) / 1000)));
  const [confirm, setConfirm] = useState(false);
  const [saveState, setSaveState] = useState('saved');
  const submitted = useRef(false);
  const dirty = useRef(false);
  const latest = useRef(answers);
  latest.current = answers;

  const payload = useCallback(() => attempt.questions.map((q) => ({ questionId: q.id, optionIds: latest.current[q.id]?.optionIds || [], text: latest.current[q.id]?.text || null })), [attempt.questions]);
  const answered = (q) => { const a = answers[q.id]; return q.type === 'SHORT_ANSWER' ? !!a?.text?.trim() : (a?.optionIds || []).length > 0; };
  const answeredCount = attempt.questions.filter(answered).length;

  const finish = useCallback((auto = false) => {
    if (submitted.current) return;
    submitted.current = true;
    submit.mutate({ attemptId: attempt.attemptId, answers: payload() }, {
      onSuccess: (res) => { qc.setQueryData(['quizzes', 'student', 'attempt', String(attempt.attemptId)], res); toast.success(auto ? 'Time is up. Your answers were submitted.' : 'Quiz submitted'); },
      onError: (e) => { submitted.current = false; const p = parseError(e); toast.error(p.message); if (p.status === 409) qc.invalidateQueries({ queryKey: ['quizzes', 'student', 'attempt'] }); },
    });
  }, [attempt.attemptId, payload, qc, submit]);

  useEffect(() => {
    const t = setInterval(() => {
      const left = Math.max(0, Math.round((deadline - (Date.now() + offset.current)) / 1000));
      setRemaining(left);
      if (left <= 0) finish(true);
    }, 1000);
    return () => clearInterval(t);
  }, [deadline, finish]);

  // debounced autosave
  useEffect(() => {
    if (!dirty.current) return undefined;
    setSaveState('saving');
    const t = setTimeout(() => {
      save.mutate({ attemptId: attempt.attemptId, answers: payload() }, {
        onSuccess: () => setSaveState('saved'),
        onError: (e) => { setSaveState('error'); if (parseError(e).status === 409) qc.invalidateQueries({ queryKey: ['quizzes', 'student', 'attempt'] }); },
      });
    }, 800);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [answers]);

  useEffect(() => {
    const warn = (e) => { if (!submitted.current) { e.preventDefault(); e.returnValue = ''; } };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, []);

  const q = attempt.questions[index];
  const set = (patch) => { dirty.current = true; setAnswers((a) => ({ ...a, [q.id]: { ...a[q.id], ...patch } })); };
  const toggle = (id) => {
    const cur = answers[q.id].optionIds;
    if (q.type === 'MULTIPLE_SELECT') set({ optionIds: cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id] });
    else set({ optionIds: [id] });
  };
  const low = remaining <= 60;

  return (
    <div className="mx-auto max-w-5xl">
      <div className="sticky top-16 z-10 -mx-4 mb-5 border-b bg-background/90 px-4 py-3 backdrop-blur sm:-mx-6 sm:px-6">
        <div className="flex items-center gap-3">
          <div className="min-w-0 flex-1"><h1 className="truncate text-lg font-bold">{attempt.title}</h1><p className="text-xs text-muted-foreground">Attempt #{attempt.attemptNo} · {answeredCount}/{total} answered · <span aria-live="polite">{saveState === 'saving' ? 'Saving…' : saveState === 'error' ? 'Could not save, retrying on next change' : 'All answers saved'}</span></p></div>
          <div role="timer" aria-label="Time remaining" className={cn('inline-flex items-center gap-2 rounded-xl px-4 py-2 font-mono text-lg font-bold', low ? 'bg-destructive/10 text-destructive' : 'bg-primary/10 text-primary')}><Clock className="h-5 w-5" aria-hidden="true" />{formatDuration(remaining)}</div>
        </div>
        <Progress value={(answeredCount / total) * 100} className="mt-3" label="Answered questions" />
      </div>
      <div className="grid gap-5 lg:grid-cols-[1fr_16rem]">
        <Card className="p-6">
          <div className="mb-3 flex items-center justify-between"><Badge variant="secondary">Question {index + 1} of {total}</Badge><span className="text-xs text-muted-foreground">{q.marks} mark{q.marks === 1 ? '' : 's'}{q.type === 'MULTIPLE_SELECT' ? ' · select all that apply' : ''}</span></div>
          <p className="whitespace-pre-wrap break-words text-lg font-semibold">{q.text}</p>
          <div className="mt-5 space-y-2.5">
            {q.type === 'SHORT_ANSWER' ? (
              <Textarea rows={6} value={answers[q.id].text} onChange={(e) => set({ text: e.target.value })} aria-label="Your answer" placeholder="Type your answer…" />
            ) : (
              q.options.map((o) => {
                const on = answers[q.id].optionIds.includes(o.id);
                return (
                  <label key={o.id} className={cn('flex cursor-pointer items-center gap-3 rounded-xl border-2 px-4 py-3 transition-all', on ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/40')}>
                    <input type={q.type === 'MULTIPLE_SELECT' ? 'checkbox' : 'radio'} name={`q-${q.id}`} checked={on} onChange={() => toggle(o.id)} className="h-4 w-4 accent-[hsl(var(--primary))]" />
                    <span className="min-w-0 break-words">{o.text}</span>
                  </label>
                );
              })
            )}
          </div>
          <div className="mt-6 flex items-center justify-between">
            <Button variant="outline" disabled={index === 0} onClick={() => setIndex((i) => i - 1)}><ChevronLeft className="h-4 w-4" /> Previous</Button>
            {index < total - 1 ? <Button onClick={() => setIndex((i) => i + 1)}>Next <ChevronRight className="h-4 w-4" /></Button> : <Button variant="accent" onClick={() => setConfirm(true)}><Send className="h-4 w-4" /> Submit quiz</Button>}
          </div>
        </Card>
        <Card className="h-fit p-4">
          <h2 className="mb-3 text-sm font-semibold">Question navigator</h2>
          <div className="grid grid-cols-5 gap-2" role="group" aria-label="Question navigator">
            {attempt.questions.map((x, i) => (
              <button key={x.id} onClick={() => setIndex(i)} aria-label={`Question ${i + 1}${answered(x) ? ', answered' : ', not answered'}`} aria-current={i === index ? 'true' : undefined}
                className={cn('h-10 rounded-lg border text-sm font-semibold transition-colors', i === index ? 'border-primary bg-primary text-primary-foreground' : answered(x) ? 'border-accent/40 bg-accent/15 text-emerald-700 dark:text-emerald-300' : 'hover:bg-muted')}>{i + 1}</button>
            ))}
          </div>
          <Button variant="accent" className="mt-4 w-full" onClick={() => setConfirm(true)}><Send className="h-4 w-4" /> Submit</Button>
        </Card>
      </div>
      <ConfirmDialog open={confirm} onOpenChange={setConfirm} confirmLabel="Submit now" loading={submit.isPending} title="Submit your quiz?"
        description={answeredCount < total ? `You have ${total - answeredCount} unanswered question${total - answeredCount > 1 ? 's' : ''}. You cannot change answers after submitting.` : 'You cannot change your answers after submitting.'}
        onConfirm={() => { setConfirm(false); finish(false); }} />
      {remaining <= 0 && <p role="alert" className="mt-4 flex items-center gap-2 text-sm font-medium text-destructive"><AlertTriangle className="h-4 w-4" /> Time is up. Submitting your answers…</p>}
    </div>
  );
}

export default function QuizTakePage() {
  const { attemptId } = useParams();
  const q = useStudentAttempt(attemptId);
  return (
    <QueryBoundary query={q} skeleton={<ListSkeleton />}>
      {(data) => (data.questions ? <Taker key={data.attemptId} attempt={data} /> : <ResultView r={data} />)}
    </QueryBoundary>
  );
}
