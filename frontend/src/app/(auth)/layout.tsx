import Link from 'next/link';

import { RevexaLogo } from '@/components/layout/logo';
import { ThemeToggle } from '@/components/layout/theme-toggle';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="revexa-glow flex min-h-dvh flex-col">
      <header className="flex items-center justify-between px-5 py-4">
        <Link href="/" aria-label="Revexa.Ai home">
          <RevexaLogo />
        </Link>
        <ThemeToggle />
      </header>
      <main className="flex flex-1 items-center justify-center px-5 pb-16">{children}</main>
    </div>
  );
}
