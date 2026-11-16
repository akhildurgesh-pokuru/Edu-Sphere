import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { LogOut, Menu, Moon, Sun, User } from 'lucide-react';
import { Avatar } from '@/components/ui/misc';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { NotificationBell } from '@/features/notifications/NotificationBell';
import { useAuth } from '@/features/auth/AuthContext';
import { SearchBox } from './SearchBox';
import { useTheme } from './useTheme';

export function TopNavbar({ onMenu }) {
  const { user, logout } = useAuth();
  const { dark, toggle } = useTheme();
  const navigate = useNavigate();

  const signOut = async () => {
    try {
      await logout();
      navigate('/login', { replace: true });
    } catch {
      toast.error('Could not sign out. Please try again.');
    }
  };

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b bg-background/80 px-4 backdrop-blur-md sm:px-6">
      <button onClick={onMenu} className="rounded-lg p-2 hover:bg-muted lg:hidden" aria-label="Open menu"><Menu className="h-5 w-5" /></button>
      <SearchBox role={user?.role} />
      <div className="ml-auto flex items-center gap-1">
        <button onClick={toggle} className="rounded-lg p-2 hover:bg-muted" aria-label={dark ? 'Switch to light mode' : 'Switch to dark mode'}>
          {dark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
        </button>
        <NotificationBell />
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="ml-1 flex items-center gap-2 rounded-lg p-1 pr-2 hover:bg-muted" aria-label="Account menu">
              <Avatar name={user?.name} />
              <span className="hidden max-w-[10rem] truncate text-sm font-medium md:block">{user?.name}</span>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuLabel>
              <span className="block truncate text-sm font-semibold text-foreground">{user?.name}</span>
              <span className="block truncate font-normal">{user?.email}</span>
              <span className="mt-1 inline-block rounded-full bg-secondary px-2 py-0.5 text-[10px] font-bold uppercase text-secondary-foreground">{user?.role}</span>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild><Link to="/profile"><User className="h-4 w-4" /> Profile</Link></DropdownMenuItem>
            <DropdownMenuItem destructive onSelect={signOut}><LogOut className="h-4 w-4" /> Sign out</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
