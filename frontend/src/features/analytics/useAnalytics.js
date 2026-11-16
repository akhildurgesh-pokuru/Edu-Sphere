import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';

const get = (url) => api.get(url).then((r) => r.data);
export const useTeacherDashboard = () => useQuery({ queryKey: ['teacher', 'dashboard'], queryFn: () => get('/teacher/dashboard') });
export const useSubjectAnalytics = (id) => useQuery({ queryKey: ['teacher', 'analytics', id], queryFn: () => get(`/teacher/subjects/${id}/analytics`), enabled: !!id });
export const useStudentDashboard = () => useQuery({ queryKey: ['student', 'dashboard'], queryFn: () => get('/student/dashboard') });
export const useStudentProgress = () => useQuery({ queryKey: ['student', 'progress'], queryFn: () => get('/student/progress') });
