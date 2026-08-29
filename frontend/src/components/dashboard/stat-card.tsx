import type { LucideIcon } from 'lucide-react';
import { TrendingDownIcon, TrendingUpIcon } from 'lucide-react';

import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';

export function StatCard({
  icon: Icon,
  label,
  value,
  suffix,
  delta,
  hint,
}: {
  icon: LucideIcon;
  label: string;
  value: string | number;
  suffix?: string;
  delta?: number;
  hint?: string;
}) {
  const hasDelta = typeof delta === 'number' && delta !== 0;

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-center gap-2">
          <Icon className="text-muted-foreground size-4" />
          <p className="text-muted-foreground text-xs font-medium tracking-wide uppercase">{label}</p>
        </div>
        <div className="mt-2 flex items-baseline gap-1.5">
          <span className="text-2xl font-semibold tracking-tight tabular-nums">{value}</span>
          {suffix && <span className="text-muted-foreground text-sm">{suffix}</span>}
          {hasDelta && (
            <span
              className={cn(
                'ml-1 flex items-center gap-0.5 text-xs font-medium',
                delta > 0 ? 'text-[var(--optimal)]' : 'text-[var(--inefficient)]',
              )}
            >
              {delta > 0 ? <TrendingUpIcon className="size-3" /> : <TrendingDownIcon className="size-3" />}
              {delta > 0 ? '+' : ''}
              {delta}
            </span>
          )}
        </div>
        {hint && <p className="text-muted-foreground mt-1 text-xs text-pretty">{hint}</p>}
      </CardContent>
    </Card>
  );
}
