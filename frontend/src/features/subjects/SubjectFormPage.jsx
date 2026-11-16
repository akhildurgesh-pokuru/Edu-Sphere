import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Field } from '@/components/ui/label';
import { Input, Select, Textarea } from '@/components/ui/input';
import { ErrorState, ListSkeleton } from '@/components/ui/states';
import { applyFieldErrors, parseError } from '@/lib/api';
import { CATEGORIES, subjectSchema } from '@/lib/schemas';
import { useCreateSubject, useTeacherSubject, useUpdateSubject } from './useSubjects';

export default function SubjectFormPage() {
  const { id } = useParams();
  const editing = !!id;
  const navigate = useNavigate();
  const subject = useTeacherSubject(id);
  const create = useCreateSubject();
  const update = useUpdateSubject(id);
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(subjectSchema), defaultValues: { title: '', description: '', category: '' },
  });

  useEffect(() => {
    if (subject.data) reset({ title: subject.data.title, description: subject.data.description || '', category: subject.data.category || '' });
  }, [subject.data, reset]);

  const onSubmit = async (values) => {
    setFormError('');
    try {
      const saved = editing ? await update.mutateAsync(values) : await create.mutateAsync(values);
      toast.success(editing ? 'Subject updated' : 'Subject created. Now add modules and lessons.');
      navigate(`/teacher/subjects/${saved.id}`);
    } catch (err) {
      if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message);
    }
  };

  if (editing && subject.isPending) return <ListSkeleton rows={3} />;
  if (editing && subject.isError) return <ErrorState error={subject.error} onRetry={() => subject.refetch()} />;

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title={editing ? 'Edit subject' : 'Create a subject'} description="Students see this information in the catalog once you publish." />
      <Card>
        <CardContent className="p-6">
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
            {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
            <Field id="title" label="Title" error={errors.title?.message} required>
              {(p) => <Input {...p} placeholder="e.g. Java Programming" {...register('title')} />}
            </Field>
            <Field id="category" label="Category" error={errors.category?.message} hint="Helps students filter the catalog">
              {(p) => (
                <Select {...p} {...register('category')}>
                  <option value="">Choose a category</option>
                  {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
                </Select>
              )}
            </Field>
            <Field id="description" label="Description" error={errors.description?.message}>
              {(p) => <Textarea {...p} rows={6} placeholder="What will students learn?" {...register('description')} />}
            </Field>
            <div className="flex justify-end gap-2">
              <Button asChild variant="ghost"><Link to={editing ? `/teacher/subjects/${id}` : '/teacher/subjects'}>Cancel</Link></Button>
              <Button type="submit" loading={isSubmitting}>{editing ? 'Save changes' : 'Create subject'}</Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
