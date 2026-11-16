import * as M from '@radix-ui/react-dropdown-menu';
import { cn } from '@/lib/utils';

export const DropdownMenu = M.Root;
export const DropdownMenuTrigger = M.Trigger;

export function DropdownMenuContent({ className, sideOffset = 8, align = 'end', ...props }) {
  return (
    <M.Portal>
      <M.Content sideOffset={sideOffset} align={align} className={cn('z-50 min-w-[12rem] overflow-hidden rounded-xl border bg-popover p-1.5 text-popover-foreground shadow-xl', className)} {...props} />
    </M.Portal>
  );
}

export function DropdownMenuItem({ className, destructive, ...props }) {
  return (
    <M.Item
      className={cn('flex cursor-pointer select-none items-center gap-2 rounded-lg px-2.5 py-2 text-sm outline-none transition-colors data-[highlighted]:bg-muted', destructive && 'text-destructive', className)}
      {...props}
    />
  );
}

export function DropdownMenuLabel({ className, ...props }) {
  return <M.Label className={cn('px-2.5 py-1.5 text-xs font-semibold text-muted-foreground', className)} {...props} />;
}

export function DropdownMenuSeparator({ className, ...props }) {
  return <M.Separator className={cn('-mx-1.5 my-1.5 h-px bg-border', className)} {...props} />;
}
