import { NavLink } from 'react-router-dom';
import { X } from 'lucide-react';
import { Logo } from '@/components/ui/logo';
import { Badge } from '@/components/ui/misc';
import { cn } from '@/lib/utils';
import { navFor } from './navItems';

function NavList({ role, onNavigate }) {
  return (
    <nav aria-label="Main" className="flex-1 space-y-6 overflow-y-auto px-3 py-4">
      {navFor(role).map((section) => (
        <div key={section.group}>
          <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{section.group}</p>
          <ul className="space-y-1">
            {section.items.map((item) => {
              const Icon = item.icon;
              if (!item.ready) {
                return (
                  <li key={item.to}>
                    <span aria-disabled="true" className="flex cursor-not-allowed items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground/60">
                      <Icon className="h-5 w-5" aria-hidden="true" /> {item.label}
                      <Badge variant="muted" className="ml-auto">Soon</Badge>
                    </span>
                  </li>
                );
              }
              return (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.to.endsWith('/subjects') || item.to.endsWith('/dashboard')}
                    onClick={onNavigate}
                    className={({ isActive }) =>
                      cn('group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                        isActive ? 'bg-primary text-primary-foreground shadow-pop' : 'text-muted-foreground hover:bg-muted hover:text-foreground')
                    }
                  >
                    <Icon className="h-5 w-5" aria-hidden="true" /> {item.label}
                  </NavLink>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </nav>
  );
}

export function AppSidebar({ role, open, onClose }) {
  return (
    <>
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r bg-card lg:flex">
        <div className="flex h-16 items-center border-b px-5"><Logo to={role === 'TEACHER' ? '/teacher/dashboard' : '/student/dashboard'} /></div>
        <NavList role={role} />
        <div className="m-3 rounded-xl bg-gradient-to-br from-primary to-violet-500 p-4 text-primary-foreground">
          <p className="text-sm font-semibold">{role === 'TEACHER' ? 'Share your knowledge' : 'Keep learning'}</p>
          <p className="mt-1 text-xs opacity-90">{role === 'TEACHER' ? 'Publish a course and students can enroll in one click.' : 'Pick up where you left off and finish what you started.'}</p>
        </div>
      </aside>
      {open && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation menu">
          <div className="absolute inset-0 bg-slate-950/50 backdrop-blur-sm" onClick={onClose} aria-hidden="true" />
          <aside className="absolute inset-y-0 left-0 flex w-72 max-w-[85%] flex-col bg-card shadow-2xl">
            <div className="flex h-16 items-center justify-between border-b px-5">
              <Logo to={role === 'TEACHER' ? '/teacher/dashboard' : '/student/dashboard'} />
              <button onClick={onClose} className="rounded-md p-1.5 hover:bg-muted" aria-label="Close menu"><X className="h-5 w-5" /></button>
            </div>
            <NavList role={role} onNavigate={onClose} />
          </aside>
        </div>
      )}
    </>
  );
}
