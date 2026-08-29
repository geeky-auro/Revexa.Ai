import type {
  AuthResponse,
  BookmarkView,
  ChatThreadView,
  ChatTurn,
  ComparisonView,
  ExecutionResult,
  HintSessionView,
  ImportResult,
  LearningReport,
  MetaResponse,
  PageResponse,
  PlatformProviderInfo,
  ProblemDetail,
  ProblemSummary,
  ProgressSummary,
  RecommendationBundle,
  ReviewSummary,
  ReviewView,
  SandboxInfo,
  SubmissionView,
  UserView,
} from './types.ts';

/** Thrown for any non-2xx response, carrying the backend's error envelope. */
export class RevexaApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors: { field: string; message: string }[] = [],
  ) {
    super(message);
    this.name = 'RevexaApiError';
  }

  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isRateLimited(): boolean {
    return this.status === 429;
  }
}

/**
 * Where tokens live. The web app uses `localStorage`; a VS Code extension would use
 * `SecretStorage`, and a mobile app the platform keychain. The client itself never assumes.
 */
export interface TokenStore {
  getAccessToken(): string | null | Promise<string | null>;
  getRefreshToken(): string | null | Promise<string | null>;
  setTokens(accessToken: string, refreshToken: string): void | Promise<void>;
  clear(): void | Promise<void>;
}

export class MemoryTokenStore implements TokenStore {
  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  getAccessToken() {
    return this.accessToken;
  }

  getRefreshToken() {
    return this.refreshToken;
  }

  setTokens(accessToken: string, refreshToken: string) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  clear() {
    this.accessToken = null;
    this.refreshToken = null;
  }
}

export interface RevexaClientOptions {
  baseUrl: string;
  tokens?: TokenStore;
  fetchImpl?: typeof fetch;
  /** Called when the refresh token is rejected, so the surface can send the user to sign in. */
  onAuthExpired?: () => void;
}

/**
 * The single API client for every Revexa surface.
 *
 * Transport-agnostic (inject any `fetch`), storage-agnostic (inject any `TokenStore`) and free of
 * UI-framework imports, so the same file backs the web app, a React Native app and an editor
 * extension. It transparently retries once through the refresh endpoint on a 401.
 */
export class RevexaClient {
  private readonly baseUrl: string;
  private readonly tokens: TokenStore;
  private readonly fetchImpl: typeof fetch;
  private readonly onAuthExpired?: () => void;
  private refreshInFlight: Promise<boolean> | null = null;

  constructor(options: RevexaClientOptions) {
    this.baseUrl = options.baseUrl.replace(/\/$/, '');
    this.tokens = options.tokens ?? new MemoryTokenStore();
    this.fetchImpl = options.fetchImpl ?? globalThis.fetch.bind(globalThis);
    this.onAuthExpired = options.onAuthExpired;
  }

  // ------------------------------------------------------------------ auth

  async register(email: string, displayName: string, password: string): Promise<AuthResponse> {
    const auth = await this.request<AuthResponse>('POST', '/auth/register', {
      body: { email, displayName, password },
      anonymous: true,
    });
    await this.tokens.setTokens(auth.accessToken, auth.refreshToken);
    return auth;
  }

  async login(email: string, password: string): Promise<AuthResponse> {
    const auth = await this.request<AuthResponse>('POST', '/auth/login', {
      body: { email, password },
      anonymous: true,
    });
    await this.tokens.setTokens(auth.accessToken, auth.refreshToken);
    return auth;
  }

  async logout(): Promise<void> {
    await this.tokens.clear();
  }

  me(): Promise<UserView> {
    return this.request<UserView>('GET', '/auth/me');
  }

  updateProfile(patch: Partial<Pick<UserView, 'displayName' | 'theme' | 'preferredLanguage' | 'mentorMode'>>) {
    return this.request<UserView>('PATCH', '/users/me', { body: patch });
  }

  // -------------------------------------------------------------- problems

  listProblems(params: { search?: string; difficulty?: string; page?: number; size?: number } = {}) {
    return this.request<PageResponse<ProblemSummary>>('GET', '/problems', { query: params });
  }

  getProblem(id: string) {
    return this.request<ProblemDetail>('GET', `/problems/${id}`);
  }

  createProblem(body: {
    title: string;
    statement: string;
    constraintsText?: string;
    difficulty?: string;
    topics?: string[];
    url?: string;
  }) {
    return this.request<ProblemDetail>('POST', '/problems', { body });
  }

  importProblem(body: {
    providerId?: string;
    rawText?: string;
    url?: string;
    title?: string;
    difficulty?: string;
    structuredPayload?: string;
  }) {
    return this.request<ImportResult>('POST', '/problems/import', { body });
  }

  listPlatforms() {
    return this.request<PlatformProviderInfo[]>('GET', '/problems/platforms');
  }

  // ------------------------------------------------------------- practice

  createSubmission(body: { problemId: string; code: string; language: string; notes?: string }) {
    return this.request<SubmissionView>('POST', '/submissions', { body });
  }

  listSubmissions(params: { problemId?: string; page?: number; size?: number } = {}) {
    return this.request<PageResponse<SubmissionView>>('GET', '/submissions', { query: params });
  }

  getSubmission(id: string) {
    return this.request<SubmissionView>('GET', `/submissions/${id}`);
  }

  review(body: { problemId?: string; submissionId?: string; code?: string; language?: string; force?: boolean }) {
    return this.request<ReviewView>('POST', '/reviews', { body });
  }

  getReview(id: string) {
    return this.request<ReviewView>('GET', `/reviews/${id}`);
  }

