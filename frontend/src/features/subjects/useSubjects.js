import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

const get = (url, params) => api.get(url, { params }).then((r) => r.data);

// ---------- teacher ----------
export const useTeacherSubjects = () => useQuery({ queryKey: ['teacher', 'subjects'], queryFn: () => get('/teacher/subjects') });
export const useTeacherSubject = (id) => useQuery({ queryKey: ['teacher', 'subject', id], queryFn: () => get(`/teacher/subjects/${id}`), enabled: !!id });
export const useSubjectStudents = (id) => useQuery({ queryKey: ['teacher', 'subject', id, 'students'], queryFn: () => get(`/teacher/subjects/${id}/students`), enabled: !!id });

function useInvalidate() {
  const qc = useQueryClient();
  return () => Promise.all([qc.invalidateQueries({ queryKey: ['teacher'] }), qc.invalidateQueries({ queryKey: ['student'] })]);
}

export function useCreateSubject() {
  const inv = useInvalidate();
  return useMutation({ mutationFn: (body) => api.post('/teacher/subjects', body).then((r) => r.data), onSuccess: inv });
}

export function useUpdateSubject(id) {
  const inv = useInvalidate();
  return useMutation({ mutationFn: (body) => api.put(`/teacher/subjects/${id}`, body).then((r) => r.data), onSuccess: inv });
}

export function useSetStatus(id) {
  const inv = useInvalidate();
  return useMutation({ mutationFn: (status) => api.post(`/teacher/subjects/${id}/status`, { status }).then((r) => r.data), onSuccess: inv });
}

export function useUploadCover(id) {
  const inv = useInvalidate();
  return useMutation({
    mutationFn: ({ file, onProgress }) => {
      const form = new FormData();
      form.append('file', file);
      return api.post(`/teacher/subjects/${id}/cover`, form, { onUploadProgress: (e) => onProgress?.(e.total ? Math.round((e.loaded * 100) / e.total) : 0) }).then((r) => r.data);
    },
    onSuccess: inv,
  });
}

export function useRemoveStudent(subjectId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (studentId) => api.delete(`/teacher/subjects/${subjectId}/students/${studentId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['teacher'] }),
  });
}

// ---------- student ----------
export const useCatalog = ({ q, category, page }) =>
  useQuery({ queryKey: ['student', 'catalog', q, category, page], queryFn: () => get('/student/catalog', { q: q || undefined, category: category || undefined, page }), placeholderData: (p) => p });
export const useCategories = () => useQuery({ queryKey: ['student', 'categories'], queryFn: () => get('/student/categories'), staleTime: 60000 });
export const useStudentSubjects = () => useQuery({ queryKey: ['student', 'subjects'], queryFn: () => get('/student/subjects') });
export const useStudentSubject = (id) => useQuery({ queryKey: ['student', 'subject', id], queryFn: () => get(`/student/subjects/${id}`), enabled: !!id });

export function useEnroll(id) {
  const inv = useInvalidate();
  return useMutation({ mutationFn: () => api.post(`/student/subjects/${id}/enroll`).then((r) => r.data), onSuccess: inv });
}

export function useLeave(id) {
  const inv = useInvalidate();
  return useMutation({ mutationFn: () => api.post(`/student/subjects/${id}/leave`), onSuccess: inv });
}
