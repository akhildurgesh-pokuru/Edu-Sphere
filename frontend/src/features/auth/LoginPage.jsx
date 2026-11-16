import { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { applyFieldErrors, parseError } from '@/lib/api';
import { loginSchema } from '@/lib/schemas';
import { AuthShell } from './AuthShell';
import { GoogleButton } from './GoogleButton';
import { homePathFor, useAuth } from './AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const [show, setShow] = useState(false);
  const [formError, setFormError] = useState(params.get('error') === 'google' ? 'Google sign-in did not complete. Please try again or use your password.' : '');
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } });

  const onSubmit = async (values) => {
    setFormError('');
    try {
      const user = await login(values);
      toast.success(`Welcome back, ${user.name.split(' ')[0]}!`);
      const from = location.state?.from;
      navigate(from && from !== '/login' ? from : homePathFor(user), { replace: true });
    } catch (err) {
      if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message);
    }
  };

  return (
    <AuthShell title="Welcome back" subtitle="Sign in to continue learning or teaching."
      footer={<>New to EduSphere? <Link to="/register" className="font-semibold text-primary hover:underline">Create an account</Link></>}>
      <GoogleButton />
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
        <Field id="email" label="Email" error={errors.email?.message} required>
          {(p) => <Input {...p} type="email" autoComplete="email" placeholder="you@example.com" {...register('email')} />}
        </Field>
        <Field id="password" label="Password" error={errors.password?.message} required>
          {(p) => (
            <div className="relative">
              <Input {...p} type={show ? 'text' : 'password'} autoComplete="current-password" className="pr-10" {...register('password')} />
              <button type="button" onClick={() => setShow((s) => !s)} className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-muted-foreground hover:text-foreground" aria-label={show ? 'Hide password' : 'Show password'}>
                {show ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          )}
        </Field>
        <div className="text-right"><Link to="/forgot-password" className="text-sm font-medium text-primary hover:underline">Forgot password?</Link></div>
        <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>Sign in</Button>
      </form>
    </AuthShell>
  );
}
