import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { MailCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { api, parseError } from '@/lib/api';
import { forgotSchema } from '@/lib/schemas';
import { AuthShell } from './AuthShell';

export default function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(forgotSchema), defaultValues: { email: '' } });

  const onSubmit = async (values) => {
    setFormError('');
    try {
      await api.post('/auth/forgot-password', values);
      setSent(true);
    } catch (err) {
      setFormError(parseError(err).message);
    }
  };

  return (
    <AuthShell title="Reset your password" subtitle="We will prepare a single-use link that expires in one hour."
      footer={<Link to="/login" className="font-semibold text-primary hover:underline">Back to sign in</Link>}>
      {sent ? (
        <div role="status" className="rounded-2xl border bg-card p-6 text-center">
          <MailCheck className="mx-auto h-10 w-10 text-accent" aria-hidden="true" />
          <p className="mt-3 font-semibold">Check your reset link</p>
          <p className="mt-1 text-sm text-muted-foreground">If that email has an account, we have sent a password reset link to it. The link works once and expires in 1 hour. Check your spam folder if it does not arrive.</p>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
          <Field id="email" label="Email" error={errors.email?.message} required>
            {(p) => <Input {...p} type="email" autoComplete="email" {...register('email')} />}
          </Field>
          <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>Send reset link</Button>
        </form>
      )}
    </AuthShell>
  );
}
