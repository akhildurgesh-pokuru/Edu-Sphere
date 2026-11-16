import { useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { Archive, Eye, EyeOff, ImagePlus, Pencil, Users } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { Badge, Progress, statusVariant } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { EmptyState, ErrorState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { SubjectAnalyticsTab } from '@/features/analytics/SubjectAnalyticsTab';
import { TeacherAssignmentsTab } from '@/features/assignments/TeacherAssignmentsTab';
import { AnnouncementsTab } from '@/features/community/AnnouncementsTab';
import { DiscussionsTab } from '@/features/community/DiscussionsTab';
import { CurriculumBuilder } from '@/features/content/CurriculumBuilder';
import { TeacherQuizzesTab } from '@/features/quizzes/TeacherQuizzesTab';
import { parseError } from '@/lib/api';
import { formatDate } from '@/lib/utils';
import { SubjectThumb } from './SubjectCard';
import { useRemoveStudent, useSetStatus, useSubjectStudents, useTeacherSubject, useUploadCover } from './useSubjects';

function StudentsTab({ subjectId }) {
  const q = useSubjectStudents(subjectId);
  const remove = useRemoveStudent(subjectId);
  const [confirm, setConfirm] = useState(null);
  return (
    <div>
      <QueryBoundary query={q} skeleton={<ListSkeleton />} isEmpty={(d) => d.length === 0} empty={<EmptyState icon={Users} title="No students yet" description="Publish the subject and students can find it in the catalog and enroll in one click." />}>
        {(list) => (
          <Card className="overflow-x-auto"><table className="w-full min-w-[560px] text-sm"><caption className="sr-only">Enrolled students</caption>
            <thead><tr className="border-b text-left text-xs uppercase tracking-wide text-muted-foreground"><th scope="col" className="p-4">Student</th><th scope="col" className="p-4">Enrolled</th><th scope="col" className="p-4">Progress</th><th scope="col" className="p-4"><span className="sr-only">Actions</span></th></tr></thead>
            <tbody>{list.map((s) => (
              <tr key={s.studentId} className="border-b last:border-0"><td className="p-4"><p className="font-medium">{s.name}</p><p className="text-xs text-muted-foreground">{s.email}</p></td><td className="p-4 text-muted-foreground">{formatDate(s.enrolledAt)}</td>
                <td className="p-4"><div className="flex items-center gap-2"><Progress value={s.progressPercent} className="w-24" label={`${s.name} progress`} /><span className="text-xs font-semibold">{s.progressPercent}%</span></div></td>
                <td className="p-4 text-right"><Button size="sm" variant="ghost" className="text-destructive" onClick={() => setConfirm(s)}>Remove</Button></td></tr>))}</tbody></table></Card>
        )}
      </QueryBoundary>
      <ConfirmDialog open={!!confirm} onOpenChange={(o) => !o && setConfirm(null)} destructive confirmLabel="Remove student" title={`Remove ${confirm?.name}?`} description="They lose access and cannot enroll again." loading={remove.isPending}
        onConfirm={() => remove.mutate(confirm.studentId, { onSuccess: () => { toast.success('Student removed'); setConfirm(null); }, onError: (e) => toast.error(parseError(e).message) })} />
    </div>
  );
}

export default function SubjectManagePage() {
  const { id } = useParams();
  const [params, setParams] = useSearchParams();
  const q = useTeacherSubject(id);
  const setStatus = useSetStatus(id);
  const cover = useUploadCover(id);
  const fileInput = useRef(null);
  const [confirmArchive, setConfirmArchive] = useState(false);
  const tab = params.get('tab') || 'curriculum';

  if (q.isPending) return <ListSkeleton rows={3} />;
  if (q.isError) return <ErrorState error={q.error} onRetry={() => q.refetch()} />;
  const s = q.data;
  const change = (status, msg) => setStatus.mutate(status, { onSuccess: () => { toast.success(msg); setConfirmArchive(false); }, onError: (e) => toast.error(parseError(e).message) });
  const pickCover = (e) => {
    const file = e.target.files?.[0]; e.target.value = '';
    if (file) cover.mutate({ file }, { onSuccess: () => toast.success('Cover image updated'), onError: (err) => toast.error(parseError(err).message) });
  };

  return (
    <div>
      <Card className="mb-6 overflow-hidden"><div className="grid md:grid-cols-[14rem_1fr]">
        <SubjectThumb subject={s} className="h-40 md:h-full" />
        <div className="p-5">
          <div className="flex flex-wrap items-center gap-2"><Badge variant={statusVariant(s.status)}>{s.status}</Badge>{s.category && <Badge variant="secondary">{s.category}</Badge>}</div>
          <h1 className="mt-2 break-words text-2xl font-bold tracking-tight">{s.title}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{s.lessonCount} published lessons · {s.enrollmentCount} students{s.publishedAt ? ` · first published ${formatDate(s.publishedAt)}` : ''}</p>
          <div className="mt-4 flex flex-wrap gap-2">
            {s.status !== 'PUBLISHED' ? <Button variant="accent" loading={setStatus.isPending} onClick={() => change('PUBLISHED', 'Published. Students can now find and enroll.')}><Eye className="h-4 w-4" /> Publish</Button>
              : <Button variant="secondary" loading={setStatus.isPending} onClick={() => change('DRAFT', 'Moved back to draft. Hidden from students.')}><EyeOff className="h-4 w-4" /> Unpublish</Button>}
            <Button asChild variant="outline"><Link to={`/teacher/subjects/${id}/edit`}><Pencil className="h-4 w-4" /> Edit details</Link></Button>
            <input ref={fileInput} type="file" accept=".jpg,.jpeg,.png,.gif,.webp" className="sr-only" onChange={pickCover} aria-label="Choose cover image" />
            <Button variant="outline" loading={cover.isPending} onClick={() => fileInput.current?.click()}><ImagePlus className="h-4 w-4" /> Cover image</Button>
            {s.status !== 'ARCHIVED' && <Button variant="ghost" onClick={() => setConfirmArchive(true)}><Archive className="h-4 w-4" /> Archive</Button>}
          </div>
        </div>
      </div></Card>
      <Tabs value={tab} onValueChange={(v) => setParams({ tab: v })}>
        <TabsList><TabsTrigger value="curriculum">Curriculum</TabsTrigger><TabsTrigger value="quizzes">Quizzes</TabsTrigger><TabsTrigger value="assignments">Assignments</TabsTrigger><TabsTrigger value="students">Students</TabsTrigger><TabsTrigger value="announcements">Announcements</TabsTrigger><TabsTrigger value="discussions">Discussions</TabsTrigger><TabsTrigger value="analytics">Analytics</TabsTrigger></TabsList>
        <TabsContent value="curriculum"><CurriculumBuilder subjectId={id} /></TabsContent>
        <TabsContent value="quizzes"><TeacherQuizzesTab subjectId={id} /></TabsContent>
        <TabsContent value="assignments"><TeacherAssignmentsTab subjectId={id} /></TabsContent>
        <TabsContent value="students"><StudentsTab subjectId={id} /></TabsContent>
        <TabsContent value="announcements"><AnnouncementsTab subjectId={id} isTeacher /></TabsContent>
        <TabsContent value="discussions"><DiscussionsTab subjectId={id} /></TabsContent>
        <TabsContent value="analytics"><SubjectAnalyticsTab subjectId={id} /></TabsContent>
      </Tabs>
      <ConfirmDialog open={confirmArchive} onOpenChange={setConfirmArchive} confirmLabel="Archive" title="Archive this subject?" description="It disappears from the catalog. Enrolled students keep read-only access." loading={setStatus.isPending} onConfirm={() => change('ARCHIVED', 'Subject archived')} />
    </div>
  );
}
