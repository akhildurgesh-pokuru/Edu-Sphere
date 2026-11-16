import { useRef, useState } from 'react';
import { CheckCircle2, FileUp, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/misc';
import { parseError } from '@/lib/api';
import { formatBytes } from '@/lib/utils';
import { uploadResource } from './useContent';

/** Uploads on selection with a progress bar. onUploaded(resource) / onCleared(). */
export function UploadField({ subjectId, accept, hint, resource, onUploaded, onCleared }) {
  const input = useRef(null);
  const abort = useRef(null);
  const [progress, setProgress] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const pick = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setError(''); setBusy(true); setProgress(0);
    abort.current = new AbortController();
    try {
      const res = await uploadResource(subjectId, file, setProgress, abort.current.signal);
      onUploaded(res);
    } catch (err) {
      const p = parseError(err);
      if (p.kind !== 'canceled') setError(p.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <input ref={input} type="file" accept={accept} onChange={pick} className="sr-only" id="lesson-file" aria-label="Choose file" />
      {resource && !busy ? (
        <div className="flex items-center gap-3 rounded-lg border bg-muted/40 px-3 py-2.5">
          <CheckCircle2 className="h-5 w-5 shrink-0 text-accent" aria-hidden="true" />
          <div className="min-w-0 flex-1"><p className="truncate text-sm font-medium">{resource.name}</p><p className="text-xs text-muted-foreground">{formatBytes(resource.sizeBytes)} uploaded</p></div>
          <Button type="button" size="sm" variant="ghost" onClick={() => input.current?.click()}>Replace</Button>
          <Button type="button" size="icon" variant="ghost" onClick={onCleared} aria-label="Remove file"><X className="h-4 w-4" /></Button>
        </div>
      ) : busy ? (
        <div className="rounded-lg border p-3" aria-live="polite">
          <div className="mb-2 flex items-center justify-between text-sm"><span>Uploading… {progress}%</span><Button type="button" size="sm" variant="ghost" onClick={() => abort.current?.abort()}>Cancel</Button></div>
          <Progress value={progress} label="Upload progress" />
        </div>
      ) : (
        <button type="button" onClick={() => input.current?.click()} className="flex w-full flex-col items-center gap-1 rounded-xl border-2 border-dashed border-input px-4 py-6 text-center transition-colors hover:border-primary hover:bg-primary/5">
          <FileUp className="h-7 w-7 text-primary" aria-hidden="true" />
          <span className="text-sm font-medium">Click to choose a file</span>
          {hint && <span className="text-xs text-muted-foreground">{hint}</span>}
        </button>
      )}
      {error && <p role="alert" className="mt-2 text-xs font-medium text-destructive">{error}</p>}
    </div>
  );
}
