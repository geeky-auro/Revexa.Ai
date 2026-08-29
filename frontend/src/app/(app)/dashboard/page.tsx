'use client';

import { useQuery } from '@tanstack/react-query';
import { formatRelativeTime } from '@revexa/core';
import {
  ActivityIcon,
  BookmarkIcon,
  DownloadIcon,
  FlameIcon,
  GaugeIcon,
  Loader2Icon,
  RocketIcon,
  TargetIcon,
} from 'lucide-react';
import Link from 'next/link';

import { ComplexityMix } from '@/components/dashboard/complexity-mix';
import { HintDependencyCard } from '@/components/dashboard/hint-dependency';
import { MistakeList } from '@/components/dashboard/mistake-list';
import { QualityTrend } from '@/components/dashboard/quality-trend';
import { Recommendations } from '@/components/dashboard/recommendations';
import { StatCard } from '@/components/dashboard/stat-card';
import { TopicMasteryCard } from '@/components/dashboard/topic-mastery';
import { EmptyState } from '@/components/empty-state';
import { VerdictBadge } from '@/components/verdict-badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { queryKeys } from '@/lib/query-keys';

export default function DashboardPage() {
  const { user } = useAuth();
  const progress = useQuery({ queryKey: queryKeys.progress, queryFn: () => api.progressSummary() });
  const recommendations = useQuery({ queryKey: queryKeys.recommendations, queryFn: () => api.recommendations() });

  if (progress.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-9 w-64" />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-28" />
          ))}
        </div>
        <div className="grid gap-4 lg:grid-cols-3">
          <Skeleton className="h-72 lg:col-span-2" />
          <Skeleton className="h-72" />
        </div>
      </div>
    );
  }

  if (progress.isError || !progress.data) {
    return (
      <EmptyState
        icon={ActivityIcon}
        title="Could not load your progress"
        description="The API did not respond. Check that the backend is running, then try again."
        action={
          <Button variant="outline" size="sm" onClick={() => void progress.refetch()}>
            Retry
          </Button>
        }
      />
    );
  }

  const { headline, strongTopics, weakTopics, recurringMistakes, qualityTrend, complexityMix, hintDependency, recentActivity } =
    progress.data;
  const firstName = user?.displayName.split(' ')[0] ?? 'there';

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Welcome back, {firstName}</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            {headline.reviewsRun === 0
              ? 'Run your first review and this page fills in.'
              : `${headline.reviewsRun} reviews across ${headline.problemsAttempted} problems.`}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" asChild>
            <a href="/backend/progress/report/export" download>
              <DownloadIcon />
              Export report
            </a>
          </Button>
          <Button size="sm" asChild>
            <Link href="/problems">
              <RocketIcon />
              New review
            </Link>
          </Button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          icon={TargetIcon}
          label="Optimal rate"
          value={`${headline.optimalRate}%`}
          hint={`${headline.optimalSolutions} of ${headline.reviewsRun} reviews hit the best known bound`}
        />
        <StatCard
          icon={GaugeIcon}
          label="Average score"
          value={headline.averageScore}
          suffix="/ 100"
          delta={headline.scoreDelta}
          hint={
            headline.scoreDelta === 0
              ? 'Measured across all reviews'
              : `${headline.scoreDelta > 0 ? 'Improving' : 'Slipping'} against your earliest reviews`
          }
        />
        <StatCard
          icon={FlameIcon}
          label="Streak"
          value={headline.currentStreakDays}
          suffix={headline.currentStreakDays === 1 ? 'day' : 'days'}
          hint="Consecutive days with at least one review"
        />
        <StatCard
          icon={BookmarkIcon}
          label="Saved insights"
          value={headline.bookmarks}
          hint="Explanations you kept for later"
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <QualityTrend points={qualityTrend} />
          <div className="grid gap-4 md:grid-cols-2">
            <TopicMasteryCard
              title="Weak spots"
              description="Where you keep landing on a slower approach."
              topics={weakTopics}
              tone="weak"
            />
            <TopicMasteryCard
              title="Strengths"
              description="Topics where you reliably reach the optimal approach."
              topics={strongTopics}
              tone="strong"
            />
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <MistakeList mistakes={recurringMistakes} />
            <ComplexityMix buckets={complexityMix} />
          </div>
        </div>

        <div className="space-y-4">
          {recommendations.isLoading ? (
            <Card>
              <CardContent className="flex h-40 items-center justify-center">
                <Loader2Icon className="text-muted-foreground size-5 animate-spin" />
              </CardContent>
            </Card>
          ) : (
            recommendations.data && <Recommendations bundle={recommendations.data} />
          )}

          <HintDependencyCard dependency={hintDependency} />

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Recent reviews</CardTitle>
              <CardDescription>Your last few submissions.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-1">
              {recentActivity.length === 0 ? (
                <p className="text-muted-foreground py-4 text-center text-sm">Nothing reviewed yet.</p>
              ) : (
                recentActivity.map((activity) => (
                  <Link
                    key={activity.reviewId}
                    href={`/workspace/${activity.problemId}?review=${activity.reviewId}`}
                    className="hover:bg-accent/60 flex items-center gap-3 rounded-md px-2 py-2 transition-colors"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{activity.problemTitle}</p>
                      <p className="text-muted-foreground text-xs">{formatRelativeTime(activity.at)}</p>
                    </div>
                    <span className="text-muted-foreground text-xs tabular-nums">{activity.score}</span>
                    <VerdictBadge verdict={activity.verdict} />
                  </Link>
                ))
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
