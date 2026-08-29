'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { InfoIcon, Loader2Icon } from 'lucide-react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { z } from 'zod';
import { RevexaApiError } from '@revexa/core';

import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { DEMO_CREDENTIALS } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';

const schema = z.object({
  email: z.email('Enter a valid email address'),
  password: z.string().min(1, 'Enter your password'),
});

type FormValues = z.infer<typeof schema>;

function LoginForm() {
  const { login, status } = useAuth();
  const router = useRouter();
  const params = useSearchParams();
  const expired = params.get('expired') === '1';
  const wantsDemo = params.get('demo') === '1';
  const [submitting, setSubmitting] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: wantsDemo ? DEMO_CREDENTIALS : { email: '', password: '' },
  });

  useEffect(() => {
    if (status === 'authenticated') {
      router.replace('/dashboard');
    }
  }, [status, router]);

  async function onSubmit(values: FormValues) {
    setSubmitting(true);
    try {
      await login(values.email, values.password);
      toast.success('Welcome back');
      router.push('/dashboard');
    } catch (error) {
      const message =
        error instanceof RevexaApiError ? error.message : 'Could not reach the server. Is the backend running?';
      form.setError('password', { message });
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="w-full max-w-sm shadow-lg">
      <CardHeader>
        <CardTitle className="text-xl">Welcome back</CardTitle>
        <CardDescription>Sign in to pick up where you left off.</CardDescription>
      </CardHeader>
      <CardContent>
        {expired && (
          <Alert variant="warning" className="mb-4">
            <InfoIcon />
            <AlertDescription>Your session expired. Sign in again to continue.</AlertDescription>
          </Alert>
        )}

        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              aria-invalid={!!form.formState.errors.email}
              {...form.register('email')}
            />
            {form.formState.errors.email && (
              <p className="text-destructive text-xs">{form.formState.errors.email.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              aria-invalid={!!form.formState.errors.password}
              {...form.register('password')}
            />
            {form.formState.errors.password && (
              <p className="text-destructive text-xs">{form.formState.errors.password.message}</p>
            )}
          </div>

          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting && <Loader2Icon className="animate-spin" />}
            Sign in
          </Button>
        </form>

        <div className="mt-4 space-y-3">
          <Button
            type="button"
            variant="outline"
            className="w-full"
            disabled={submitting}
            onClick={() => {
              form.setValue('email', DEMO_CREDENTIALS.email);
              form.setValue('password', DEMO_CREDENTIALS.password);
              void onSubmit(DEMO_CREDENTIALS);
            }}
          >
            Use the demo account
          </Button>
          <p className="text-muted-foreground text-center text-xs">
            The demo account ships with three weeks of practice history so the dashboard has something to show.
          </p>
        </div>

        <p className="text-muted-foreground mt-6 text-center text-sm">
          No account yet?{' '}
          <Link href="/register" className="text-primary font-medium hover:underline">
            Create one
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<Card className="h-96 w-full max-w-sm animate-pulse" />}>
      <LoginForm />
    </Suspense>
  );
}
