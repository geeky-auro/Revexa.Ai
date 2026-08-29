'use client';

import type { ProblemDetail } from '@revexa/core';
import { ExternalLinkIcon } from 'lucide-react';

import { DifficultyBadge } from '@/components/verdict-badge';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';

export function ProblemPanel({ problem }: { problem: ProblemDetail }) {
  return (
    <div className="flex h-full flex-col">
      <div className="space-y-3 border-b p-4">
        <div className="flex items-start justify-between gap-3">
          <h1 className="text-lg leading-snug font-semibold tracking-tight text-pretty">{problem.title}</h1>
          <DifficultyBadge difficulty={problem.difficulty} />
        </div>
        <div className="flex flex-wrap gap-1.5">
          {problem.topics.map((topic) => (
            <Badge key={topic} variant="muted" className="font-normal">
              {topic}
            </Badge>
          ))}
        </div>
        {problem.url && (
          <a
            href={problem.url}
            target="_blank"
            rel="noreferrer noopener"
            className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-xs transition-colors"
          >
            <ExternalLinkIcon className="size-3" />
            Open the original problem
          </a>
        )}
      </div>

      <ScrollArea className="flex-1">
        <div className="space-y-5 p-4">
          <div className="text-sm leading-relaxed whitespace-pre-wrap text-pretty">{problem.statement}</div>

          {problem.examples.length > 0 && (
            <div className="space-y-2">
              <h2 className="text-muted-foreground text-xs font-medium tracking-wide uppercase">Examples</h2>
              {problem.examples.map((example, index) => (
                <div key={index} className="bg-code-bg space-y-1 rounded-lg border p-3 font-mono text-xs">
                  {example.input && (
                    <p>
                      <span className="text-muted-foreground">Input: </span>
                      {example.input}
                    </p>
                  )}
                  {example.output && (
                    <p>
                      <span className="text-muted-foreground">Output: </span>
                      {example.output}
                    </p>
                  )}
                  {example.explanation && (
                    <p className="text-muted-foreground font-sans text-[0.75rem] leading-relaxed">
                      {example.explanation}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}

          {problem.constraintsText && (
            <div className="space-y-2">
              <h2 className="text-muted-foreground text-xs font-medium tracking-wide uppercase">Constraints</h2>
              <pre className="bg-code-bg rounded-lg border p-3 font-mono text-xs leading-relaxed whitespace-pre-wrap">
                {problem.constraintsText.replace(/^Constraints:?\s*/i, '')}
              </pre>
            </div>
          )}
        </div>
      </ScrollArea>
    </div>
  );
}
