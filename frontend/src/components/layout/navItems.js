import {
  BarChart3, BookOpen, Bell, Compass, GraduationCap, LayoutDashboard, LibraryBig, PlusCircle, TrendingUp, User,
} from 'lucide-react';

// `ready: false` shows a "Soon" badge and disables the entry. Every module is shipped, so all entries are ready.
export const studentNav = [
  { group: 'Learn', items: [
    { to: '/student/dashboard', label: 'Dashboard', icon: LayoutDashboard, ready: true },
    { to: '/student/catalog', label: 'Browse Courses', icon: Compass, ready: true },
    { to: '/student/subjects', label: 'My Subjects', icon: BookOpen, ready: true },
    { to: '/student/progress', label: 'My Progress', icon: TrendingUp, ready: true },
  ] },
  { group: 'Account', items: [
    { to: '/notifications', label: 'Notifications', icon: Bell, ready: true },
    { to: '/profile', label: 'Profile', icon: User, ready: true },
  ] },
];

export const teacherNav = [
  { group: 'Teach', items: [
    { to: '/teacher/dashboard', label: 'Dashboard', icon: LayoutDashboard, ready: true },
    { to: '/teacher/subjects', label: 'My Subjects', icon: LibraryBig, ready: true },
    { to: '/teacher/subjects/new', label: 'Create Subject', icon: PlusCircle, ready: true },
  ] },
  { group: 'Account', items: [
    { to: '/notifications', label: 'Notifications', icon: Bell, ready: true },
    { to: '/profile', label: 'Profile', icon: User, ready: true },
  ] },
];

export const navFor = (role) => (role === 'TEACHER' ? teacherNav : studentNav);
export { BarChart3, GraduationCap };
