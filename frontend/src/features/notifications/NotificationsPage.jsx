import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { Pagination } from '@/features/subjects/Pagination';
import { cn, timeAgo } from '@/lib/utils';
import { useMarkAllRead, useMarkRead, useNotifications } from './useNotifications';

export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const q = useNotifications(page, 20);
  const markRead = useMarkRead();
  const markAll = useMarkAllRead();
  const navigate = useNavigate();
  return (
    <div>
      <PageHeader title="Notifications" description="Enrollments, new material, deadlines, results and replies." actions={<Button variant="outline" loading={markAll.isPending} onClick={() => markAll.mutate()}><CheckCheck className="h-4 w-4" /> Mark all read</Button>} />
      <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.content.length === 0} empty={<EmptyState icon={Bell} title="You're all caught up" description="New notifications will appear here." />}>
        {(d) => (
          <>
            <Card className="divide-y overflow-hidden">
              {d.content.map((n) => (
                <button key={n.id} onClick={() => { if (!n.read) markRead.mutate(n.id); if (n.link) navigate(n.link); }} className={cn('flex w-full items-start gap-3 px-5 py-4 text-left transition-colors hover:bg-muted', !n.read && 'bg-primary/5')}>
                  <span className={cn('mt-2 h-2 w-2 shrink-0 rounded-full', n.read ? 'bg-transparent' : 'bg-primary')} aria-label={n.read ? undefined : 'Unread'} />
                  <span className="min-w-0 flex-1"><span className={cn('block break-words text-sm', !n.read && 'font-semibold')}>{n.title}</span>{n.message && <span className="block break-words text-sm text-muted-foreground">{n.message}</span>}</span>
                  <span className="shrink-0 text-xs text-muted-foreground">{timeAgo(n.createdAt)}</span>
                </button>
              ))}
            </Card>
            <Pagination page={d.page} totalPages={d.totalPages} onChange={setPage} />
          </>
        )}
      </QueryBoundary>
    </div>
  );
}
