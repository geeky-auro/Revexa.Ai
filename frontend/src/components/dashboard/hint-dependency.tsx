import type { HintDependency } from '@revexa/core';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { cn } from '@/lib/utils';

export function HintDependencyCard({ dependency }: { dependency: HintDependency }) {
  const healthy = dependency.independenceScore >= 70;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Independence</CardTitle>
        <CardDescription>How much of the thinking is yours.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-semibold tabular-nums">{dependency.independenceScore}</span>
          <span className="text-muted-foreground text-sm">/ 100</span>
          <span
            className={cn(
              'ml-auto text-sm font-medium',
              healthy ? 'text-[var(--optimal)]' : 'text-[var(--improvable)]',
            )}
          >
            {dependency.verdict}
          </span>
        </div>

        <Progress
          value={dependency.independenceScore}
          indicatorClassName={healthy ? 'bg-[var(--optimal)]' : 'bg-[var(--improvable)]'}
        />

        <div className="text-muted-foreground grid grid-cols-2 gap-3 pt-1 text-xs">
          <div>
            <p className="text-foreground text-lg font-semibold tabular-nums">{dependency.hintsPerProblem}</p>
            <p>hints per problem</p>
          </div>
          <div>
            <p className="text-foreground text-lg font-semibold tabular-nums">{dependency.solutionsRevealed}</p>
            <p>solutions revealed</p>
          </div>
        </div>

        <p className="text-muted-foreground text-xs leading-relaxed text-pretty">{dependency.advice}</p>
      </CardContent>
    </Card>
  );
}
