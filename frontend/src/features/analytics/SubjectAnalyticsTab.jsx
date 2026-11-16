import { AlertTriangle, CheckCircle2, Percent, Users, UserX } from 'lucide-react';
import { Badge, Progress } from '@/components/ui/misc';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { EmptyState, ListSkeleton, QueryBoundary, StatCard } from '@/components/ui/states';
import { timeAgo } from '@/lib/utils';
import { BarsChart } from './charts';
import { useSubjectAnalytics } from './useAnalytics';

export function SubjectAnalyticsTab({ subjectId }) {
  const q = useSubjectAnalytics(subjectId);
  return (
    <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.enrolled === 0}
      empty={<EmptyState icon={Users} title="No analytics yet" description="Numbers appear once students enroll and start learning." />}>
      {(d) => (
        <div className="space-y-5">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard icon={Users} tone="indigo" label="Enrolled" value={d.enrolled} hint={`${d.publishedLessons} published lessons`} />
            <StatCard icon={Percent} tone="violet" label="Average progress" value={`${d.averageProgress}%`} />
            <StatCard icon={CheckCircle2} tone="emerald" label="Completion rate" value={`${d.completionRate}%`} hint="students at 100%" />
            <StatCard icon={UserX} tone="amber" label="Active vs inactive" value={`${d.activeStudents} / ${d.inactiveStudents}`} hint="active in last 7 days" />
          </div>
          <div className="grid gap-5 lg:grid-cols-2">
            <Card><CardHeader><CardTitle>Progress distribution</CardTitle><CardDescription>Students by lesson progress</CardDescription></CardHeader><CardContent><BarsChart data={d.progressDistribution} xKey="range" yKey="students" name="Students" /></CardContent></Card>
            <Card><CardHeader><CardTitle>Quiz performance</CardTitle><CardDescription>Average graded score per quiz</CardDescription></CardHeader><CardContent>
              {d.quizzes.some((x) => x.averagePercent != null) ? <BarsChart data={d.quizzes.filter((x) => x.averagePercent != null)} xKey="title" yKey="averagePercent" name="Average" suffix="%" domain={[0, 100]} /> : <p className="py-10 text-center text-sm text-muted-foreground">No graded quiz attempts yet.</p>}</CardContent></Card>
          </div>
          <Card className="overflow-hidden"><CardHeader><CardTitle className="flex items-center gap-2"><AlertTriangle className="h-4 w-4 text-amber-500" /> Students ({d.needingSupport} may need support)</CardTitle>
            <CardDescription>Needs support: quiz average below 50%, inactive 14+ days under 100% progress, or no progress after a week.</CardDescription></CardHeader>
            <div className="overflow-x-auto"><table className="w-full min-w-[720px] text-sm"><caption className="sr-only">Student progress</caption>
              <thead><tr className="border-y text-left text-xs uppercase tracking-wide text-muted-foreground"><th scope="col" className="px-5 py-3">Student</th><th scope="col" className="px-5 py-3">Progress</th><th scope="col" className="px-5 py-3">Quiz avg</th><th scope="col" className="px-5 py-3">Assignments</th><th scope="col" className="px-5 py-3">Last active</th><th scope="col" className="px-5 py-3">Status</th></tr></thead>
              <tbody>{d.students.map((s) => (
                <tr key={s.studentId} className="border-b last:border-0">
                  <td className="px-5 py-3"><p className="font-medium">{s.name}</p><p className="text-xs text-muted-foreground">{s.email}</p></td>
                  <td className="px-5 py-3"><div className="flex items-center gap-2"><Progress value={s.progressPercent} className="w-24" label={`${s.name} progress`} /><span className="text-xs font-semibold">{s.progressPercent}%</span></div></td>
                  <td className="px-5 py-3">{s.quizAverage != null ? `${s.quizAverage}%` : '—'}</td>
                  <td className="px-5 py-3">{s.assignmentsSubmitted}/{d.assignmentTotal}</td>
                  <td className="px-5 py-3 text-muted-foreground">{s.lastActiveAt ? timeAgo(s.lastActiveAt) : 'Never'}</td>
                  <td className="px-5 py-3">{s.needsSupport ? <span title={s.supportReasons.join(', ')}><Badge variant="warning">Needs support</Badge></span> : s.active ? <Badge variant="success">Active</Badge> : <Badge variant="muted">Inactive</Badge>}</td>
                </tr>))}</tbody></table></div></Card>
          <div className="grid gap-5 lg:grid-cols-2">
            <Card><CardHeader><CardTitle>Quizzes</CardTitle></CardHeader><CardContent>{d.quizzes.length === 0 ? <p className="text-sm text-muted-foreground">No quizzes.</p> : <ul className="divide-y">{d.quizzes.map((x) => <li key={x.quizId} className="flex items-center justify-between py-2.5 text-sm"><span className="font-medium">{x.title}</span><span className="text-muted-foreground">{x.gradedAttempts} graded · avg {x.averagePercent ?? '—'}{x.averagePercent != null && '%'} · pass {x.passRate ?? '—'}{x.passRate != null && '%'}</span></li>)}</ul>}</CardContent></Card>
            <Card><CardHeader><CardTitle>Assignment completion</CardTitle></CardHeader><CardContent>{d.assignments.length === 0 ? <p className="text-sm text-muted-foreground">No assignments.</p> : <ul className="divide-y">{d.assignments.map((x) => <li key={x.assignmentId} className="flex items-center justify-between gap-3 py-2.5 text-sm"><span className="min-w-0 truncate font-medium">{x.title}</span><span className="shrink-0 text-muted-foreground">{x.submitted}/{x.enrolled} submitted ({x.completionRate}%){x.averageMarksPercent != null && ` · avg ${x.averageMarksPercent}%`}</span></li>)}</ul>}</CardContent></Card>
          </div>
        </div>
      )}
    </QueryBoundary>
  );
}
