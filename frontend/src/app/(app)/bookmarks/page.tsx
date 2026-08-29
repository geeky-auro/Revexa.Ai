'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { formatRelativeTime } from '@revexa/core';
import { BookmarkIcon, Trash2Icon } from 'lucide-react';
import Link from 'next/link';
import { toast } from 'sonner';

import { EmptyState } from '@/components/empty-state';
import { Markdown } from '@/components/markdown';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { api } from '@/lib/api';
import { queryKeys } from '@/lib/query-keys';

export default function BookmarksPage() {
  const queryClient = useQueryClient();
  const bookmarks = useQuery({ queryKey: queryKeys.bookmarks, queryFn: () => api.listBookmarks() });

  const remove = useMutation({
    mutationFn: (id: string) => api.deleteBookmark(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.bookmarks });
      queryClient.invalidateQueries({ queryKey: queryKeys.progress });
      toast.success('Bookmark removed');
    },
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Bookmarks</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Saved explanations, snapshotted at the moment you kept them — re-running a review never rewrites one.
        </p>
      </div>

      {bookmarks.isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-28" />
          ))}
        </div>
      ) : bookmarks.data && bookmarks.data.length > 0 ? (
        <div className="grid gap-3 md:grid-cols-2">
          {bookmarks.data.map((bookmark) => (
            <Card key={bookmark.id}>
              <CardContent className="p-4">
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant="muted" className="capitalize">
                        {bookmark.kind.toLowerCase()}
                      </Badge>
                      <p className="font-medium text-pretty">{bookmark.title}</p>
                    </div>
                    {bookmark.problemTitle && (
                      <Link
                        href={`/workspace/${bookmark.problemId}`}
                        className="text-muted-foreground hover:text-foreground mt-1 inline-block text-xs transition-colors"
                      >
                        {bookmark.problemTitle}
                      </Link>
                    )}
                  </div>
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => remove.mutate(bookmark.id)}
                    aria-label="Remove bookmark"
                  >
                    <Trash2Icon />
                  </Button>
                </div>

                <Markdown content={bookmark.content} className="text-muted-foreground mt-2.5" />

                {bookmark.note && (
                  <p className="mt-2.5 border-l-2 border-primary/40 pl-2.5 text-xs italic">{bookmark.note}</p>
                )}
                <p className="text-muted-foreground mt-3 text-xs">{formatRelativeTime(bookmark.createdAt)}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <EmptyState
          icon={BookmarkIcon}
          title="Nothing saved yet"
          description="Any hint, insight or complexity explanation can be bookmarked from the workspace — use the bookmark icon beside it."
        />
      )}
    </div>
  );
}
