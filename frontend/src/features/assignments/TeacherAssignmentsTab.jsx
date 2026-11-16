import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { ClipboardCheck, Paperclip, Pencil, Plus, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Field } from '@/components/ui/label';
import { Input, Textarea } from '@/components/ui/input';
import { Badge } from '@/components/ui/misc';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { applyFieldErrors, parseError, resourceUrl } from '@/lib/api';
import { assignmentSchema } from '@/lib/schemas';
import { formatDateTime } from '@/lib/utils';
import { useAssignmentActions, useTeacherAssignments } from './useAssignments';

const toLocal = (iso) => { if (!iso) return ''; const d = new Date(iso); const p = (n) => String(n).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`; };
const ALL_TYPES = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'txt'];

function AssignmentDialog({ open, onOpenChange, assignment, actions }) {
  const editing = !!assignment;
  const [types, setTypes] = useState(ALL_TYPES);
  const [file, setFile] = useState(null);
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(assignmentSchema), defaultValues: { title: '', description: '', instructions: '', dueAt: '', maxMarks: 100 } });
  useEffect(() => {
    if (!open) return;
    setFormError(''); setFile(null);
    setTypes(assignment?.allowedTypes || ALL_TYPES);
    reset(assignment ? { title: assignment.title, description: assignment.description || '', instructions: assignment.instructions || '', dueAt: toLocal(assignment.dueAt), maxMarks: assignment.maxMarks } : { title: '', description: '', instructions: '', dueAt: '', maxMarks: 100 });
  }, [open, assignment, reset]);

  const onSubmit = async (v) => {
    setFormError('');
    if (types.length === 0) { setFormError('Choose at least one allowed file type'); return; }
    const body = { ...v, dueAt: new Date(v.dueAt).toISOString(), allowedTypes: types };
    try {
      const saved = editing ? await actions.update.mutateAsync({ id: assignment.id, ...body }) : await actions.create.mutateAsync(body);
      if (file) await actions.attach.mutateAsync({ id: saved.id, file });
      toast.success(editing ? 'Assignment updated' : 'Assignment created');
      onOpenChange(false);
    } catch (err) {
      if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message);
    }
  };
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={editing ? 'Edit assignment' : 'New assignment'} className="max-w-xl">
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
          <Field id="as-title" label="Title" error={errors.title?.message} required>{(p) => <Input {...p} {...register('title')} />}</Field>
          <Field id="as-desc" label="Description" error={errors.description?.message}>{(p) => <Textarea {...p} rows={2} {...register('description')} />}</Field>
          <Field id="as-instr" label="Instructions" error={errors.instructions?.message}>{(p) => <Textarea {...p} rows={3} {...register('instructions')} />}</Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field id="as-due" label="Due date" error={errors.dueAt?.message} required>{(p) => <Input {...p} type="datetime-local" {...register('dueAt')} />}</Field>
            <Field id="as-marks" label="Max marks" error={errors.maxMarks?.message} required>{(p) => <Input {...p} type="number" min="1" {...register('maxMarks')} />}</Field>
          </div>
          <fieldset><legend className="mb-1.5 text-sm font-medium">Allowed file types</legend>
            <div className="flex flex-wrap gap-2">{ALL_TYPES.map((t) => (
              <label key={t} className={`cursor-pointer rounded-full border px-3 py-1 text-xs font-semibold uppercase ${types.includes(t) ? 'border-primary bg-primary/10 text-primary' : 'text-muted-foreground'}`}>
                <input type="checkbox" className="sr-only" checked={types.includes(t)} onChange={() => setTypes((s) => (s.includes(t) ? s.filter((x) => x !== t) : [...s, t]))} />{t}</label>
            ))}</div>
          </fieldset>
          <Field id="as-file" label="Attachment (optional)" hint={assignment?.attachment ? `Current: ${assignment.attachment.name}. Choosing a file replaces it.` : 'PDF, slides or documents'}>
            {(p) => <Input {...p} type="file" accept=".pdf,.doc,.docx,.ppt,.pptx,.txt" onChange={(e) => setFile(e.target.files?.[0] || null)} />}
          </Field>
          <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button><Button type="submit" loading={isSubmitting}>{editing ? 'Save' : 'Create assignment'}</Button></div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function TeacherAssignmentsTab({ subjectId }) {
  const q = useTeacherAssignments(subjectId);
  const actions = useAssignmentActions(subjectId);
  const [dialog, setDialog] = useState({ open: false, assignment: null });
  const [confirm, setConfirm] = useState(null);
  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div><h2 className="text-lg font-semibold">Assignments</h2><p className="text-sm text-muted-foreground">Students upload one file before the due date.</p></div>
        <Button onClick={() => setDialog({ open: true, assignment: null })}><Plus className="h-4 w-4" /> New assignment</Button>
      </div>
      <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.length === 0}
        empty={<EmptyState icon={ClipboardCheck} title="No assignments yet" description="Create an assignment with a due date and enrolled students will be notified." action={<Button onClick={() => setDialog({ open: true, assignment: null })}><Plus className="h-4 w-4" /> Create assignment</Button>} />}>
        {(list) => (
          <div className="space-y-3">
            {list.map((a) => (
              <Card key={a.id} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2"><h3 className="font-semibold">{a.title}</h3>{a.pastDue ? <Badge variant="muted">Closed</Badge> : <Badge variant="success">Open</Badge>}{a.pendingGrading > 0 && <Badge variant="warning">{a.pendingGrading} to grade</Badge>}</div>
                  <p className="mt-1 text-xs text-muted-foreground">Due {formatDateTime(a.dueAt)} · {a.maxMarks} marks · {a.submissionCount} submitted · {a.allowedTypes.join(', ').toUpperCase()}</p>
                  {a.attachment && <a href={resourceUrl(a.attachment.id, true)} className="mt-1 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"><Paperclip className="h-3 w-3" />{a.attachment.name}</a>}
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button asChild size="sm"><Link to={`/teacher/assignments/${a.id}/submissions`}>Submissions</Link></Button>
                  <Button size="icon" variant="ghost" aria-label={`Edit ${a.title}`} onClick={() => setDialog({ open: true, assignment: a })}><Pencil className="h-4 w-4" /></Button>
                  <Button size="icon" variant="ghost" aria-label={`Delete ${a.title}`} onClick={() => setConfirm(a)}><Trash2 className="h-4 w-4 text-destructive" /></Button>
                </div>
              </Card>
            ))}
          </div>
        )}
      </QueryBoundary>
      <AssignmentDialog open={dialog.open} onOpenChange={(o) => setDialog((s) => ({ ...s, open: o }))} assignment={dialog.assignment} actions={actions} />
      <ConfirmDialog open={!!confirm} onOpenChange={(o) => !o && setConfirm(null)} destructive confirmLabel="Delete" title="Delete this assignment?" description="Submitted files and marks will be removed. This cannot be undone." loading={actions.remove.isPending}
        onConfirm={() => actions.remove.mutate(confirm.id, { onSuccess: () => { toast.success('Assignment deleted'); setConfirm(null); }, onError: (e) => toast.error(parseError(e).message) })} />
    </div>
  );
}
