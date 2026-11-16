import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  const pages = [];
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  for (let i = start; i < Math.min(totalPages, start + 5); i++) pages.push(i);
  return (
    <nav className="mt-8 flex items-center justify-center gap-1" aria-label="Pagination">
      <Button variant="outline" size="icon" disabled={page === 0} onClick={() => onChange(page - 1)} aria-label="Previous page"><ChevronLeft className="h-4 w-4" /></Button>
      {pages.map((p) => (
        <Button key={p} variant={p === page ? 'default' : 'ghost'} size="icon" onClick={() => onChange(p)} aria-current={p === page ? 'page' : undefined} aria-label={`Page ${p + 1}`}>{p + 1}</Button>
      ))}
      <Button variant="outline" size="icon" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)} aria-label="Next page"><ChevronRight className="h-4 w-4" /></Button>
    </nav>
  );
}
