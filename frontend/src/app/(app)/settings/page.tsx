'use client';

import { useMutation, useQuery } from '@tanstack/react-query';
import { LANGUAGES, RevexaApiError } from '@revexa/core';
import { CheckCircle2Icon, CircleIcon, DownloadIcon, Loader2Icon } from 'lucide-react';
import { useTheme } from 'next-themes';
import * as React from 'react';
import { toast } from 'sonner';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { queryKeys } from '@/lib/query-keys';

export default function SettingsPage() {
  const { user, setUser } = useAuth();
  const { theme, setTheme } = useTheme();
  const [displayName, setDisplayName] = React.useState(user?.displayName ?? '');
  const [preferredLanguage, setPreferredLanguage] = React.useState(user?.preferredLanguage ?? 'python');
  const [guided, setGuided] = React.useState((user?.mentorMode ?? 'guided') === 'guided');

  const meta = useQuery({ queryKey: queryKeys.meta, queryFn: () => api.meta() });
  const sandbox = useQuery({ queryKey: queryKeys.sandbox, queryFn: () => api.sandboxInfo() });

  const save = useMutation({
    mutationFn: () =>
      api.updateProfile({
        displayName,
        preferredLanguage,
        mentorMode: guided ? 'guided' : 'direct',
        theme: theme ?? 'system',
      }),
    onSuccess: (updated) => {
      setUser(updated);
      toast.success('Settings saved');
    },
    onError: (error) => toast.error(error instanceof RevexaApiError ? error.message : 'Could not save settings'),
  });

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
        <p className="text-muted-foreground mt-1 text-sm">Your profile, how the mentor behaves, and what this deployment can do.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Profile</CardTitle>
          <CardDescription>How you appear in the app.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="displayName">Display name</Label>
            <Input id="displayName" value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="email">Email</Label>
            <Input id="email" value={user?.email ?? ''} disabled />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Practice</CardTitle>
          <CardDescription>Defaults for the workspace.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          <div className="space-y-1.5">
            <Label htmlFor="language">Default language</Label>
            <Select value={preferredLanguage} onValueChange={setPreferredLanguage}>
              <SelectTrigger id="language" className="w-full sm:w-56">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {LANGUAGES.map((option) => (
                  <SelectItem key={option.id} value={option.id}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex items-start justify-between gap-6 rounded-lg border p-3.5">
            <div>
              <Label htmlFor="guided" className="mb-1">
                Guided mode
              </Label>
              <p className="text-muted-foreground text-sm text-pretty">
                Keeps hints progressive and the solution behind an explicit ask. Turning it off lets you jump further
                ahead, but the ladder is the part that builds intuition.
              </p>
            </div>
            <Switch id="guided" checked={guided} onCheckedChange={setGuided} />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="theme">Theme</Label>
            <Select value={theme ?? 'system'} onValueChange={setTheme}>
              <SelectTrigger id="theme" className="w-full sm:w-56">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="system">Match my system</SelectItem>
                <SelectItem value="light">Light</SelectItem>
                <SelectItem value="dark">Dark</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <Button disabled={save.isPending} onClick={() => save.mutate()}>
            {save.isPending && <Loader2Icon className="animate-spin" />}
            Save changes
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">This deployment</CardTitle>
          <CardDescription>Which AI provider is answering, and whether code execution is real.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <p className="text-muted-foreground text-xs font-medium tracking-wide uppercase">AI providers</p>
            {meta.data?.aiProviders.map((provider) => (
              <div key={provider.id} className="flex items-center gap-2.5 rounded-lg border p-3">
                {provider.active ? (
                  <CheckCircle2Icon className="size-4 text-[var(--optimal)]" />
                ) : (
                  <CircleIcon className="text-muted-foreground size-4" />
                )}
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium capitalize">{provider.id}</p>
                  <p className="text-muted-foreground truncate font-mono text-xs">{provider.model}</p>
                </div>
                {provider.active && <Badge variant="optimal">Active</Badge>}
                {!provider.available && !provider.active && <Badge variant="muted">No API key</Badge>}
              </div>
            ))}
            <p className="text-muted-foreground text-xs text-pretty">
              The heuristic provider is a deterministic offline engine — static analysis plus a curated pattern
              catalogue. Set an API key and switch <code className="font-mono">revexa.ai.provider</code> to route the
              same pipeline through a hosted model.
            </p>
          </div>

          {sandbox.data && (
            <div className="space-y-2">
              <p className="text-muted-foreground text-xs font-medium tracking-wide uppercase">Execution sandbox</p>
              <div className="rounded-lg border p-3">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium capitalize">{sandbox.data.provider}</p>
                  <Badge variant={sandbox.data.provider === 'simulated' ? 'improvable' : 'muted'}>
                    {sandbox.data.enabled ? 'Enabled' : 'Disabled'}
                  </Badge>
                </div>
                <p className="text-muted-foreground mt-1 text-xs text-pretty">
                  {sandbox.data.provider === 'simulated'
                    ? 'Dry runs only — no code is executed. Point revexa.sandbox.provider at a real runner to change that.'
                    : 'Code execution is disabled in this deployment.'}
                </p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Your data</CardTitle>
          <CardDescription>Everything Revexa knows about your practice, in one file.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button variant="outline" asChild>
            <a href="/backend/progress/report/export" download>
              <DownloadIcon />
              Export learning report (Markdown)
            </a>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
