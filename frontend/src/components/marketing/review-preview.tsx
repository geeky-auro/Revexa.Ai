import { AlertTriangleIcon, CheckIcon, TrendingDownIcon } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';

/**
 * A static rendering of a real review, using the same visual language as the workspace panels.
 * Values here match what the analyser actually returns for this snippet.
 */
export function ReviewPreview() {
  return (
    <Card className="overflow-hidden shadow-lg">
      <div className="bg-muted/60 flex items-center gap-2 border-b px-4 py-2.5">
        <div className="flex gap-1.5">
          <span className="size-2.5 rounded-full bg-[var(--risky)]/60" />
          <span className="size-2.5 rounded-full bg-[var(--improvable)]/60" />
          <span className="size-2.5 rounded-full bg-[var(--optimal)]/60" />
        </div>
        <span className="text-muted-foreground ml-1 font-mono text-xs">two_sum.py — AI review</span>
      </div>

      <pre className="bg-code-bg overflow-x-auto border-b px-4 py-3.5 font-mono text-[0.78rem] leading-relaxed">
        <code>
          <span className="text-muted-foreground">for</span> i <span className="text-muted-foreground">in range</span>(len(nums)):{'\n'}
          {'    '}
          <span className="text-muted-foreground">for</span> j <span className="text-muted-foreground">in range</span>(i + 1, len(nums)):{'\n'}
          {'        '}
          <span className="text-muted-foreground">if</span> nums[i] + nums[j] == target:{'\n'}
          {'            '}
          <span className="text-muted-foreground">return</span> [i, j]
        </code>
      </pre>

      <div className="space-y-3.5 p-4">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="inefficient">Too slow</Badge>
          <span className="text-muted-foreground text-xs">Brute-force pair scan · score 45/100</span>
        </div>

        <div className="grid grid-cols-2 gap-2.5">
          <div className="bg-muted/50 rounded-lg border p-3">
            <p className="text-muted-foreground text-[0.7rem] tracking-wide uppercase">Your time</p>
            <p className="mt-1 font-mono text-lg font-semibold text-[var(--inefficient)]">O(n²)</p>
          </div>
          <div className="bg-muted/50 rounded-lg border p-3">
            <p className="text-muted-foreground text-[0.7rem] tracking-wide uppercase">Achievable</p>
            <p className="mt-1 font-mono text-lg font-semibold text-[var(--optimal)]">O(n)</p>
          </div>
        </div>

        <div className="space-y-2 text-sm">
          <div className="flex items-start gap-2">
            <AlertTriangleIcon className="mt-0.5 size-3.5 shrink-0 text-[var(--inefficient)]" />
            <span className="text-pretty">
              <span className="font-medium">Too slow for the stated constraints.</span>{' '}
              <span className="text-muted-foreground">The bound is 10⁴, so O(n²) will time out.</span>
            </span>
          </div>
          <div className="flex items-start gap-2">
            <TrendingDownIcon className="mt-0.5 size-3.5 shrink-0 text-[var(--improvable)]" />
            <span className="text-pretty">
              <span className="font-medium">Repeated work between iterations.</span>{' '}
              <span className="text-muted-foreground">The inner loop re-asks one membership question.</span>
            </span>
          </div>
          <div className="flex items-start gap-2">
            <CheckIcon className="mt-0.5 size-3.5 shrink-0 text-[var(--optimal)]" />
            <span className="text-muted-foreground text-pretty">
              The decomposition is right — it is the lookup that is expensive, not the idea.
            </span>
          </div>
        </div>

        <div className="border-primary/25 bg-primary/6 rounded-lg border p-3">
          <p className="text-primary text-[0.7rem] font-medium tracking-wide uppercase">Key insight — withheld until asked</p>
          <p className="mt-1.5 text-sm leading-relaxed text-pretty">
            While scanning left to right you already know everything to your left. Instead of searching for the
            partner, ask whether the partner has already been seen.
          </p>
        </div>
      </div>
    </Card>
  );
}
