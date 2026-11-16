import { describe, expect, it, vi } from 'vitest';
import { applyFieldErrors, parseError } from '@/lib/api';

const axiosError = (status, data) => ({ response: { status, data } });

describe('parseError', () => {
  it('detects network failures', () => {
    const p = parseError({ message: 'Network Error' });
    expect(p.kind).toBe('network');
    expect(p.message).toMatch(/Cannot reach the server/);
  });
  it('maps status codes to kinds', () => {
    expect(parseError(axiosError(401, {})).kind).toBe('unauthorized');
    expect(parseError(axiosError(403, {})).kind).toBe('forbidden');
    expect(parseError(axiosError(404, {})).kind).toBe('notfound');
    expect(parseError(axiosError(409, {})).kind).toBe('conflict');
    expect(parseError(axiosError(429, {})).kind).toBe('ratelimit');
    expect(parseError(axiosError(400, {})).kind).toBe('validation');
    expect(parseError(axiosError(500, {})).kind).toBe('server');
  });
  it('uses the backend message and field errors', () => {
    const p = parseError(axiosError(400, { message: 'Please correct the highlighted fields', fieldErrors: { email: 'Enter a valid email' } }));
    expect(p.message).toBe('Please correct the highlighted fields');
    expect(p.fieldErrors.email).toBe('Enter a valid email');
  });
  it('treats cancelled requests separately', () => {
    expect(parseError({ code: 'ERR_CANCELED' }).kind).toBe('canceled');
  });
});

describe('applyFieldErrors', () => {
  it('maps backend field errors onto the form', () => {
    const setError = vi.fn();
    const mapped = applyFieldErrors(axiosError(400, { fieldErrors: { email: 'Taken', name: 'Required' } }), setError);
    expect(mapped).toBe(true);
    expect(setError).toHaveBeenCalledWith('email', { type: 'server', message: 'Taken' });
    expect(setError).toHaveBeenCalledWith('name', { type: 'server', message: 'Required' });
  });
  it('returns false when there are no field errors', () => {
    expect(applyFieldErrors(axiosError(409, { message: 'x' }), vi.fn())).toBe(false);
  });
});
