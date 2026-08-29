'use client';

import type { RecommendationBundle } from '@revexa/core';
import { ArrowRightIcon, LightbulbIcon } from 'lucide-react';
import Link from 'next/link';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const KIND_LABEL: Record<string, string> = {
  WEAK_TOPIC: 'Weak topic',
  RECURRING_MISTAKE: 'Habit',
  HABIT: 'Habit',
  COMPLEXITY: 'Complexity',
  STRETCH: 'Stretch',
  ONBOARDING: 'Start here',
};

export function Recommendations({ bundle }: { bundle: RecommendationBundle }) {
  return (
    <Card className="border-primary/25">
      <CardHeader>
        <div className="flex items-center gap-2">
          <div className="bg-primary/12 text-primary flex size-7 items-center justify-center rounded-md">
            <LightbulbIcon className="size-4" />
          </div>
          <CardTitle className="text-base">What to work on next</CardTitle>
        </div>
        <CardDescription className="text-pretty">{bundle.focusOfTheWeek}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {bundle.recommendations.map((recommendation) => (
          <div key={recommendation.id} className="rounded-lg border p-3.5">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="muted">{KIND_LABEL[recommendation.kind] ?? recommendation.kind}</Badge>
              <p className="text-sm font-medium text-pretty">{recommendation.title}</p>
            </div>
            <p className="text-muted-foreground mt-1.5 text-xs">{recommendation.reason}</p>
            <p className="mt-2 text-sm leading-relaxed text-pretty">{recommendation.action}</p>

            {recommendation.practiceProblems.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-1.5">
                {recommendation.practiceProblems.map((problem) => (
                  <Badge key={problem} variant="outline" className="font-normal">
                    {problem}
                  </Badge>
                ))}
              </div>
            )}
          </div>
        ))}

        <Button variant="outline" size="sm" className="w-full" asChild>
          <Link href="/problems">
            Pick a problem
            <ArrowRightIcon />
          </Link>
        </Button>
      </CardContent>
    </Card>
  );
}
