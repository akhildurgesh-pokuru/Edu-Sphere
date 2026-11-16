import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { PageHeader } from '@/components/PageHeader';
import { Avatar, Badge } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Field } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { api, applyFieldErrors, parseError } from '@/lib/api';
import { profileSchema } from '@/lib/schemas';
import { formatDate } from '@/lib/utils';
import { useAuth } from './AuthContext';

export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting, isDirty } } = useForm({ resolver: zodResolver(profileSchema), defaultValues: { name: user.name, emailNotifications: user.emailNotifications ?? true } });
  useEffect(() => reset({ name: user.name, emailNotifications: user.emailNotifications ?? true }), [user.name, user.emailNotifications, reset]);
  const onSubmit = async (v) => {
    setFormError('');
    try { const { data } = await api.put('/auth/profile', v); setUser(data); toast.success('Profile updated'); }
    catch (err) { if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message); }
  };
  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title="Profile" description="Your account details." />
      <Card><CardContent className="p-6">
        <div className="mb-6 flex items-center gap-4"><Avatar name={user.name} className="h-16 w-16 text-xl" /><div className="min-w-0"><p className="truncate text-lg font-semibold">{user.name}</p><p className="truncate text-sm text-muted-foreground">{user.email}</p><div className="mt-1 flex gap-2"><Badge>{user.role === 'TEACHER' ? 'Teacher' : 'Student'}</Badge><span className="text-xs text-muted-foreground">Member since {formatDate(user.createdAt)}</span></div></div></div>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
          <Field id="name" label="Full name" error={errors.name?.message} required>{(p) => <Input {...p} autoComplete="name" {...register('name')} />}</Field>
          <Field id="email" label="Email" hint="Email cannot be changed">{(p) => <Input {...p} value={user.email} readOnly disabled />}</Field>
          <label className="flex cursor-pointer items-start gap-3 rounded-lg border p-3">
            <input type="checkbox" className="mt-0.5 h-4 w-4 rounded border-input accent-[hsl(var(--primary))]" {...register('emailNotifications')} />
            <span><span className="block text-sm font-medium">Email me about activity</span><span className="block text-xs text-muted-foreground">New lessons, quizzes, assignments, grades and deadline reminders. Password and security emails are always sent.</span></span>
          </label>
          <Button type="submit" loading={isSubmitting} disabled={!isDirty}>Save changes</Button>
        </form>
      </CardContent></Card>
    </div>
  );
}
