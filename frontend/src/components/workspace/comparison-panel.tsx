'use client';

import { useMutation } from '@tanstack/react-query';
import type { ApproachOption, SolutionComparison } from '@revexa/core';
import { RevexaApiError } from '@revexa/core';
import { CheckIcon, EyeIcon, GitCompareIcon, Loader2Icon, MinusIcon, UserIcon } from 'lucide-react';
import * as React from 'react';
import { toast } from 'sonner';

import { EmptyState } from '@/components/empty-state';
import { BookmarkButton } from '@/components/workspace/bookmark-button';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { api } from '@/lib/api';
import { cn } from '@/lib/utils';

export function ComparisonPanel({
  problemId,
  code,
  language,
}: {
  problemId: string;
  code: string;
  language: string;
}) {
  const [comparison, setComparison] = React.useState<SolutionComparison | null>(null);
  const [revealed, setRevealed] = React.useState(false);

  const run = useMutation({
    mutationFn: (revealPseudocode: boolean) =>
      api.compare({ problemId, code, language, revealPseudocode }),
    onSuccess: (result, revealPseudocode) => {
      setComparison(result.comparison);
      setRevealed(revealPseudocode);
    },
    onError: (error) => {
      toast.error(error instanceof RevexaApiError ? error.message : 'Could not build the comparison');
    },
  });

  if (!comparison) {
    return (
      <EmptyState
        icon={GitCompareIcon}
        title="Compare your approach"
        description="Put what you wrote beside the optimal approach and the realistic alternatives — with the trade-offs and the single missing observation named."
        action={
          <Button size="sm" disabled={!code.trim() || run.isPending} onClick={() => run.mutate(false)}>
            {run.isPending && <Loader2Icon className="animate-spin" />}
            Build comparison
          </Button>
        }
        className="m-4"
      />
    );
  }

  const options = [comparison.userApproach, ...comparison.alternatives];

  return (
    <div className="space-y-4 p-4">
      <div className="border-primary/25 bg-primary/6 rounded-lg border p-3.5">
        <div className="flex items-start justify-between gap-2">
          <div>
            <p className="text-primary text-[0.7rem] font-medium tracking-wide uppercase">Missing insight</p>
            <p className="mt-1.5 text-sm leading-relaxed text-pretty">{comparison.missingInsight}</p>
          </div>
          <BookmarkButton
            kind="COMPARISON"
            title="Missing insight"
            content={comparison.missingInsight}
            problemId={problemId}
          />
        </div>
      </div>

      {/* Complexity table */}
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="bg-muted/60">
            <tr className="text-muted-foreground text-left text-xs">
              <th className="px-3 py-2 font-medium">Approach</th>
              <th className="px-3 py-2 font-medium">Time</th>
              <th className="px-3 py-2 font-medium">Space</th>
            </tr>
          </thead>
          <tbody>
            {options.map((option) => (
              <tr key={option.id} className={cn('border-t', option.userApproach && 'bg-accent/40')}>
                <td className="px-3 py-2">
                  <div className="flex items-center gap-1.5">
                    {option.userApproach && <UserIcon className="text-muted-foreground size-3" />}
                    <span className="font-medium">{option.name}</span>
                    {option.optimal && (
                      <Badge variant="optimal" className="text-[0.6rem]">
                        optimal
                      </Badge>
                    )}
                  </div>
                </td>
                <td className="px-3 py-2 font-mono text-xs">{option.timeComplexity}</td>
                <td className="px-3 py-2 font-mono text-xs">{option.spaceComplexity}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="space-y-3">
        {options.map((option) => (
          <ApproachCard key={option.id} option={option} />
        ))}
      </div>

      <Separator />

      <section className="space-y-2">
        <h3 className="text-muted-foreground text-xs font-medium tracking-wide uppercase">Trade-offs</h3>
        <p className="text-sm leading-relaxed text-pretty">{comparison.tradeoffSummary}</p>
      </section>

      <section className="space-y-2">
        <h3 className="text-muted-foreground text-xs font-medium tracking-wide uppercase">Recommendation</h3>
        <p className="text-sm leading-relaxed text-pretty">{comparison.recommendation}</p>
      </section>

      {!revealed && (
        <Button variant="outline" className="w-full" disabled={run.isPending} onClick={() => run.mutate(true)}>
          {run.isPending ? <Loader2Icon className="animate-spin" /> : <EyeIcon />}
          Show the pseudocode for the optimal approach
        </Button>
      )}
    </div>
  );
}

function ApproachCard({ option }: { option: ApproachOption }) {
  return (
    <div
      className={cn(
        'rounded-lg border p-3.5',
        option.userApproach && 'border-primary/35 bg-accent/30',
        option.optimal && !option.userApproach && 'border-[var(--optimal)]/35',
      )}
    >
      <div className="flex flex-wrap items-center gap-2">
        <p className="font-medium">{option.name}</p>
        {option.userApproach && <Badge variant="default">Yours</Badge>}
        {option.optimal && <Badge variant="optimal">Optimal</Badge>}
        <span className="text-muted-foreground ml-auto font-mono text-xs">
          {option.timeComplexity} / {option.spaceComplexity}
        </span>
      </div>

      <p className="text-muted-foreground mt-1.5 text-sm leading-relaxed text-pretty">{option.summary}</p>

      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        <ul className="space-y-1">
          {option.pros.map((pro, index) => (
            <li key={index} className="flex gap-1.5 text-xs">
              <CheckIcon className="mt-0.5 size-3 shrink-0 text-[var(--optimal)]" />
              <span className="text-pretty">{pro}</span>
            </li>
          ))}
        </ul>
        <ul className="space-y-1">
          {option.cons.map((con, index) => (
            <li key={index} className="flex gap-1.5 text-xs">
              <MinusIcon className="text-muted-foreground mt-0.5 size-3 shrink-0" />
              <span className="text-muted-foreground text-pretty">{con}</span>
            </li>
          ))}
        </ul>
      </div>

      {option.whenToPrefer && (
        <p className="text-muted-foreground mt-3 border-t pt-2.5 text-xs leading-relaxed text-pretty">
          <span className="text-foreground font-medium">When to prefer it: </span>
          {option.whenToPrefer}
        </p>
      )}

      {option.pseudocode && (
        <pre className="bg-code-bg mt-3 overflow-x-auto rounded-md border p-3 font-mono text-xs leading-relaxed">
          <code>{option.pseudocode}</code>
        </pre>
      )}
    </div>
  );
}
