import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RegisterPage from '@/features/auth/RegisterPage';

const register = vi.fn();
let behavior = async () => ({ name: 'Ada', role: 'STUDENT' });
let providers = { google: false, teacherSignup: true };
vi.mock('@/features/auth/AuthContext', () => ({ useAuth: () => ({ register: async (...a) => { register(...a); return behavior(...a); } }), homePathFor: () => '/student/dashboard' }));
vi.mock('@/features/auth/useProviders', () => ({ useProviders: () => ({ data: providers }) }));
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const setup = () => render(<MemoryRouter><RegisterPage /></MemoryRouter>);
beforeEach(() => { register.mockReset(); behavior = async () => ({ name: 'Ada', role: 'STUDENT' }); providers = { google: false, teacherSignup: true }; });

describe('RegisterPage', () => {
  it('offers Learn and Teach and defaults to Learn', () => {
    setup();
    expect(screen.getByRole('radio', { name: /learn/i })).toBeChecked();
    expect(screen.getByRole('radio', { name: /teach/i })).not.toBeChecked();
  });

  it('disables Teach when teacher sign-up is turned off', () => {
    providers = { google: false, teacherSignup: false };
    setup();
    expect(screen.getByRole('radio', { name: /teach/i })).toBeDisabled();
  });

  it('validates passwords before sending', async () => {
    setup();
    await userEvent.type(screen.getByLabelText(/full name/i), 'Ada');
    await userEvent.type(screen.getByLabelText(/^email/i), 'ada@example.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'short');
    await userEvent.type(screen.getByLabelText(/confirm password/i), 'other');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));
    expect(await screen.findByText('Use at least 8 characters')).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
  });

  it('registers a teacher when Teach is chosen', async () => {
    setup();
    await userEvent.click(screen.getByRole('radio', { name: /teach/i }));
    await userEvent.type(screen.getByLabelText(/full name/i), 'Ada Lovelace');
    await userEvent.type(screen.getByLabelText(/^email/i), 'ada@example.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'Passw0rdX');
    await userEvent.type(screen.getByLabelText(/confirm password/i), 'Passw0rdX');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(register).toHaveBeenCalled());
    expect(register.mock.calls[0][0]).toMatchObject({ role: 'TEACHER', email: 'ada@example.com' });
  });

  it('maps backend field errors onto the form', async () => {
    behavior = async () => { throw { response: { status: 409, data: { message: 'An account with this email already exists' } } }; };
    setup();
    await userEvent.type(screen.getByLabelText(/full name/i), 'Ada Lovelace');
    await userEvent.type(screen.getByLabelText(/^email/i), 'ada@example.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'Passw0rdX');
    await userEvent.type(screen.getByLabelText(/confirm password/i), 'Passw0rdX');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('already exists');
  });
});
