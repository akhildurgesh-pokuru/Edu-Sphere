import { describe, expect, it } from 'vitest';
import { assignmentSchema, loginSchema, quizSchema, registerSchema, resetSchema, subjectSchema } from '@/lib/schemas';

const validRegister = { name: 'Ada Lovelace', email: 'ada@example.com', password: 'Passw0rdX', confirmPassword: 'Passw0rdX', role: 'STUDENT' };

describe('loginSchema', () => {
  it('accepts a valid login', () => {
    expect(loginSchema.safeParse({ email: 'a@b.co', password: 'x' }).success).toBe(true);
  });
  it('rejects a bad email and an empty password', () => {
    const r = loginSchema.safeParse({ email: 'nope', password: '' });
    expect(r.success).toBe(false);
    const fields = r.error.flatten().fieldErrors;
    expect(fields.email).toBeTruthy();
    expect(fields.password).toBeTruthy();
  });
});

describe('registerSchema', () => {
  it('accepts a valid student and teacher', () => {
    expect(registerSchema.safeParse(validRegister).success).toBe(true);
    expect(registerSchema.safeParse({ ...validRegister, role: 'TEACHER' }).success).toBe(true);
  });
  it('rejects weak passwords', () => {
    expect(registerSchema.safeParse({ ...validRegister, password: 'short1', confirmPassword: 'short1' }).success).toBe(false);
    expect(registerSchema.safeParse({ ...validRegister, password: 'onlyletters', confirmPassword: 'onlyletters' }).success).toBe(false);
    expect(registerSchema.safeParse({ ...validRegister, password: '12345678', confirmPassword: '12345678' }).success).toBe(false);
  });
  it('requires matching passwords', () => {
    const r = registerSchema.safeParse({ ...validRegister, confirmPassword: 'Different1' });
    expect(r.success).toBe(false);
    expect(r.error.flatten().fieldErrors.confirmPassword).toContain('Passwords do not match');
  });
  it('rejects an unknown role', () => {
    expect(registerSchema.safeParse({ ...validRegister, role: 'ADMIN' }).success).toBe(false);
  });
});

describe('resetSchema', () => {
  it('requires matching strong passwords', () => {
    expect(resetSchema.safeParse({ password: 'Passw0rdX', confirmPassword: 'Passw0rdX' }).success).toBe(true);
    expect(resetSchema.safeParse({ password: 'Passw0rdX', confirmPassword: 'nope' }).success).toBe(false);
  });
});

describe('subjectSchema', () => {
  it('requires a title and limits its length', () => {
    expect(subjectSchema.safeParse({ title: '  ' }).success).toBe(false);
    expect(subjectSchema.safeParse({ title: 'x'.repeat(201) }).success).toBe(false);
    expect(subjectSchema.safeParse({ title: 'Java Programming', description: '', category: '' }).success).toBe(true);
  });
});

describe('quizSchema', () => {
  const base = { title: 'Quiz', durationMinutes: 10, maxAttempts: 2, passingPercentage: 50 };
  it('coerces numbers from form strings', () => {
    const r = quizSchema.safeParse({ ...base, durationMinutes: '15', maxAttempts: '3', passingPercentage: '70' });
    expect(r.success).toBe(true);
    expect(r.data.durationMinutes).toBe(15);
  });
  it('enforces bounds', () => {
    expect(quizSchema.safeParse({ ...base, durationMinutes: 0 }).success).toBe(false);
    expect(quizSchema.safeParse({ ...base, passingPercentage: 101 }).success).toBe(false);
    expect(quizSchema.safeParse({ ...base, maxAttempts: 21 }).success).toBe(false);
  });
});

describe('assignmentSchema', () => {
  it('needs a due date and positive marks', () => {
    expect(assignmentSchema.safeParse({ title: 'HW', dueAt: '', maxMarks: 10 }).success).toBe(false);
    expect(assignmentSchema.safeParse({ title: 'HW', dueAt: '2030-01-01T10:00', maxMarks: 0 }).success).toBe(false);
    expect(assignmentSchema.safeParse({ title: 'HW', dueAt: '2030-01-01T10:00', maxMarks: 100 }).success).toBe(true);
  });
});
