'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RevexaApiError } from '@revexa/core';
import { InfoIcon, Loader2Icon, PlusIcon } from 'lucide-react';
import { useRouter } from 'next/navigation';
import * as React from 'react';
import { toast } from 'sonner';

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Textarea } from '@/components/ui/textarea';
import { api } from '@/lib/api';
import { queryKeys } from '@/lib/query-keys';

/**
 * Import is deliberately explicit about what each provider can and cannot do — the capability notes
 * come straight from the backend registry rather than being restated here, so the UI cannot promise
 * an integration the server does not have.
 */
export function ImportProblemDialog({ trigger }: { trigger?: React.ReactNode }) {
  const [open, setOpen] = React.useState(false);
  const [rawText, setRawText] = React.useState('');
  const [url, setUrl] = React.useState('');
  const [title, setTitle] = React.useState('');
  const [difficulty, setDifficulty] = React.useState('UNKNOWN');
  const router = useRouter();
  const queryClient = useQueryClient();

  const platforms = useQuery({ queryKey: queryKeys.platforms, queryFn: () => api.listPlatforms(), enabled: open });

  const importMutation = useMutation({
    mutationFn: () =>
      api.importProblem({
        rawText: rawText.trim() || undefined,
        url: url.trim() || undefined,
        title: title.trim() || undefined,
        difficulty: difficulty === 'UNKNOWN' ? undefined : difficulty,
      }),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['problems'] });
      result.warnings.forEach((warning) => toast.warning(warning, { duration: 8000 }));
      toast.success(`Imported via ${result.providerName}`);
      setOpen(false);
      setRawText('');
      setUrl('');
      setTitle('');
      router.push(`/workspace/${result.problem.id}`);
    },
    onError: (error) => {
      toast.error(error instanceof RevexaApiError ? error.message : 'Import failed');
    },
  });

  const canSubmit = rawText.trim().length > 0 || url.trim().length > 0;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {trigger ?? (
          <Button size="sm">
            <PlusIcon />
            Add a problem
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Add a problem</DialogTitle>
          <DialogDescription>
            Paste the statement from any platform. Revexa parses the constraints, examples and likely topics out of it.
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="paste">
          <TabsList className="w-full">
            <TabsTrigger value="paste">Paste</TabsTrigger>
            <TabsTrigger value="link">Link + paste</TabsTrigger>
            <TabsTrigger value="integrations">Integrations</TabsTrigger>
          </TabsList>

          <TabsContent value="paste" className="mt-4 space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="import-title">Title (optional)</Label>
              <Input
                id="import-title"
                placeholder="Inferred from the first line if you leave this blank"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="import-text">Problem statement</Label>
              <Textarea
                id="import-text"
                className="min-h-56 font-mono text-xs"
                placeholder={'Two Sum\n\nGiven an array of integers nums and an integer target...\n\nConstraints:\n2 <= nums.length <= 10^4'}
                value={rawText}
                onChange={(event) => setRawText(event.target.value)}
              />
              <p className="text-muted-foreground text-xs">
                Include the Constraints block — the bounds are what tell the reviewer which complexity you are aiming for.
              </p>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="import-difficulty">Difficulty</Label>
              <Select value={difficulty} onValueChange={setDifficulty}>
                <SelectTrigger id="import-difficulty" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="UNKNOWN">Detect automatically</SelectItem>
                  <SelectItem value="EASY">Easy</SelectItem>
                  <SelectItem value="MEDIUM">Medium</SelectItem>
                  <SelectItem value="HARD">Hard</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </TabsContent>

          <TabsContent value="link" className="mt-4 space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="import-url">Problem URL</Label>
              <Input
                id="import-url"
                placeholder="https://leetcode.com/problems/two-sum/"
                value={url}
                onChange={(event) => setUrl(event.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="import-link-text">Statement</Label>
              <Textarea
                id="import-link-text"
                className="min-h-40 font-mono text-xs"
                placeholder="Paste the problem text from the page…"
                value={rawText}
                onChange={(event) => setRawText(event.target.value)}
              />
            </div>
            <Alert variant="info">
              <InfoIcon />
              <AlertTitle>Why the paste is still needed</AlertTitle>
              <AlertDescription>
                LeetCode publishes no public problem API, and scraping it server-side would breach their terms. The URL
                gives Revexa the slug and title; the text has to come from the page you already have open.
              </AlertDescription>
            </Alert>
          </TabsContent>

          <TabsContent value="integrations" className="mt-4 space-y-3">
            {platforms.isLoading ? (
              <div className="flex justify-center py-8">
                <Loader2Icon className="text-muted-foreground size-5 animate-spin" />
              </div>
            ) : (
              platforms.data?.map((provider) => (
                <div key={provider.id} className="rounded-lg border p-3.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-medium">{provider.displayName}</p>
                    {provider.automaticImport ? (
                      <Badge variant="optimal">One click</Badge>
                    ) : (
                      <Badge variant="muted">Manual</Badge>
                    )}
                    {provider.requiresUserAuthorization && <Badge variant="outline">You authorise it</Badge>}
                  </div>
                  <p className="text-muted-foreground mt-1.5 text-xs leading-relaxed text-pretty">{provider.notes}</p>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {provider.acceptedInputs.map((input) => (
                      <Badge key={input} variant="outline" className="font-normal">
                        {input}
                      </Badge>
                    ))}
                  </div>
                </div>
              ))
            )}
          </TabsContent>
        </Tabs>

        <DialogFooter>
          <Button variant="ghost" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button disabled={!canSubmit || importMutation.isPending} onClick={() => importMutation.mutate()}>
            {importMutation.isPending && <Loader2Icon className="animate-spin" />}
            Import and open workspace
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
