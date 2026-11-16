import { Suspense, lazy } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute, PublicOnlyRoute, RoleBasedRoute } from '@/components/ProtectedRoute';
import { Skeleton } from '@/components/ui/misc';
import { homePathFor, useAuth } from '@/features/auth/AuthContext';
import LandingPage from '@/pages/LandingPage';
import { NotFoundPage, UnauthorizedPage } from '@/pages/SimplePages';

const LoginPage = lazy(() => import('@/features/auth/LoginPage'));
const RegisterPage = lazy(() => import('@/features/auth/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('@/features/auth/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('@/features/auth/ResetPasswordPage'));
const ProfilePage = lazy(() => import('@/features/auth/ProfilePage'));
const NotificationsPage = lazy(() => import('@/features/notifications/NotificationsPage'));
const StudentDashboardPage = lazy(() => import('@/features/analytics/StudentDashboardPage'));
const StudentProgressPage = lazy(() => import('@/features/analytics/StudentProgressPage'));
const TeacherDashboardPage = lazy(() => import('@/features/analytics/TeacherDashboardPage'));
const CatalogPage = lazy(() => import('@/features/subjects/CatalogPage'));
const StudentSubjectsPage = lazy(() => import('@/features/subjects/StudentSubjectsPage'));
const CourseDetailPage = lazy(() => import('@/features/learning/CourseDetailPage'));
const PlayerPage = lazy(() => import('@/features/learning/PlayerPage'));
const QuizTakePage = lazy(() => import('@/features/quizzes/QuizTakePage'));
const TeacherSubjectsPage = lazy(() => import('@/features/subjects/TeacherSubjectsPage'));
const SubjectFormPage = lazy(() => import('@/features/subjects/SubjectFormPage'));
const SubjectManagePage = lazy(() => import('@/features/subjects/SubjectManagePage'));
const QuizBuilderPage = lazy(() => import('@/features/quizzes/QuizBuilderPage'));
const QuizAttemptsPage = lazy(() => import('@/features/quizzes/QuizAttemptsPage'));
const AttemptReviewPage = lazy(() => import('@/features/quizzes/AttemptReviewPage'));
const SubmissionsPage = lazy(() => import('@/features/assignments/SubmissionsPage'));

function PageFallback() {
  return <div className="space-y-3 p-6" aria-busy="true" aria-label="Loading"><Skeleton className="h-10 w-1/3" /><Skeleton className="h-40 w-full" /><Skeleton className="h-40 w-full" /></div>;
}

function DashboardRedirect() {
  const { user } = useAuth();
  return <Navigate to={homePathFor(user)} replace />;
}

export default function App() {
  return (
    <Suspense fallback={<PageFallback />}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/register/teacher" element={<RegisterPage invite />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
        </Route>
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<DashboardRedirect />} />
          <Route element={<AppLayout />}>
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route element={<RoleBasedRoute role="STUDENT" />}>
              <Route path="/student/dashboard" element={<StudentDashboardPage />} />
              <Route path="/student/catalog" element={<CatalogPage />} />
              <Route path="/student/subjects" element={<StudentSubjectsPage />} />
              <Route path="/student/subjects/:id" element={<CourseDetailPage />} />
              <Route path="/student/subjects/:id/learn" element={<PlayerPage />} />
              <Route path="/student/subjects/:id/learn/:lessonId" element={<PlayerPage />} />
              <Route path="/student/attempts/:attemptId" element={<QuizTakePage />} />
              <Route path="/student/progress" element={<StudentProgressPage />} />
            </Route>
            <Route element={<RoleBasedRoute role="TEACHER" />}>
              <Route path="/teacher/dashboard" element={<TeacherDashboardPage />} />
              <Route path="/teacher/subjects" element={<TeacherSubjectsPage />} />
              <Route path="/teacher/subjects/new" element={<SubjectFormPage />} />
              <Route path="/teacher/subjects/:id" element={<SubjectManagePage />} />
              <Route path="/teacher/subjects/:id/edit" element={<SubjectFormPage />} />
              <Route path="/teacher/quizzes/:quizId" element={<QuizBuilderPage />} />
              <Route path="/teacher/quizzes/:quizId/attempts" element={<QuizAttemptsPage />} />
              <Route path="/teacher/attempts/:attemptId" element={<AttemptReviewPage />} />
              <Route path="/teacher/assignments/:assignmentId/submissions" element={<SubmissionsPage />} />
            </Route>
          </Route>
        </Route>
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
