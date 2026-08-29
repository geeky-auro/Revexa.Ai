'use client';

import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import * as React from 'react';
import type { UserView } from '@revexa/core';

import { api, tokenStore } from '@/lib/api';
import { queryKeys } from '@/lib/query-keys';

interface AuthState {
  user: UserView | null;
  status: 'loading' | 'authenticated' | 'anonymous';
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, displayName: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  setUser: (user: UserView) => void;
}

const AuthContext = React.createContext<AuthState | null>(null);

/**
 * The session is a query, not an effect.
 *
 * Fetching the profile through TanStack Query — like every other server value in the app — gets
 * caching, deduplication and a single retry policy for free, and removes the mount effect that would
 * otherwise have to set state on its own. A failed `/auth/me` needs no cleanup here: the API client
 * already clears the tokens and redirects when a refresh is rejected.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  // Read once during the first render so signed-out visitors never see a loading flash.
  const [hasToken, setHasToken] = React.useState(() => Boolean(tokenStore.getAccessToken()));
  const router = useRouter();
  const queryClient = useQueryClient();

  const session = useQuery({
    queryKey: queryKeys.me,
    queryFn: () => api.me(),
    enabled: hasToken,
    retry: false,
    staleTime: 5 * 60_000,
  });

  const status: AuthState['status'] = !hasToken
    ? 'anonymous'
    : session.isError
      ? 'anonymous'
      : session.data
        ? 'authenticated'
        : 'loading';

  const value = React.useMemo<AuthState>(
    () => ({
      user: session.data ?? null,
      status,
      async login(email, password) {
        const auth = await api.login(email, password);
        queryClient.clear();
        queryClient.setQueryData(queryKeys.me, auth.user);
        setHasToken(true);
      },
      async register(email, displayName, password) {
        const auth = await api.register(email, displayName, password);
        queryClient.clear();
        queryClient.setQueryData(queryKeys.me, auth.user);
        setHasToken(true);
      },
      async logout() {
        await api.logout();
        setHasToken(false);
        queryClient.clear();
        router.push('/login');
      },
      async refreshUser() {
        await queryClient.invalidateQueries({ queryKey: queryKeys.me });
      },
      setUser(user) {
        queryClient.setQueryData(queryKeys.me, user);
      },
    }),
    [session.data, status, queryClient, router],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = React.useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return context;
}
