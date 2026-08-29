import {
  ArrowRightIcon,
  BrainIcon,
  GaugeIcon,
  GitCompareIcon,
  LayersIcon,
  LineChartIcon,
  MessagesSquareIcon,
  ShieldCheckIcon,
  SparklesIcon,
} from 'lucide-react';
import Link from 'next/link';

import { RevexaLogo } from '@/components/layout/logo';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import { HintLadderPreview } from '@/components/marketing/hint-ladder-preview';
import { ReviewPreview } from '@/components/marketing/review-preview';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

const FEATURES = [
  {
    icon: BrainIcon,
    title: 'Reads your intent, not just your syntax',
    body: 'The review names the approach you reached for and the intuition behind it, then explains it back to you in plain terms — so you can tell whether you solved the problem you thought you were solving.',
  },
  {
    icon: GaugeIcon,
    title: 'Complexity with the reasoning shown',
    body: 'Time and space are estimated from the actual structure of your code — loop nesting, recursion shape, the data structures in play — and every figure comes with the breakdown that justifies it.',
  },
  {
    icon: LayersIcon,
    title: 'Six rungs, one at a time',
    body: 'Clarify, observe, concept, direction, pseudocode, solution. The ladder only ever moves up one step per request, and the last rung needs you to ask for it by name.',
  },
  {
    icon: MessagesSquareIcon,
    title: 'A conversation that holds the context',
    body: 'The chat already knows the problem and your current code. Ask why an approach fails, what you are missing, or what to test — it answers with a sharper question when you are close.',
  },
  {
    icon: GitCompareIcon,
    title: 'Honest comparison',
    body: 'Your approach beside the optimal one and the real alternatives, with the trade-offs spelled out and the single missing observation named. If you are already optimal, it says so.',
  },
  {
    icon: LineChartIcon,
    title: 'Progress that means something',
    body: 'Weak topics, recurring mistakes, complexity mix and hint dependency, all derived from your actual reviews — plus an exportable learning report.',
  },
];

const LADDER = [
  { step: 'Understanding', detail: 'What is really being asked, and what is easy to misread.' },
  { step: 'Observation', detail: 'The structure in the data the solution turns on.' },
  { step: 'Hint', detail: 'The concept, named without naming the algorithm.' },
  { step: 'Approach', detail: 'The family of algorithms and the target complexity.' },
  { step: 'Implementation', detail: 'Pseudocode — the shape, not the code.' },
  { step: 'Optimisation', detail: 'The full solution, only once you ask for it.' },
];

