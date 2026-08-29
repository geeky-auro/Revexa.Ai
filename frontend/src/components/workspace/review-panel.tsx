'use client';

import type { CodeReviewResult, Finding } from '@revexa/core';
import { sortFindings, VERDICT_DESCRIPTIONS } from '@revexa/core';
import {
  AlertTriangleIcon,
  BugIcon,
  EyeIcon,
  GaugeIcon,
  LightbulbIcon,
  ListChecksIcon,
  PlayIcon,
  SparklesIcon,
  WrenchIcon,
} from 'lucide-react';

import { EmptyState } from '@/components/empty-state';
import { Markdown } from '@/components/markdown';
import { VerdictBadge } from '@/components/verdict-badge';
import { BookmarkButton } from '@/components/workspace/bookmark-button';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Separator } from '@/components/ui/separator';
import { cn } from '@/lib/utils';

const FINDING_ICON = {
  CORRECTNESS: BugIcon,
  EDGE_CASE: AlertTriangleIcon,
  PERFORMANCE: GaugeIcon,
  READABILITY: WrenchIcon,
} as const;

const SEVERITY_COLOR = {
  CRITICAL: 'text-[var(--risky)]',
  HIGH: 'text-[var(--inefficient)]',
  MEDIUM: 'text-[var(--improvable)]',
  LOW: 'text-muted-foreground',
} as const;

