import { useState } from 'react';
import * as Accordion from '@radix-ui/react-accordion';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { ArrowDown, ArrowUp, ChevronDown, Layers, Pencil, Plus, Trash2 } from 'lucide-react';
import { Badge } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Field } from '@/components/ui/label';
import { Input, Textarea } from '@/components/ui/input';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { applyFieldErrors, parseError } from '@/lib/api';
import { moduleSchema } from '@/lib/schemas';
import { formatDuration, pluralize } from '@/lib/utils';
import { LESSON_ICONS, LESSON_LABELS } from './lessonIcons';
import { LessonDialog } from './LessonDialog';
import { useLesson, useLessonActions, useModuleActions, useTeacherCurriculum } from './useContent';

function ModuleDialog({ open, onOpenChange, module, actions }) {
  const editing = !!module;
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(moduleSchema), defaultValues: { title: '', description: '' } });
  const [lastKey, setLastKey] = useState(null);
  const key = open ? (module?.id ?? 'new') : null;
  if (key !== lastKey) { setLastKey(key); if (open) reset({ title: module?.title || '', description: module?.description || '' }); }

  const onSubmit = async (v) => {
    setFormError('');
    try {
      if (editing) await actions.update.mutateAsync({ id: module.id, ...v }); else await actions.create.mutateAsync(v);
      toast.success(editing ? 'Module updated' : 'Module added');
      onOpenChange(false);
    } catch (err) {
      if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message);
    }
  };
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={editing ? 'Edit module' : 'Add a module'} description="Modules group related lessons.">
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
          <Field id="module-title" label="Title" error={errors.title?.message} required>{(p) => <Input {...p} placeholder="e.g. Getting started" {...register('title')} />}</Field>
          <Field id="module-desc" label="Description" error={errors.description?.message}>{(p) => <Textarea {...p} rows={3} {...register('description')} />}</Field>
          <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button><Button type="submit" loading={isSubmitting}>{editing ? 'Save' : 'Add module'}</Button></div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function LessonEditLoader({ lessonId, children }) {
  const q = useLesson(lessonId);
  return children(q.data);
}

