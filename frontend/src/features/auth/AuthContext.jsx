import { createContext, useCallback, useContext, useEffect, useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, parseError } from '@/lib/api';

const AuthContext = createContext(null);

// Drop cached data of the previous account but keep the ['me'] query alive, otherwise its observer is orphaned.
function clearOtherData(qc) {
  qc.removeQueries({ predicate: (q) => q.queryKey[0] !== 'me' });
}

async function fetchMe() {
  try {
    const { data } = await api.get('/auth/me');
    return data;
  } catch (err) {
    if (parseError(err).kind === 'unauthorized') return null; // not signed in is a normal state
    throw err;
  }
}

export function AuthProvider({ children }) {
  const qc = useQueryClient();
  const me = useQuery({ queryKey: ['me'], queryFn: fetchMe, retry: false, staleTime: 5 * 60 * 1000 });

  useEffect(() => {
    const onExpired = () => {
      qc.setQueryData(['me'], null);
    };
    window.addEventListener('edusphere:unauthorized', onExpired);
    return () => window.removeEventListener('edusphere:unauthorized', onExpired);
  }, [qc]);

  const login = useCallback(async (values) => {
    const { data } = await api.post('/auth/login', values);
    clearOtherData(qc);
    qc.setQueryData(['me'], data);
    return data;
  }, [qc]);

  const register = useCallback(async (values, inviteToken) => {
    const body = { name: values.name, email: values.email, password: values.password, role: values.role };
    const url = inviteToken ? `/auth/register-teacher?token=${encodeURIComponent(inviteToken)}` : '/auth/register';
    const { data } = await api.post(url, body);
    clearOtherData(qc);
    qc.setQueryData(['me'], data);
    return data;
  }, [qc]);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } finally {
      clearOtherData(qc);
      qc.setQueryData(['me'], null);
    }
  }, [qc]);

  const value = useMemo(
    () => ({
      user: me.data ?? null,
      loading: me.isPending,
      error: me.isError ? me.error : null,
      refetch: me.refetch,
      login,
      register,
      logout,
      setUser: (u) => qc.setQueryData(['me'], u),
    }),
    [me.data, me.isPending, me.isError, me.error, me.refetch, login, register, logout, qc],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}

export function homePathFor(user) {
  if (!user) return '/login';
  return user.role === 'TEACHER' ? '/teacher/dashboard' : '/student/dashboard';
}
