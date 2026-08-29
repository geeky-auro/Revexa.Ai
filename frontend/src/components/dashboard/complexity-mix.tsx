import type { ComplexityBucket } from '@revexa/core';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';

/**
 * How often each complexity class shows up in your submissions. Bars are coloured by whether that
 * class was optimal for the problems it appeared on — an O(n²) bar is not automatically bad.
 */
export function ComplexityMix({ buckets }: { buckets: ComplexityBucket[] }) {
  const total = buckets.reduce((sum, bucket) => sum + bucket.count, 0);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Complexity mix</CardTitle>
        <CardDescription>
          {total === 0
            ? 'Your submissions will be grouped by measured time complexity here.'
            : 'Where your solutions land, and whether that was the right place to land.'}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-2.5">
        {buckets.map((bucket) => (
          <div key={bucket.complexity} className="space-y-1">
            <div className="flex items-baseline justify-between gap-3">
              <span className="font-mono text-sm">{bucket.complexity}</span>
              <span className="text-muted-foreground text-xs">
                {bucket.count} {bucket.count === 1 ? 'solution' : 'solutions'}
                {bucket.optimalForItsProblems && ' · all optimal'}
              </span>
            </div>
            <div className="bg-muted h-2 w-full overflow-hidden rounded-full">
              <div
                className={cn('h-full rounded-full transition-all', bucket.optimalForItsProblems ? 'bg-[var(--optimal)]' : 'bg-[var(--improvable)]')}
                style={{ width: `${total === 0 ? 0 : Math.max(4, (bucket.count / total) * 100)}%` }}
              />
            </div>
          </div>
        ))}
        {total === 0 && <p className="text-muted-foreground py-4 text-center text-sm">No reviews yet.</p>}
      </CardContent>
    </Card>
  );
}
