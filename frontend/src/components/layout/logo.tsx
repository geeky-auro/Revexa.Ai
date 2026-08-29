import { cn } from '@/lib/utils';

/**
 * The mark: a bracket that opens into an ascending path — the product's whole claim in one glyph,
 * that code review should move you upward rather than hand you an answer.
 */
export function RevexaMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" fill="none" className={cn('size-7', className)} aria-hidden="true">
      <rect width="32" height="32" rx="9" fill="url(#revexa-gradient)" />
      <path
        d="M12.5 9.5 8.5 16l4 6.5"
        stroke="white"
        strokeWidth="2.1"
        strokeLinecap="round"
        strokeLinejoin="round"
        opacity="0.85"
      />
      <path
        d="M15.5 21.5 19 14.5l2.5 4 2-3.5"
        stroke="white"
        strokeWidth="2.1"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <defs>
        <linearGradient id="revexa-gradient" x1="0" y1="0" x2="32" y2="32" gradientUnits="userSpaceOnUse">
          <stop stopColor="#7C5CFF" />
          <stop offset="1" stopColor="#4F46E5" />
        </linearGradient>
      </defs>
    </svg>
  );
}

export function RevexaLogo({ className, showText = true }: { className?: string; showText?: boolean }) {
  return (
    <span className={cn('flex items-center gap-2', className)}>
      <RevexaMark />
      {showText && (
        <span className="text-[0.98rem] font-semibold tracking-tight">
          Revexa<span className="text-primary">.Ai</span>
        </span>
      )}
    </span>
  );
}
