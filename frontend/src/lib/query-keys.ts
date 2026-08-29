/** One place for every cache key, so an invalidation can never miss a consumer. */
export const queryKeys = {
  me: ['me'] as const,
  meta: ['meta'] as const,
  problems: (search?: string, difficulty?: string) => ['problems', search ?? '', difficulty ?? ''] as const,
  problem: (id: string) => ['problem', id] as const,
  platforms: ['platforms'] as const,
  submissions: (problemId?: string) => ['submissions', problemId ?? 'all'] as const,
  reviews: (problemId?: string) => ['reviews', problemId ?? 'all'] as const,
  review: (id: string) => ['review', id] as const,
  hintSession: (id: string) => ['hint-session', id] as const,
  threads: (problemId?: string) => ['threads', problemId ?? 'all'] as const,
  thread: (id: string) => ['thread', id] as const,
  progress: ['progress'] as const,
  recommendations: ['recommendations'] as const,
  report: ['report'] as const,
  bookmarks: ['bookmarks'] as const,
  sandbox: ['sandbox'] as const,
};
