import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { ArrowLeft, Lock, MessageSquare, Pin, Plus, Search, Trash2 } from 'lucide-react';
import { Avatar, Badge } from '@/components/ui/misc';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Field } from '@/components/ui/label';
import { Input, Textarea } from '@/components/ui/input';
import { EmptyState, ListSkeleton, QueryBoundary } from '@/components/ui/states';
import { Pagination } from '@/features/subjects/Pagination';
import { applyFieldErrors, parseError } from '@/lib/api';
import { threadSchema } from '@/lib/schemas';
import { timeAgo } from '@/lib/utils';
import { useCommunityActions, useThread, useThreads } from './useCommunity';

function NewThreadDialog({ open, onOpenChange, actions, onCreated }) {
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(threadSchema), defaultValues: { title: '', body: '' } });
  const [formError, setFormError] = useState('');
  useEffect(() => { if (open) { reset({ title: '', body: '' }); setFormError(''); } }, [open, reset]);
  const onSubmit = async (v) => {
    try { const t = await actions.createThread.mutateAsync(v); toast.success('Question posted'); onOpenChange(false); onCreated(t.id); }
    catch (err) { if (!applyFieldErrors(err, setError)) setFormError(parseError(err).message); }
  };
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title="Ask a question" description="Your teacher and classmates can reply.">
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          {formError && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{formError}</div>}
          <Field id="th-title" label="Title" error={errors.title?.message} required>{(p) => <Input {...p} {...register('title')} />}</Field>
          <Field id="th-body" label="Message" error={errors.body?.message} required>{(p) => <Textarea {...p} rows={5} {...register('body')} />}</Field>
          <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button><Button type="submit" loading={isSubmitting}>Post</Button></div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ThreadView({ id, subjectId, onBack, actions }) {
  const q = useThread(id);
  const [text, setText] = useState('');
  const [confirm, setConfirm] = useState(null);
  const fail = (e) => toast.error(parseError(e).message);
  return (
    <QueryBoundary query={q} skeleton={<ListSkeleton rows={3} />}>
      {(t) => (
        <div>
          <Button variant="ghost" size="sm" className="mb-3 -ml-2" onClick={onBack}><ArrowLeft className="h-4 w-4" /> All discussions</Button>
          <Card className="p-5">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div className="min-w-0"><h2 className="break-words text-xl font-bold">{t.title}</h2>
                <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted-foreground"><span>{t.authorName}</span>{t.authorRole === 'TEACHER' && <Badge>Teacher</Badge>}<span>· {timeAgo(t.createdAt)}</span>{t.pinned && <Badge variant="warning"><Pin className="h-3 w-3" />Pinned</Badge>}{t.closed && <Badge variant="muted"><Lock className="h-3 w-3" />Closed</Badge>}</div></div>
              <div className="flex gap-1">
                {t.canModerate && <Button size="sm" variant="outline" onClick={() => actions.pin.mutate({ id: t.id, value: !t.pinned }, { onError: fail })}>{t.pinned ? 'Unpin' : 'Pin'}</Button>}
                {t.canModerate && <Button size="sm" variant="outline" onClick={() => actions.close.mutate({ id: t.id, value: !t.closed }, { onError: fail })}>{t.closed ? 'Reopen' : 'Close'}</Button>}
                {t.canDelete && <Button size="icon" variant="ghost" aria-label="Delete discussion" onClick={() => setConfirm({ thread: true })}><Trash2 className="h-4 w-4 text-destructive" /></Button>}
              </div>
            </div>
            <p className="prose-user mt-4">{t.body}</p>
          </Card>
          <h3 className="mb-3 mt-6 text-sm font-semibold text-muted-foreground">{t.replies.length} {t.replies.length === 1 ? 'reply' : 'replies'}</h3>
          <ul className="space-y-3">
            {t.replies.map((r) => (
              <li key={r.id}><Card className="flex gap-3 p-4">
                <Avatar name={r.authorName} />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground"><span className="font-semibold text-foreground">{r.authorName}</span>{r.authorRole === 'TEACHER' && <Badge>Teacher</Badge>}<span>{timeAgo(r.createdAt)}</span></div>
                  <p className="prose-user mt-1">{r.body}</p>
                </div>
                {r.canDelete && <Button size="icon" variant="ghost" aria-label="Delete reply" onClick={() => setConfirm({ reply: r })}><Trash2 className="h-4 w-4 text-destructive" /></Button>}
              </Card></li>
            ))}
          </ul>
          {t.closed ? <p className="mt-4 rounded-xl bg-muted px-4 py-3 text-sm text-muted-foreground">This discussion is closed{t.canModerate ? ', but you can still reply as the teacher.' : '.'}</p> : null}
          {(!t.closed || t.canModerate) && (
            <form className="mt-4" onSubmit={(e) => { e.preventDefault(); if (!text.trim()) return; actions.reply.mutate({ id: t.id, body: text }, { onSuccess: () => setText(''), onError: fail }); }}>
              <label htmlFor="reply-text" className="sr-only">Your reply</label>
              <Textarea id="reply-text" rows={3} value={text} onChange={(e) => setText(e.target.value)} placeholder="Write a reply…" maxLength={10000} />
              <div className="mt-2 flex justify-end"><Button type="submit" loading={actions.reply.isPending} disabled={!text.trim()}>Reply</Button></div>
            </form>
          )}
          <ConfirmDialog open={!!confirm} onOpenChange={(o) => !o && setConfirm(null)} destructive confirmLabel="Delete" title={confirm?.thread ? 'Delete this discussion?' : 'Delete this reply?'} description="This cannot be undone."
            onConfirm={() => {
              if (confirm.thread) actions.deleteThread.mutate(t.id, { onSuccess: () => { setConfirm(null); onBack(); toast.success('Deleted'); }, onError: fail });
              else actions.deleteReply.mutate(confirm.reply.id, { onSuccess: () => { setConfirm(null); toast.success('Deleted'); }, onError: fail });
            }} />
        </div>
      )}
    </QueryBoundary>
  );
}

