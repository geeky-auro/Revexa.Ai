'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { HintSessionView } from '@revexa/core';
import { RevexaApiError, SOLUTION_LEVEL } from '@revexa/core';
import { ChevronRightIcon, EyeIcon, LightbulbIcon, Loader2Icon, LockIcon } from 'lucide-react';
import * as React from 'react';
import { toast } from 'sonner';

import { Markdown } from '@/components/markdown';
import { BookmarkButton } from '@/components/workspace/bookmark-button';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { api } from '@/lib/api';
import { queryKeys } from '@/lib/query-keys';
import { cn } from '@/lib/utils';

/**
 * The hint ladder.
 *
 * The UI mirrors the rule the server enforces: one rung per request, and the final rung behind a
 * confirmation. Showing the locked rungs matters — the learner can see how much is deliberately
 * being held back, which is what makes stopping early feel like a choice rather than a dead end.
 */
export function HintPanel({
  problemId,
  code,
  language,
}: {
  problemId: string;
  code: string;
  language: string;
}) {
  const [sessionId, setSessionId] = React.useState<string | null>(null);
  const [confirmReveal, setConfirmReveal] = React.useState(false);
  const queryClient = useQueryClient();

  const start = useMutation({
    mutationFn: () => api.startHintSession({ problemId }),
    onSuccess: (session) => {
      setSessionId(session.id);
      queryClient.setQueryData(queryKeys.hintSession(session.id), session);
    },
  });

  React.useEffect(() => {
    if (!sessionId && !start.isPending) {
      start.mutate();
    }
    // Opening the panel opens (or resumes) the session exactly once per problem.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [problemId]);

  const session = useQuery({
    queryKey: queryKeys.hintSession(sessionId ?? ''),
    queryFn: () => api.getHintSession(sessionId!),
    enabled: !!sessionId,
  });

  const next = useMutation({
    mutationFn: (revealSolution: boolean) => api.nextHint(sessionId!, { revealSolution, code, language }),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.hintSession(updated.id), updated);
      queryClient.invalidateQueries({ queryKey: queryKeys.progress });
    },
    onError: (error) => {
      toast.error(error instanceof RevexaApiError ? error.message : 'Could not fetch the next hint');
    },
  });

  const data: HintSessionView | undefined = session.data;

  if (!data) {
    return (
      <div className="flex h-40 items-center justify-center">
        <Loader2Icon className="text-muted-foreground size-5 animate-spin" />
      </div>
    );
  }

  const nextLevel = data.currentLevel + 1;
  const atSolution = nextLevel >= SOLUTION_LEVEL;
  const finished = data.currentLevel >= SOLUTION_LEVEL;

  return (
    <div className="space-y-4 p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-1.5 font-medium">
            <LightbulbIcon className="text-primary size-4" />
            Hint ladder
          </h2>
          <p className="text-muted-foreground mt-1 text-sm text-pretty">
            One rung at a time. Nothing below your current step is shown, and the solution needs an explicit ask.
          </p>
        </div>
        <Badge variant="muted" className="shrink-0 tabular-nums">
          {data.currentLevel}/{data.maxLevel}
        </Badge>
      </div>

      {/* Ladder rail */}
      <div className="flex gap-1">
        {data.ladder.map((step) => (
          <div
            key={step.level}
            title={step.title}
            className={cn(
              'h-1.5 flex-1 rounded-full transition-colors',
              step.unlocked ? (step.spoiler ? 'bg-[var(--improvable)]' : 'bg-primary') : 'bg-muted',
            )}
          />
        ))}
      </div>

      {/* Revealed hints */}
      <div className="space-y-3">
        {data.hints.length === 0 && (
          <div className="rounded-lg border border-dashed p-4 text-center">
            <p className="text-muted-foreground text-sm text-pretty">
              Stuck? Start at rung one — it only clarifies what the problem is asking, with no algorithmic content at all.
            </p>
          </div>
        )}

        {data.hints.map((hint) => (
          <div
            key={hint.level}
            className={cn('rounded-lg border p-3.5', hint.spoiler ? 'border-[var(--improvable)]/40 bg-[var(--improvable)]/6' : 'bg-card')}
          >
            <div className="flex items-center gap-2">
              <span className="bg-primary/12 text-primary flex size-5 items-center justify-center rounded font-mono text-[0.65rem] font-semibold">
                {hint.level}
              </span>
              <p className="text-sm font-medium">{hint.title}</p>
              {hint.spoiler && (
                <Badge variant="improvable" className="text-[0.65rem]">
                  spoiler
                </Badge>
              )}
              <div className="ml-auto">
                <BookmarkButton
                  kind="HINT"
                  title={`${hint.title} — level ${hint.level}`}
                  content={hint.content}
                  problemId={problemId}
                />
              </div>
            </div>

            <Markdown content={hint.content} className="mt-2" />

            {hint.socraticQuestions.length > 0 && (
              <ul className="mt-3 space-y-1 border-t pt-2.5">
                {hint.socraticQuestions.map((question, index) => (
                  <li key={index} className="text-muted-foreground flex gap-2 text-sm">
                    <span className="text-primary">?</span>
                    <span className="text-pretty">{question}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        ))}
      </div>

      {/* Locked rungs */}
      {!finished && (
        <div className="space-y-1.5">
          {data.ladder
            .filter((step) => !step.unlocked)
            .map((step) => (
              <div
                key={step.level}
                className="text-muted-foreground flex items-center gap-2 rounded-lg border border-dashed px-3 py-2 text-sm"
              >
                <LockIcon className="size-3.5" />
                <span>{step.title}</span>
                {step.spoiler && (
                  <Badge variant="outline" className="ml-auto text-[0.65rem]">
                    spoiler
                  </Badge>
                )}
              </div>
            ))}
        </div>
      )}

      {finished ? (
        <p className="text-muted-foreground rounded-lg border border-dashed p-3 text-center text-sm text-pretty">
          You have the full solution. The useful part now is covering it up and re-deriving the key step yourself.
        </p>
      ) : (
        <Button
          className="w-full"
          variant={atSolution ? 'outline' : 'default'}
          disabled={next.isPending}
          onClick={() => (atSolution ? setConfirmReveal(true) : next.mutate(false))}
        >
          {next.isPending ? <Loader2Icon className="animate-spin" /> : atSolution ? <EyeIcon /> : <ChevronRightIcon />}
          {atSolution ? 'Reveal the full solution' : `Next hint — ${data.ladder[nextLevel - 1]?.title ?? ''}`}
        </Button>
      )}

      <Dialog open={confirmReveal} onOpenChange={setConfirmReveal}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Reveal the whole solution?</DialogTitle>
            <DialogDescription className="text-pretty">
              You have already seen the pseudocode. Most people who stop here and implement it themselves remember the
              pattern next time; most people who read the finished code do not. Your call.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setConfirmReveal(false)}>
              Let me try first
            </Button>
            <Button
              onClick={() => {
                setConfirmReveal(false);
                next.mutate(true);
              }}
            >
              Show the solution
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
