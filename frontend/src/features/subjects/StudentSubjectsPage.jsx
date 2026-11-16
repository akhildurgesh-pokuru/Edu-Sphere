import { Link } from 'react-router-dom';
import { Compass } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { CardGridSkeleton, EmptyState, QueryBoundary } from '@/components/ui/states';
import { SubjectCard } from './SubjectCard';
import { useStudentSubjects } from './useSubjects';

export default function StudentSubjectsPage() {
  const q = useStudentSubjects();
  return (
    <div>
      <PageHeader title="My Subjects" description="Courses you are enrolled in." actions={<Button asChild variant="outline"><Link to="/student/catalog">Browse more</Link></Button>} />
      <QueryBoundary query={q} skeleton={<CardGridSkeleton />} isEmpty={(d) => d.length === 0}
        empty={<EmptyState icon={Compass} title="You have not enrolled in any course yet" description="Browse the catalog and enroll in one click." action={<Button asChild><Link to="/student/catalog">Browse courses</Link></Button>} />}>
        {(list) => (
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {list.map((s) => <SubjectCard key={s.id} subject={s} to={`/student/subjects/${s.id}`} showProgress action={<Button asChild className="w-full" variant="secondary"><Link to={`/student/subjects/${s.id}`}>Open course</Link></Button>} />)}
          </div>
        )}
      </QueryBoundary>
    </div>
  );
}
