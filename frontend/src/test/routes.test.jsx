import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProtectedRoute, PublicOnlyRoute, RoleBasedRoute } from '@/components/ProtectedRoute';

let auth;
vi.mock('@/features/auth/AuthContext', () => ({
  useAuth: () => auth,
  homePathFor: (u) => (u?.role === 'TEACHER' ? '/teacher/dashboard' : '/student/dashboard'),
}));

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<div>login-page</div>} />
        <Route path="/unauthorized" element={<div>unauthorized-page</div>} />
        <Route path="/student/dashboard" element={<div>student-home</div>} />
        <Route element={<ProtectedRoute />}>
          <Route element={<RoleBasedRoute role="TEACHER" />}><Route path="/teacher/dashboard" element={<div>teacher-home</div>} /></Route>
        </Route>
        <Route element={<PublicOnlyRoute />}><Route path="/register" element={<div>register-page</div>} /></Route>
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => { auth = { user: null, loading: false, error: null, refetch: vi.fn() }; });

describe('route protection', () => {
  it('redirects anonymous visitors to login', () => {
    renderAt('/teacher/dashboard');
    expect(screen.getByText('login-page')).toBeInTheDocument();
  });
  it('shows a loading skeleton while the session is being checked', () => {
    auth = { ...auth, loading: true };
    renderAt('/teacher/dashboard');
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
    expect(screen.queryByText('login-page')).toBeNull();
  });
  it('lets the right role through', () => {
    auth = { ...auth, user: { role: 'TEACHER' } };
    renderAt('/teacher/dashboard');
    expect(screen.getByText('teacher-home')).toBeInTheDocument();
  });
  it('blocks the wrong role', () => {
    auth = { ...auth, user: { role: 'STUDENT' } };
    renderAt('/teacher/dashboard');
    expect(screen.getByText('unauthorized-page')).toBeInTheDocument();
  });
  it('shows an error state with retry when the session check fails', () => {
    auth = { ...auth, error: { message: 'Network Error' } };
    renderAt('/teacher/dashboard');
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });
  it('sends signed-in users away from public-only pages', () => {
    auth = { ...auth, user: { role: 'STUDENT' } };
    renderAt('/register');
    expect(screen.getByText('student-home')).toBeInTheDocument();
  });
});
