import { Link } from 'react-router-dom';
import { Compass, ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Logo } from '@/components/ui/logo';
import { useAuth, homePathFor } from '@/features/auth/AuthContext';

function Centered({ icon: Icon, title, text }) {
  const { user } = useAuth();
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
      <Logo />
      <span className="flex h-16 w-16 items-center justify-center rounded-2xl bg-secondary text-secondary-foreground"><Icon className="h-8 w-8" aria-hidden="true" /></span>
      <h1 className="text-3xl font-bold">{title}</h1>
      <p className="max-w-md text-muted-foreground">{text}</p>
      <Button asChild><Link to={user ? homePathFor(user) : '/'}>{user ? 'Go to my dashboard' : 'Back to home'}</Link></Button>
    </div>
  );
}

export const NotFoundPage = () => <Centered icon={Compass} title="Page not found" text="The page you are looking for does not exist or was moved." />;
export const UnauthorizedPage = () => <Centered icon={ShieldAlert} title="You do not have access" text="This page is for a different type of account." />;
