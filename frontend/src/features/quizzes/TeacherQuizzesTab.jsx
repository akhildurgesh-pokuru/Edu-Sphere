import { useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { ClipboardList, Plus } from 'lucide-react';
import { Badge } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { parseError } from '@/lib/api';
import { formatDateTime } from '@/lib/utils';
import { QuizFormDialog } from './QuizFormDialog';
import { useQuizActions, useTeacherQuizzes } from './useQuizzes';

export function TeacherQuizzesTab({ subjectId }) {
  const q = useTeacherQuizzes(subjectId);
  const actions = useQuizActions(subjectId);
  const [open, setOpen] = useState(false);
  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div><h2 className="text-lg font-semibold">Quizzes</h2><p className="text-sm text-muted-foreground">Timed quizzes, auto-graded with manual grading for short answers.</p></div>
        <Button onClick={() => setOpen(true)}><Plus className="h-4 w-4" /> New quiz</Button>
      </div>
      <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.length === 0}
        empty={<EmptyState icon={ClipboardList} title="No quizzes yet" description="Create a quiz, add questions and publish it for enrolled students." action={<Button onClick={() => setOpen(true)}><Plus className="h-4 w-4" /> Create a quiz</Button>} />}>
        {(list) => (
          <div className="space-y-3">
            {list.map((z) => (
              <Card key={z.id} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2"><Link to={`/teacher/quizzes/${z.id}`} className="font-semibold hover:text-primary">{z.title}</Link><Badge variant={z.published ? 'success' : 'warning'}>{z.published ? 'Published' : 'Draft'}</Badge></div>
                  <p className="mt-1 text-xs text-muted-foreground">{z.questionCount} questions · {z.totalMarks} marks · {z.durationMinutes} min · {z.maxAttempts} attempt{z.maxAttempts > 1 ? 's' : ''} · {z.attemptCount} taken{z.endAt ? ` · closes ${formatDateTime(z.endAt)}` : ''}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button asChild size="sm" variant="outline"><Link to={`/teacher/quizzes/${z.id}`}>Edit</Link></Button>
                  <Button asChild size="sm" variant="outline"><Link to={`/teacher/quizzes/${z.id}/attempts`}>Results</Link></Button>
                  <Button size="sm" variant={z.published ? 'secondary' : 'default'} loading={actions.publish.isPending && actions.publish.variables?.id === z.id}
                    onClick={() => actions.publish.mutate({ id: z.id, published: !z.published }, { onSuccess: () => toast.success(z.published ? 'Quiz unpublished' : 'Quiz published'), onError: (e) => toast.error(parseError(e).message) })}>
                    {z.published ? 'Unpublish' : 'Publish'}
                  </Button>
                </div>
              </Card>
            ))}
          </div>
        )}
      </QueryBoundary>
      <QuizFormDialog open={open} onOpenChange={setOpen} quiz={null} onSave={(b) => actions.create.mutateAsync(b)} />
    </div>
  );
}
