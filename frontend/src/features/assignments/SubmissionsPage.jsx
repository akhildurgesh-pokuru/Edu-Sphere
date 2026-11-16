import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { ArrowLeft, Download, Users } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Badge, statusVariant } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Field } from '@/components/ui/label';
import { Input, Textarea } from '@/components/ui/input';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { parseError, resourceUrl } from '@/lib/api';
import { formatDateTime } from '@/lib/utils';
import { useAssignmentActions, useSubmissionRows, useTeacherAssignment } from './useAssignments';

function GradeDialog({ row, maxMarks, onClose, actions }) {
  const [marks, setMarks] = useState(row?.submission?.marks ?? '');
  const [feedback, setFeedback] = useState(row?.submission?.feedback ?? '');
  const [error, setError] = useState('');
  const save = (e) => {
    e.preventDefault();
    const m = Number(marks);
    if (marks === '' || Number.isNaN(m) || m < 0 || m > maxMarks) { setError(`Enter marks between 0 and ${maxMarks}`); return; }
    actions.grade.mutate({ id: row.submission.id, marks: m, feedback }, { onSuccess: () => { toast.success('Marks saved and student notified'); onClose(); }, onError: (err) => setError(parseError(err).message) });
  };
  return (
    <Dialog open={!!row} onOpenChange={(o) => !o && onClose()}>
      <DialogContent title={`Grade ${row?.studentName ?? ''}`} description={row?.submission?.file?.name}>
        <form onSubmit={save} className="space-y-4" noValidate>
          <Field id="g-marks" label={`Marks (out of ${maxMarks})`} error={error} required>{(p) => <Input {...p} type="number" min="0" max={maxMarks} step="0.5" value={marks} onChange={(e) => setMarks(e.target.value)} />}</Field>
          <Field id="g-fb" label="Feedback">{(p) => <Textarea {...p} rows={4} value={feedback} onChange={(e) => setFeedback(e.target.value)} />}</Field>
          <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={onClose}>Cancel</Button><Button type="submit" loading={actions.grade.isPending}>Save marks</Button></div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function SubmissionsPage() {
  const { assignmentId } = useParams();
  const a = useTeacherAssignment(assignmentId);
  const rows = useSubmissionRows(assignmentId);
  const actions = useAssignmentActions(a.data?.subjectId);
  const [grading, setGrading] = useState(null);
  return (
    <div>
      <Button asChild variant="ghost" size="sm" className="mb-3 -ml-2"><Link to={a.data ? `/teacher/subjects/${a.data.subjectId}?tab=assignments` : '/teacher/subjects'}><ArrowLeft className="h-4 w-4" /> Back to subject</Link></Button>
      <PageHeader title={a.data?.title || 'Submissions'} description={a.data ? `Due ${formatDateTime(a.data.dueAt)} · ${a.data.maxMarks} marks` : undefined} />
      <QueryBoundary query={rows} skeleton={<ListSkeleton />} isEmpty={(d) => d.length === 0} empty={<EmptyState icon={Users} title="No students are enrolled yet" />}>
        {(list) => (
          <Card className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-sm">
              <caption className="sr-only">Assignment submissions</caption>
              <thead><tr className="border-b text-left text-xs uppercase tracking-wide text-muted-foreground"><th scope="col" className="p-4">Student</th><th scope="col" className="p-4">Status</th><th scope="col" className="p-4">Submitted</th><th scope="col" className="p-4">Marks</th><th scope="col" className="p-4"><span className="sr-only">Actions</span></th></tr></thead>
              <tbody>
                {list.map((r) => (
                  <tr key={r.studentId} className="border-b last:border-0">
                    <td className="p-4"><p className="font-medium">{r.studentName}</p><p className="text-xs text-muted-foreground">{r.studentEmail}</p></td>
                    <td className="p-4"><Badge variant={statusVariant(r.status)}>{r.status.replace('_', ' ')}</Badge></td>
                    <td className="p-4 text-muted-foreground">{r.submission ? formatDateTime(r.submission.submittedAt) : '—'}</td>
                    <td className="p-4 font-semibold">{r.submission?.marks != null ? `${r.submission.marks} / ${a.data?.maxMarks}` : '—'}</td>
                    <td className="p-4"><div className="flex justify-end gap-2">
                      {r.submission?.file && <Button asChild size="sm" variant="outline"><a href={resourceUrl(r.submission.file.id, true)}><Download className="h-4 w-4" /> File</a></Button>}
                      {r.submission && <Button size="sm" onClick={() => setGrading(r)}>{r.submission.marks != null ? 'Edit marks' : 'Grade'}</Button>}
                    </div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        )}
      </QueryBoundary>
      {grading && <GradeDialog key={grading.studentId} row={grading} maxMarks={a.data?.maxMarks} onClose={() => setGrading(null)} actions={actions} />}
    </div>
  );
}
