import { Link } from 'react-router-dom';
import { TrendingUp } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Badge, Progress, statusVariant } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { formatDateTime } from '@/lib/utils';
import { useStudentProgress } from './useAnalytics';

export default function StudentProgressPage() {
  const q = useStudentProgress();
  return (
    <div>
      <PageHeader title="My Progress" description="Lessons, quiz scores, assignment marks and deadlines across all your courses." />
      <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.subjects.length === 0}
        empty={<EmptyState icon={TrendingUp} title="No progress yet" description="Enroll in a course and your progress will show up here." action={<Button asChild><Link to="/student/catalog">Browse courses</Link></Button>} />}>
        {(d) => (
          <div className="space-y-5">
            <Card className="p-6"><div className="flex items-end justify-between"><div><p className="text-sm text-muted-foreground">Overall progress</p><p className="text-4xl font-extrabold">{d.overallPercent}%</p></div><p className="text-sm text-muted-foreground">{d.completedLessons} of {d.totalLessons} lessons completed</p></div><Progress value={d.overallPercent} className="mt-4 h-3" label="Overall progress" /></Card>
            <Card><CardHeader><CardTitle>By subject</CardTitle></CardHeader><CardContent className="space-y-4">
              {d.subjects.map((s) => (<div key={s.subjectId}><div className="mb-1 flex flex-wrap items-center justify-between gap-2"><Link to={`/student/subjects/${s.subjectId}`} className="font-semibold hover:text-primary">{s.title}</Link><span className="text-sm text-muted-foreground"><strong className="text-foreground">{s.completedLessons}</strong> done · {s.pendingLessons} pending · {s.percent}%</span></div><Progress value={s.percent} label={`${s.title} progress`} /></div>))}</CardContent></Card>
            <div className="grid gap-5 lg:grid-cols-2">
              <Card className="overflow-hidden"><CardHeader><CardTitle>Quiz scores</CardTitle></CardHeader>
                {d.quizScores.length === 0 ? <CardContent><p className="text-sm text-muted-foreground">No quizzes available yet.</p></CardContent> : (
                  <ul className="divide-y">{d.quizScores.map((x) => <li key={x.quizId} className="flex items-center justify-between gap-3 px-5 py-3 text-sm"><span className="min-w-0"><span className="block truncate font-medium">{x.quiz}</span><span className="block truncate text-xs text-muted-foreground">{x.subject} · {x.attempts} graded attempt{x.attempts === 1 ? '' : 's'}</span></span><span className="shrink-0 font-bold">{x.bestPercent != null ? `${x.bestPercent}%` : '—'}</span></li>)}</ul>)}</Card>
              <Card className="overflow-hidden"><CardHeader><CardTitle>Assignments</CardTitle></CardHeader>
                {d.assignments.length === 0 ? <CardContent><p className="text-sm text-muted-foreground">No assignments yet.</p></CardContent> : (
                  <ul className="divide-y">{d.assignments.map((x) => <li key={x.assignmentId} className="flex items-center justify-between gap-3 px-5 py-3 text-sm"><span className="min-w-0"><span className="block truncate font-medium">{x.title}</span><span className="block truncate text-xs text-muted-foreground">{x.subject} · due {formatDateTime(x.dueAt)}</span></span><span className="shrink-0 text-right">{x.marks != null ? <strong>{x.marks}/{x.maxMarks}</strong> : <Badge variant={statusVariant(x.status)}>{x.status === 'PENDING' ? 'To do' : x.status === 'MISSED' ? 'Missed' : 'Submitted'}</Badge>}</span></li>)}</ul>)}</Card>
            </div>
            {d.deadlines.length > 0 && <Card><CardHeader><CardTitle>Upcoming deadlines</CardTitle></CardHeader><CardContent><ul className="space-y-2">{d.deadlines.map((x, i) => <li key={i}><Link to={x.link} className="flex items-center gap-3 rounded-xl border p-3 hover:bg-muted"><Badge variant={x.kind === 'QUIZ' ? 'secondary' : 'default'}>{x.kind === 'QUIZ' ? 'Quiz' : 'Assignment'}</Badge><span className="min-w-0 flex-1 truncate text-sm font-medium">{x.title}</span><span className="shrink-0 text-xs text-muted-foreground">{formatDateTime(x.at)}</span></Link></li>)}</ul></CardContent></Card>}
          </div>
        )}
      </QueryBoundary>
    </div>
  );
}
