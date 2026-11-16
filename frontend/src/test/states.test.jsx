import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { EmptyState, ErrorState, QueryBoundary, StatCard } from '@/components/ui/states';
import { BookOpen } from 'lucide-react';

describe('EmptyState', () => {
  it('shows a message and an action', () => {
    render(<EmptyState title="Nothing yet" description="Create one" action={<button>Create</button>} />);
    expect(screen.getByText('Nothing yet')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create' })).toBeInTheDocument();
  });
});

describe('ErrorState', () => {
  it('explains a network failure and offers a retry', async () => {
    const retry = vi.fn();
    render(<ErrorState error={{ message: 'Network Error' }} onRetry={retry} />);
    expect(screen.getByRole('alert')).toHaveTextContent(/offline/i);
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(retry).toHaveBeenCalled();
  });
  it('shows a forbidden message without a retry button', () => {
    render(<ErrorState error={{ response: { status: 403, data: { message: 'No access here' } } }} onRetry={() => {}} />);
    expect(screen.getByText('No access here')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /try again/i })).toBeNull();
  });
});

describe('QueryBoundary', () => {
  const skeleton = <div>loading-skeleton</div>;
  it('renders the skeleton while pending', () => {
    render(<QueryBoundary query={{ isPending: true }} skeleton={skeleton}>{() => <div>data</div>}</QueryBoundary>);
    expect(screen.getByText('loading-skeleton')).toBeInTheDocument();
  });
  it('renders the error state with retry', async () => {
    const refetch = vi.fn();
    render(<QueryBoundary query={{ isPending: false, isError: true, error: { message: 'x' }, refetch }}>{() => <div>data</div>}</QueryBoundary>);
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(refetch).toHaveBeenCalled();
  });
  it('renders the empty state for empty data', () => {
    render(<QueryBoundary query={{ isPending: false, isError: false, data: [] }} isEmpty={(d) => d.length === 0} empty={<div>empty-here</div>}>{() => <div>data</div>}</QueryBoundary>);
    expect(screen.getByText('empty-here')).toBeInTheDocument();
  });
  it('renders children with data', () => {
    render(<QueryBoundary query={{ isPending: false, isError: false, data: [1, 2] }} isEmpty={(d) => d.length === 0}>{(d) => <div>{d.length} items</div>}</QueryBoundary>);
    expect(screen.getByText('2 items')).toBeInTheDocument();
  });
});

describe('StatCard', () => {
  it('shows a value and hides it behind a skeleton while loading', () => {
    const { rerender } = render(<StatCard icon={BookOpen} label="Subjects" value={4} />);
    expect(screen.getByText('4')).toBeInTheDocument();
    rerender(<StatCard icon={BookOpen} label="Subjects" value={4} loading />);
    expect(screen.queryByText('4')).toBeNull();
  });
});
