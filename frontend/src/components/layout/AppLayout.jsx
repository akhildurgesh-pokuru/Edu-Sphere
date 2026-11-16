import { useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { WifiOff } from 'lucide-react';
import { useAuth } from '@/features/auth/AuthContext';
import { AppSidebar } from './AppSidebar';
import { TopNavbar } from './TopNavbar';
import { useOnlineStatus } from './useTheme';

export function AppLayout() {
  const { user } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const online = useOnlineStatus();
  const { pathname } = useLocation();

  useEffect(() => { setMenuOpen(false); window.scrollTo({ top: 0 }); }, [pathname]);

  return (
    <div className="min-h-screen">
      <a href="#main-content" className="skip-link">Skip to content</a>
      <AppSidebar role={user?.role} open={menuOpen} onClose={() => setMenuOpen(false)} />
      <div className="lg:pl-64">
        <TopNavbar onMenu={() => setMenuOpen(true)} />
        {!online && (
          <div role="status" className="flex items-center justify-center gap-2 bg-amber-500/15 px-4 py-2 text-sm font-medium text-amber-800 dark:text-amber-200">
            <WifiOff className="h-4 w-4" aria-hidden="true" /> You are offline. Changes will not be saved until you reconnect.
          </div>
        )}
        <main id="main-content" tabIndex={-1} className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
