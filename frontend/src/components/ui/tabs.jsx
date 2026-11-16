import * as T from '@radix-ui/react-tabs';
import { cn } from '@/lib/utils';

export const Tabs = T.Root;

export function TabsList({ className, ...props }) {
  return <T.List className={cn('flex gap-1 overflow-x-auto rounded-xl bg-muted p-1', className)} {...props} />;
}

export function TabsTrigger({ className, ...props }) {
  return (
    <T.Trigger
      className={cn('inline-flex shrink-0 items-center gap-2 whitespace-nowrap rounded-lg px-3.5 py-2 text-sm font-medium text-muted-foreground transition-all hover:text-foreground data-[state=active]:bg-card data-[state=active]:text-foreground data-[state=active]:shadow-sm', className)}
      {...props}
    />
  );
}

export function TabsContent({ className, ...props }) {
  return <T.Content className={cn('mt-5 focus-visible:outline-none', className)} {...props} />;
}
