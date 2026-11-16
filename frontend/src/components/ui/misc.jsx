import { cva } from 'class-variance-authority';
import { cn, initials } from '@/lib/utils';

export function Skeleton({ className, ...props }) {
  return <div className={cn('relative overflow-hidden rounded-lg bg-muted before:absolute before:inset-0 before:-translate-x-full before:animate-[shimmer_1.5s_infinite] before:bg-gradient-to-r before:from-transparent before:via-white/40 before:to-transparent dark:before:via-white/10', className)} aria-hidden="true" {...props} />;
}

const badgeVariants = cva('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold', {
  variants: {
    variant: {
      default: 'bg-primary/10 text-primary',
      secondary: 'bg-secondary text-secondary-foreground',
      success: 'bg-accent/15 text-emerald-700 dark:text-emerald-300',
      warning: 'bg-warning/15 text-amber-700 dark:text-amber-300',
      danger: 'bg-destructive/10 text-destructive',
      muted: 'bg-muted text-muted-foreground',
    },
  },
  defaultVariants: { variant: 'default' },
});

export function Badge({ className, variant, ...props }) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export function statusVariant(status) {
  switch (status) {
    case 'PUBLISHED': case 'GRADED': case 'ACTIVE': case 'OPEN': return 'success';
    case 'DRAFT': case 'PENDING': case 'NOT_SUBMITTED': case 'UPCOMING': return 'warning';
    case 'ARCHIVED': case 'CLOSED': case 'NO_ATTEMPTS': return 'muted';
    case 'MISSING': case 'MISSED': case 'LATE': return 'danger';
    case 'PENDING_GRADING': case 'SUBMITTED': return 'default';
    default: return 'muted';
  }
}

export function Progress({ value = 0, className, label }) {
  const v = Math.max(0, Math.min(100, Number(value) || 0));
  return (
    <div className={cn('h-2 w-full overflow-hidden rounded-full bg-muted', className)} role="progressbar" aria-valuenow={Math.round(v)} aria-valuemin={0} aria-valuemax={100} aria-label={label || 'Progress'}>
      <div className="h-full rounded-full bg-gradient-to-r from-primary to-violet-500 transition-all duration-500" style={{ width: `${v}%` }} />
    </div>
  );
}

export function Avatar({ name, className }) {
  return (
    <span className={cn('inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-primary to-violet-500 text-xs font-bold text-white', className)} aria-hidden="true">
      {initials(name)}
    </span>
  );
}

export function Spinner({ className }) {
  return <span role="status" aria-label="Loading" className={cn('inline-block h-5 w-5 animate-spin rounded-full border-2 border-primary border-t-transparent', className)} />;
}
