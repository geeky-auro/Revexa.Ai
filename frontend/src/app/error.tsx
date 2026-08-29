'use client';

import { RefreshCwIcon } from 'lucide-react';
import { useEffect } from 'react';

import { Button } from '@/components/ui/button';

export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    // In a real deployment this is where the error reporter would be called.
    console.error(error);
  }, [error]);

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-4 px-5 text-center">
      <h1 className="text-2xl font-semibold tracking-tight">Something went wrong</h1>
      <p className="text-muted-foreground max-w-md text-pretty">
        The page hit an unexpected error. Retrying usually clears it; if it keeps happening, check that the backend is
        running.
      </p>
      <Button onClick={reset}>
        <RefreshCwIcon />
        Try again
      </Button>
    </div>
  );
}
