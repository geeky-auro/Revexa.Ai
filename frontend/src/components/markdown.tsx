import * as React from 'react';

import { cn } from '@/lib/utils';

/**
 * A deliberately small markdown renderer for mentor output.
 *
 * The AI is prompted for a constrained subset — paragraphs, bold, inline code and fenced blocks —
 * and rendering only that subset means no third-party parser and no raw HTML path, so model output
 * can never inject markup into the page.
 */
export function Markdown({ content, className }: { content: string; className?: string }) {
  const blocks = React.useMemo(() => splitBlocks(content ?? ''), [content]);

  return (
    <div className={cn('prose-mentor text-sm', className)}>
      {blocks.map((block, index) =>
        block.type === 'code' ? (
          <pre key={index}>
            <code>{block.content}</code>
          </pre>
        ) : block.type === 'list' ? (
          <ul key={index} className="my-2 space-y-1 pl-4">
            {block.items.map((item, itemIndex) => (
              <li key={itemIndex} className="list-disc">
                {renderInline(item)}
              </li>
            ))}
          </ul>
        ) : (
          <p key={index}>{renderInline(block.content)}</p>
        ),
      )}
    </div>
  );
}

type Block =
  | { type: 'paragraph'; content: string }
  | { type: 'code'; content: string }
  | { type: 'list'; items: string[] };

function splitBlocks(text: string): Block[] {
  const blocks: Block[] = [];
  const segments = text.split(/```/);

  segments.forEach((segment, index) => {
    if (index % 2 === 1) {
      // Odd segments sit inside a fence; drop an optional language tag on the first line.
      const withoutLanguage = segment.replace(/^[a-zA-Z]*\n/, '');
      blocks.push({ type: 'code', content: withoutLanguage.replace(/\n$/, '') });
      return;
    }
    for (const chunk of segment.split(/\n{2,}/)) {
      const trimmed = chunk.trim();
      if (!trimmed) continue;
      const lines = trimmed.split('\n');
      if (lines.every((line) => /^\s*[-*]\s+/.test(line))) {
        blocks.push({ type: 'list', items: lines.map((line) => line.replace(/^\s*[-*]\s+/, '')) });
      } else {
        blocks.push({ type: 'paragraph', content: trimmed });
      }
    }
  });

  return blocks;
}

function renderInline(text: string): React.ReactNode[] {
  const parts = text.split(/(\*\*[^*]+\*\*|`[^`]+`)/g);
  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }
    if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      return <code key={index}>{part.slice(1, -1)}</code>;
    }
    return <React.Fragment key={index}>{part}</React.Fragment>;
  });
}
