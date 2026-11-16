import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { SubjectCard } from '@/features/subjects/SubjectCard';

const subject = { id: 7, title: 'Java Programming', teacherName: 'Demo Teacher', category: 'Programming', lessonCount: 3, enrollmentCount: 1, status: 'PUBLISHED', hasCover: false, enrolled: true, progressPercent: 40 };

describe('SubjectCard', () => {
  it('shows title, teacher, category and plural counts', () => {
    render(<MemoryRouter><SubjectCard subject={subject} to="/student/subjects/7" /></MemoryRouter>);
    expect(screen.getByRole('link', { name: 'Java Programming' })).toHaveAttribute('href', '/student/subjects/7');
    expect(screen.getByText('by Demo Teacher')).toBeInTheDocument();
    expect(screen.getByText('Programming')).toBeInTheDocument();
    expect(screen.getByText('3 lessons')).toBeInTheDocument();
    expect(screen.getByText('1 student')).toBeInTheDocument();
  });
  it('shows progress for enrolled students and the status for teachers', () => {
    render(<MemoryRouter><SubjectCard subject={subject} to="/x" showProgress showStatus action={<button>Open</button>} /></MemoryRouter>);
    expect(screen.getByRole('progressbar', { name: /java programming progress/i })).toHaveAttribute('aria-valuenow', '40');
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Open' })).toBeInTheDocument();
  });
});
