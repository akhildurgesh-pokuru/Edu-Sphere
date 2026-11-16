import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { ArrowLeft } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Badge, statusVariant } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input, Textarea } from '@/components/ui/input';
import { ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { parseError } from '@/lib/api';
import { ReviewList } from './ReviewList';
import { useQuizActions, useTeacherAttempt } from './useQuizzes';

export default function AttemptReviewPage() {
  const { attemptId } = useParams();
  const q = useTeacherAttempt(attemptId);
  const actions = useQuizActions();
  const [grades, setGrades] = useState({});
  const set = (id, patch) => setGrades((g) => ({ ...g, [id]: { ...g[id], ...patch } }));

  return (
    <QueryBoundary query={q} skeleton={<ListSkeleton />}>
      {(r) => {
        const shorts = r.review.filter((x) => x.type === 'SHORT_ANSWER');
        const objective = r.review.filter((x) => x.type !== 'SHORT_ANSWER');
        const save = () => {
          const payload = shorts.filter((s) => grades[s.answerId]?.marks !== undefined && grades[s.answerId].marks !== '')
            .map((s) => ({ answerId: s.answerId, marks: Number(grades[s.answerId].marks), feedback: grades[s.answerId].feedback ?? s.feedback ?? '' }));
          if (payload.length === 0) { toast.info('Enter marks for at least one answer'); return; }
          actions.grade.mutate({ attemptId: r.attemptId, grades: payload }, { onSuccess: () => { toast.success('Grades saved'); setGrades({}); }, onError: (e) => toast.error(parseError(e).message) });
        };
        return (
          <div>
            <Button asChild variant="ghost" size="sm" className="mb-3 -ml-2"><Link to={`/teacher/quizzes/${r.quizId}/attempts`}><ArrowLeft className="h-4 w-4" /> All attempts</Link></Button>
            <PageHeader title={`${r.quizTitle} · attempt #${r.attemptNo}`} description={`Score ${r.score} / ${r.maxScore} (${r.percent}%)${r.expired ? ' · auto-submitted when time ran out' : ''}`}>
              <Badge variant={statusVariant(r.status)} className="mt-2">{r.status.replace('_', ' ')}</Badge>
            </PageHeader>
            {shorts.length > 0 && (
              <section aria-labelledby="short-heading" className="mb-8">
                <h2 id="short-heading" className="mb-3 text-lg font-semibold">Short answers</h2>
                <div className="space-y-3">
                  {shorts.map((s) => (
                    <Card key={s.answerId} className="p-4">
                      <p className="whitespace-pre-wrap break-words font-medium">{s.text}</p>
                      <p className="mt-3 rounded-lg bg-muted/60 p-3 text-sm whitespace-pre-wrap break-words">{s.textAnswer || <span className="text-muted-foreground">No answer given</span>}</p>
                      <div className="mt-3 grid gap-3 sm:grid-cols-[8rem_1fr]">
                        <label className="text-sm font-medium">Marks (max {s.marks})
                          <Input type="number" min="0" max={s.marks} step="0.5" className="mt-1" defaultValue={s.marksAwarded ?? ''} onChange={(e) => set(s.answerId, { marks: e.target.value })} />
                        </label>
                        <label className="text-sm font-medium">Feedback
                          <Textarea rows={2} className="mt-1" defaultValue={s.feedback ?? ''} onChange={(e) => set(s.answerId, { feedback: e.target.value })} />
                        </label>
                      </div>
                    </Card>
                  ))}
                </div>
                <Button className="mt-4" loading={actions.grade.isPending} onClick={save}>Save grades</Button>
              </section>
            )}
            <h2 className="mb-3 text-lg font-semibold">Auto-graded questions</h2>
            <ReviewList items={objective} />
          </div>
        );
      }}
    </QueryBoundary>
  );
}
