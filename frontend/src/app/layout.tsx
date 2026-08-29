import type { Metadata, Viewport } from 'next';
import { Inter, JetBrains_Mono } from 'next/font/google';

import { Providers } from '@/lib/providers';

import './globals.css';

const inter = Inter({ subsets: ['latin'], variable: '--font-inter', display: 'swap' });
const mono = JetBrains_Mono({ subsets: ['latin'], variable: '--font-mono-code', display: 'swap' });

export const metadata: Metadata = {
  title: {
    default: 'Revexa.Ai — the mentor beside you, not the answer key',
    template: '%s · Revexa.Ai',
  },
  description:
    'Revexa reviews your competitive programming solutions, estimates their real complexity, and walks you up a six-rung hint ladder — revealing the full solution only when you explicitly ask.',
  keywords: ['competitive programming', 'leetcode', 'code review', 'algorithms', 'complexity analysis', 'ai mentor'],
  authors: [{ name: 'Revexa.Ai' }],
  openGraph: {
    title: 'Revexa.Ai',
    description: 'An AI mentor for competitive programming. Understanding first, answers last.',
    type: 'website',
  },
};

export const viewport: Viewport = {
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#ffffff' },
    { media: '(prefers-color-scheme: dark)', color: '#12131a' },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${inter.variable} ${mono.variable} antialiased`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
