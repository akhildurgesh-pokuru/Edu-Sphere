import { Link, Navigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { BarChart3, BookOpenCheck, GraduationCap, MessagesSquare, PlayCircle, ShieldCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Logo } from '@/components/ui/logo';
import { useAuth, homePathFor } from '@/features/auth/AuthContext';

const FEATURES = [
  { icon: PlayCircle, title: 'Video lessons that resume', text: 'Pick up exactly where you stopped, with playback speed control and notes.' },
  { icon: BookOpenCheck, title: 'Quizzes and assignments', text: 'Timed quizzes graded on the server, plus file assignments with feedback.' },
  { icon: MessagesSquare, title: 'Discussions per course', text: 'Ask questions and get answers from your teacher and classmates.' },
  { icon: BarChart3, title: 'Real progress and analytics', text: 'Every number comes from stored activity, for students and teachers.' },
  { icon: GraduationCap, title: 'Teach your own course', text: 'Build modules, upload videos and PDFs, then publish to one shared catalog.' },
  { icon: ShieldCheck, title: 'Locked until you enroll', text: 'Lessons, files and quizzes are protected on the server, not just hidden.' },
];

export default function LandingPage() {
  const { user, loading } = useAuth();
  if (!loading && user) return <Navigate to={homePathFor(user)} replace />;
  return (
    <div className="min-h-screen">
      <header className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6"><Logo /><nav className="flex items-center gap-2" aria-label="Account"><Button asChild variant="ghost"><Link to="/login">Sign in</Link></Button><Button asChild><Link to="/register">Get started</Link></Button></nav></header>
      <main id="main-content">
        <section className="relative overflow-hidden">
          <div className="absolute inset-x-0 -top-40 -z-10 h-[32rem] bg-gradient-to-b from-indigo-500/15 via-violet-500/10 to-transparent" aria-hidden="true" />
          <div className="mx-auto max-w-4xl px-4 py-20 text-center sm:py-28">
            <motion.h1 initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }} className="text-4xl font-extrabold tracking-tight sm:text-6xl">Learn anything. <span className="bg-gradient-to-r from-indigo-600 to-violet-500 bg-clip-text text-transparent">Teach everything.</span></motion.h1>
            <motion.p initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }} className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground">EduSphere is an e-learning platform where teachers publish courses and students browse one shared catalog and join in a click.</motion.p>
            <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.2 }} className="mt-8 flex flex-wrap justify-center gap-3"><Button asChild size="lg"><Link to="/register">Start learning</Link></Button><Button asChild size="lg" variant="outline"><Link to="/register">Become a teacher</Link></Button></motion.div>
          </div>
        </section>
        <section className="mx-auto max-w-6xl px-4 pb-24 sm:px-6" aria-labelledby="features-heading">
          <h2 id="features-heading" className="sr-only">Features</h2>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map((f, i) => { const Icon = f.icon; return (
              <motion.div key={f.title} initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ delay: i * 0.05 }} className="rounded-2xl border bg-card p-6 shadow-card transition-all hover:-translate-y-1 hover:shadow-pop">
                <span className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary"><Icon className="h-6 w-6" aria-hidden="true" /></span><h3 className="font-semibold">{f.title}</h3><p className="mt-1 text-sm text-muted-foreground">{f.text}</p></motion.div>); })}
          </div>
        </section>
      </main>
      <footer className="border-t py-8 text-center text-sm text-muted-foreground">© {new Date().getFullYear()} EduSphere. Built for a college demonstration.</footer>
    </div>
  );
}
