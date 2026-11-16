import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Megaphone, Pencil, Pin, Plus, Trash2 } from 'lucide-react';
import { Badge } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Field } from '@/components/ui/label';
import { Checkbox, Input, Textarea } from '@/components/ui/input';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { applyFieldErrors, parseError } from '@/lib/api';
import { announcementSchema } from '@/lib/schemas';
import { formatDateTime } from '@/lib/utils';
import { useAnnouncements, useCommunityActions } from './useCommunity';

function AnnouncementDialog({ open, onOpenChange, item, actions }) {
  const editing = !!item;
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(announcementSchema), defaultValues: { title: '', body: '', pinned: false } });
  useEffect(() => { if (open) { setFormError(''); reset(item ? { title: item.title, body: item.body, pinned: item.pinned } : { title: '', body: '', pinned: false }); } }, [open, item, reset]);
  const onSubmit = async (v) => {
    try {
      if (editing) await actions.updateAnnouncement.mutateAsync({ id: item.id, ...v }); else await actions.createAnnouncement.mutateAsync(v);
      toast.success(editing ? 'Announcement updated' : 'Announcement posted. Students were notified.'); onOpenChange(false);
    } catch (err) { if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message); }
  };
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={editing ? 'Edit announcement' : 'New announcement'}>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
          <Field id="an-title" label="Title" error={errors.title?.message} required>{(p) => <Input {...p} {...register('title')} />}</Field>
          <Field id="an-body" label="Message" error={errors.body?.message} required>{(p) => <Textarea {...p} rows={5} {...register('body')} />}</Field>
          <label className="flex items-center gap-2 text-sm font-medium"><Checkbox {...register('pinned')} /> Pin to the top</label>
          <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button><Button type="submit" loading={isSubmitting}>{editing ? 'Save' : 'Post'}</Button></div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function AnnouncementsTab({ subjectId, isTeacher }) {
  const q = useAnnouncements(subjectId);
  const actions = useCommunityActions(subjectId);
  const [dialog, setDialog] = useState({ open: false, item: null });
  const [confirm, setConfirm] = useState(null);
  return (
    <div>
      {isTeacher && <div className="mb-4 flex justify-end"><Button onClick={() => setDialog({ open: true, item: null })}><Plus className="h-4 w-4" /> New announcement</Button></div>}
      <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.length === 0}
        empty={<EmptyState icon={Megaphone} title="No announcements yet" description={isTeacher ? 'Post an update and every enrolled student will be notified.' : 'Updates from your teacher will appear here.'} />}>
        {(list) => (
          <div className="space-y-3">
            {list.map((a) => (
              <Card key={a.id} className="p-5">
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h3 className="break-words font-semibold">{a.title}</h3>{a.pinned && <Badge variant="warning"><Pin className="h-3 w-3" />Pinned</Badge>}</div>
                    <p className="text-xs text-muted-foreground">{a.authorName} · {formatDateTime(a.createdAt)}</p></div>
                  {isTeacher && <div className="flex shrink-0"><Button size="icon" variant="ghost" aria-label={`Edit ${a.title}`} onClick={() => setDialog({ open: true, item: a })}><Pencil className="h-4 w-4" /></Button><Button size="icon" variant="ghost" aria-label={`Delete ${a.title}`} onClick={() => setConfirm(a)}><Trash2 className="h-4 w-4 text-destructive" /></Button></div>}
                </div>
                <p className="prose-user mt-3">{a.body}</p>
              </Card>
            ))}
          </div>
        )}
      </QueryBoundary>
      <AnnouncementDialog open={dialog.open} onOpenChange={(o) => setDialog((s) => ({ ...s, open: o }))} item={dialog.item} actions={actions} />
      <ConfirmDialog open={!!confirm} onOpenChange={(o) => !o && setConfirm(null)} destructive confirmLabel="Delete" title="Delete this announcement?" description="It will disappear for students." loading={actions.deleteAnnouncement.isPending}
        onConfirm={() => actions.deleteAnnouncement.mutate(confirm.id, { onSuccess: () => { toast.success('Deleted'); setConfirm(null); }, onError: (e) => toast.error(parseError(e).message) })} />
    </div>
  );
}
