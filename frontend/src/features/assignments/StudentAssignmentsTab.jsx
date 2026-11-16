import { useRef, useState } from 'react';
import { toast } from 'sonner';
import { ClipboardCheck, Download, Paperclip, Upload } from 'lucide-react';
import { Badge, Progress, statusVariant } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { parseError, resourceUrl } from '@/lib/api';
import { formatBytes, formatDateTime } from '@/lib/utils';
import { useStudentAssignments, useSubmit } from './useAssignments';

function AssignmentCard({ a }) {
  const input = useRef(null);
  const submit = useSubmit();
  const [progress, setProgress] = useState(0);
  const mine = a.mySubmission;
  const status = mine ? (mine.marks != null ? 'GRADED' : 'SUBMITTED') : a.pastDue ? 'MISSED' : 'PENDING';
  const pick = (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setProgress(0);
    submit.mutate({ id: a.id, file, onProgress: setProgress }, { onSuccess: () => toast.success('Submitted. Your teacher has been notified.'), onError: (err) => toast.error(parseError(err).message) });
  };
  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-center gap-2"><h3 className="font-semibold">{a.title}</h3><Badge variant={statusVariant(status)}>{status === 'PENDING' ? 'To do' : status === 'MISSED' ? 'Missed' : status === 'GRADED' ? 'Graded' : 'Submitted'}</Badge></div>
      <p className="mt-1 text-xs text-muted-foreground">Due {formatDateTime(a.dueAt)} · {a.maxMarks} marks · accepts {a.allowedTypes.join(', ').toUpperCase()}</p>
      {a.description && <p className="prose-user mt-3">{a.description}</p>}
      {a.instructions && <p className="prose-user mt-2 rounded-lg bg-muted/60 p-3"><strong>Instructions: </strong>{a.instructions}</p>}
      {a.attachment && <a href={resourceUrl(a.attachment.id, true)} className="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline"><Paperclip className="h-4 w-4" />{a.attachment.name} ({formatBytes(a.attachment.sizeBytes)})</a>}
      {mine && (
        <div className="mt-4 rounded-xl border bg-muted/30 p-3 text-sm">
          <p className="font-medium">Your submission</p>
          <p className="text-muted-foreground">{mine.file?.name} · {formatDateTime(mine.submittedAt)}</p>
          {mine.file && <a href={resourceUrl(mine.file.id, true)} className="mt-1 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"><Download className="h-3 w-3" />Download</a>}
          {mine.marks != null && <p className="mt-2 font-semibold text-emerald-700 dark:text-emerald-300">Marks: {mine.marks} / {a.maxMarks}</p>}
          {mine.feedback && <p className="prose-user mt-1">Feedback: {mine.feedback}</p>}
        </div>
      )}
      {!a.pastDue && (
        <div className="mt-4">
          <input ref={input} type="file" className="sr-only" accept={a.allowedTypes.map((t) => `.${t}`).join(',')} onChange={pick} aria-label={`Choose file for ${a.title}`} />
          {submit.isPending ? (
            <div aria-live="polite"><p className="mb-1 text-sm">Uploading… {progress}%</p><Progress value={progress} label="Upload progress" /></div>
          ) : (
            <Button variant={mine ? 'outline' : 'default'} onClick={() => input.current?.click()}><Upload className="h-4 w-4" />{mine ? 'Replace submission' : 'Upload your work'}</Button>
          )}
          {mine?.marks != null && <p className="mt-2 text-xs text-muted-foreground">Re-submitting clears your current marks.</p>}
        </div>
      )}
      {a.pastDue && !mine && <p className="mt-4 text-sm font-medium text-destructive">The deadline has passed, so submissions are closed.</p>}
    </Card>
  );
}

export function StudentAssignmentsTab({ subjectId }) {
  const q = useStudentAssignments(subjectId);
  return (
    <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.length === 0} empty={<EmptyState icon={ClipboardCheck} title="No assignments yet" description="Assignments from your teacher will appear here." />}>
      {(list) => <div className="space-y-4">{list.map((a) => <AssignmentCard key={a.id} a={a} />)}</div>}
    </QueryBoundary>
  );
}
