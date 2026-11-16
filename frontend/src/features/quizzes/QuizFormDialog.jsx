import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Field } from '@/components/ui/label';
import { Checkbox, Input, Textarea } from '@/components/ui/input';
import { applyFieldErrors, parseError } from '@/lib/api';
import { quizSchema } from '@/lib/schemas';

const toLocal = (iso) => {
  if (!iso) return '';
  const d = new Date(iso);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
};
const toIso = (local) => (local ? new Date(local).toISOString() : null);

export function QuizFormDialog({ open, onOpenChange, quiz, onSave }) {
  const editing = !!quiz;
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(quizSchema),
    defaultValues: { title: '', description: '', durationMinutes: 30, maxAttempts: 1, passingPercentage: 50, startAt: '', endAt: '', showResults: true, randomize: false },
  });
  useEffect(() => {
    if (!open) return;
    setFormError('');
    reset(quiz ? {
      title: quiz.title, description: quiz.description || '', durationMinutes: quiz.durationMinutes, maxAttempts: quiz.maxAttempts, passingPercentage: quiz.passingPercentage,
      startAt: toLocal(quiz.startAt), endAt: toLocal(quiz.endAt), showResults: quiz.showResults, randomize: quiz.randomize,
    } : { title: '', description: '', durationMinutes: 30, maxAttempts: 1, passingPercentage: 50, startAt: '', endAt: '', showResults: true, randomize: false });
  }, [open, quiz, reset]);

  const onSubmit = async (v) => {
    setFormError('');
    try {
      await onSave({ ...v, startAt: toIso(v.startAt), endAt: toIso(v.endAt) });
      toast.success(editing ? 'Quiz updated' : 'Quiz created');
      onOpenChange(false);
    } catch (err) {
      if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={editing ? 'Quiz settings' : 'Create a quiz'} description="Add questions after saving, then publish when it is ready." className="max-w-xl">
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
          <Field id="quiz-title" label="Title" error={errors.title?.message} required>{(p) => <Input {...p} {...register('title')} />}</Field>
          <Field id="quiz-desc" label="Description" error={errors.description?.message}>{(p) => <Textarea {...p} rows={2} {...register('description')} />}</Field>
          <div className="grid gap-4 sm:grid-cols-3">
            <Field id="quiz-duration" label="Duration (min)" error={errors.durationMinutes?.message} required>{(p) => <Input {...p} type="number" min="1" {...register('durationMinutes')} />}</Field>
            <Field id="quiz-attempts" label="Max attempts" error={errors.maxAttempts?.message} required>{(p) => <Input {...p} type="number" min="1" {...register('maxAttempts')} />}</Field>
            <Field id="quiz-pass" label="Passing %" error={errors.passingPercentage?.message} required>{(p) => <Input {...p} type="number" min="0" max="100" {...register('passingPercentage')} />}</Field>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field id="quiz-start" label="Opens (optional)" error={errors.startAt?.message}>{(p) => <Input {...p} type="datetime-local" {...register('startAt')} />}</Field>
            <Field id="quiz-end" label="Closes (optional)" error={errors.endAt?.message}>{(p) => <Input {...p} type="datetime-local" {...register('endAt')} />}</Field>
          </div>
          <div className="space-y-2">
            <label className="flex items-center gap-2 text-sm font-medium"><Checkbox {...register('showResults')} /> Let students review answers after submitting</label>
            <label className="flex items-center gap-2 text-sm font-medium"><Checkbox {...register('randomize')} /> Randomize question order</label>
          </div>
          <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button><Button type="submit" loading={isSubmitting}>{editing ? 'Save settings' : 'Create quiz'}</Button></div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
