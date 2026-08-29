import { EyeOffIcon, LockIcon } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';

const RUNGS = [
  {
    level: 1,
    title: 'Clarify the problem',
    body: 'Are you returning indices or values? Is exactly one answer guaranteed? Can the same element be used twice?',
    state: 'revealed',
  },
  {
    level: 2,
    title: 'Find the observation',
    body: 'For each element the inner loop re-scans the rest of the array asking one question: does target − x exist? That is a membership question, not a scanning question.',
    state: 'revealed',
  },
  {
    level: 3,
    title: 'Conceptual hint',
    body: 'Membership questions answered in O(1) are what hash tables are for.',
    state: 'current',
  },
  { level: 4, title: 'Algorithmic direction', body: '', state: 'locked' },
  { level: 5, title: 'Pseudocode', body: '', state: 'locked' },
  { level: 6, title: 'Full solution', body: '', state: 'gated' },
] as const;

export function HintLadderPreview() {
  return (
    <Card className="shadow-lg">
      <CardContent className="space-y-2 p-4">
        {RUNGS.map((rung) => (
          <div
            key={rung.level}
            className={cn(
              'rounded-lg border p-3 transition-colors',
              rung.state === 'current' && 'border-primary/40 bg-primary/6',
              rung.state === 'revealed' && 'bg-muted/40',
              (rung.state === 'locked' || rung.state === 'gated') && 'border-dashed opacity-70',
            )}
          >
            <div className="flex items-center gap-2">
              <span
                className={cn(
                  'flex size-5 items-center justify-center rounded font-mono text-[0.65rem] font-semibold',
                  rung.state === 'current' ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground',
                )}
              >
                {rung.level}
              </span>
              <span className="text-sm font-medium">{rung.title}</span>
              {rung.state === 'current' && (
                <Badge variant="default" className="ml-auto text-[0.65rem]">
                  You are here
                </Badge>
              )}
              {rung.state === 'locked' && <LockIcon className="text-muted-foreground ml-auto size-3.5" />}
              {rung.state === 'gated' && (
                <Badge variant="muted" className="ml-auto gap-1 text-[0.65rem]">
                  <EyeOffIcon className="size-2.5" />
                  Explicit request only
                </Badge>
              )}
            </div>
            {rung.body && (
              <p className="text-muted-foreground mt-1.5 pl-7 text-sm leading-relaxed text-pretty">{rung.body}</p>
            )}
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
