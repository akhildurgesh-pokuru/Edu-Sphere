import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { ArrowLeft, Download, Users } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Badge, statusVariant } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { parseError } from '@/lib/api';
import { formatDateTime } from '@/lib/utils';
import { downloadQuizCsv, useQuizAttempts, useTeacherQuiz } from './useQuizzes';

export default function QuizAttemptsPage() {
  const { quizId } = useParams();
  const quiz = useTeacherQuiz(quizId);
  const attempts = useQuizAttempts(quizId);
  const [busy, setBusy] = useState(false);
  const exportCsv = async () => {
    setBusy(true);
    try { await downloadQuizCsv(quizId); } catch (e) { toast.error(parseError(e).message); } finally { setBusy(false); }
  };
  return (
    <div>
      <Button asChild variant="ghost" size="sm" className="mb-3 -ml-2"><Link to={`/teacher/quizzes/${quizId}`}><ArrowLeft className="h-4 w-4" /> Back to quiz</Link></Button>
      <PageHeader title="Quiz results" description={quiz.data?.quiz.title} actions={<Button variant="outline" loading={busy} onClick={exportCsv}><Download className="h-4 w-4" /> Export CSV</Button>} />
      <QueryBoundary query={attempts} skeleton={<ListSkeleton />} isEmpty={(d) => d.length === 0}
        empty={<EmptyState icon={Users} title="No attempts yet" description="Results appear here as soon as students submit." />}>
        {(rows) => (
          <Card className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-sm">
              <caption className="sr-only">Quiz attempts</caption>
              <thead><tr className="border-b text-left text-xs uppercase tracking-wide text-muted-foreground"><th scope="col" className="p-4">Student</th><th scope="col" className="p-4">Attempt</th><th scope="col" className="p-4">Score</th><th scope="col" className="p-4">Status</th><th scope="col" className="p-4">Submitted</th><th scope="col" className="p-4"><span className="sr-only">Actions</span></th></tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id} className="border-b last:border-0">
                    <td className="p-4"><p className="font-medium">{r.studentName}</p><p className="text-xs text-muted-foreground">{r.studentEmail}</p></td>
                    <td className="p-4">#{r.attemptNo}</td>
                    <td className="p-4 font-semibold">{r.status === 'IN_PROGRESS' ? '—' : `${r.score} / ${r.maxScore} (${r.percent}%)`}</td>
                    <td className="p-4"><Badge variant={statusVariant(r.status)}>{r.status.replace('_', ' ')}</Badge></td>
                    <td className="p-4 text-muted-foreground">{formatDateTime(r.submittedAt)}</td>
                    <td className="p-4 text-right"><Button asChild size="sm" variant={r.needsGrading ? 'default' : 'outline'}><Link to={`/teacher/attempts/${r.id}`}>{r.needsGrading ? 'Grade' : 'Review'}</Link></Button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        )}
      </QueryBoundary>
    </div>
  );
}
