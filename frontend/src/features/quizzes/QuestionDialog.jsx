import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { Plus, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Field } from '@/components/ui/label';
import { Input, Select, Textarea } from '@/components/ui/input';
import { parseError } from '@/lib/api';

const blank = () => ({ type: 'SINGLE_CHOICE', text: '', marks: 1, explanation: '', options: [{ text: '', correct: true }, { text: '', correct: false }], trueFalseAnswer: true });

export function QuestionDialog({ open, onOpenChange, question, onSave }) {
  const editing = !!question;
  const [q, setQ] = useState(blank());
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    if (!question) { setQ(blank()); return; }
    const tf = question.type === 'TRUE_FALSE';
    setQ({
      type: question.type, text: question.text, marks: question.marks, explanation: question.explanation || '',
      options: tf || question.type === 'SHORT_ANSWER' ? blank().options : question.options.map((o) => ({ text: o.text, correct: !!o.correct })),
      trueFalseAnswer: tf ? !!question.options.find((o) => o.text === 'True')?.correct : true,
    });
  }, [open, question]);

  const set = (patch) => setQ((s) => ({ ...s, ...patch }));
  const setOption = (i, patch) => setQ((s) => ({ ...s, options: s.options.map((o, j) => (j === i ? { ...o, ...patch } : o)) }));
  const markCorrect = (i) => setQ((s) => ({ ...s, options: s.options.map((o, j) => (s.type === 'MULTIPLE_SELECT' ? (j === i ? { ...o, correct: !o.correct } : o) : { ...o, correct: j === i }) ) }));

  const submit = async (e) => {
    e.preventDefault();
    const errs = {};
    if (!q.text.trim()) errs.text = 'Question text is required';
    if (!(Number(q.marks) >= 0.5)) errs.marks = 'At least 0.5 marks';
    const objective = q.type === 'SINGLE_CHOICE' || q.type === 'MULTIPLE_SELECT';
    if (objective) {
      if (q.options.some((o) => !o.text.trim())) errs.options = 'Fill in every option or remove the empty ones';
      else if (q.options.length < 2) errs.options = 'Add at least two options';
      else if (!q.options.some((o) => o.correct)) errs.options = 'Mark the correct answer';
    }
    setErrors(errs);
    if (Object.keys(errs).length) return;
    setBusy(true);
    try {
      await onSave({
        type: q.type, text: q.text, marks: Number(q.marks), explanation: q.explanation || null,
        options: objective ? q.options.map((o) => ({ text: o.text, correct: o.correct })) : null,
        trueFalseAnswer: q.type === 'TRUE_FALSE' ? q.trueFalseAnswer : null,
      });
      toast.success(editing ? 'Question updated' : 'Question added');
      onOpenChange(false);
    } catch (err) {
      setErrors({ form: parseError(err).message });
    } finally {
      setBusy(false);
    }
  };

  const multiple = q.type === 'MULTIPLE_SELECT';
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={editing ? 'Edit question' : 'Add a question'} className="max-w-xl">
        <form onSubmit={submit} noValidate className="space-y-4">
          {errors.form && <div role="alert" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">{errors.form}</div>}
          <div className="grid gap-4 sm:grid-cols-[1fr_8rem]">
            <Field id="q-type" label="Type">
              {(p) => (
                <Select {...p} value={q.type} onChange={(e) => set({ type: e.target.value, options: q.options.map((o, i) => ({ ...o, correct: e.target.value === 'SINGLE_CHOICE' ? i === 0 : o.correct })) })}>
                  <option value="SINGLE_CHOICE">Single choice</option><option value="MULTIPLE_SELECT">Multiple select</option><option value="TRUE_FALSE">True / False</option><option value="SHORT_ANSWER">Short answer (teacher graded)</option>
                </Select>
              )}
            </Field>
            <Field id="q-marks" label="Marks" error={errors.marks} required>{(p) => <Input {...p} type="number" min="0.5" step="0.5" value={q.marks} onChange={(e) => set({ marks: e.target.value })} />}</Field>
          </div>
          <Field id="q-text" label="Question" error={errors.text} required>{(p) => <Textarea {...p} rows={3} value={q.text} onChange={(e) => set({ text: e.target.value })} />}</Field>
          {(q.type === 'SINGLE_CHOICE' || multiple) && (
            <fieldset>
              <legend className="mb-1.5 text-sm font-medium">Options {multiple ? '(tick every correct answer)' : '(select the correct answer)'}</legend>
              <div className="space-y-2">
                {q.options.map((o, i) => (
                  <div key={i} className="flex items-center gap-2">
                    <input type={multiple ? 'checkbox' : 'radio'} name="correct" checked={o.correct} onChange={() => markCorrect(i)} aria-label={`Option ${i + 1} is correct`} className="h-4 w-4 accent-[hsl(var(--primary))]" />
                    <Input value={o.text} onChange={(e) => setOption(i, { text: e.target.value })} placeholder={`Option ${i + 1}`} aria-label={`Option ${i + 1} text`} />
                    <Button type="button" size="icon" variant="ghost" disabled={q.options.length <= 2} aria-label={`Remove option ${i + 1}`} onClick={() => setQ((s) => ({ ...s, options: s.options.filter((_, j) => j !== i) }))}><Trash2 className="h-4 w-4" /></Button>
                  </div>
                ))}
              </div>
              {errors.options && <p role="alert" className="mt-1 text-xs font-medium text-destructive">{errors.options}</p>}
              {q.options.length < 10 && <Button type="button" size="sm" variant="outline" className="mt-2" onClick={() => setQ((s) => ({ ...s, options: [...s.options, { text: '', correct: false }] }))}><Plus className="h-4 w-4" /> Add option</Button>}
            </fieldset>
          )}
          {q.type === 'TRUE_FALSE' && (
            <fieldset>
              <legend className="mb-1.5 text-sm font-medium">The statement is</legend>
              <div className="flex gap-4">{[true, false].map((v) => <label key={String(v)} className="flex items-center gap-2 text-sm"><input type="radio" name="tf" checked={q.trueFalseAnswer === v} onChange={() => set({ trueFalseAnswer: v })} className="h-4 w-4 accent-[hsl(var(--primary))]" /> {v ? 'True' : 'False'}</label>)}</div>
            </fieldset>
          )}
          {q.type === 'SHORT_ANSWER' && <p className="rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">Students type an answer and you award marks when you review their attempt.</p>}
          <Field id="q-expl" label="Explanation (shown when review is allowed)">{(p) => <Textarea {...p} rows={2} value={q.explanation} onChange={(e) => set({ explanation: e.target.value })} />}</Field>
          <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button><Button type="submit" loading={busy}>{editing ? 'Save question' : 'Add question'}</Button></div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
