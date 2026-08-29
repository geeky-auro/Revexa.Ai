import Link from 'next/link';

import { Button } from '@/components/ui/button';

export default function NotFound() {
  return (
    <div className="revexa-glow flex min-h-dvh flex-col items-center justify-center gap-4 px-5 text-center">
      <p className="text-primary font-mono text-sm">404</p>
      <h1 className="text-2xl font-semibold tracking-tight">This page does not exist</h1>
      <p className="text-muted-foreground max-w-sm text-pretty">
        The link may be stale, or the problem may belong to another account.
      </p>
      <Button asChild>
        <Link href="/dashboard">Back to the dashboard</Link>
      </Button>
    </div>
  );
}
