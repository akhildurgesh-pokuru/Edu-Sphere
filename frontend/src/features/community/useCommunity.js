import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

const get = (url, params) => api.get(url, { params }).then((r) => r.data);
export const useThreads = (subjectId, q, page) => useQuery({ queryKey: ['community', 'threads', subjectId, q, page], queryFn: () => get(`/subjects/${subjectId}/discussions`, { q: q || undefined, page }), enabled: !!subjectId, placeholderData: (p) => p });
export const useThread = (id) => useQuery({ queryKey: ['community', 'thread', id], queryFn: () => get(`/discussions/${id}`), enabled: !!id });
export const useAnnouncements = (subjectId) => useQuery({ queryKey: ['community', 'announcements', subjectId], queryFn: () => get(`/subjects/${subjectId}/announcements`), enabled: !!subjectId });
export const useStudentAnnouncements = () => useQuery({ queryKey: ['community', 'announcements', 'student'], queryFn: () => get('/student/announcements') });

export function useCommunityActions(subjectId) {
  const qc = useQueryClient();
  const refresh = () => qc.invalidateQueries({ queryKey: ['community'] });
  const o = { onSuccess: refresh };
  return {
    createThread: useMutation({ mutationFn: (b) => api.post(`/subjects/${subjectId}/discussions`, b).then((r) => r.data), ...o }),
    reply: useMutation({ mutationFn: ({ id, body }) => api.post(`/discussions/${id}/replies`, { body }).then((r) => r.data), ...o }),
    deleteThread: useMutation({ mutationFn: (id) => api.delete(`/discussions/${id}`), ...o }),
    deleteReply: useMutation({ mutationFn: (id) => api.delete(`/replies/${id}`), ...o }),
    pin: useMutation({ mutationFn: ({ id, value }) => api.post(`/teacher/discussions/${id}/pin`, { value }), ...o }),
    close: useMutation({ mutationFn: ({ id, value }) => api.post(`/teacher/discussions/${id}/close`, { value }), ...o }),
    createAnnouncement: useMutation({ mutationFn: (b) => api.post(`/teacher/subjects/${subjectId}/announcements`, b).then((r) => r.data), ...o }),
    updateAnnouncement: useMutation({ mutationFn: ({ id, ...b }) => api.put(`/teacher/announcements/${id}`, b).then((r) => r.data), ...o }),
    deleteAnnouncement: useMutation({ mutationFn: (id) => api.delete(`/teacher/announcements/${id}`), ...o }),
  };
}
