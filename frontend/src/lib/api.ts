'use client';

import { RevexaClient, type TokenStore } from '@revexa/core';

const ACCESS_KEY = 'revexa.access-token';
const REFRESH_KEY = 'revexa.refresh-token';

/**
 * Browser token storage.
 *
 * `localStorage` is the pragmatic choice for a prototype: it survives a refresh and keeps the API
 * stateless. A production deployment would move the refresh token into an httpOnly, SameSite cookie
 * issued by the backend — which is a change to this class alone, because nothing else in the app
 * touches token storage.
 */
class BrowserTokenStore implements TokenStore {
  getAccessToken() {
    return safeRead(ACCESS_KEY);
  }

  getRefreshToken() {
    return safeRead(REFRESH_KEY);
  }

  setTokens(accessToken: string, refreshToken: string) {
    safeWrite(ACCESS_KEY, accessToken);
    safeWrite(REFRESH_KEY, refreshToken);
  }

  clear() {
    safeRemove(ACCESS_KEY);
    safeRemove(REFRESH_KEY);
  }
}

function safeRead(key: string): string | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function safeWrite(key: string, value: string) {
  try {
    window.localStorage.setItem(key, value);
  } catch {
    // Storage can be unavailable (private mode, blocked cookies); the session simply won't persist.
  }
}

function safeRemove(key: string) {
  try {
    window.localStorage.removeItem(key);
  } catch {
    // Nothing to clean up.
  }
}

export const tokenStore = new BrowserTokenStore();

export const api = new RevexaClient({
  baseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? '/backend',
  tokens: tokenStore,
  onAuthExpired: () => {
    // A hard navigation on purpose: an expired session should drop every cached query and every
    // piece of in-memory state, which a client-side route transition would preserve. This also runs
    // outside React, where the router is not available.
    if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.href = '/login?expired=1';
    }
  },
});

export const DEMO_CREDENTIALS = { email: 'demo@revexa.ai', password: 'revexa-demo' };
