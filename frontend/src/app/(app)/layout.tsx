'use client';

import { Loader2Icon } from 'lucide-react';
import { useRouter } from 'next/navigation';
import * as React from 'react';

import { AppShell } from '@/components/layout/app-shell';
import { useAuth } from '@/lib/auth-context';

export default function AuthenticatedLayout({ children }: { children: React.ReactNode }) {
  const { status } = useAuth();
  const router = useRouter();

  React.useEffect(() => {
    if (status === 'anonymous') {
      router.replace('/login');
    }
  }, [status, router]);

  if (status !== 'authenticated') {
    return (
      <div className="flex min-h-dvh items-center justify-center">
        <Loader2Icon className="text-muted-foreground size-6 animate-spin" />
        <span className="sr-only">Loading your session</span>
      </div>
    );
  }

  return <AppShell>{children}</AppShell>;
}
