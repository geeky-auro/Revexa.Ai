import { VERDICT_LABELS, type Verdict } from '@revexa/core';

import { Badge } from '@/components/ui/badge';

const VARIANT: Record<Verdict, 'optimal' | 'solid' | 'improvable' | 'inefficient' | 'risky'> = {
  OPTIMAL: 'optimal',
  SOLID: 'solid',
  IMPROVABLE: 'improvable',
  INEFFICIENT: 'inefficient',
  RISKY: 'risky',
};

export function VerdictBadge({ verdict, className }: { verdict: Verdict; className?: string }) {
  return (
    <Badge variant={VARIANT[verdict] ?? 'muted'} className={className}>
      {VERDICT_LABELS[verdict] ?? verdict}
    </Badge>
  );
}

export function DifficultyBadge({ difficulty }: { difficulty: string }) {
  const variant =
    difficulty === 'EASY' ? 'optimal' : difficulty === 'MEDIUM' ? 'improvable' : difficulty === 'HARD' ? 'risky' : 'muted';
  return (
    <Badge variant={variant} className="capitalize">
      {difficulty.toLowerCase()}
    </Badge>
  );
}
