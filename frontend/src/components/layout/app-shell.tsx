'use client';

import {
  BookmarkIcon,
  CodeIcon,
  HistoryIcon,
  LayoutDashboardIcon,
  LibraryIcon,
  LogOutIcon,
  MenuIcon,
  SettingsIcon,
  XIcon,
} from 'lucide-react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import * as React from 'react';
import { initials } from '@revexa/core';

import { RevexaLogo } from '@/components/layout/logo';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useAuth } from '@/lib/auth-context';
import { cn } from '@/lib/utils';

const NAV = [
  { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboardIcon },
  { href: '/problems', label: 'Problems', icon: LibraryIcon },
  { href: '/history', label: 'Review history', icon: HistoryIcon },
  { href: '/bookmarks', label: 'Bookmarks', icon: BookmarkIcon },
  { href: '/settings', label: 'Settings', icon: SettingsIcon },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = React.useState(false);

  // The workspace is a dense, three-pane view; it gets the full width with no side rail.
  const isWorkspace = pathname.startsWith('/workspace');

  const nav = (
    <nav className="space-y-0.5">
      {NAV.map((item) => {
        const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={() => setMobileOpen(false)}
            className={cn(
              'flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition-colors',
              active ? 'bg-accent text-accent-foreground' : 'text-muted-foreground hover:bg-accent/60 hover:text-foreground',
            )}
          >
            <item.icon className="size-4" />
            {item.label}
          </Link>
        );
      })}
    </nav>
  );

  return (
    <div className="min-h-dvh">
      <header className="bg-background/85 sticky top-0 z-40 border-b backdrop-blur-md">
        <div className={cn('flex h-14 items-center gap-3 px-4', isWorkspace ? 'max-w-none' : 'mx-auto max-w-7xl')}>
          <Button
            variant="ghost"
            size="icon-sm"
            className="lg:hidden"
            onClick={() => setMobileOpen((open) => !open)}
            aria-label="Toggle navigation"
          >
            {mobileOpen ? <MenuIcon /> : <MenuIcon />}
          </Button>

          <Link href="/dashboard" aria-label="Revexa.Ai dashboard">
            <RevexaLogo />
          </Link>

          {isWorkspace && (
            <span className="text-muted-foreground ml-2 hidden items-center gap-1.5 text-sm sm:flex">
              <CodeIcon className="size-3.5" />
              Workspace
            </span>
          )}

          <div className="ml-auto flex items-center gap-1.5">
            <ThemeToggle />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Account menu">
                  <Avatar>
                    <AvatarFallback>{user ? initials(user.displayName) : '··'}</AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel className="font-normal">
                  <p className="text-sm font-medium">{user?.displayName}</p>
                  <p className="text-muted-foreground truncate text-xs">{user?.email}</p>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link href="/settings">
                    <SettingsIcon />
                    Settings
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem variant="destructive" onSelect={() => void logout()}>
                  <LogOutIcon />
                  Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>

        {mobileOpen && (
          <div className="bg-background border-t px-4 py-3 lg:hidden">
            {nav}
          </div>
        )}
      </header>

      {isWorkspace ? (
        <main className="px-0">{children}</main>
      ) : (
        <div className="mx-auto flex max-w-7xl gap-8 px-4 py-6">
          <aside className="hidden w-52 shrink-0 lg:block">
            <div className="sticky top-20">{nav}</div>
          </aside>
          <main className="min-w-0 flex-1 pb-16">{children}</main>
        </div>
      )}
    </div>
  );
}

/** Exported so pages can close a drawer without importing the icon set. */
export { XIcon as CloseIcon };