  listReviews(params: { problemId?: string; page?: number; size?: number } = {}) {
    return this.request<PageResponse<ReviewSummary>>('GET', '/reviews', { query: params });
  }

  compare(body: {
    problemId: string;
    submissionId?: string;
    code?: string;
    language?: string;
    revealPseudocode?: boolean;
  }) {
    return this.request<ComparisonView>('POST', '/comparisons', { body });
  }

  // ---------------------------------------------------------------- hints

  startHintSession(body: { problemId: string; submissionId?: string }) {
    return this.request<HintSessionView>('POST', '/hints/sessions', { body });
  }

  getHintSession(id: string) {
    return this.request<HintSessionView>('GET', `/hints/sessions/${id}`);
  }

  nextHint(id: string, body: { revealSolution?: boolean; code?: string; language?: string } = {}) {
    return this.request<HintSessionView>('POST', `/hints/sessions/${id}/next`, { body });
  }

  // ----------------------------------------------------------------- chat

  startThread(body: { problemId: string; submissionId?: string; title?: string }) {
    return this.request<ChatThreadView>('POST', '/chat/threads', { body });
  }

  listThreads(problemId?: string) {
    return this.request<ChatThreadView[]>('GET', '/chat/threads', { query: { problemId } });
  }

  getThread(id: string) {
    return this.request<ChatThreadView>('GET', `/chat/threads/${id}`);
  }

  sendMessage(
    threadId: string,
    body: { message: string; revealSolution?: boolean; code?: string; language?: string },
  ) {
    return this.request<ChatTurn>('POST', `/chat/threads/${threadId}/messages`, { body });
  }

  // ------------------------------------------------------------- progress

  progressSummary() {
    return this.request<ProgressSummary>('GET', '/progress/summary');
  }

  recommendations() {
    return this.request<RecommendationBundle>('GET', '/progress/recommendations');
  }

  learningReport() {
    return this.request<LearningReport>('GET', '/progress/report');
  }

  // ------------------------------------------------------------ bookmarks

  listBookmarks() {
    return this.request<BookmarkView[]>('GET', '/bookmarks');
  }

  createBookmark(body: {
    kind: string;
    problemId?: string;
    referenceId?: string;
    title: string;
    content: string;
    note?: string;
  }) {
    return this.request<BookmarkView>('POST', '/bookmarks', { body });
  }

  deleteBookmark(id: string) {
    return this.request<void>('DELETE', `/bookmarks/${id}`);
  }

  // ----------------------------------------------------------------- meta

  meta() {
    return this.request<MetaResponse>('GET', '/meta', { anonymous: true });
  }

  sandboxInfo() {
    return this.request<SandboxInfo>('GET', '/execution');
  }

  run(body: { language: string; code: string; stdin?: string }) {
    return this.request<ExecutionResult>('POST', '/execution/run', { body });
  }

  // -------------------------------------------------------------- transport

  private async request<T>(
    method: string,
    path: string,
    options: { body?: unknown; query?: Record<string, unknown>; anonymous?: boolean; retry?: boolean } = {},
  ): Promise<T> {
    // `baseUrl` may be relative (the web app proxies through /backend), so resolve against the
    // current origin when there is one. Node callers must pass an absolute base.
    const origin = (globalThis as { location?: { origin?: string } }).location?.origin;
    const url = new URL(
      this.baseUrl + path,
      this.baseUrl.startsWith('http') ? undefined : (origin ?? 'http://localhost'),
    );
    for (const [key, value] of Object.entries(options.query ?? {})) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, String(value));
      }
    }

    const headers: Record<string, string> = { Accept: 'application/json' };
    if (options.body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }
    if (!options.anonymous) {
      const token = await this.tokens.getAccessToken();
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
    }

    const response = await this.fetchImpl(url.toString(), {
      method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });

    if (response.status === 401 && !options.anonymous && !options.retry) {
      // One transparent refresh, shared across concurrent callers.
      if (await this.refresh()) {
        return this.request<T>(method, path, { ...options, retry: true });
      }
    }

    if (!response.ok) {
      throw await this.toError(response);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }

  private async refresh(): Promise<boolean> {
    this.refreshInFlight ??= (async () => {
      try {
        const refreshToken = await this.tokens.getRefreshToken();
        if (!refreshToken) {
          return false;
        }
        const auth = await this.request<AuthResponse>('POST', '/auth/refresh', {
          body: { refreshToken },
          anonymous: true,
        });
        await this.tokens.setTokens(auth.accessToken, auth.refreshToken);
        return true;
      } catch {
        await this.tokens.clear();
        this.onAuthExpired?.();
        return false;
      } finally {
        // Release the latch on the next tick so concurrent callers all see this result first.
        queueMicrotask(() => {
          this.refreshInFlight = null;
        });
      }
    })();
    return this.refreshInFlight;
  }

  private async toError(response: Response): Promise<RevexaApiError> {
    let code = 'request_failed';
    let message = `Request failed with status ${response.status}`;
    let fieldErrors: { field: string; message: string }[] = [];
    try {
      const body = await response.json();
      code = body.code ?? code;
      message = body.message ?? message;
      fieldErrors = body.fieldErrors ?? [];
    } catch {
      // A non-JSON error body (a proxy page, say) — keep the generic message.
    }
    if (response.status === 429) {
      const retryAfter = response.headers.get('Retry-After');
      message = retryAfter ? `Too many requests — try again in ${retryAfter}s.` : message;
    }
    return new RevexaApiError(response.status, code, message, fieldErrors);
  }
}
