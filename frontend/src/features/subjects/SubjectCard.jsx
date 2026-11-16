import { useState } from 'react';
import { Link } from 'react-router-dom';
import { BookOpen, Users } from 'lucide-react';
import { Badge, Progress, statusVariant } from '@/components/ui/misc';
import { Card } from '@/components/ui/card';
import { coverUrl } from '@/lib/api';
import { cn, gradientFor, pluralize } from '@/lib/utils';

export function SubjectThumb({ subject, className }) {
  const [broken, setBroken] = useState(false);
  return (
    <div className={cn('relative h-36 overflow-hidden', !subject.hasCover || broken ? `bg-gradient-to-br ${gradientFor(subject.id)}` : 'bg-muted', className)}>
      {subject.hasCover && !broken ? (
        <img src={coverUrl(subject.id)} alt="" loading="lazy" onError={() => setBroken(true)} className="h-full w-full object-cover" />
      ) : (
        <>
          <div className="absolute -right-6 -top-6 h-28 w-28 rounded-full bg-white/15" aria-hidden="true" />
          <div className="absolute -bottom-10 left-4 h-24 w-24 rounded-full bg-white/10" aria-hidden="true" />
          <BookOpen className="absolute bottom-4 left-4 h-9 w-9 text-white/90" aria-hidden="true" />
        </>
      )}
      {subject.category && <span className="absolute left-3 top-3 rounded-full bg-white/90 px-2.5 py-1 text-xs font-semibold text-slate-800 shadow-sm">{subject.category}</span>}
    </div>
  );
}

/** Course card shared by the catalog, My Subjects and the teacher list. `action` is rendered at the bottom. */
export function SubjectCard({ subject, to, action, showStatus, showProgress }) {
  return (
    <Card className="group flex flex-col overflow-hidden transition-all duration-200 hover:-translate-y-1 hover:shadow-pop">
      <Link to={to} className="block" aria-label={`Open ${subject.title}`}><SubjectThumb subject={subject} /></Link>
      <div className="flex flex-1 flex-col gap-3 p-5">
        <div className="flex items-start justify-between gap-2">
          <h3 className="line-clamp-2 text-base font-semibold leading-snug"><Link to={to} className="hover:text-primary">{subject.title}</Link></h3>
          {showStatus && <Badge variant={statusVariant(subject.status)}>{subject.status}</Badge>}
        </div>
        <p className="text-sm text-muted-foreground">by {subject.teacherName}</p>
        <div className="flex items-center gap-4 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1"><BookOpen className="h-3.5 w-3.5" aria-hidden="true" />{pluralize(subject.lessonCount, 'lesson')}</span>
          <span className="inline-flex items-center gap-1"><Users className="h-3.5 w-3.5" aria-hidden="true" />{pluralize(subject.enrollmentCount, 'student')}</span>
        </div>
        {showProgress && subject.enrolled && (
          <div>
            <div className="mb-1 flex justify-between text-xs"><span className="text-muted-foreground">Progress</span><span className="font-semibold">{Math.round(subject.progressPercent || 0)}%</span></div>
            <Progress value={subject.progressPercent} label={`${subject.title} progress`} />
          </div>
        )}
        {action && <div className="mt-auto pt-1">{action}</div>}
      </div>
    </Card>
  );
}
