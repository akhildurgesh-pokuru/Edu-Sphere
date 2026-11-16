import { CheckCircle2, XCircle } from 'lucide-react';
import { Card } from '@/components/ui/card';

export function ReviewList({ items }) {
  return (
    <ol className="space-y-3">
      {items.map((it, i) => {
        const chosen = new Set(it.selectedOptionIds || []);
        const correct = new Set(it.correctOptionIds || []);
        return (
          <Card key={it.questionId} className="p-4">
            <p className="mb-2 text-xs text-muted-foreground">Question {i + 1} · {it.marks} mark{it.marks === 1 ? '' : 's'}{it.marksAwarded != null ? ` · awarded ${it.marksAwarded}` : ''}</p>
            <p className="whitespace-pre-wrap break-words font-medium">{it.text}</p>
            {it.type === 'SHORT_ANSWER' && (
              <div className="mt-3 space-y-2">
                <p className="rounded-lg bg-muted/60 p-3 text-sm whitespace-pre-wrap break-words">{it.textAnswer || <span className="text-muted-foreground">No answer given</span>}</p>
                {it.feedback && <p className="rounded-lg bg-secondary px-3 py-2 text-sm text-secondary-foreground">Teacher feedback: {it.feedback}</p>}
              </div>
            )}
            {it.options.length > 0 && (
              <ul className="mt-3 space-y-1.5">
                {it.options.map((o) => (
                  <li key={o.id} className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm ${correct.has(o.id) ? 'bg-emerald-500/10 font-medium' : chosen.has(o.id) ? 'bg-destructive/10' : 'bg-muted/40'}`}>
                    {correct.has(o.id) ? <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-label="Correct answer" /> : chosen.has(o.id) ? <XCircle className="h-4 w-4 text-destructive" aria-label="Wrong answer chosen" /> : <span className="h-4 w-4" />}
                    <span className="min-w-0 break-words">{o.text}</span>{chosen.has(o.id) && <span className="ml-auto text-xs text-muted-foreground">student's choice</span>}
                  </li>
                ))}
              </ul>
            )}
            {it.explanation && <p className="mt-3 rounded-lg bg-secondary px-3 py-2 text-sm text-secondary-foreground">{it.explanation}</p>}
          </Card>
        );
      })}
    </ol>
  );
}

