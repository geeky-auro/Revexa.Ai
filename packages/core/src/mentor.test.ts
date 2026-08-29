import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  complexityGap,
  formatRelativeTime,
  hintLevel,
  isBetterComplexity,
  languageOption,
  requiresExplicitReveal,
  sortFindings,
} from './mentor.ts';

test('the hint ladder marks only the last two rungs as spoilers', () => {
  assert.equal(hintLevel(1).spoiler, false);
  assert.equal(hintLevel(4).spoiler, false);
  assert.equal(hintLevel(5).spoiler, true);
  assert.equal(hintLevel(6).spoiler, true);
});

test('only the solution rung demands an explicit reveal', () => {
  assert.equal(requiresExplicitReveal(5), false);
  assert.equal(requiresExplicitReveal(6), true);
});

test('complexity ordering is a total order over the known bounds', () => {
  assert.equal(isBetterComplexity('O(n)', 'O(n^2)'), true);
  assert.equal(isBetterComplexity('O(n log n)', 'O(n)'), false);
  assert.equal(isBetterComplexity('O(1)', 'O(log n)'), true);
});

test('the complexity gap is zero when already optimal and grows with distance', () => {
  assert.equal(complexityGap({ time: 'O(n)', optimalTime: 'O(n)' }), 0);
  assert.ok(complexityGap({ time: 'O(n^2)', optimalTime: 'O(n)' }) > 0);
  assert.ok(
    complexityGap({ time: 'O(2^n)', optimalTime: 'O(n)' }) > complexityGap({ time: 'O(n^2)', optimalTime: 'O(n)' }),
  );
});

test('findings sort most severe first', () => {
  const sorted = sortFindings([
    { severity: 'LOW' as const, id: 1 },
    { severity: 'CRITICAL' as const, id: 2 },
    { severity: 'MEDIUM' as const, id: 3 },
  ]);
  assert.deepEqual(
    sorted.map((f) => f.id),
    [2, 3, 1],
  );
});

test('an unknown language falls back to the first option rather than throwing', () => {
  assert.equal(languageOption('python').id, 'python');
  assert.equal(languageOption('brainfuck').id, 'python');
});

test('relative time reads naturally at each scale', () => {
  const now = new Date('2026-01-10T12:00:00Z');
  assert.equal(formatRelativeTime('2026-01-10T11:59:30Z', now), 'just now');
  assert.equal(formatRelativeTime('2026-01-10T11:30:00Z', now), '30m ago');
  assert.equal(formatRelativeTime('2026-01-09T12:00:00Z', now), '1d ago');
  assert.equal(formatRelativeTime('not-a-date', now), '');
});
