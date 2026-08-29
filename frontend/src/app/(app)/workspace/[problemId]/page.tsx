'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { CodeReviewResult } from '@revexa/core';
import { LANGUAGES, RevexaApiError, languageOption } from '@revexa/core';
import {
  BookOpenIcon,
  GaugeIcon,
  GitCompareIcon,
  LightbulbIcon,
  Loader2Icon,
  MessagesSquareIcon,
  PanelLeftCloseIcon,
  PanelLeftOpenIcon,
  PlayIcon,
  SparklesIcon,
  TerminalIcon,
} from 'lucide-react';
import { useParams, useSearchParams } from 'next/navigation';
import * as React from 'react';
import { toast } from 'sonner';

import { EmptyState } from '@/components/empty-state';
import { ChatPanel } from '@/components/workspace/chat-panel';
import { CodeEditor } from '@/components/workspace/code-editor';
import { ComparisonPanel } from '@/components/workspace/comparison-panel';
import { ComplexityPanel } from '@/components/workspace/complexity-panel';
import { HintPanel } from '@/components/workspace/hint-panel';
import { ProblemPanel } from '@/components/workspace/problem-panel';
import { ReviewPanel } from '@/components/workspace/review-panel';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { queryKeys } from '@/lib/query-keys';
import { cn } from '@/lib/utils';

const DRAFT_PREFIX = 'revexa.draft.';

/** Reads a saved draft for this problem, falling back to the starter for the preferred language. */
function restoreDraft(problemId: string, preferredLanguage: string): { code: string; language: string } {
  try {
    const saved = window.localStorage.getItem(DRAFT_PREFIX + problemId);
    if (saved) {
      const parsed = JSON.parse(saved) as { code?: string; language?: string };
      const language = parsed.language ?? preferredLanguage;
      return { language, code: parsed.code || languageOption(language).starter };
    }
  } catch {
    // A corrupt or unavailable draft is not worth failing the page over.
  }
  return { language: preferredLanguage, code: languageOption(preferredLanguage).starter };
}

/**
 * The workspace: problem, editor and mentor panels in one view.
 *
 * Panel state lives here rather than in each panel so that switching tabs never loses a review or a
 * conversation, and the code in the editor is the single source every panel analyses.
 */
