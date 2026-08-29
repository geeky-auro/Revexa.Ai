'use client';

import { loader } from '@monaco-editor/react';

/**
 * Self-hosts Monaco instead of pulling it from a CDN at runtime.
 *
 * `@monaco-editor/react` defaults to fetching Monaco from jsDelivr, which makes the editor — the
 * centre of the workspace — depend on a third party being reachable. Bundling it means the app works
 * offline, behind a corporate proxy, and in an air-gapped deployment, and the version is pinned by
 * the lockfile rather than by a URL.
 */
let setupPromise: Promise<void> | null = null;

export function setupMonaco(): Promise<void> {
  setupPromise ??= (async () => {
    const monaco = await import('monaco-editor');

    // Monaco offloads tokenisation and language services to web workers. Turbopack and webpack both
    // understand this `new URL(..., import.meta.url)` form and emit the worker as its own chunk.
    // The specifiers go through monaco's exports map ("./*.js" -> "./esm/vs/*.js"), so they must not
    // include the esm/vs prefix.
    window.MonacoEnvironment = {
      getWorker(_workerId: string, label: string) {
        switch (label) {
          case 'typescript':
          case 'javascript':
            return new Worker(
              new URL('monaco-editor/language/typescript/ts.worker.js', import.meta.url),
              { type: 'module' },
            );
          case 'json':
            return new Worker(new URL('monaco-editor/language/json/json.worker.js', import.meta.url), {
              type: 'module',
            });
          default:
            return new Worker(new URL('monaco-editor/editor/editor.worker.js', import.meta.url), {
              type: 'module',
            });
        }
      },
    };

    loader.config({ monaco });
    await loader.init();
  })();

  return setupPromise;
}
