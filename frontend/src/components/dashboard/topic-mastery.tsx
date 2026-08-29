import type { TopicMastery } from '@revexa/core';

import { EmptyState } from '@/components/empty-state';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { TargetIcon } from 'lucide-react';

export function TopicMasteryCard({
  title,
  description,
  topics,
  tone,
}: {
  title: string;
  description: string;
  topics: TopicMastery[];
  tone: 'strong' | 'weak';
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {topics.length === 0 ? (
          <EmptyState
            icon={TargetIcon}
            title={tone === 'weak' ? 'Nothing consistently weak' : 'No strengths established yet'}
            description={
              tone === 'weak'
                ? 'A topic needs at least two attempts before it counts as a weakness — one bad day is not a pattern.'
                : 'Reach the optimal approach a couple of times on a topic and it will show up here.'
            }
            className="border-0 py-6"
          />
        ) : (
          topics.map((topic) => (
            <div key={topic.topic} className="space-y-1.5">
              <div className="flex items-baseline justify-between gap-3">
                <span className="text-sm font-medium">{topic.topic}</span>
                <span className="text-muted-foreground text-xs tabular-nums">
                  {topic.optimal}/{topic.attempts} optimal
                </span>
              </div>
              <Progress
                value={topic.mastery}
                indicatorClassName={tone === 'weak' ? 'bg-[var(--improvable)]' : 'bg-[var(--optimal)]'}
              />
              <p className="text-muted-foreground text-xs leading-relaxed text-pretty">{topic.advice}</p>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}
