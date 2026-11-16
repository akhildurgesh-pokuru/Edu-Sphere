import { useNavigate } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { timeAgo } from '@/lib/utils';
import { useMarkAllRead, useMarkRead, useNotifications, useUnreadCount } from './useNotifications';

export function NotificationBell() {
  const navigate = useNavigate();
  const unread = useUnreadCount();
  const list = useNotifications(0, 6);
  const markRead = useMarkRead();
  const markAll = useMarkAllRead();
  const count = unread.data || 0;

  const open = (n) => {
    if (!n.read) markRead.mutate(n.id);
    if (n.link) navigate(n.link);
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button className="relative rounded-lg p-2 hover:bg-muted" aria-label={count ? `Notifications, ${count} unread` : 'Notifications'}>
          <Bell className="h-5 w-5" aria-hidden="true" />
          {count > 0 && (
            <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-bold text-white" aria-hidden="true">
              {count > 9 ? '9+' : count}
            </span>
          )}
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-[22rem] max-w-[calc(100vw-2rem)]">
        <div className="flex items-center justify-between pr-1">
          <DropdownMenuLabel>Notifications</DropdownMenuLabel>
          {count > 0 && (
            <button onClick={() => markAll.mutate()} className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-primary hover:bg-muted">
              <CheckCheck className="h-3.5 w-3.5" /> Mark all read
            </button>
          )}
        </div>
        <DropdownMenuSeparator />
        {list.isPending && <p className="px-3 py-6 text-center text-sm text-muted-foreground">Loading…</p>}
        {list.isError && <p className="px-3 py-6 text-center text-sm text-destructive">Could not load notifications</p>}
        {list.data && list.data.content.length === 0 && <p className="px-3 py-6 text-center text-sm text-muted-foreground">You're all caught up.</p>}
        <div className="max-h-96 overflow-y-auto">
          {list.data?.content.map((n) => (
            <DropdownMenuItem key={n.id} onSelect={() => open(n)} className="items-start">
              <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${n.read ? 'bg-transparent' : 'bg-primary'}`} aria-hidden="true" />
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-medium">{n.title}</span>
                {n.message && <span className="block truncate text-xs text-muted-foreground">{n.message}</span>}
                <span className="text-[11px] text-muted-foreground">{timeAgo(n.createdAt)}</span>
              </span>
            </DropdownMenuItem>
          ))}
        </div>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={() => navigate('/notifications')} className="justify-center font-medium text-primary">View all notifications</DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