export function ReviewPanel({
  review,
  problemId,
  reviewId,
  isPending,
  onRun,
  hasCode,
}: {
  review: CodeReviewResult | null;
  problemId: string;
  reviewId?: string;
  isPending: boolean;
  onRun: () => void;
  hasCode: boolean;
}) {
  if (!review) {
    return (
      <EmptyState
        icon={SparklesIcon}
        title={isPending ? 'Reading your solution…' : 'No review yet'}
        description={
          isPending
            ? 'Understanding the problem, analysing your approach and costing it — all at once.'
            : 'Write or paste your solution, then run a review. You will get the approach read back to you, the real complexity, and the observation you have not used yet.'
        }
        action={
          !isPending && (
            <Button size="sm" disabled={!hasCode} onClick={onRun}>
              <PlayIcon />
              Run review
            </Button>
          )
        }
        className="m-4"
      />
    );
  }

  const findings = sortFindings(review.findings);

  return (
    <div className="space-y-5 p-4">
      {/* Verdict */}
      <section className="space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <VerdictBadge verdict={review.verdict} />
          <span className="text-muted-foreground text-xs">{VERDICT_DESCRIPTIONS[review.verdict]}</span>
          <div className="ml-auto">
            <BookmarkButton
              kind="REVIEW"
              title={`${review.approach.name} — ${review.verdict}`}
              content={review.mentorNote}
              problemId={problemId}
              referenceId={reviewId}
            />
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Progress
            value={review.score}
            className="h-2.5"
            indicatorClassName={
              review.score >= 85
                ? 'bg-[var(--optimal)]'
                : review.score >= 65
                  ? 'bg-[var(--solid)]'
                  : review.score >= 45
                    ? 'bg-[var(--improvable)]'
                    : 'bg-[var(--inefficient)]'
            }
          />
          <span className="w-14 shrink-0 text-right text-sm font-semibold tabular-nums">{review.score}/100</span>
        </div>

        <p className="text-sm leading-relaxed text-pretty">{review.mentorNote}</p>
      </section>

      <Separator />

      {/* What you built */}
      <section className="space-y-2.5">
        <SectionHeading icon={EyeIcon}>What you built</SectionHeading>
        <div className="flex flex-wrap items-center gap-2">
          <p className="font-medium">{review.approach.name}</p>
          {review.approach.patterns.slice(0, 3).map((pattern) => (
            <Badge key={pattern} variant="muted" className="font-normal">
              {pattern}
            </Badge>
          ))}
        </div>
        <p className="text-muted-foreground text-sm leading-relaxed text-pretty">{review.approach.plainExplanation}</p>

        <div className="bg-accent/40 rounded-lg border p-3">
          <p className="text-muted-foreground text-[0.7rem] font-medium tracking-wide uppercase">Your intuition, as read</p>
          <p className="mt-1.5 text-sm leading-relaxed text-pretty">{review.approach.inferredIntuition}</p>
        </div>

        {review.approach.steps.length > 0 && (
          <ol className="text-muted-foreground space-y-1 pt-1 text-sm">
            {review.approach.steps.map((step, index) => (
              <li key={index} className="flex gap-2.5">
                <span className="text-muted-foreground/70 mt-0.5 font-mono text-[0.7rem]">{index + 1}</span>
                <span className="text-pretty">{step}</span>
              </li>
            ))}
          </ol>
        )}
      </section>

      <Separator />

      {/* Understanding */}
      <section className="space-y-2.5">
        <SectionHeading icon={ListChecksIcon}>Did you read it right?</SectionHeading>
        <p className="text-sm leading-relaxed text-pretty">{review.understanding.restatement}</p>
        {review.understanding.easyToMisread.length > 0 && (
          <ul className="space-y-1.5">
            {review.understanding.easyToMisread.map((note, index) => (
              <li key={index} className="text-muted-foreground flex gap-2 text-sm">
                <AlertTriangleIcon className="mt-0.5 size-3.5 shrink-0 text-[var(--improvable)]" />
                <span className="text-pretty">{note}</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <Separator />

      {/* Findings */}
      <section className="space-y-2.5">
        <SectionHeading icon={BugIcon}>
          Findings
          {findings.length > 0 && (
            <Badge variant="muted" className="ml-2">
              {findings.length}
            </Badge>
          )}
        </SectionHeading>
        {findings.length === 0 ? (
          <p className="text-muted-foreground text-sm">
            Nothing flagged — no correctness risks, no missing guards, no performance traps.
          </p>
        ) : (
          <div className="space-y-2">
            {findings.map((finding, index) => (
              <FindingCard key={index} finding={finding} />
            ))}
          </div>
        )}
      </section>

      <Separator />

      {/* Optimisation */}
      <section className="space-y-2.5">
        <SectionHeading icon={LightbulbIcon}>
          {review.optimization.betterApproachExists ? 'The observation you have not used' : 'Already optimal'}
        </SectionHeading>

        <div
          className={cn(
            'rounded-lg border p-3.5',
            review.optimization.betterApproachExists
              ? 'border-primary/30 bg-primary/6'
              : 'border-[var(--optimal)]/30 bg-[var(--optimal)]/8',
          )}
        >
          <div className="flex items-start justify-between gap-2">
            <p className="text-sm leading-relaxed text-pretty">{review.optimization.keyInsight}</p>
            <BookmarkButton
              kind="INSIGHT"
              title={review.optimization.patternName || 'Key insight'}
              content={review.optimization.keyInsight}
              problemId={problemId}
              referenceId={reviewId}
            />
          </div>
          {review.optimization.betterApproachExists && (
            <p className="text-muted-foreground mt-2.5 text-xs">
              Target: <span className="font-mono">{review.optimization.targetTime}</span> time,{' '}
              <span className="font-mono">{review.optimization.targetSpace}</span> space · {review.optimization.patternName}
            </p>
          )}
        </div>

        {review.optimization.nudges.length > 0 && (
          <ul className="space-y-2">
            {review.optimization.nudges.map((nudge, index) => (
              <li key={index} className="text-muted-foreground flex gap-2.5 text-sm">
                <span className="bg-muted mt-0.5 flex size-4 shrink-0 items-center justify-center rounded font-mono text-[0.6rem]">
                  {index + 1}
                </span>
                <span className="text-pretty">{nudge}</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {review.nextSteps.length > 0 && (
        <>
          <Separator />
          <section className="space-y-2.5">
            <SectionHeading icon={ListChecksIcon}>Next steps</SectionHeading>
            <ul className="space-y-1.5">
              {review.nextSteps.map((step, index) => (
                <li key={index} className="flex gap-2 text-sm">
                  <span className="text-primary mt-1">→</span>
                  <span className="text-pretty">{step}</span>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}

      <p className="text-muted-foreground border-t pt-3 text-xs">
        Analysed by <span className="font-mono">{review.provider}</span> · {review.model} · complexity confidence{' '}
        {review.complexity.confidence}%
      </p>
    </div>
  );
}

function SectionHeading({ icon: Icon, children }: { icon: typeof BugIcon; children: React.ReactNode }) {
  return (
    <h2 className="text-muted-foreground flex items-center gap-1.5 text-xs font-medium tracking-wide uppercase">
      <Icon className="size-3.5" />
      {children}
    </h2>
  );
}

function FindingCard({ finding }: { finding: Finding }) {
  const Icon = FINDING_ICON[finding.type] ?? BugIcon;
  return (
    <div className="rounded-lg border p-3">
      <div className="flex items-start gap-2.5">
        <Icon className={cn('mt-0.5 size-4 shrink-0', SEVERITY_COLOR[finding.severity])} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-sm font-medium text-pretty">{finding.title}</p>
            <Badge
              variant={
                finding.severity === 'CRITICAL'
                  ? 'risky'
                  : finding.severity === 'HIGH'
                    ? 'inefficient'
                    : finding.severity === 'MEDIUM'
                      ? 'improvable'
                      : 'muted'
              }
              className="text-[0.65rem]"
            >
              {finding.severity.toLowerCase()}
            </Badge>
            {finding.line != null && (
              <span className="text-muted-foreground font-mono text-[0.7rem]">line {finding.line}</span>
            )}
          </div>
          <Markdown content={finding.detail} className="text-muted-foreground mt-1.5" />
          {finding.suggestion && (
            <p className="mt-2 flex gap-1.5 text-sm">
              <WrenchIcon className="text-muted-foreground mt-0.5 size-3.5 shrink-0" />
              <span className="text-pretty">{finding.suggestion}</span>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
