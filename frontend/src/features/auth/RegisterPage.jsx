import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { BookOpen, GraduationCap } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { applyFieldErrors, parseError } from '@/lib/api';
import { registerSchema } from '@/lib/schemas';
import { cn } from '@/lib/utils';
import { AuthShell } from './AuthShell';
import { GoogleButton } from './GoogleButton';
import { homePathFor, useAuth } from './AuthContext';
import { useProviders } from './useProviders';

const ROLES = [
  { value: 'STUDENT', title: 'Learn', desc: 'Browse courses and enroll', icon: GraduationCap },
  { value: 'TEACHER', title: 'Teach', desc: 'Create and publish courses', icon: BookOpen },
];

export default function RegisterPage({ invite = false }) {
  const { register: signUp } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const token = invite ? params.get('token') : null;
  const providers = useProviders();
  const teacherAllowed = invite || providers.data?.teacherSignup !== false;
  const [formError, setFormError] = useState('');
  const { register, handleSubmit, setError, watch, setValue, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(registerSchema),
    defaultValues: { name: '', email: '', password: '', confirmPassword: '', role: invite ? 'TEACHER' : 'STUDENT' },
  });
  const role = watch('role');

  const onSubmit = async (values) => {
    setFormError('');
    try {
      const user = await signUp(values, token);
      toast.success('Your account is ready. Welcome to EduSphere!');
      navigate(homePathFor(user), { replace: true });
    } catch (err) {
      if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message);
    }
  };

  if (invite && !token) {
    return <AuthShell title="Invitation needed" subtitle="Open the invitation link you were sent to create a teacher account." footer={<Link to="/login" className="font-semibold text-primary hover:underline">Back to sign in</Link>} />;
  }

  return (
    <AuthShell title={invite ? 'Accept your teacher invitation' : 'Create your account'} subtitle="Choose how you want to use EduSphere."
      footer={<>Already have an account? <Link to="/login" className="font-semibold text-primary hover:underline">Sign in</Link></>}>
      {!invite && <GoogleButton label="Sign up with Google (as a learner)" />}
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
        {!invite && (
          <fieldset>
            <legend className="mb-1.5 text-sm font-medium">I want to</legend>
            <div className="grid grid-cols-2 gap-3">
              {ROLES.map((r) => {
                const disabled = r.value === 'TEACHER' && !teacherAllowed;
                const Icon = r.icon;
                return (
                  <label key={r.value} className={cn('relative flex cursor-pointer flex-col gap-1 rounded-xl border-2 p-4 transition-all', role === r.value ? 'border-primary bg-primary/5 shadow-pop' : 'border-border hover:border-primary/40', disabled && 'cursor-not-allowed opacity-50')}>
                    <input type="radio" value={r.value} disabled={disabled} className="sr-only" {...register('role')} onChange={() => setValue('role', r.value)} checked={role === r.value} />
                    <Icon className="h-6 w-6 text-primary" aria-hidden="true" />
                    <span className="font-semibold">{r.title}</span>
                    <span className="text-xs text-muted-foreground">{disabled ? 'By invitation only' : r.desc}</span>
                  </label>
                );
              })}
            </div>
          </fieldset>
        )}
        <Field id="name" label="Full name" error={errors.name?.message} required>
          {(p) => <Input {...p} autoComplete="name" {...register('name')} />}
        </Field>
        <Field id="email" label="Email" error={errors.email?.message} required>
          {(p) => <Input {...p} type="email" autoComplete="email" {...register('email')} />}
        </Field>
        <Field id="password" label="Password" error={errors.password?.message} hint="8+ characters with a letter and a number" required>
          {(p) => <Input {...p} type="password" autoComplete="new-password" {...register('password')} />}
        </Field>
        <Field id="confirmPassword" label="Confirm password" error={errors.confirmPassword?.message} required>
          {(p) => <Input {...p} type="password" autoComplete="new-password" {...register('confirmPassword')} />}
        </Field>
        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>Create account</Button>
      </form>
    </AuthShell>
  );
}
