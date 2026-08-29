'use client';

import Editor, { type OnMount } from '@monaco-editor/react';
import { languageOption } from '@revexa/core';
import { Loader2Icon } from 'lucide-react';
import { useTheme } from 'next-themes';
import * as React from 'react';

import { setupMonaco } from '@/lib/monaco-setup';

/**
 * Monaco, wired to the app's theme and to the language ids the backend analyser understands.
 *
 * The editor is loaded lazily by `@monaco-editor/react`; until it is ready a matching skeleton keeps
 * the workspace layout from jumping.
 */
export function CodeEditor({
  value,
  language,
  onChange,
  readOnly = false,
}: {
  value: string;
  language: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
}) {
  const { resolvedTheme } = useTheme();
  const editorRef = React.useRef<Parameters<OnMount>[0] | null>(null);
  const [ready, setReady] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    setupMonaco()
      .then(() => !cancelled && setReady(true))
      .catch(() => !cancelled && setReady(true));
    return () => {
      cancelled = true;
    };
  }, []);

  if (!ready) {
    return (
      <div className="text-muted-foreground flex h-full items-center justify-center gap-2 text-sm">
        <Loader2Icon className="size-4 animate-spin" />
        Loading editor…
      </div>
    );
  }

  return (
    <Editor
      value={value}
      language={languageOption(language).monacoId}
      theme={resolvedTheme === 'dark' ? 'vs-dark' : 'light'}
      onChange={(next) => onChange(next ?? '')}
      onMount={(editor) => {
        editorRef.current = editor;
      }}
      loading={
        <div className="text-muted-foreground flex h-full items-center justify-center gap-2 text-sm">
          <Loader2Icon className="size-4 animate-spin" />
          Loading editor…
        </div>
      }
      options={{
        readOnly,
        fontSize: 13,
        fontFamily: 'var(--font-mono), ui-monospace, monospace',
        fontLigatures: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        smoothScrolling: true,
        padding: { top: 14, bottom: 14 },
        renderLineHighlight: 'line',
        lineNumbersMinChars: 3,
        tabSize: 4,
        automaticLayout: true,
        scrollbar: { verticalScrollbarSize: 10, horizontalScrollbarSize: 10 },
        overviewRulerLanes: 0,
        bracketPairColorization: { enabled: true },
      }}
    />
  );
}
