import { describe, expect, it } from 'vitest';
import { fmt, riskBadgeVariant } from './StateViews';

describe('fmt', () => {
  it('renders missing values as a dash, never as zero', () => {
    expect(fmt(null, '%')).toBe('—');
    expect(fmt(undefined, '%')).toBe('—');
    expect(fmt(Number.NaN)).toBe('—');
  });
  it('keeps real zeros and formats digits', () => {
    expect(fmt(0, '%')).toBe('0.0%');
    expect(fmt(76.25, '%')).toBe('76.3%');
    expect(fmt(-20, ' pts', 0)).toBe('-20 pts');
  });
});

describe('riskBadgeVariant', () => {
  it('maps levels and treats unknown as neutral', () => {
    expect(riskBadgeVariant('HIGH')).toBe('destructive');
    expect(riskBadgeVariant('MEDIUM')).toBe('warning');
    expect(riskBadgeVariant('LOW')).toBe('success');
    expect(riskBadgeVariant(null)).toBe('outline');
  });
});
