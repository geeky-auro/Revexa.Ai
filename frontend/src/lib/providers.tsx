'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from 'next-themes';
import * as React from 'react';
import { Toaster } from 'sonner';
import { RevexaApiError } from '@revexa/core';

import { TooltipProvider } from '@/components/ui/tooltip';
import { AuthProvider } from '@/lib/auth-context';

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        retry: (failureCount, error) => {
          // Never retry a rejection the user has to act on — only genuinely transient failures.
          if (error instanceof RevexaApiError) {
            return !error.isUnauthorized && !error.isRateLimited && error.status >= 500 && failureCount < 2;
          }
          return failureCount < 2;
        },
      },
      mutations: { retry: false },
    },
  });
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = React.useState(makeQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
        <TooltipProvider>
          <AuthProvider>{children}</AuthProvider>
          <Toaster position="bottom-right" richColors closeButton />
        </TooltipProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
