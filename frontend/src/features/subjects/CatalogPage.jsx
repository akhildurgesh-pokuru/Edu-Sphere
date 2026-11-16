import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Search, SearchX } from 'lucide-react';
import { toast } from 'sonner';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { Input, Select } from '@/components/ui/input';
import { CardGridSkeleton, EmptyState, ErrorState } from '@/components/ui/states';
import { parseError } from '@/lib/api';
import { Pagination } from './Pagination';
import { SubjectCard } from './SubjectCard';
import { useCatalog, useCategories, useEnroll } from './useSubjects';

function EnrollButton({ subject }) {
  const enroll = useEnroll(subject.id);
  if (subject.enrolled) return <Button asChild variant="secondary" className="w-full"><Link to={`/student/subjects/${subject.id}`}>Continue learning</Link></Button>;
  return (
    <Button className="w-full" loading={enroll.isPending} onClick={() => enroll.mutate(undefined, {
      onSuccess: () => toast.success(`Enrolled in ${subject.title}`),
      onError: (e) => toast.error(parseError(e).message),
    })}>Enroll now</Button>
  );
}

export default function CatalogPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get('q') || '';
  const category = params.get('category') || '';
  const page = Number(params.get('page') || 0);
  const [text, setText] = useState(q);
  const categories = useCategories();
  const catalog = useCatalog({ q, category, page });

  useEffect(() => setText(q), [q]);
  useEffect(() => {
    const t = setTimeout(() => {
      if (text !== q) setParams((p) => { const n = new URLSearchParams(p); text ? n.set('q', text) : n.delete('q'); n.delete('page'); return n; });
    }, 350);
    return () => clearTimeout(t);
  }, [text, q, setParams]);

  const setParam = (key, value) => setParams((p) => { const n = new URLSearchParams(p); value ? n.set(key, value) : n.delete(key); if (key !== 'page') n.delete('page'); return n; });

  return (
    <div>
      <PageHeader title="Browse Courses" description="Every published course from every teacher, in one catalog." />
      <div className="mb-6 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
          <Input value={text} onChange={(e) => setText(e.target.value)} placeholder="Search courses by title or topic" aria-label="Search courses" className="pl-9" />
        </div>
        <Select value={category} onChange={(e) => setParam('category', e.target.value)} aria-label="Filter by category" className="sm:w-56">
          <option value="">All categories</option>
          {(categories.data || []).map((c) => <option key={c} value={c}>{c}</option>)}
        </Select>
      </div>

      {catalog.isPending && <CardGridSkeleton />}
      {catalog.isError && <ErrorState error={catalog.error} onRetry={() => catalog.refetch()} />}
      {catalog.data && catalog.data.content.length === 0 && (
        <EmptyState icon={SearchX} title={q || category ? 'No courses match your search' : 'No courses are published yet'}
          description={q || category ? 'Try a different keyword or clear the filters.' : 'Check back soon, teachers are preparing new courses.'}
          action={(q || category) && <Button variant="outline" onClick={() => setParams({})}>Clear filters</Button>} />
      )}
      {catalog.data && catalog.data.content.length > 0 && (
        <>
          <p className="mb-3 text-sm text-muted-foreground" aria-live="polite">{catalog.data.totalElements} course{catalog.data.totalElements === 1 ? '' : 's'} found</p>
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {catalog.data.content.map((s) => <SubjectCard key={s.id} subject={s} to={`/student/subjects/${s.id}`} showProgress action={<EnrollButton subject={s} />} />)}
          </div>
          <Pagination page={catalog.data.page} totalPages={catalog.data.totalPages} onChange={(p) => setParam('page', p ? String(p) : '')} />
        </>
      )}
    </div>
  );
}
