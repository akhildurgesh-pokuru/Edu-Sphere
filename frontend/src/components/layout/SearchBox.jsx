import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { BookOpen, Search, Users } from 'lucide-react';
import { api } from '@/lib/api';
import { Input } from '@/components/ui/input';

function useDebounced(value, ms = 250) {
  const [v, setV] = useState(value);
  useEffect(() => { const t = setTimeout(() => setV(value), ms); return () => clearTimeout(t); }, [value, ms]);
  return v;
}

export function SearchBox({ role }) {
  const [text, setText] = useState('');
  const [open, setOpen] = useState(false);
  const wrap = useRef(null);
  const navigate = useNavigate();
  const q = useDebounced(text.trim());
  const results = useQuery({
    queryKey: ['search', q],
    queryFn: async () => (await api.get('/search', { params: { q } })).data,
    enabled: q.length >= 2,
  });

  useEffect(() => {
    const onDoc = (e) => { if (wrap.current && !wrap.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  const go = (link) => { setOpen(false); setText(''); navigate(link); };
  const subjects = results.data?.subjects || [];
  const students = results.data?.students || [];
  const none = results.data && subjects.length === 0 && students.length === 0;

  return (
    <div ref={wrap} className="relative w-full max-w-md" role="search">
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
      <Input
        value={text}
        onChange={(e) => { setText(e.target.value); setOpen(true); }}
        onFocus={() => setOpen(true)}
        onKeyDown={(e) => {
          if (e.key === 'Escape') setOpen(false);
          if (e.key === 'Enter' && role === 'STUDENT' && text.trim()) go(`/student/catalog?q=${encodeURIComponent(text.trim())}`);
        }}
        placeholder={role === 'TEACHER' ? 'Search your subjects and students' : 'Search courses'}
        aria-label="Search"
        className="pl-9"
      />
      {open && q.length >= 2 && (
        <div className="absolute left-0 right-0 top-12 z-50 max-h-96 overflow-y-auto rounded-xl border bg-popover p-2 shadow-xl">
          {results.isPending && <p className="p-3 text-sm text-muted-foreground">Searching…</p>}
          {results.isError && <p className="p-3 text-sm text-destructive">Search is unavailable right now.</p>}
          {none && <p className="p-3 text-sm text-muted-foreground">No matches for “{q}”.</p>}
          {subjects.length > 0 && <p className="px-2 pt-1 text-xs font-semibold text-muted-foreground">{role === 'TEACHER' ? 'Your subjects' : 'Courses'}</p>}
          {subjects.map((s) => (
            <button key={`s${s.id}`} onClick={() => go(s.link)} className="flex w-full items-center gap-2 rounded-lg px-2 py-2 text-left text-sm hover:bg-muted">
              <BookOpen className="h-4 w-4 text-primary" aria-hidden="true" /> <span className="truncate">{s.title}</span>
            </button>
          ))}
          {students.length > 0 && <p className="px-2 pt-2 text-xs font-semibold text-muted-foreground">Students</p>}
          {students.map((s) => (
            <button key={`u${s.id}${s.link}`} onClick={() => go(s.link)} className="flex w-full items-center gap-2 rounded-lg px-2 py-2 text-left text-sm hover:bg-muted">
              <Users className="h-4 w-4 text-emerald-600" aria-hidden="true" />
              <span className="min-w-0"><span className="block truncate">{s.name}</span><span className="block truncate text-xs text-muted-foreground">{s.subject}</span></span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
