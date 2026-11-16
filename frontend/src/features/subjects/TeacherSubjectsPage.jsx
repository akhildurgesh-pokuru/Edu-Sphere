import { Link } from 'react-router-dom';
import { LibraryBig, Plus } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { CardGridSkeleton, EmptyState, QueryBoundary } from '@/components/ui/states';
import { SubjectCard } from './SubjectCard';
import { useTeacherSubjects } from './useSubjects';

export default function TeacherSubjectsPage() {
  const q = useTeacherSubjects();
  return (
    <div>
      <PageHeader title="My Subjects" description="Create courses, add lessons and publish them to the catalog."
        actions={<Button asChild><Link to="/teacher/subjects/new"><Plus className="h-4 w-4" /> New subject</Link></Button>} />
      <QueryBoundary query={q} skeleton={<CardGridSkeleton />} isEmpty={(d) => d.length === 0}
        empty={<EmptyState icon={LibraryBig} title="Create your first subject" description="A subject is a course. Add modules and lessons, then publish it so students can enroll."
          action={<Button asChild><Link to="/teacher/subjects/new"><Plus className="h-4 w-4" /> Create subject</Link></Button>} />}>
        {(list) => (
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {list.map((s) => <SubjectCard key={s.id} subject={s} to={`/teacher/subjects/${s.id}`} showStatus action={<Button asChild variant="secondary" className="w-full"><Link to={`/teacher/subjects/${s.id}`}>Manage</Link></Button>} />)}
          </div>
        )}
      </QueryBoundary>
    </div>
  );
}
