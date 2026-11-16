import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { ClipboardList, Clock } from 'lucide-react';
import { Badge, statusVariant } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { parseError } from '@/lib/api';
import { formatDateTime } from '@/lib/utils';
import { useAttemptActions, useStudentQuizzes } from './useQuizzes';

const LABEL = { OPEN: 'Open', UPCOMING: 'Not open yet', CLOSED: 'Closed', NO_ATTEMPTS: 'No attempts left' };

export function StudentQuizzesTab({ subjectId }) {
  const q = useStudentQuizzes(subjectId);
  const { start } = useAttemptActions();
  const navigate = useNavigate();
  const go = (quizId) => start.mutate(quizId, {
    onSuccess: (a) => navigate(`/student/attempts/${a.attemptId}`),
    onError: (e) => toast.error(parseError(e).message),
  });
  return (
    <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.length === 0}
      empty={<EmptyState icon={ClipboardList} title="No quizzes yet" description="Your teacher has not published a quiz for this course." />}>
      {(list) => (
        <div className="space-y-3">
          {list.map((z) => (
            <Card key={z.id} className="p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2"><h3 className="font-semibold">{z.title}</h3><Badge variant={statusVariant(z.availability)}>{LABEL[z.availability]}</Badge></div>
                  {z.description && <p className="mt-1 text-sm text-muted-foreground">{z.description}</p>}
                  <p className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1"><Clock className="h-3.5 w-3.5" />{z.durationMinutes} min</span>
                    <span>{z.questionCount} questions · {z.totalMarks} marks</span><span>Pass at {z.passingPercentage}%</span>
                    <span>Attempts: {z.attemptsUsed}/{z.maxAttempts}</span>
                    {z.startAt && <span>Opens {formatDateTime(z.startAt)}</span>}{z.endAt && <span>Closes {formatDateTime(z.endAt)}</span>}
                    {z.bestPercent != null && <span className="font-semibold text-foreground">Best: {z.bestPercent}%</span>}
                  </p>
                </div>
                {z.availability === 'OPEN' && (
                  <Button loading={start.isPending && start.variables === z.id} onClick={() => go(z.id)}>{z.inProgressAttemptId ? 'Resume attempt' : z.attemptsUsed ? 'Try again' : 'Start quiz'}</Button>
                )}
              </div>
              {z.attempts.filter((a) => a.status !== 'IN_PROGRESS').length > 0 && (
                <ul className="mt-3 flex flex-wrap gap-2 border-t pt-3">
                  {z.attempts.filter((a) => a.status !== 'IN_PROGRESS').map((a) => (
                    <li key={a.id}><Link to={`/student/attempts/${a.id}`} className="inline-flex items-center gap-2 rounded-lg border px-3 py-1.5 text-xs hover:bg-muted">
                      Attempt #{a.attemptNo} · {a.status === 'PENDING_GRADING' ? 'awaiting grading' : `${a.percent}%`}</Link></li>
                  ))}
                </ul>
              )}
            </Card>
          ))}
        </div>
      )}
    </QueryBoundary>
  );
}
