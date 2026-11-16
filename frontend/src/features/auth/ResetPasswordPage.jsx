import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { api, applyFieldErrors, parseError } from '@/lib/api';
import { resetSchema } from '@/lib/schemas';
import { AuthShell } from './AuthShell';

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const navigate = useNavigate();
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(resetSchema), defaultValues: { password: '', confirmPassword: '' } });

  const onSubmit = async (values) => {
    setFormError('');
    try {
      await api.post('/auth/reset-password', { token, password: values.password });
      toast.success('Password updated. Please sign in.');
      navigate('/login', { replace: true });
    } catch (err) {
      if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message);
    }
  };

  if (!token) {
    return <AuthShell title="Link missing" subtitle="Open the reset link you were given, or request a new one." footer={<Link to="/forgot-password" className="font-semibold text-primary hover:underline">Request a new link</Link>} />;
  }

  return (
    <AuthShell title="Choose a new password" footer={<Link to="/login" className="font-semibold text-primary hover:underline">Back to sign in</Link>}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
        <Field id="password" label="New password" error={errors.password?.message} hint="8+ characters with a letter and a number" required>
          {(p) => <Input {...p} type="password" autoComplete="new-password" {...register('password')} />}
        </Field>
        <Field id="confirmPassword" label="Confirm password" error={errors.confirmPassword?.message} required>
          {(p) => <Input {...p} type="password" autoComplete="new-password" {...register('confirmPassword')} />}
        </Field>
        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>Update password</Button>
      </form>
    </AuthShell>
  );
}
