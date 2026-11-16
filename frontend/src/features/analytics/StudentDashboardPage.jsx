import { Link } from 'react-router-dom';
import { BookOpen, CalendarClock, Megaphone, PlayCircle, Target, TrendingUp } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Badge, Progress } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { EmptyState, ErrorState, StatCard } from '@/components/ui/states';
import { useAuth } from '@/features/auth/AuthContext';
import { useStudentAnnouncements } from '@/features/community/useCommunity';
import { formatDateTime, timeAgo } from '@/lib/utils';
import { useStudentDashboard } from './useAnalytics';

export default function StudentDashboardPage() {
  const { user } = useAuth();
  const q = useStudentDashboard();
  const ann = useStudentAnnouncements();
  const d = q.data;
  return (
    <div>
      <PageHeader title={`Hello, ${user.name.split(' ')[0]}`} description="Pick up where you left off." actions={<Button asChild><Link to="/student/catalog">Browse courses</Link></Button>} />
      {q.isError && <ErrorState error={q.error} onRetry={() => q.refetch()} />}
      {!q.isError && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard loading={q.isPending} icon={BookOpen} tone="indigo" label="Enrolled courses" value={d?.enrolledCourses} />
            <StatCard loading={q.isPending} icon={TrendingUp} tone="emerald" label="Overall progress" value={d && `${d.overallPercent}%`} hint={d && `${d.completedLessons}/${d.totalLessons} lessons`} />
            <StatCard loading={q.isPending} icon={Target} tone="violet" label="Avg quiz score" value={d?.averageQuizScore != null ? `${d.averageQuizScore}%` : '—'} hint="best of each quiz" />
            <StatCard loading={q.isPending} icon={CalendarClock} tone="amber" label="Open deadlines" value={d?.pendingDeadlines} />
          </div>
          {d && d.enrolledCourses === 0 && <div className="mt-6"><EmptyState icon={BookOpen} title="Start your first course" description="Browse the catalog, enroll in one click and your progress shows up here." action={<Button asChild><Link to="/student/catalog">Browse courses</Link></Button>} /></div>}
          {d && d.enrolledCourses > 0 && (
            <div className="mt-6 grid gap-5 lg:grid-cols-3">
              <div className="space-y-5 lg:col-span-2">
                {d.continueLearning && (
                  <Card className="overflow-hidden bg-gradient-to-br from-indigo-600 to-violet-600 text-white">
                    <CardContent className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center">
                      <PlayCircle className="h-12 w-12 shrink-0" aria-hidden="true" />
                      <div className="min-w-0 flex-1"><p className="text-sm text-indigo-100">Continue learning</p><p className="truncate text-xl font-bold">{d.continueLearning.lesson}</p><p className="truncate text-sm text-indigo-100">{d.continueLearning.subject}</p></div>
                      <Button asChild variant="secondary" className="bg-white text-indigo-700 hover:bg-white/90"><Link to={`/student/subjects/${d.continueLearning.subjectId}/learn/${d.continueLearning.lessonId}`}>Resume</Link></Button>
                    </CardContent>
                  </Card>
                )}
                <Card><CardHeader><CardTitle>My courses</CardTitle></CardHeader><CardContent className="space-y-4">
                  {d.subjects.map((s) => (
                    <Link key={s.subjectId} to={`/student/subjects/${s.subjectId}`} className="block rounded-xl border p-4 transition-colors hover:bg-muted">
                      <div className="mb-2 flex items-center justify-between gap-2"><span className="truncate font-semibold">{s.title}</span><span className="text-sm font-bold text-primary">{s.percent}%</span></div>
                      <Progress value={s.percent} label={`${s.title} progress`} /><p className="mt-1.5 text-xs text-muted-foreground">{s.completedLessons} of {s.totalLessons} lessons completed</p>
                    </Link>))}</CardContent></Card>
              </div>
              <div className="space-y-5">
                <Card><CardHeader><CardTitle className="flex items-center gap-2"><CalendarClock className="h-4 w-4 text-amber-500" /> Upcoming deadlines</CardTitle></CardHeader><CardContent>
                  {d.upcomingDeadlines.length === 0 ? <p className="py-4 text-center text-sm text-muted-foreground">Nothing due. Nice!</p> : <ul className="space-y-2">{d.upcomingDeadlines.map((x, i) => <li key={i}><Link to={x.link} className="block rounded-xl border p-3 hover:bg-muted"><div className="flex items-center gap-2"><Badge variant={x.kind === 'QUIZ' ? 'secondary' : 'default'}>{x.kind === 'QUIZ' ? 'Quiz' : 'Assignment'}</Badge><span className="truncate text-sm font-medium">{x.title}</span></div><p className="mt-1 text-xs text-muted-foreground">{x.subject} · {formatDateTime(x.at)}</p></Link></li>)}</ul>}</CardContent></Card>
                <Card><CardHeader><CardTitle className="flex items-center gap-2"><Megaphone className="h-4 w-4 text-primary" /> Announcements</CardTitle></CardHeader><CardContent>
                  {ann.isPending ? <p className="py-4 text-center text-sm text-muted-foreground">Loading…</p> : ann.isError ? <p className="py-4 text-center text-sm text-destructive">Could not load announcements.</p> : ann.data.length === 0 ? <p className="py-4 text-center text-sm text-muted-foreground">No announcements yet.</p> : (
                    <ul className="space-y-3">{ann.data.slice(0, 4).map((a) => <li key={a.id}><Link to={`/student/subjects/${a.subjectId}?tab=announcements`} className="block rounded-xl border p-3 hover:bg-muted"><p className="truncate text-sm font-semibold">{a.title}</p><p className="line-clamp-2 text-xs text-muted-foreground">{a.body}</p><p className="mt-1 text-[11px] text-muted-foreground">{a.subjectTitle} · {timeAgo(a.createdAt)}</p></Link></li>)}</ul>)}</CardContent></Card>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
