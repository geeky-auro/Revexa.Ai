'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ChatThreadView } from '@revexa/core';
import { RevexaApiError, hintLevel } from '@revexa/core';
import { EyeOffIcon, Loader2Icon, SendIcon, SparklesIcon } from 'lucide-react';
import * as React from 'react';
import { toast } from 'sonner';

import { Markdown } from '@/components/markdown';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { api } from '@/lib/api';
import { queryKeys } from '@/lib/query-keys';
import { cn } from '@/lib/utils';

const STARTERS = [
  'I do not understand what this problem is asking',
  'Is my approach on the right track?',
  'What is the time complexity of my code?',
  'What edge cases should I test?',
];

/**
 * The conversational mentor.
 *
 * The thread carries the problem and the current editor contents, so questions like "is my approach
 * right?" resolve against what the learner has actually written rather than a generic answer.
 */
export function ChatPanel({
  problemId,
  code,
  language,
}: {
  problemId: string;
  code: string;
  language: string;
}) {
  const [threadId, setThreadId] = React.useState<string | null>(null);
  const [draft, setDraft] = React.useState('');
  const bottomRef = React.useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();

  const start = useMutation({
    mutationFn: () => api.startThread({ problemId }),
    onSuccess: (thread) => {
      setThreadId(thread.id);
      queryClient.setQueryData(queryKeys.thread(thread.id), thread);
    },
  });

  React.useEffect(() => {
    if (!threadId && !start.isPending) {
      start.mutate();
    }
    // One thread per problem view; switching problems remounts the panel.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [problemId]);

  const thread = useQuery({
    queryKey: queryKeys.thread(threadId ?? ''),
    queryFn: () => api.getThread(threadId!),
    enabled: !!threadId,
  });

  const send = useMutation({
    mutationFn: (message: string) => api.sendMessage(threadId!, { message, code, language }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.thread(threadId!) });
    },
    onError: (error) => {
      toast.error(error instanceof RevexaApiError ? error.message : 'The mentor did not respond');
    },
  });

  const data: ChatThreadView | undefined = thread.data;

  React.useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [data?.messages.length, send.isPending]);

  function submit(message: string) {
    const trimmed = message.trim();
    if (!trimmed || send.isPending) {
      return;
    }
    setDraft('');
    send.mutate(trimmed);
  }

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 space-y-4 overflow-y-auto p-4">
        {!data || data.messages.length === 0 ? (
          <div className="space-y-4 py-6">
            <div className="text-center">
              <div className="bg-primary/10 text-primary mx-auto flex size-10 items-center justify-center rounded-lg">
                <SparklesIcon className="size-5" />
              </div>
              <p className="mt-3 font-medium">Ask me anything about this problem</p>
              <p className="text-muted-foreground mx-auto mt-1 max-w-sm text-sm text-pretty">
                I already have the statement and your current code. I will nudge before I answer, and I will not hand
                over the solution unless you ask for it in so many words.
              </p>
            </div>
            <div className="space-y-2">
              {STARTERS.map((starter) => (
                <button
                  key={starter}
                  type="button"
                  onClick={() => submit(starter)}
                  className="hover:bg-accent w-full rounded-lg border px-3 py-2 text-left text-sm transition-colors"
                >
                  {starter}
                </button>
              ))}
            </div>
          </div>
        ) : (
          data.messages.map((message) => (
            <div
              key={message.id}
              className={cn('flex flex-col gap-1.5', message.role === 'user' ? 'items-end' : 'items-start')}
            >
              <div
                className={cn(
                  'max-w-[92%] rounded-xl px-3.5 py-2.5',
                  message.role === 'user' ? 'bg-primary text-primary-foreground' : 'bg-muted',
                )}
              >
                {message.role === 'user' ? (
                  <p className="text-sm leading-relaxed whitespace-pre-wrap">{message.content}</p>
                ) : (
                  <Markdown content={message.content} />
                )}
              </div>

              {message.role === 'assistant' && (
                <div className="flex flex-wrap items-center gap-1.5">
                  {message.revealedSolution ? (
                    <Badge variant="improvable" className="gap-1 text-[0.65rem]">
                      <EyeOffIcon className="size-2.5" />
                      Solution revealed
                    </Badge>
                  ) : (
                    <Badge variant="muted" className="text-[0.65rem]">
                      {hintLevel(message.spoilerLevel).short} level
                    </Badge>
                  )}
                  {message.followUpQuestions.slice(0, 2).map((question) => (
                    <button
                      key={question}
                      type="button"
                      onClick={() => submit(question)}
                      className="text-muted-foreground hover:border-primary/40 hover:text-foreground rounded-md border px-2 py-0.5 text-[0.7rem] transition-colors"
                    >
                      {question}
                    </button>
                  ))}
                </div>
              )}
            </div>
          ))
        )}

        {send.isPending && (
          <div className="text-muted-foreground flex items-center gap-2 text-sm">
            <Loader2Icon className="size-3.5 animate-spin" />
            Thinking…
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <div className="border-t p-3">
        <form
          onSubmit={(event) => {
            event.preventDefault();
            submit(draft);
          }}
          className="flex items-end gap-2"
        >
          <Textarea
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                submit(draft);
              }
            }}
            placeholder="Ask about the problem, your approach, or why something fails…"
            className="max-h-32 min-h-10 resize-none"
            rows={1}
          />
          <Button type="submit" size="icon" disabled={!draft.trim() || send.isPending} aria-label="Send message">
            {send.isPending ? <Loader2Icon className="animate-spin" /> : <SendIcon />}
          </Button>
        </form>
        <p className="text-muted-foreground mt-1.5 text-[0.7rem]">
          Spoilers stay closed unless you say &ldquo;reveal the solution&rdquo;.
        </p>
      </div>
    </div>
  );
}
