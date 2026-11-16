import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { ArrowLeft, BarChart3, Pencil, Plus, Settings, Trash2 } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Badge } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { parseError } from '@/lib/api';
import { QuestionDialog } from './QuestionDialog';
import { QuizFormDialog } from './QuizFormDialog';
import { useQuizActions, useTeacherQuiz } from './useQuizzes';

const TYPE_LABEL = { SINGLE_CHOICE: 'Single choice', MULTIPLE_SELECT: 'Multiple select', TRUE_FALSE: 'True / False', SHORT_ANSWER: 'Short answer' };

export default function QuizBuilderPage() {
  const { quizId } = useParams();
  const navigate = useNavigate();
  const q = useTeacherQuiz(quizId);
  const subjectId = q.data?.quiz.subjectId;
  const actions = useQuizActions(subjectId);
  const [settings, setSettings] = useState(false);
  const [qDialog, setQDialog] = useState({ open: false, question: null });
  const [confirm, setConfirm] = useState(null);
  const fail = (e) => toast.error(parseError(e).message);

  return (
    <QueryBoundary query={q} skeleton={<ListSkeleton />}>
      {({ quiz, questions }) => {
        const locked = quiz.attemptCount > 0;
        return (
          <div>
            <Button asChild variant="ghost" size="sm" className="mb-3 -ml-2"><Link to={`/teacher/subjects/${quiz.subjectId}?tab=quizzes`}><ArrowLeft className="h-4 w-4" /> Back to subject</Link></Button>
            <PageHeader title={quiz.title} description={`${quiz.questionCount} questions · ${quiz.totalMarks} marks · ${quiz.durationMinutes} min · pass at ${quiz.passingPercentage}%`}
              actions={<>
                <Button variant="outline" onClick={() => setSettings(true)}><Settings className="h-4 w-4" /> Settings</Button>
                <Button asChild variant="outline"><Link to={`/teacher/quizzes/${quiz.id}/attempts`}><BarChart3 className="h-4 w-4" /> Results ({quiz.attemptCount})</Link></Button>
                <Button variant={quiz.published ? 'secondary' : 'accent'} loading={actions.publish.isPending}
                  onClick={() => actions.publish.mutate({ id: quiz.id, published: !quiz.published }, { onSuccess: () => toast.success(quiz.published ? 'Quiz unpublished' : 'Quiz published. Enrolled students were notified.'), onError: fail })}>
                  {quiz.published ? 'Unpublish' : 'Publish quiz'}
                </Button>
              </>}>
              <Badge variant={quiz.published ? 'success' : 'warning'} className="mt-2">{quiz.published ? 'Published' : 'Draft'}</Badge>
            </PageHeader>
            {locked && <div role="status" className="mb-4 rounded-xl bg-amber-500/10 px-4 py-3 text-sm text-amber-800 dark:text-amber-200">Students have started this quiz, so questions are locked. You can still change settings.</div>}
            <div className="mb-3 flex items-center justify-between"><h2 className="text-lg font-semibold">Questions</h2><Button disabled={locked} onClick={() => setQDialog({ open: true, question: null })}><Plus className="h-4 w-4" /> Add question</Button></div>
            {questions.length === 0 ? (
              <EmptyState title="No questions yet" description="Add at least one question before you publish." action={<Button onClick={() => setQDialog({ open: true, question: null })}><Plus className="h-4 w-4" /> Add question</Button>} />
            ) : (
              <ol className="space-y-3">
                {questions.map((qq, i) => (
                  <Card key={qq.id} className="p-4">
                    <div className="flex items-start gap-3">
                      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-sm font-bold text-primary">{i + 1}</span>
                      <div className="min-w-0 flex-1">
                        <div className="mb-1 flex flex-wrap items-center gap-2"><Badge variant="secondary">{TYPE_LABEL[qq.type]}</Badge><span className="text-xs text-muted-foreground">{qq.marks} mark{qq.marks === 1 ? '' : 's'}</span></div>
                        <p className="whitespace-pre-wrap break-words font-medium">{qq.text}</p>
                        {qq.options.length > 0 && (
                          <ul className="mt-2 space-y-1">{qq.options.map((o) => <li key={o.id} className={`text-sm ${o.correct ? 'font-semibold text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'}`}>{o.correct ? '✓' : '○'} {o.text}</li>)}</ul>
                        )}
                        {qq.explanation && <p className="mt-2 text-xs text-muted-foreground">Explanation: {qq.explanation}</p>}
                      </div>
                      <div className="flex shrink-0">
                        <Button size="icon" variant="ghost" disabled={locked} aria-label={`Edit question ${i + 1}`} onClick={() => setQDialog({ open: true, question: qq })}><Pencil className="h-4 w-4" /></Button>
                        <Button size="icon" variant="ghost" disabled={locked} aria-label={`Delete question ${i + 1}`} onClick={() => setConfirm(qq)}><Trash2 className="h-4 w-4 text-destructive" /></Button>
                      </div>
                    </div>
                  </Card>
                ))}
              </ol>
            )}
            <QuizFormDialog open={settings} onOpenChange={setSettings} quiz={quiz} onSave={(b) => actions.update.mutateAsync({ id: quiz.id, ...b })} />
            <QuestionDialog open={qDialog.open} onOpenChange={(o) => setQDialog((s) => ({ ...s, open: o }))} question={qDialog.question}
              onSave={(b) => (qDialog.question ? actions.updateQuestion.mutateAsync({ id: qDialog.question.id, ...b }) : actions.addQuestion.mutateAsync({ quizId: quiz.id, ...b }))} />
            <ConfirmDialog open={!!confirm} onOpenChange={(o) => !o && setConfirm(null)} destructive confirmLabel="Delete question" title="Delete this question?" description="This cannot be undone." loading={actions.deleteQuestion.isPending}
              onConfirm={() => actions.deleteQuestion.mutate(confirm.id, { onSuccess: () => { toast.success('Question deleted'); setConfirm(null); }, onError: fail })} />
            <div className="mt-8 border-t pt-4">
              <Button variant="ghost" className="text-destructive hover:bg-destructive/10" onClick={() => setConfirm({ quiz: true })}><Trash2 className="h-4 w-4" /> Delete quiz</Button>
            </div>
            <ConfirmDialog open={!!confirm?.quiz} onOpenChange={(o) => !o && setConfirm(null)} destructive confirmLabel="Delete quiz" title="Delete this quiz?" description="All questions and student attempts will be removed. This cannot be undone." loading={actions.remove.isPending}
              onConfirm={() => actions.remove.mutate(quiz.id, { onSuccess: () => { toast.success('Quiz deleted'); navigate(`/teacher/subjects/${quiz.subjectId}?tab=quizzes`); }, onError: fail })} />
          </div>
        );
      }}
    </QueryBoundary>
  );
}
