import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Field } from '@/components/ui/label';
import { Checkbox, Input, Select, Textarea } from '@/components/ui/input';
import { applyFieldErrors, parseError } from '@/lib/api';
import { lessonSchema } from '@/lib/schemas';
import { UploadField } from './UploadField';
import { useLessonActions } from './useContent';

const ACCEPT = { VIDEO: '.mp4,.webm,video/mp4,video/webm', DOCUMENT: '.pdf,.ppt,.pptx,.doc,.docx,.txt' };
const HINT = { VIDEO: 'MP4 or WebM, up to 500 MB', DOCUMENT: 'PDF, PPT, PPTX, DOC, DOCX or TXT, up to 100 MB' };

export function LessonDialog({ open, onOpenChange, subjectId, moduleId, lesson }) {
  const editing = !!lesson;
  const actions = useLessonActions();
  const [resource, setResource] = useState(null);
  const [fileError, setFileError] = useState('');
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, watch, reset, setError, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(lessonSchema), defaultValues: { title: '', type: 'VIDEO', contentText: '', externalUrl: '', durationMinutes: 0, published: false },
  });
  const type = watch('type');

  useEffect(() => {
    if (!open) return;
    setFormError(''); setFileError('');
    if (lesson) {
      reset({ title: lesson.title, type: lesson.type, contentText: lesson.contentText || '', externalUrl: lesson.externalUrl || '', durationMinutes: Math.round((lesson.durationSeconds || 0) / 60), published: lesson.published });
      setResource(lesson.resource || null);
    } else {
      reset({ title: '', type: 'VIDEO', contentText: '', externalUrl: '', durationMinutes: 0, published: false });
      setResource(null);
    }
  }, [open, lesson, reset]);

  const onSubmit = async (v) => {
    setFormError(''); setFileError('');
    if ((v.type === 'VIDEO' || v.type === 'DOCUMENT') && !resource) { setFileError('Upload a file for this lesson first'); return; }
    const body = {
      title: v.title, type: v.type, contentText: v.contentText || null, externalUrl: v.type === 'LINK' ? v.externalUrl : null,
      resourceId: v.type === 'VIDEO' || v.type === 'DOCUMENT' ? resource.id : null,
      durationSeconds: Math.round((Number(v.durationMinutes) || 0) * 60), published: !!v.published,
    };
    try {
      if (editing) await actions.update.mutateAsync({ id: lesson.id, ...body });
      else await actions.create.mutateAsync({ moduleId, ...body });
      toast.success(editing ? 'Lesson updated' : 'Lesson added');
      onOpenChange(false);
    } catch (err) {
      if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={editing ? 'Edit lesson' : 'Add a lesson'} description="Students only see published lessons.">
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
          <Field id="lesson-title" label="Title" error={errors.title?.message} required>{(p) => <Input {...p} {...register('title')} />}</Field>
          <Field id="lesson-type" label="Lesson type">
            {(p) => (
              <Select {...p} {...register('type', { onChange: () => { setResource(null); setFileError(''); } })}>
                <option value="VIDEO">Video</option><option value="TEXT">Text</option><option value="DOCUMENT">Document (PDF, slides, doc)</option><option value="LINK">External link</option>
              </Select>
            )}
          </Field>
          {(type === 'VIDEO' || type === 'DOCUMENT') && (
            <div>
              <UploadField key={type} subjectId={subjectId} accept={ACCEPT[type]} hint={HINT[type]} resource={resource} onUploaded={(r) => { setResource(r); setFileError(''); }} onCleared={() => setResource(null)} />
              {fileError && <p role="alert" className="mt-1 text-xs font-medium text-destructive">{fileError}</p>}
            </div>
          )}
          {type === 'LINK' && <Field id="lesson-url" label="Link (https://…)" error={errors.externalUrl?.message} required>{(p) => <Input {...p} type="url" placeholder="https://example.com/article" {...register('externalUrl')} />}</Field>}
          {type === 'TEXT' && <Field id="lesson-text" label="Lesson text" error={errors.contentText?.message} required>{(p) => <Textarea {...p} rows={8} {...register('contentText')} />}</Field>}
          {type !== 'TEXT' && <Field id="lesson-notes" label="Description (optional)" error={errors.contentText?.message}>{(p) => <Textarea {...p} rows={3} {...register('contentText')} />}</Field>}
          {type === 'VIDEO' && <Field id="lesson-duration" label="Video length (minutes)" hint="Used to keep saved positions realistic" error={errors.durationMinutes?.message}>{(p) => <Input {...p} type="number" min="0" step="1" {...register('durationMinutes')} />}</Field>}
          <label className="flex items-center gap-2 text-sm font-medium"><Checkbox {...register('published')} /> Publish this lesson (visible to enrolled students)</label>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" loading={isSubmitting}>{editing ? 'Save lesson' : 'Add lesson'}</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
