'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { BookmarkCheckIcon, BookmarkPlusIcon } from 'lucide-react';
import * as React from 'react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { api } from '@/lib/api';
import { queryKeys } from '@/lib/query-keys';

/** Saves a snapshot of an explanation, so it survives the review being re-run. */
export function BookmarkButton({
  kind,
  title,
  content,
  problemId,
  referenceId,
}: {
  kind: string;
  title: string;
  content: string;
  problemId?: string;
  referenceId?: string;
}) {
  const [saved, setSaved] = React.useState(false);
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => api.createBookmark({ kind, title, content, problemId, referenceId }),
    onSuccess: () => {
      setSaved(true);
      queryClient.invalidateQueries({ queryKey: queryKeys.bookmarks });
      queryClient.invalidateQueries({ queryKey: queryKeys.progress });
      toast.success('Saved to your bookmarks');
    },
    onError: () => toast.error('Could not save that bookmark'),
  });

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          variant="ghost"
          size="icon-sm"
          disabled={saved || mutation.isPending}
          onClick={() => mutation.mutate()}
          aria-label={saved ? 'Saved' : 'Save this explanation'}
        >
          {saved ? <BookmarkCheckIcon className="text-[var(--optimal)]" /> : <BookmarkPlusIcon />}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{saved ? 'Saved to bookmarks' : 'Save this explanation'}</TooltipContent>
    </Tooltip>
  );
}
