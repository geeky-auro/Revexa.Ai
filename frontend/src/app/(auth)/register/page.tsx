'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2Icon } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { z } from 'zod';
import { RevexaApiError } from '@revexa/core';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAuth } from '@/lib/auth-context';

const schema = z.object({
  displayName: z.string().min(2, 'Use at least 2 characters').max(120),
  email: z.email('Enter a valid email address'),
  password: z.string().min(8, 'Use at least 8 characters').max(100),
});

type FormValues = z.infer<typeof schema>;

export default function RegisterPage() {
  const { register: signUp, status } = useAuth();
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { displayName: '', email: '', password: '' },
  });

  useEffect(() => {
    if (status === 'authenticated') {
      router.replace('/dashboard');
    }
  }, [status, router]);

  async function onSubmit(values: FormValues) {
    setSubmitting(true);
    try {
      await signUp(values.email, values.displayName, values.password);
      toast.success('Account created — import a problem to get started');
      router.push('/problems');
    } catch (error) {
      const message =
        error instanceof RevexaApiError ? error.message : 'Could not reach the server. Is the backend running?';
      form.setError('email', { message });
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="w-full max-w-sm shadow-lg">
      <CardHeader>
        <CardTitle className="text-xl">Create your account</CardTitle>
        <CardDescription>Free, and no card required for the prototype.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="displayName">Name</Label>
            <Input
              id="displayName"
              autoComplete="name"
              placeholder="Ada Lovelace"
              aria-invalid={!!form.formState.errors.displayName}
              {...form.register('displayName')}
            />
            {form.formState.errors.displayName && (
              <p className="text-destructive text-xs">{form.formState.errors.displayName.message}</p>
            )}
          </div>

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
              autoComplete="new-password"
              aria-invalid={!!form.formState.errors.password}
              {...form.register('password')}
            />
            {form.formState.errors.password ? (
              <p className="text-destructive text-xs">{form.formState.errors.password.message}</p>
            ) : (
              <p className="text-muted-foreground text-xs">At least 8 characters.</p>
            )}
          </div>

          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting && <Loader2Icon className="animate-spin" />}
            Create account
          </Button>
        </form>

        <p className="text-muted-foreground mt-6 text-center text-sm">
          Already have an account?{' '}
          <Link href="/login" className="text-primary font-medium hover:underline">
            Sign in
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
