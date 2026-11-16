import { Link } from 'react-router-dom';
import { Activity, BookOpen, CalendarClock, ClipboardCheck, GraduationCap, Plus, Target, UserPlus, Users } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Badge, Progress } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { EmptyState, ErrorState, ListSkeleton, StatCard } from '@/components/ui/states';
import { useAuth } from '@/features/auth/AuthContext';
import { formatDateTime, timeAgo } from '@/lib/utils';
import { BarsChart, TrendChart } from './charts';
import { useTeacherDashboard } from './useAnalytics';

export default function TeacherDashboardPage() {
  const { user } = useAuth();
  const q = useTeacherDashboard();
  const d = q.data;
  const t = d?.totals;
  const noData = d && t.subjects === 0;
  return (
    <div>
      <PageHeader title={`Welcome back, ${user.name.split(' ')[0]}`} description="Here is what is happening across your courses."
        actions={<><Button asChild variant="outline"><Link to="/teacher/subjects">My subjects</Link></Button><Button asChild><Link to="/teacher/subjects/new"><Plus className="h-4 w-4" /> New subject</Link></Button></>} />
      {q.isError && <ErrorState error={q.error} onRetry={() => q.refetch()} />}
      {!q.isError && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
            <StatCard loading={q.isPending} icon={BookOpen} tone="indigo" label="Total subjects" value={t?.subjects} hint={t && `${t.publishedSubjects} published`} />
            <StatCard loading={q.isPending} icon={Users} tone="emerald" label="Enrolled students" value={t?.enrolledStudents} />
            <StatCard loading={q.isPending} icon={GraduationCap} tone="violet" label="Lessons" value={t?.lessons} />
            <StatCard loading={q.isPending} icon={ClipboardCheck} tone="amber" label="Pending to grade" value={t?.pendingAssignments} hint="assignment submissions" />
            <StatCard loading={q.isPending} icon={Target} tone="sky" label="Avg quiz score" value={t?.averageQuizScore != null ? `${t.averageQuizScore}%` : '—'} hint="graded attempts" />
          </div>
          {q.isPending && <div className="mt-6"><ListSkeleton rows={3} /></div>}
          {noData && <div className="mt-6"><EmptyState icon={BookOpen} title="Create your first subject" description="Your dashboard fills with real numbers as students enroll and learn." action={<Button asChild><Link to="/teacher/subjects/new"><Plus className="h-4 w-4" /> Create subject</Link></Button>} /></div>}
          {d && !noData && (
            <>
              <div className="mt-6 grid gap-5 lg:grid-cols-2">
                <Card><CardHeader><CardTitle>Top courses by enrollment</CardTitle><CardDescription>Active enrollments per subject</CardDescription></CardHeader>
                  <CardContent>{d.topCourses.length ? <BarsChart data={d.topCourses} xKey="title" yKey="enrollments" name="Students" /> : <p className="py-10 text-center text-sm text-muted-foreground">No data yet</p>}</CardContent></Card>
                <Card><CardHeader><CardTitle>New enrollments, last 30 days</CardTitle><CardDescription>Students joining your courses each day</CardDescription></CardHeader>
                  <CardContent><TrendChart data={d.enrollmentsOverTime} xKey="date" yKey="count" name="Enrollments" /></CardContent></Card>
              </div>
              <div className="mt-5 grid gap-5 lg:grid-cols-2">
                <Card><CardHeader><CardTitle>Student performance</CardTitle><CardDescription>Average graded quiz score per subject</CardDescription></CardHeader>
                  <CardContent>{d.studentPerformance.some((s) => s.averageQuizScore != null)
                    ? <BarsChart data={d.studentPerformance.filter((s) => s.averageQuizScore != null)} xKey="title" yKey="averageQuizScore" name="Average score" suffix="%" domain={[0, 100]} />
                    : <p className="py-10 text-center text-sm text-muted-foreground">Performance appears after students complete quizzes.</p>}</CardContent></Card>
                <Card><CardHeader><CardTitle className="flex items-center gap-2"><CalendarClock className="h-4 w-4 text-primary" /> Upcoming deadlines</CardTitle></CardHeader>
                  <CardContent>{d.upcomingDeadlines.length === 0 ? <p className="py-6 text-center text-sm text-muted-foreground">No upcoming assignment or quiz deadlines.</p> : (
                    <ul className="space-y-3">{d.upcomingDeadlines.map((x, i) => (
                      <li key={i}><Link to={x.link} className="flex items-center gap-3 rounded-xl border p-3 transition-colors hover:bg-muted">
                        <Badge variant={x.kind === 'QUIZ' ? 'secondary' : 'default'}>{x.kind === 'QUIZ' ? 'Quiz' : 'Assignment'}</Badge>
                        <span className="min-w-0 flex-1"><span className="block truncate text-sm font-medium">{x.title}</span><span className="block truncate text-xs text-muted-foreground">{x.subject}</span></span>
                        <span className="shrink-0 text-xs text-muted-foreground">{formatDateTime(x.at)}</span></Link></li>))}</ul>)}</CardContent></Card>
              </div>
              <Card className="mt-5 overflow-hidden"><CardHeader><CardTitle>Engagement by subject</CardTitle><CardDescription>Progress is completed lessons / published lessons; active means any activity in the last 7 days.</CardDescription></CardHeader>
                <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-sm"><caption className="sr-only">Engagement per subject</caption>
                  <thead><tr className="border-y text-left text-xs uppercase tracking-wide text-muted-foreground"><th scope="col" className="px-5 py-3">Subject</th><th scope="col" className="px-5 py-3">Students</th><th scope="col" className="px-5 py-3">Avg progress</th><th scope="col" className="px-5 py-3">Completion</th><th scope="col" className="px-5 py-3">Active (7d)</th></tr></thead>
                  <tbody>{d.engagement.map((e) => (
                    <tr key={e.subjectId} className="border-b last:border-0"><td className="px-5 py-3"><Link to={`/teacher/subjects/${e.subjectId}`} className="font-medium hover:text-primary">{e.title}</Link></td><td className="px-5 py-3">{e.students}</td>
                      <td className="px-5 py-3"><div className="flex items-center gap-2"><Progress value={e.averageProgress} className="w-24" label={`${e.title} average progress`} /><span className="text-xs font-semibold">{e.averageProgress}%</span></div></td>
                      <td className="px-5 py-3">{e.completionRate}%</td><td className="px-5 py-3">{e.activeStudents}</td></tr>))}</tbody></table></div></Card>
              <div className="mt-5 grid gap-5 lg:grid-cols-2">
                <Card><CardHeader><CardTitle className="flex items-center gap-2"><UserPlus className="h-4 w-4 text-emerald-600" /> Recent enrollments</CardTitle></CardHeader>
                  <CardContent>{d.recentEnrollments.length === 0 ? <p className="py-6 text-center text-sm text-muted-foreground">No students yet.</p> : (
                    <ul className="divide-y">{d.recentEnrollments.map((r, i) => <li key={i} className="flex items-center justify-between gap-3 py-2.5 text-sm"><span className="min-w-0"><span className="block truncate font-medium">{r.student}</span><span className="block truncate text-xs text-muted-foreground">{r.subject}</span></span><span className="shrink-0 text-xs text-muted-foreground">{timeAgo(r.at)}</span></li>)}</ul>)}</CardContent></Card>
                <Card><CardHeader><CardTitle className="flex items-center gap-2"><Activity className="h-4 w-4 text-violet-600" /> Recent quiz activity</CardTitle></CardHeader>
                  <CardContent>{d.recentQuizActivity.length === 0 ? <p className="py-6 text-center text-sm text-muted-foreground">No quiz attempts yet.</p> : (
                    <ul className="divide-y">{d.recentQuizActivity.map((r) => <li key={r.attemptId}><Link to={`/teacher/attempts/${r.attemptId}`} className="flex items-center justify-between gap-3 py-2.5 text-sm hover:text-primary"><span className="min-w-0"><span className="block truncate font-medium">{r.student}</span><span className="block truncate text-xs text-muted-foreground">{r.quiz}</span></span><span className="shrink-0 text-right"><span className="block font-semibold">{r.status === 'PENDING_GRADING' ? 'To grade' : `${r.percent}%`}</span><span className="text-xs text-muted-foreground">{timeAgo(r.at)}</span></span></Link></li>)}</ul>)}</CardContent></Card>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}