export function DiscussionsTab({ subjectId }) {
  const [params, setParams] = useSearchParams();
  const threadId = params.get('thread');
  const [q, setQ] = useState('');
  const [debounced, setDebounced] = useState('');
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const actions = useCommunityActions(subjectId);
  const list = useThreads(subjectId, debounced, page);
  useEffect(() => { const t = setTimeout(() => { setDebounced(q.trim()); setPage(0); }, 300); return () => clearTimeout(t); }, [q]);
  const openThread = (id) => setParams((p) => { const n = new URLSearchParams(p); n.set('tab', 'discussions'); n.set('thread', id); return n; });
  const back = () => setParams((p) => { const n = new URLSearchParams(p); n.delete('thread'); return n; });

  if (threadId) return <ThreadView id={threadId} subjectId={subjectId} onBack={back} actions={actions} />;
  return (
    <div>
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1"><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" /><Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search discussions" aria-label="Search discussions" className="pl-9" /></div>
        <Button onClick={() => setOpen(true)}><Plus className="h-4 w-4" /> New discussion</Button>
      </div>
      <QueryBoundary query={list} skeleton={<ListSkeleton />} isEmpty={(d) => d.content.length === 0}
        empty={<EmptyState icon={MessageSquare} title={debounced ? 'No discussions match your search' : 'No discussions yet'} description="Ask the first question. Your teacher and classmates will see it." action={!debounced && <Button onClick={() => setOpen(true)}>Start a discussion</Button>} />}>
        {(d) => (
          <>
            <ul className="space-y-2">
              {d.content.map((t) => (
                <li key={t.id}>
                  <button onClick={() => openThread(t.id)} className="flex w-full items-start gap-3 rounded-2xl border bg-card p-4 text-left shadow-card transition-all hover:-translate-y-0.5 hover:shadow-pop">
                    <Avatar name={t.authorName} />
                    <span className="min-w-0 flex-1">
                      <span className="flex flex-wrap items-center gap-2">{t.unread && <span className="h-2 w-2 rounded-full bg-primary" aria-label="Unread" />}<span className={`break-words ${t.unread ? 'font-bold' : 'font-semibold'}`}>{t.title}</span>
                        {t.pinned && <Badge variant="warning"><Pin className="h-3 w-3" />Pinned</Badge>}{t.closed && <Badge variant="muted"><Lock className="h-3 w-3" />Closed</Badge>}</span>
                      <span className="mt-0.5 line-clamp-1 block text-sm text-muted-foreground">{t.preview}</span>
                      <span className="mt-1 block text-xs text-muted-foreground">{t.authorName} · {t.replyCount} {t.replyCount === 1 ? 'reply' : 'replies'} · active {timeAgo(t.lastActivityAt)}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            <Pagination page={d.page} totalPages={d.totalPages} onChange={setPage} />
          </>
        )}
      </QueryBoundary>
      <NewThreadDialog open={open} onOpenChange={setOpen} actions={actions} onCreated={openThread} />
    </div>
  );
}
