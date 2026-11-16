import { forwardRef } from 'react';
import { cn } from '@/lib/utils';

const base =
  'w-full rounded-lg border border-input bg-card px-3 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:border-primary disabled:cursor-not-allowed disabled:opacity-60 aria-[invalid=true]:border-destructive';

export const Input = forwardRef(function Input({ className, type = 'text', ...props }, ref) {
  return <input ref={ref} type={type} className={cn(base, 'h-10', className)} {...props} />;
});

export const Textarea = forwardRef(function Textarea({ className, rows = 4, ...props }, ref) {
  return <textarea ref={ref} rows={rows} className={cn(base, 'py-2', className)} {...props} />;
});

export const Select = forwardRef(function Select({ className, children, ...props }, ref) {
  return (
    <select ref={ref} className={cn(base, 'h-10 pr-8', className)} {...props}>
      {children}
    </select>
  );
});

export const Checkbox = forwardRef(function Checkbox({ className, ...props }, ref) {
  return <input ref={ref} type="checkbox" className={cn('h-4 w-4 rounded border-input accent-[hsl(var(--primary))]', className)} {...props} />;
});
