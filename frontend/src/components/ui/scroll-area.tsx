'use client';

import * as ScrollAreaPrimitive from '@radix-ui/react-scroll-area';
import * as React from 'react';

import { cn } from '@/lib/utils';

function ScrollArea({ className, children, ...props }: React.ComponentProps<typeof ScrollAreaPrimitive.Root>) {
  return (
    <ScrollAreaPrimitive.Root className={cn('relative', className)} {...props}>
      <ScrollAreaPrimitive.Viewport className="focus-visible:ring-ring/40 size-full rounded-[inherit] outline-none focus-visible:ring-[3px]">
        {children}
      </ScrollAreaPrimitive.Viewport>
      <ScrollAreaPrimitive.Scrollbar
        orientation="vertical"
        className="flex w-2 touch-none p-px transition-colors select-none"
      >
        <ScrollAreaPrimitive.Thumb className="bg-border relative flex-1 rounded-full" />
      </ScrollAreaPrimitive.Scrollbar>
      <ScrollAreaPrimitive.Corner />
    </ScrollAreaPrimitive.Root>
  );
}

export { ScrollArea };
