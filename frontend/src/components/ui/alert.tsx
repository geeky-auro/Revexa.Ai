import { cva, type VariantProps } from 'class-variance-authority';
import * as React from 'react';

import { cn } from '@/lib/utils';

const alertVariants = cva(
  'relative grid w-full grid-cols-[0_1fr] items-start gap-y-1 rounded-lg border px-4 py-3 text-sm has-[>svg]:grid-cols-[calc(var(--spacing)*4)_1fr] has-[>svg]:gap-x-3 [&>svg]:size-4 [&>svg]:translate-y-0.5',
  {
    variants: {
      variant: {
        default: 'bg-card text-card-foreground',
        info: 'bg-primary/8 border-primary/25 text-foreground [&>svg]:text-primary',
        warning: 'bg-[var(--improvable)]/10 border-[var(--improvable)]/30 [&>svg]:text-[var(--improvable)]',
        destructive: 'bg-destructive/8 border-destructive/30 text-foreground [&>svg]:text-destructive',
      },
    },
    defaultVariants: { variant: 'default' },
  },
);

function Alert({ className, variant, ...props }: React.ComponentProps<'div'> & VariantProps<typeof alertVariants>) {
  return <div role="alert" className={cn(alertVariants({ variant }), className)} {...props} />;
}

function AlertTitle({ className, ...props }: React.ComponentProps<'div'>) {
  return <div className={cn('col-start-2 min-h-4 font-medium tracking-tight', className)} {...props} />;
}

function AlertDescription({ className, ...props }: React.ComponentProps<'div'>) {
  return <div className={cn('text-muted-foreground col-start-2 text-sm [&_p]:leading-relaxed', className)} {...props} />;
}

export { Alert, AlertTitle, AlertDescription };
