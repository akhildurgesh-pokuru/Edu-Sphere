import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

const get = (url) => api.get(url).then((r) => r.data);

export const useTeacherCurriculum = (subjectId) => useQuery({ queryKey: ['curriculum', 'teacher', subjectId], queryFn: () => get(`/teacher/subjects/${subjectId}/curriculum`), enabled: !!subjectId });
export const useStudentCurriculum = (subjectId) => useQuery({ queryKey: ['curriculum', 'student', subjectId], queryFn: () => get(`/student/subjects/${subjectId}/curriculum`), enabled: !!subjectId });
export const useLesson = (id) => useQuery({ queryKey: ['lesson', id], queryFn: () => get(`/lessons/${id}`), enabled: !!id });

function useRefresh() {
  const qc = useQueryClient();
  return () => Promise.all([
    qc.invalidateQueries({ queryKey: ['curriculum'] }),
    qc.invalidateQueries({ queryKey: ['lesson'] }),
    qc.invalidateQueries({ queryKey: ['teacher'] }),
    qc.invalidateQueries({ queryKey: ['student'] }),
  ]);
}

export function useModuleActions(subjectId) {
  const refresh = useRefresh();
  const opts = { onSuccess: refresh };
  return {
    create: useMutation({ mutationFn: (b) => api.post(`/teacher/subjects/${subjectId}/modules`, b).then((r) => r.data), ...opts }),
    update: useMutation({ mutationFn: ({ id, ...b }) => api.put(`/teacher/modules/${id}`, b).then((r) => r.data), ...opts }),
    remove: useMutation({ mutationFn: (id) => api.delete(`/teacher/modules/${id}`), ...opts }),
    reorder: useMutation({ mutationFn: (ids) => api.post(`/teacher/subjects/${subjectId}/modules/reorder`, { ids }), ...opts }),
  };
}

export function useLessonActions() {
  const refresh = useRefresh();
  const opts = { onSuccess: refresh };
  return {
    create: useMutation({ mutationFn: ({ moduleId, ...b }) => api.post(`/teacher/modules/${moduleId}/lessons`, b).then((r) => r.data), ...opts }),
    update: useMutation({ mutationFn: ({ id, ...b }) => api.put(`/teacher/lessons/${id}`, b).then((r) => r.data), ...opts }),
    remove: useMutation({ mutationFn: (id) => api.delete(`/teacher/lessons/${id}`), ...opts }),
    reorder: useMutation({ mutationFn: ({ moduleId, ids }) => api.post(`/teacher/modules/${moduleId}/lessons/reorder`, { ids }), ...opts }),
  };
}

/** Multipart upload with a progress callback (0-100). */
export function uploadResource(subjectId, file, onProgress, signal) {
  const form = new FormData();
  form.append('file', file);
  return api.post(`/teacher/subjects/${subjectId}/resources`, form, {
    signal,
    onUploadProgress: (e) => onProgress?.(e.total ? Math.round((e.loaded * 100) / e.total) : 0),
  }).then((r) => r.data);
}
