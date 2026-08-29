'use client';

import type { ComplexityAnalysis } from '@revexa/core';
import { CheckCircle2Icon, GaugeIcon, TriangleAlertIcon } from 'lucide-react';

import { EmptyState } from '@/components/empty-state';
import { BookmarkButton } from '@/components/workspace/bookmark-button';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Separator } from '@/components/ui/separator';
import { cn } from '@/lib/utils';

export function ComplexityPanel({
  complexity,
  problemId,
}: {
  complexity: ComplexityAnalysis | null;
  problemId: string;
}) {
  if (!complexity) {
    return (
      <EmptyState
        icon={GaugeIcon}
        title="No analysis yet"
        description="Run a review and the cost breakdown appears here — what your code costs, what it could cost, and which construct is responsible."
        className="m-4"
      />
    );
  }

  return (
    <div className="space-y-5 p-4">
      <div className="grid grid-cols-2 gap-3">
        <ComplexityTile
          label="Time"
          measured={complexity.time}
          optimal={complexity.optimalTime}
          isOptimal={complexity.timeOptimal}
        />
        <ComplexityTile
          label="Space"
          measured={complexity.space}
          optimal={complexity.optimalSpace}
          isOptimal={complexity.spaceOptimal}
        />
      </div>

      <div className="space-y-1.5">
        <div className="text-muted-foreground flex items-center justify-between text-xs">
          <span>Analysis confidence</span>
          <span className="tabular-nums">{complexity.confidence}%</span>
        </div>
        <Progress
          value={complexity.confidence}
          className="h-1.5"
          indicatorClassName={complexity.confidence >= 70 ? 'bg-[var(--solid)]' : 'bg-[var(--improvable)]'}
        />
        <p className="text-muted-foreground text-xs text-pretty">
          Estimated from the structure of your code — loop nesting, recursion shape and the data structures in play —
          rather than by running it.
        </p>
      </div>

      <Separator />

      <section className="space-y-2">
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-muted-foreground text-xs font-medium tracking-wide uppercase">Why the time cost</h3>
          <BookmarkButton
            kind="COMPLEXITY"
            title={`${complexity.time} time, ${complexity.space} space`}
            content={`${complexity.timeExplanation}\n\n${complexity.spaceExplanation}`}
            problemId={problemId}
          />
        </div>
        <p className="text-sm leading-relaxed text-pretty">{complexity.timeExplanation}</p>
      </section>

      <section className="space-y-2">
        <h3 className="text-muted-foreground text-xs font-medium tracking-wide uppercase">Why the space cost</h3>
        <p className="text-sm leading-relaxed text-pretty">{complexity.spaceExplanation}</p>
      </section>

      {complexity.contributors.length > 0 && (
        <>
          <Separator />
          <section className="space-y-2">
            <h3 className="text-muted-foreground text-xs font-medium tracking-wide uppercase">Cost breakdown</h3>
            <div className="space-y-2">
              {complexity.contributors.map((contributor, index) => (
                <div key={index} className="rounded-lg border p-3">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-medium text-pretty">{contributor.label}</p>
                    <Badge variant="outline" className="shrink-0 font-mono">
                      {contributor.complexity}
                    </Badge>
                  </div>
                  <p className="text-muted-foreground mt-1 text-xs leading-relaxed text-pretty">{contributor.reason}</p>
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function ComplexityTile({
  label,
  measured,
  optimal,
  isOptimal,
}: {
  label: string;
  measured: string;
  optimal: string;
  isOptimal: boolean;
}) {
  return (
    <div
      className={cn(
        'rounded-lg border p-3.5',
        isOptimal ? 'border-[var(--optimal)]/35 bg-[var(--optimal)]/8' : 'border-[var(--improvable)]/35 bg-[var(--improvable)]/8',
      )}
    >
      <div className="flex items-center gap-1.5">
        {isOptimal ? (
          <CheckCircle2Icon className="size-3.5 text-[var(--optimal)]" />
        ) : (
          <TriangleAlertIcon className="size-3.5 text-[var(--improvable)]" />
        )}
        <p className="text-muted-foreground text-[0.7rem] font-medium tracking-wide uppercase">{label}</p>
      </div>
      <p className="mt-1.5 font-mono text-xl font-semibold">{measured}</p>
      <p className="text-muted-foreground mt-1 text-xs">
        {isOptimal ? 'Best known bound' : (
          <>
            Achievable: <span className="font-mono">{optimal}</span>
          </>
        )}
      </p>
    </div>
  );
}
