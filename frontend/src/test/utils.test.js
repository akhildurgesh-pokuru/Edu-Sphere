import { describe, expect, it } from 'vitest';
import { formatBytes, formatDuration, gradientFor, initials, pluralize } from '@/lib/utils';

describe('utils', () => {
  it('formats durations', () => {
    expect(formatDuration(0)).toBe('0:00');
    expect(formatDuration(65)).toBe('1:05');
    expect(formatDuration(3725)).toBe('1:02:05');
  });
  it('formats bytes', () => {
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(2048)).toBe('2.0 KB');
    expect(formatBytes(5 * 1024 * 1024)).toBe('5.0 MB');
  });
  it('builds initials and plurals', () => {
    expect(initials('Ada Lovelace')).toBe('AL');
    expect(initials('')).toBe('?');
    expect(pluralize(1, 'lesson')).toBe('1 lesson');
    expect(pluralize(3, 'lesson')).toBe('3 lessons');
  });
  it('derives a stable gradient from an id', () => {
    expect(gradientFor(3)).toBe(gradientFor(3));
    expect(gradientFor(1)).not.toBe(gradientFor(2));
  });
});
