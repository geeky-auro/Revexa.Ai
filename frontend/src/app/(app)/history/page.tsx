'use client';

import { useQuery } from '@tanstack/react-query';
import { formatRelativeTime } from '@revexa/core';
import { HistoryIcon } from 'lucide-react';
import Link from 'next/link';

import { EmptyState } from '@/components/empty-state';
import { ImportProblemDialog } from '@/components/problems/import-dialog';
import { VerdictBadge } from '@/components/verdict-badge';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { api } from '@/lib/api';
import { queryKeys } from '@/lib/query-keys';

export default function HistoryPage() {
  const reviews = useQuery({ queryKey: queryKeys.reviews(), queryFn: () => api.listReviews({ size: 50 }) });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Review history</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Every review, kept exactly as it was produced — so you can see how your answer to the same problem changed.
        </p>
      </div>

      {reviews.isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, index) => (
            <Skeleton key={index} className="h-20" />
          ))}
        </div>
      ) : reviews.data && reviews.data.items.length > 0 ? (
        <div className="space-y-2">
          {reviews.data.items.map((review) => (
            <Link key={review.id} href={`/workspace/${review.problemId}?review=${review.id}`}>
              <Card className="transition-all hover:border-primary/40 hover:shadow-sm">
                <CardContent className="flex flex-wrap items-center gap-x-4 gap-y-2 p-4">
                  <div className="min-w-48 flex-1">
                    <p className="font-medium">{review.problemTitle}</p>
                    <p className="text-muted-foreground text-xs">
                      {review.approachName} · {review.language} · {formatRelativeTime(review.createdAt)}
                    </p>
                  </div>

                  <div className="flex items-center gap-1.5 font-mono text-xs">
                    <span className={review.timeOptimal ? 'text-[var(--optimal)]' : 'text-[var(--improvable)]'}>
                      {review.timeComplexity}
                    </span>
                    {!review.timeOptimal && (
                      <>
                        <span className="text-muted-foreground">→</span>
                        <span className="text-muted-foreground">{review.optimalTime}</span>
                      </>
                    )}
                  </div>

                  <div className="hidden flex-wrap gap-1 sm:flex">
                    {review.topics.slice(0, 2).map((topic) => (
                      <Badge key={topic} variant="muted" className="font-normal">
                        {topic}
                      </Badge>
                    ))}
                  </div>

                  <span className="w-10 text-right text-sm font-semibold tabular-nums">{review.score}</span>
                  <VerdictBadge verdict={review.verdict} />
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      ) : (
        <EmptyState
          icon={HistoryIcon}
          title="No reviews yet"
          description="Import a problem you have already solved and run your first review — the history builds from there."
          action={<ImportProblemDialog />}
        />
      )}
    </div>
  );
}