export default function WorkspacePage() {
  const params = useParams<{ problemId: string }>();
  const searchParams = useSearchParams();
  const problemId = params.problemId;
  const { user } = useAuth();
  const queryClient = useQueryClient();

  // Draft state is seeded during the first render rather than in an effect: the editor should never
  // paint empty and then swap in the restored code.
  const [draft] = React.useState(() => restoreDraft(problemId, user?.preferredLanguage ?? 'python'));
  const [language, setLanguage] = React.useState(draft.language);
  const [code, setCode] = React.useState(draft.code);
  const [freshReview, setFreshReview] = React.useState<{ id: string; result: CodeReviewResult } | null>(null);
  const [tab, setTab] = React.useState('review');
  const [statementOpen, setStatementOpen] = React.useState(true);

  const problem = useQuery({ queryKey: queryKeys.problem(problemId), queryFn: () => api.getProblem(problemId) });

  // Persist the draft so a refresh mid-problem does not lose work.
  React.useEffect(() => {
    try {
      window.localStorage.setItem(DRAFT_PREFIX + problemId, JSON.stringify({ code, language }));
    } catch {
      // Storage unavailable — the draft simply will not survive a refresh.
    }
  }, [code, language, problemId]);

  // Deep link from the dashboard: /workspace/:id?review=:reviewId
  const linkedReviewId = searchParams.get('review');
  const linkedReview = useQuery({
    queryKey: queryKeys.review(linkedReviewId ?? ''),
    queryFn: () => api.getReview(linkedReviewId!),
    enabled: !!linkedReviewId,
  });

  // Derived, not copied into state: a review the user just ran wins over one opened from a link.
  const review = freshReview?.result ?? linkedReview.data?.result ?? null;
  const reviewId = freshReview?.id ?? linkedReview.data?.id;

  const runReview = useMutation({
    mutationFn: () => api.review({ problemId, code, language, force: true }),
    onSuccess: (result) => {
      setFreshReview({ id: result.id, result: result.result });
      setTab('review');
      queryClient.invalidateQueries({ queryKey: queryKeys.progress });
      queryClient.invalidateQueries({ queryKey: queryKeys.recommendations });
      queryClient.invalidateQueries({ queryKey: queryKeys.reviews() });
      toast.success('Review ready');
    },
    onError: (error) => {
      toast.error(error instanceof RevexaApiError ? error.message : 'Could not run the review');
    },
  });

  const dryRun = useMutation({
    mutationFn: () => api.run({ language, code }),
    onSuccess: (result) => {
      toast.info(result.note, { description: result.stderr || undefined, duration: 8000 });
    },
    onError: () => toast.error('The sandbox is not available'),
  });

  if (problem.isLoading) {
    return (
      <div className="grid h-[calc(100dvh-3.5rem)] grid-cols-1 gap-px lg:grid-cols-[340px_1fr_400px]">
        <Skeleton className="h-full rounded-none" />
        <Skeleton className="h-full rounded-none" />
        <Skeleton className="hidden h-full rounded-none lg:block" />
      </div>
    );
  }

  if (problem.isError || !problem.data) {
    return (
      <div className="p-8">
        <EmptyState
          icon={BookOpenIcon}
          title="Problem not found"
          description="It may have been removed, or it belongs to another account."
        />
      </div>
    );
  }

  const hasCode = code.trim().length > 0;

  const panels = (
    <Tabs value={tab} onValueChange={setTab} className="flex h-full flex-col gap-0">
      <TabsList className="m-2 grid w-auto grid-cols-5">
        <TabsTrigger value="review" className="gap-1">
          <SparklesIcon />
          <span className="hidden xl:inline">Review</span>
        </TabsTrigger>
        <TabsTrigger value="hints" className="gap-1">
          <LightbulbIcon />
          <span className="hidden xl:inline">Hints</span>
        </TabsTrigger>
        <TabsTrigger value="complexity" className="gap-1">
          <GaugeIcon />
          <span className="hidden xl:inline">Cost</span>
        </TabsTrigger>
        <TabsTrigger value="compare" className="gap-1">
          <GitCompareIcon />
          <span className="hidden xl:inline">Compare</span>
        </TabsTrigger>
        <TabsTrigger value="chat" className="gap-1">
          <MessagesSquareIcon />
          <span className="hidden xl:inline">Chat</span>
        </TabsTrigger>
      </TabsList>

      <div className="min-h-0 flex-1 overflow-y-auto">
        <TabsContent value="review" className="m-0">
          <ReviewPanel
            review={review}
            problemId={problemId}
            reviewId={reviewId}
            isPending={runReview.isPending}
            onRun={() => runReview.mutate()}
            hasCode={hasCode}
          />
        </TabsContent>
        <TabsContent value="hints" className="m-0">
          <HintPanel problemId={problemId} code={code} language={language} />
        </TabsContent>
        <TabsContent value="complexity" className="m-0">
          <ComplexityPanel complexity={review?.complexity ?? null} problemId={problemId} />
        </TabsContent>
        <TabsContent value="compare" className="m-0">
          <ComparisonPanel problemId={problemId} code={code} language={language} />
        </TabsContent>
        <TabsContent value="chat" className="m-0 h-full">
          <ChatPanel problemId={problemId} code={code} language={language} />
        </TabsContent>
      </div>
    </Tabs>
  );

  return (
    <div
      className={cn(
        'grid h-[calc(100dvh-3.5rem)] grid-cols-1 divide-x lg:grid-rows-1',
        statementOpen ? 'lg:grid-cols-[340px_minmax(0,1fr)_420px]' : 'lg:grid-cols-[0px_minmax(0,1fr)_420px]',
      )}
    >
      {/* Problem statement */}
      <aside className={cn('hidden min-h-0 overflow-hidden lg:block', !statementOpen && 'lg:hidden')}>
        <ProblemPanel problem={problem.data} />
      </aside>

      {/* Editor */}
      <section className="flex min-h-0 min-w-0 flex-col">
        <div className="flex flex-wrap items-center gap-2 border-b px-3 py-2">
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon-sm"
                className="hidden lg:inline-flex"
                onClick={() => setStatementOpen((open) => !open)}
                aria-label={statementOpen ? 'Hide problem statement' : 'Show problem statement'}
              >
                {statementOpen ? <PanelLeftCloseIcon /> : <PanelLeftOpenIcon />}
              </Button>
            </TooltipTrigger>
            <TooltipContent>{statementOpen ? 'Hide statement' : 'Show statement'}</TooltipContent>
          </Tooltip>

          <Select
            value={language}
            onValueChange={(next) => {
              setLanguage(next);
              // Only replace untouched starter code, never something the learner has written.
              const isStarter = LANGUAGES.some((option) => option.starter.trim() === code.trim());
              if (!code.trim() || isStarter) {
                setCode(languageOption(next).starter);
              }
            }}
          >
            <SelectTrigger size="sm" className="w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {LANGUAGES.map((option) => (
                <SelectItem key={option.id} value={option.id}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <div className="ml-auto flex items-center gap-2">
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={!hasCode || dryRun.isPending}
                  onClick={() => dryRun.mutate()}
                >
                  {dryRun.isPending ? <Loader2Icon className="animate-spin" /> : <TerminalIcon />}
                  Dry run
                </Button>
              </TooltipTrigger>
              <TooltipContent>
                Static dry run — reports what a real judge would hit first. No code is executed in this deployment.
              </TooltipContent>
            </Tooltip>

            <Button size="sm" disabled={!hasCode || runReview.isPending} onClick={() => runReview.mutate()}>
              {runReview.isPending ? <Loader2Icon className="animate-spin" /> : <PlayIcon />}
              Review my solution
            </Button>
          </div>
        </div>

        <div className="min-h-0 flex-1">
          <CodeEditor value={code} language={language} onChange={setCode} />
        </div>

        {/* Statement on small screens, where there is no room for a third column. */}
        <details className="border-t lg:hidden">
          <summary className="cursor-pointer px-3 py-2 text-sm font-medium">Problem statement</summary>
          <div className="max-h-72 overflow-y-auto border-t">
            <ProblemPanel problem={problem.data} />
          </div>
        </details>
      </section>

      {/* Mentor panels */}
      <aside className="flex min-h-0 flex-col border-t lg:border-t-0">{panels}</aside>
    </div>
  );
}