export function CurriculumBuilder({ subjectId }) {
  const q = useTeacherCurriculum(subjectId);
  const modActions = useModuleActions(subjectId);
  const lessonActions = useLessonActions();
  const [moduleDialog, setModuleDialog] = useState({ open: false, module: null });
  const [lessonDialog, setLessonDialog] = useState({ open: false, moduleId: null, lessonId: null });
  const [confirm, setConfirm] = useState(null);
  const [openItems, setOpenItems] = useState([]);

  const move = (list, index, delta, run) => {
    const ids = list.map((x) => x.id);
    const j = index + delta;
    if (j < 0 || j >= ids.length) return;
    [ids[index], ids[j]] = [ids[j], ids[index]];
    run(ids);
  };
  const fail = (e) => toast.error(parseError(e).message);

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div><h2 className="text-lg font-semibold">Curriculum</h2><p className="text-sm text-muted-foreground">Modules and lessons. Use the arrows to reorder.</p></div>
        <Button onClick={() => setModuleDialog({ open: true, module: null })}><Plus className="h-4 w-4" /> Add module</Button>
      </div>
      <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.modules.length === 0}
        empty={<EmptyState icon={Layers} title="No modules yet" description="Start with a module such as “Getting started”, then add lessons to it." action={<Button onClick={() => setModuleDialog({ open: true, module: null })}><Plus className="h-4 w-4" /> Add the first module</Button>} />}>
        {(data) => (
          <Accordion.Root type="multiple" value={openItems.length ? openItems : data.modules.map((m) => String(m.id))} onValueChange={setOpenItems} className="space-y-3">
            {data.modules.map((m, mi) => (
              <Accordion.Item key={m.id} value={String(m.id)} className="overflow-hidden rounded-2xl border bg-card shadow-card">
                <div className="flex items-center gap-1 pr-2">
                  <Accordion.Header className="min-w-0 flex-1">
                    <Accordion.Trigger className="group flex w-full items-center gap-3 px-4 py-3.5 text-left">
                      <ChevronDown className="h-4 w-4 shrink-0 transition-transform group-data-[state=open]:rotate-180" aria-hidden="true" />
                      <span className="min-w-0"><span className="block truncate font-semibold">{m.title}</span><span className="text-xs text-muted-foreground">{pluralize(m.lessons.length, 'lesson')}</span></span>
                    </Accordion.Trigger>
                  </Accordion.Header>
                  <Button size="icon" variant="ghost" disabled={mi === 0} aria-label={`Move ${m.title} up`} onClick={() => move(data.modules, mi, -1, (ids) => modActions.reorder.mutate(ids, { onError: fail }))}><ArrowUp className="h-4 w-4" /></Button>
                  <Button size="icon" variant="ghost" disabled={mi === data.modules.length - 1} aria-label={`Move ${m.title} down`} onClick={() => move(data.modules, mi, 1, (ids) => modActions.reorder.mutate(ids, { onError: fail }))}><ArrowDown className="h-4 w-4" /></Button>
                  <Button size="icon" variant="ghost" aria-label={`Edit ${m.title}`} onClick={() => setModuleDialog({ open: true, module: m })}><Pencil className="h-4 w-4" /></Button>
                  <Button size="icon" variant="ghost" aria-label={`Delete ${m.title}`} onClick={() => setConfirm({ kind: 'module', item: m })}><Trash2 className="h-4 w-4 text-destructive" /></Button>
                </div>
                <Accordion.Content className="overflow-hidden data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down">
                  <div className="border-t px-4 py-3">
                    {m.lessons.length === 0 && <p className="py-3 text-sm text-muted-foreground">No lessons in this module yet.</p>}
                    <ul className="space-y-2">
                      {m.lessons.map((l, li) => {
                        const Icon = LESSON_ICONS[l.type];
                        return (
                          <li key={l.id} className="flex items-center gap-2 rounded-xl border bg-background px-3 py-2.5">
                            <Icon className="h-5 w-5 shrink-0 text-primary" aria-hidden="true" />
                            <div className="min-w-0 flex-1">
                              <p className="truncate text-sm font-medium">{l.title}</p>
                              <p className="text-xs text-muted-foreground">{LESSON_LABELS[l.type]}{l.durationSeconds ? ` · ${formatDuration(l.durationSeconds)}` : ''}</p>
                            </div>
                            <Badge variant={l.published ? 'success' : 'warning'}>{l.published ? 'Published' : 'Draft'}</Badge>
                            <Button size="icon" variant="ghost" disabled={li === 0} aria-label={`Move ${l.title} up`} onClick={() => move(m.lessons, li, -1, (ids) => lessonActions.reorder.mutate({ moduleId: m.id, ids }, { onError: fail }))}><ArrowUp className="h-4 w-4" /></Button>
                            <Button size="icon" variant="ghost" disabled={li === m.lessons.length - 1} aria-label={`Move ${l.title} down`} onClick={() => move(m.lessons, li, 1, (ids) => lessonActions.reorder.mutate({ moduleId: m.id, ids }, { onError: fail }))}><ArrowDown className="h-4 w-4" /></Button>
                            <Button size="icon" variant="ghost" aria-label={`Edit ${l.title}`} onClick={() => setLessonDialog({ open: true, moduleId: m.id, lessonId: l.id })}><Pencil className="h-4 w-4" /></Button>
                            <Button size="icon" variant="ghost" aria-label={`Delete ${l.title}`} onClick={() => setConfirm({ kind: 'lesson', item: l })}><Trash2 className="h-4 w-4 text-destructive" /></Button>
                          </li>
                        );
                      })}
                    </ul>
                    <Button variant="outline" size="sm" className="mt-3" onClick={() => setLessonDialog({ open: true, moduleId: m.id, lessonId: null })}><Plus className="h-4 w-4" /> Add lesson</Button>
                  </div>
                </Accordion.Content>
              </Accordion.Item>
            ))}
          </Accordion.Root>
        )}
      </QueryBoundary>

      <ModuleDialog open={moduleDialog.open} onOpenChange={(o) => setModuleDialog((s) => ({ ...s, open: o }))} module={moduleDialog.module} actions={modActions} />
      {lessonDialog.lessonId ? (
        <LessonEditLoader lessonId={lessonDialog.lessonId}>
          {(lesson) => lesson && <LessonDialog open={lessonDialog.open} onOpenChange={(o) => setLessonDialog((s) => ({ ...s, open: o }))} subjectId={subjectId} moduleId={lessonDialog.moduleId} lesson={lesson} />}
        </LessonEditLoader>
      ) : (
        <LessonDialog open={lessonDialog.open} onOpenChange={(o) => setLessonDialog((s) => ({ ...s, open: o }))} subjectId={subjectId} moduleId={lessonDialog.moduleId} lesson={null} />
      )}
      <ConfirmDialog open={!!confirm} onOpenChange={(o) => !o && setConfirm(null)} destructive confirmLabel="Delete"
        title={confirm?.kind === 'module' ? 'Delete this module?' : 'Delete this lesson?'}
        description={confirm?.kind === 'module' ? 'All lessons inside it and their uploaded files will be removed. This cannot be undone.' : 'The lesson and its uploaded file will be removed. This cannot be undone.'}
        loading={modActions.remove.isPending || lessonActions.remove.isPending}
        onConfirm={() => {
          const run = confirm.kind === 'module' ? modActions.remove : lessonActions.remove;
          run.mutate(confirm.item.id, { onSuccess: () => { toast.success('Deleted'); setConfirm(null); }, onError: fail });
        }} />
    </div>
  );
}
