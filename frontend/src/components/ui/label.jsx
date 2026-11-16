import { cn } from '@/lib/utils';

export function Label({ className, ...props }) {
  return <label className={cn('mb-1.5 block text-sm font-medium', className)} {...props} />;
}

/** Label + control + hint + error, wired with aria attributes. `children` receives {id, 'aria-invalid', 'aria-describedby'}. */
export function Field({ id, label, error, hint, required, children, className }) {
  const errId = `${id}-error`;
  const hintId = `${id}-hint`;
  const describedBy = [error ? errId : null, hint ? hintId : null].filter(Boolean).join(' ') || undefined;
  return (
    <div className={className}>
      {label && (
        <Label htmlFor={id}>
          {label}
          {required && <span className="text-destructive" aria-hidden="true"> *</span>}
        </Label>
      )}
      {children({ id, 'aria-invalid': error ? 'true' : undefined, 'aria-describedby': describedBy })}
      {hint && !error && <p id={hintId} className="mt-1 text-xs text-muted-foreground">{hint}</p>}
      {error && <p id={errId} role="alert" className="mt-1 text-xs font-medium text-destructive">{error}</p>}
    </div>
  );
}
