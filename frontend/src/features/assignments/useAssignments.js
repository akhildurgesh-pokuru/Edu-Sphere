import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

const get = (url) => api.get(url).then((r) => r.data);
export const useTeacherAssignments = (subjectId) => useQuery({ queryKey: ['assignments', 'teacher', subjectId], queryFn: () => get(`/teacher/subjects/${subjectId}/assignments`), enabled: !!subjectId });
export const useTeacherAssignment = (id) => useQuery({ queryKey: ['assignments', 'teacher', 'one', id], queryFn: () => get(`/teacher/assignments/${id}`), enabled: !!id });
export const useSubmissionRows = (id) => useQuery({ queryKey: ['assignments', 'submissions', id], queryFn: () => get(`/teacher/assignments/${id}/submissions`), enabled: !!id });
export const useStudentAssignments = (subjectId) => useQuery({ queryKey: ['assignments', 'student', subjectId], queryFn: () => get(`/student/subjects/${subjectId}/assignments`), enabled: !!subjectId });

function useRefresh() {
  const qc = useQueryClient();
  return () => Promise.all([qc.invalidateQueries({ queryKey: ['assignments'] }), qc.invalidateQueries({ queryKey: ['teacher', 'dashboard'] }), qc.invalidateQueries({ queryKey: ['student', 'progress'] }), qc.invalidateQueries({ queryKey: ['notifications'] })]);
}

const upload = (url, file, onProgress) => {
  const form = new FormData();
  form.append('file', file);
  return api.post(url, form, { onUploadProgress: (e) => onProgress?.(e.total ? Math.round((e.loaded * 100) / e.total) : 0) }).then((r) => r.data);
};

export function useAssignmentActions(subjectId) {
  const refresh = useRefresh();
  const o = { onSuccess: refresh };
  return {
    create: useMutation({ mutationFn: (b) => api.post(`/teacher/subjects/${subjectId}/assignments`, b).then((r) => r.data), ...o }),
    update: useMutation({ mutationFn: ({ id, ...b }) => api.put(`/teacher/assignments/${id}`, b).then((r) => r.data), ...o }),
    remove: useMutation({ mutationFn: (id) => api.delete(`/teacher/assignments/${id}`), ...o }),
    attach: useMutation({ mutationFn: ({ id, file, onProgress }) => upload(`/teacher/assignments/${id}/attachment`, file, onProgress), ...o }),
    grade: useMutation({ mutationFn: ({ id, marks, feedback }) => api.post(`/teacher/submissions/${id}/grade`, { marks, feedback }).then((r) => r.data), ...o }),
  };
}

export function useSubmit() {
  const refresh = useRefresh();
  return useMutation({ mutationFn: ({ id, file, onProgress }) => upload(`/student/assignments/${id}/submit`, file, onProgress), onSuccess: refresh });
}
