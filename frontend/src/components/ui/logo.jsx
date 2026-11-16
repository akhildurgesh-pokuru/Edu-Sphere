import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';

export function LogoMark({ className }) {
  return (
    <span className={cn('inline-flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-violet-500 text-white shadow-pop', className)} aria-hidden="true">
      <svg viewBox="0 0 64 64" className="h-6 w-6" fill="currentColor">
        <path d="M32 14 12 25l20 11 16-8.8V40h4V25z" />
        <path d="M20 35v9c0 3 5.4 7 12 7s12-4 12-7v-9l-12 6.6z" opacity=".85" />
      </svg>
    </span>
  );
}

export function Logo({ to = '/', className }) {
  return (
    <Link to={to} className={cn('inline-flex items-center gap-2.5 font-bold tracking-tight', className)} aria-label="EduSphere home">
      <LogoMark />
      <span className="text-lg">Edu<span className="text-primary">Sphere</span></span>
    </Link>
  );
}
