import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth, homePathFor } from '@/features/auth/AuthContext';
import { ErrorState } from '@/components/ui/states';
import { Skeleton } from '@/components/ui/misc';

function FullPageLoading() {
  return (
    <div className="flex min-h-screen items-center justify-center p-6" role="status" aria-busy="true" aria-label="Loading">
      <div className="w-full max-w-sm space-y-3">
        <Skeleton className="h-10 w-1/2" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    </div>
  );
}

/** Convenience guard only: the backend re-checks every request. */
export function ProtectedRoute() {
  const { user, loading, error, refetch } = useAuth();
  const location = useLocation();
  if (loading) return <FullPageLoading />;
  if (error) return <div className="mx-auto max-w-lg p-6"><ErrorState error={error} onRetry={refetch} /></div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  return <Outlet />;
}

export function RoleBasedRoute({ role }) {
  const { user } = useAuth();
  if (user && user.role !== role) return <Navigate to="/unauthorized" replace />;
  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { user, loading } = useAuth();
  if (loading) return <FullPageLoading />;
  if (user) return <Navigate to={homePathFor(user)} replace />;
  return <Outlet />;
}
