import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

const get = (url) => api.get(url).then((r) => r.data);

export const useTeacherQuizzes = (subjectId) => useQuery({ queryKey: ['quizzes', 'teacher', 'list', subjectId], queryFn: () => get(`/teacher/subjects/${subjectId}/quizzes`), enabled: !!subjectId });
export const useTeacherQuiz = (id) => useQuery({ queryKey: ['quizzes', 'teacher', 'one', id], queryFn: () => get(`/teacher/quizzes/${id}`), enabled: !!id });
export const useQuizAttempts = (id) => useQuery({ queryKey: ['quizzes', 'teacher', 'attempts', id], queryFn: () => get(`/teacher/quizzes/${id}/attempts`), enabled: !!id });
export const useTeacherAttempt = (id) => useQuery({ queryKey: ['quizzes', 'teacher', 'attempt', id], queryFn: () => get(`/teacher/attempts/${id}`), enabled: !!id });
export const useStudentQuizzes = (subjectId, enabled = true) => useQuery({ queryKey: ['quizzes', 'student', 'list', subjectId], queryFn: () => get(`/student/subjects/${subjectId}/quizzes`), enabled: !!subjectId && enabled });
export const useStudentAttempt = (id) => useQuery({ queryKey: ['quizzes', 'student', 'attempt', id], queryFn: () => get(`/student/attempts/${id}`), enabled: !!id, staleTime: 0, refetchOnWindowFocus: false, retry: false });

function useRefresh() {
  const qc = useQueryClient();
  return () => Promise.all([qc.invalidateQueries({ queryKey: ['quizzes'] }), qc.invalidateQueries({ queryKey: ['student', 'progress'] }), qc.invalidateQueries({ queryKey: ['teacher', 'dashboard'] }), qc.invalidateQueries({ queryKey: ['notifications'] })]);
}

export function useQuizActions(subjectId) {
  const refresh = useRefresh();
  const o = { onSuccess: refresh };
  return {
    create: useMutation({ mutationFn: (b) => api.post(`/teacher/subjects/${subjectId}/quizzes`, b).then((r) => r.data), ...o }),
    update: useMutation({ mutationFn: ({ id, ...b }) => api.put(`/teacher/quizzes/${id}`, b).then((r) => r.data), ...o }),
    remove: useMutation({ mutationFn: (id) => api.delete(`/teacher/quizzes/${id}`), ...o }),
    publish: useMutation({ mutationFn: ({ id, published }) => api.post(`/teacher/quizzes/${id}/publish`, { published }).then((r) => r.data), ...o }),
    addQuestion: useMutation({ mutationFn: ({ quizId, ...b }) => api.post(`/teacher/quizzes/${quizId}/questions`, b).then((r) => r.data), ...o }),
    updateQuestion: useMutation({ mutationFn: ({ id, ...b }) => api.put(`/teacher/questions/${id}`, b).then((r) => r.data), ...o }),
    deleteQuestion: useMutation({ mutationFn: (id) => api.delete(`/teacher/questions/${id}`), ...o }),
    grade: useMutation({ mutationFn: ({ attemptId, grades }) => api.post(`/teacher/attempts/${attemptId}/grade`, { grades }).then((r) => r.data), ...o }),
  };
}

export function useAttemptActions() {
  const refresh = useRefresh();
  return {
    start: useMutation({ mutationFn: (quizId) => api.post(`/student/quizzes/${quizId}/start`).then((r) => r.data), onSuccess: refresh }),
    save: useMutation({ mutationFn: ({ attemptId, answers }) => api.put(`/student/attempts/${attemptId}/answers`, { answers }).then((r) => r.data) }),
    submit: useMutation({ mutationFn: ({ attemptId, answers }) => api.post(`/student/attempts/${attemptId}/submit`, { answers }).then((r) => r.data), onSuccess: refresh }),
  };
}

export async function downloadQuizCsv(quizId) {
  const res = await api.get(`/teacher/quizzes/${quizId}/export.csv`, { responseType: 'blob' });
  const url = URL.createObjectURL(res.data);
  const a = document.createElement('a');
  a.href = url;
  a.download = `quiz-${quizId}-results.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
