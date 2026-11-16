import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LoginPage from '@/features/auth/LoginPage';

// A plain async wrapper keeps rejected promises from being tracked (and reported) by the spy itself.
const login = vi.fn();
let behavior = async () => ({ name: 'Ada', role: 'STUDENT' });
vi.mock('@/features/auth/AuthContext', () => ({
  useAuth: () => ({ login: async (v) => { login(v); return behavior(v); } }),
  homePathFor: () => '/student/dashboard',
}));
vi.mock('@/features/auth/useProviders', () => ({ useProviders: () => ({ data: { google: false, teacherSignup: true } }) }));
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const setup = () => render(<MemoryRouter><LoginPage /></MemoryRouter>);

beforeEach(() => { login.mockReset(); behavior = async () => ({ name: 'Ada', role: 'STUDENT' }); });

describe('LoginPage', () => {
  it('shows validation errors and does not call the API for empty input', async () => {
    setup();
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it('submits valid credentials', async () => {
    setup();
    await userEvent.type(screen.getByLabelText(/email/i), 'ada@example.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'Passw0rdX');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    await waitFor(() => expect(login).toHaveBeenCalledWith({ email: 'ada@example.com', password: 'Passw0rdX' }));
  });

  it('shows the server error message on failure', async () => {
    behavior = async () => { throw { response: { status: 401, data: { message: 'Invalid email or password' } } }; };
    setup();
    await userEvent.type(screen.getByLabelText(/email/i), 'ada@example.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'wrongpass1');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
  });

  it('can reveal the password', async () => {
    setup();
    const input = screen.getByLabelText(/^password/i);
    expect(input).toHaveAttribute('type', 'password');
    await userEvent.click(screen.getByRole('button', { name: /show password/i }));
    expect(input).toHaveAttribute('type', 'text');
  });
});
