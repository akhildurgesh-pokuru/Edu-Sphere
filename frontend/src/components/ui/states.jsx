import { AlertTriangle, Inbox, Lock, SearchX, ServerCrash, WifiOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/misc';
import { parseError } from '@/lib/api';
import { cn } from '@/lib/utils';

export function EmptyState({ icon: Icon = Inbox, title, description, action, className }) {
  return (
    <div className={cn('flex flex-col items-center justify-center rounded-2xl border border-dashed bg-card/50 px-6 py-12 text-center', className)}>
      <span className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-secondary text-secondary-foreground">
        <Icon className="h-7 w-7" aria-hidden="true" />
      </span>
      <h3 className="text-base font-semibold">{title}</h3>
      {description && <p className="mt-1 max-w-sm text-sm text-muted-foreground">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

const ERROR_VIEW = {
  network: { icon: WifiOff, title: 'You seem to be offline' },
  forbidden: { icon: Lock, title: 'You do not have access to this' },
  unauthorized: { icon: Lock, title: 'Please sign in again' },
  notfound: { icon: SearchX, title: 'We could not find that' },
  server: { icon: ServerCrash, title: 'The server had a problem' },
  generic: { icon: AlertTriangle, title: 'Something went wrong' },
};

/** `error` is an Axios error (or already parsed). */
export function ErrorState({ error, onRetry, className }) {
  const parsed = error && error.kind ? error : parseError(error);
  const view = ERROR_VIEW[parsed.kind] || ERROR_VIEW.generic;
  const Icon = view.icon;
  return (
    <div role="alert" className={cn('flex flex-col items-center justify-center rounded-2xl border bg-card px-6 py-12 text-center', className)}>
      <span className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-destructive/10 text-destructive">
        <Icon className="h-7 w-7" aria-hidden="true" />
      </span>
      <h3 className="text-base font-semibold">{view.title}</h3>
      <p className="mt-1 max-w-md text-sm text-muted-foreground">{parsed.message}</p>
      {onRetry && parsed.kind !== 'forbidden' && (
        <Button variant="outline" className="mt-5" onClick={onRetry}>Try again</Button>
      )}
    </div>
  );
}

export function StatCard({ icon: Icon, label, value, hint, tone = 'indigo', loading }) {
  const tones = {
    indigo: 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-300',
    violet: 'bg-violet-500/10 text-violet-600 dark:text-violet-300',
    emerald: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-300',
    amber: 'bg-amber-500/10 text-amber-600 dark:text-amber-300',
    sky: 'bg-sky-500/10 text-sky-600 dark:text-sky-300',
    rose: 'bg-rose-500/10 text-rose-600 dark:text-rose-300',
  };
  return (
    <Card className="flex items-center gap-4 p-5 transition-shadow hover:shadow-pop">
      <span className={cn('flex h-12 w-12 shrink-0 items-center justify-center rounded-xl', tones[tone])}>
        {Icon && <Icon className="h-6 w-6" aria-hidden="true" />}
      </span>
      <div className="min-w-0">
        <p className="truncate text-sm text-muted-foreground">{label}</p>
        {loading ? <Skeleton className="mt-1 h-7 w-16" /> : <p className="text-2xl font-bold tracking-tight">{value}</p>}
        {hint && <p className="truncate text-xs text-muted-foreground">{hint}</p>}
      </div>
    </Card>
  );
}

export function CardGridSkeleton({ count = 6 }) {
  return (
    <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3" aria-busy="true" aria-label="Loading">
      {Array.from({ length: count }).map((_, i) => (
        <Card key={i} className="overflow-hidden">
          <Skeleton className="h-32 rounded-none" />
          <div className="space-y-3 p-5">
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-5 w-4/5" />
            <Skeleton className="h-4 w-1/2" />
          </div>
        </Card>
      ))}
    </div>
  );
}

export function ListSkeleton({ rows = 4 }) {
  return (
    <div className="space-y-3" aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} className="h-16 w-full" />
      ))}
    </div>
  );
}

/** Renders loading / error / empty / data for a TanStack Query result. */
export function QueryBoundary({ query, skeleton, isEmpty, empty, children }) {
  if (query.isPending) return skeleton || <ListSkeleton />;
  if (query.isError) return <ErrorState error={query.error} onRetry={() => query.refetch()} />;
  const data = query.data;
  if (isEmpty ? isEmpty(data) : false) return empty || <EmptyState title="Nothing here yet" />;
  return children(data);
}