export default function LandingPage() {
  return (
    <div className="min-h-dvh">
      <header className="sticky top-0 z-40 border-b bg-background/80 backdrop-blur-md">
        <nav className="mx-auto flex h-16 max-w-6xl items-center justify-between px-5">
          <Link href="/" aria-label="Revexa.Ai home">
            <RevexaLogo />
          </Link>
          <div className="flex items-center gap-2">
            <Link href="#how-it-works" className="text-muted-foreground hover:text-foreground hidden px-3 text-sm transition-colors sm:block">
              How it works
            </Link>
            <Link href="#features" className="text-muted-foreground hover:text-foreground hidden px-3 text-sm transition-colors sm:block">
              Features
            </Link>
            <ThemeToggle />
            <Button variant="ghost" size="sm" asChild>
              <Link href="/login">Sign in</Link>
            </Button>
            <Button size="sm" asChild>
              <Link href="/register">Start practising</Link>
            </Button>
          </div>
        </nav>
      </header>

      <main>
        {/* Hero */}
        <section className="revexa-glow relative overflow-hidden">
          <div className="revexa-grid absolute inset-0 -z-10" aria-hidden="true" />
          <div className="mx-auto max-w-6xl px-5 pt-16 pb-20 sm:pt-24 sm:pb-28">
            <div className="grid items-center gap-12 lg:grid-cols-[1.05fr_1fr]">
              <div>
                <Badge variant="muted" className="mb-5 gap-1.5 py-1">
                  <SparklesIcon className="size-3" />
                  Understanding → Observation → Hint → Approach
                </Badge>
                <h1 className="text-4xl leading-[1.08] font-semibold tracking-tight text-balance sm:text-5xl lg:text-6xl">
                  A mentor beside you,
                  <br />
                  <span className="text-primary">not an answer key.</span>
                </h1>
                <p className="text-muted-foreground mt-6 max-w-xl text-lg leading-relaxed text-pretty">
                  You solved it — but is it optimal? Does a better complexity exist? Did you even read the problem
                  right? Revexa reviews your solution, costs it honestly, and walks you toward the insight instead of
                  handing you the code.
                </p>
                <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                  <Button size="lg" asChild>
                    <Link href="/register">
                      Create a free account
                      <ArrowRightIcon />
                    </Link>
                  </Button>
                  <Button size="lg" variant="outline" asChild>
                    <Link href="/login?demo=1">Explore the demo account</Link>
                  </Button>
                </div>
                <p className="text-muted-foreground mt-4 flex items-center gap-1.5 text-xs">
                  <ShieldCheckIcon className="size-3.5" />
                  The full solution is never shown by default — you have to ask for it.
                </p>
              </div>

              <ReviewPreview />
            </div>
          </div>
        </section>

        {/* The problem */}
        <section className="border-y bg-surface-raised">
          <div className="mx-auto max-w-6xl px-5 py-16">
            <div className="grid gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:gap-16">
              <div>
                <h2 className="text-2xl font-semibold tracking-tight text-balance sm:text-3xl">
                  Getting &ldquo;Accepted&rdquo; is the least useful signal in practice.
                </h2>
                <p className="text-muted-foreground mt-4 leading-relaxed">
                  A green tick tells you the tests passed. It does not tell you whether you were lucky, whether your
                  solution collapses at the real constraints, or which observation you walked straight past.
                </p>
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                {[
                  'Is my solution actually optimal?',
                  'Does a better time or space complexity exist?',
                  'Why is that other approach better than mine?',
                  'Did I understand the problem correctly?',
                  'How do I get the intuition without reading the answer?',
                  'Which patterns do I keep missing?',
                ].map((question) => (
                  <div key={question} className="bg-card flex items-start gap-2.5 rounded-lg border p-3.5 text-sm">
                    <span className="text-muted-foreground mt-px font-mono text-xs">?</span>
                    <span className="text-pretty">{question}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* How it works */}
        <section id="how-it-works" className="mx-auto max-w-6xl scroll-mt-20 px-5 py-20">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-2xl font-semibold tracking-tight text-balance sm:text-3xl">
              The ladder is the product
            </h2>
            <p className="text-muted-foreground mt-4 leading-relaxed text-pretty">
              Every response sits at a level, and the level only rises when you ask it to. That constraint is enforced
              in the API, not just in the interface — so the assistant cannot talk itself into spoiling the problem.
            </p>
          </div>

          <div className="mt-12 grid gap-10 lg:grid-cols-[1fr_1.15fr] lg:items-center lg:gap-16">
            <ol className="space-y-1">
              {LADDER.map((item, index) => (
                <li key={item.step} className="flex gap-4 rounded-lg p-3 transition-colors hover:bg-accent/50">
                  <span className="bg-primary/12 text-primary flex size-7 shrink-0 items-center justify-center rounded-md font-mono text-xs font-semibold">
                    {index + 1}
                  </span>
                  <div>
                    <p className="text-sm font-medium">{item.step}</p>
                    <p className="text-muted-foreground text-sm text-pretty">{item.detail}</p>
                  </div>
                </li>
              ))}
            </ol>
            <HintLadderPreview />
          </div>
        </section>

        {/* Features */}
        <section id="features" className="border-t bg-surface-raised scroll-mt-20">
          <div className="mx-auto max-w-6xl px-5 py-20">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-2xl font-semibold tracking-tight text-balance sm:text-3xl">
                Everything a good reviewer would tell you
              </h2>
              <p className="text-muted-foreground mt-4 leading-relaxed text-pretty">
                Six analysis stages, each with its own prompt and its own structured output, so every panel renders
                something specific rather than a wall of prose.
              </p>
            </div>
            <div className="mt-12 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {FEATURES.map((feature) => (
                <Card key={feature.title} className="transition-shadow hover:shadow-md">
                  <CardContent className="p-5">
                    <div className="bg-primary/10 text-primary mb-4 flex size-9 items-center justify-center rounded-lg">
                      <feature.icon className="size-[18px]" />
                    </div>
                    <h3 className="font-medium tracking-tight">{feature.title}</h3>
                    <p className="text-muted-foreground mt-2 text-sm leading-relaxed text-pretty">{feature.body}</p>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </section>

        {/* CTA */}
        <section className="mx-auto max-w-6xl px-5 py-20">
          <Card className="revexa-glow overflow-hidden border-primary/20">
            <CardContent className="flex flex-col items-center gap-6 p-10 text-center sm:p-14">
              <h2 className="max-w-2xl text-2xl font-semibold tracking-tight text-balance sm:text-3xl">
                Paste a problem you have already solved. See what you missed.
              </h2>
              <p className="text-muted-foreground max-w-xl leading-relaxed text-pretty">
                Works with any platform and any of nine languages. No integration to set up, nothing to authorise —
                paste the statement and your code, and the review is ready in a second.
              </p>
              <div className="flex flex-col gap-3 sm:flex-row">
                <Button size="lg" asChild>
                  <Link href="/register">
                    Start practising
                    <ArrowRightIcon />
                  </Link>
                </Button>
                <Button size="lg" variant="outline" asChild>
                  <Link href="/login?demo=1">Open the demo workspace</Link>
                </Button>
              </div>
            </CardContent>
          </Card>
        </section>
      </main>

      <footer className="border-t">
        <div className="text-muted-foreground mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-5 py-8 text-sm sm:flex-row">
          <RevexaLogo />
          <p>Understanding first, answers last.</p>
        </div>
      </footer>
    </div>
  );
}
