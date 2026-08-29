/**
 * The wire contract between Revexa's backend and every client surface.
 *
 * These types live in `@revexa/core` rather than in the web app because the web app is only the
 * first surface: a mobile app, a desktop build and a VS Code extension consume the same API and
 * the same rules. Nothing in this package may import from `next`, `react` or the DOM.
 */

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD' | 'UNKNOWN';
export type PlatformSource = 'MANUAL' | 'LEETCODE' | 'CODEFORCES' | 'HACKERRANK' | 'ATCODER' | 'CUSTOM';
export type UserRole = 'USER' | 'ADMIN';

export type Verdict = 'OPTIMAL' | 'SOLID' | 'IMPROVABLE' | 'INEFFICIENT' | 'RISKY';
export type FindingType = 'CORRECTNESS' | 'EDGE_CASE' | 'PERFORMANCE' | 'READABILITY';
export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export interface UserView {
  id: string;
  email: string;
  displayName: string;
  role: UserRole;
  theme: string;
  preferredLanguage: string;
  mentorMode: string;
  createdAt: string;
}

export interface AuthResponse {
  user: UserView;
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
}

export interface ProblemSummary {
  id: string;
  title: string;
  slug: string;
  difficulty: Difficulty;
  source: PlatformSource;
  topics: string[];
  url: string | null;
  curated: boolean;
  createdAt: string;
}

export interface ProblemExample {
  input: string | null;
  output: string | null;
  explanation: string | null;
}

export interface ProblemDetail extends Omit<ProblemSummary, 'topics'> {
  statement: string;
  constraintsText: string | null;
  examples: ProblemExample[];
  topics: string[];
}

export interface PlatformProviderInfo {
  id: string;
  displayName: string;
  source: PlatformSource;
  automaticImport: boolean;
  requiresUserAuthorization: boolean;
  submissionSync: boolean;
  acceptedInputs: string[];
  notes: string;
}

export interface ImportResult {
  problem: ProblemDetail;
  providerId: string;
  providerName: string;
  warnings: string[];
}

// ------------------------------------------------------------------- review

export interface ProblemUnderstanding {
  restatement: string;
  inputs: string[];
  output: string;
  constraints: string[];
  easyToMisread: string[];
  topics: string[];
  difficultyGuess: string;
  clarifyingQuestions: string[];
}

export interface ApproachSummary {
  name: string;
  inferredIntuition: string;
  plainExplanation: string;
  steps: string[];
  dataStructures: string[];
  patterns: string[];
  language: string;
}

export interface ComplexityContributor {
  label: string;
  complexity: string;
  reason: string;
}

export interface ComplexityAnalysis {
  time: string;
  space: string;
  timeExplanation: string;
  spaceExplanation: string;
  optimalTime: string;
  optimalSpace: string;
  timeOptimal: boolean;
  spaceOptimal: boolean;
  confidence: number;
  contributors: ComplexityContributor[];
}

export interface Finding {
  type: FindingType;
  severity: Severity;
  title: string;
  detail: string;
  suggestion: string | null;
  line: number | null;
}

export interface OptimizationInsight {
  betterApproachExists: boolean;
  keyInsight: string;
  direction: string;
  nudges: string[];
  targetTime: string;
  targetSpace: string;
  patternName: string;
}

export interface CodeReviewResult {
  understanding: ProblemUnderstanding;
  approach: ApproachSummary;
  complexity: ComplexityAnalysis;
  findings: Finding[];
  optimization: OptimizationInsight;
  score: number;
  verdict: Verdict;
  mentorNote: string;
  nextSteps: string[];
  topics: string[];
  provider: string;
  model: string;
}

export interface ReviewView {
  id: string;
  submissionId: string;
  problemId: string;
  problemTitle: string;
  language: string;
  result: CodeReviewResult;
  createdAt: string;
}

export interface ReviewSummary {
  id: string;
  problemId: string;
  problemTitle: string;
  verdict: Verdict;
  score: number;
  timeComplexity: string;
  optimalTime: string;
  timeOptimal: boolean;
  approachName: string;
  language: string;
  topics: string[];
  createdAt: string;
}

export interface SubmissionView {
  id: string;
  problemId: string;
  problemTitle: string;
  language: string;
  code: string;
  notes: string | null;
  createdAt: string;
  latestReviewId: string | null;
}

// -------------------------------------------------------------------- hints

export interface GeneratedHint {
  level: number;
  levelName: string;
  intent: string;
  title: string;
  content: string;
  socraticQuestions: string[];
  spoiler: boolean;
  lastLevel: boolean;
}

export interface LadderStep {
  level: number;
  name: string;
  title: string;
  intent: string;
  unlocked: boolean;
  spoiler: boolean;
}

export interface HintSessionView {
  id: string;
  problemId: string;
  currentLevel: number;
  maxLevel: number;
  solutionRevealed: boolean;
  hints: GeneratedHint[];
  ladder: LadderStep[];
}

// --------------------------------------------------------------------- chat

export interface ChatMessageView {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  followUpQuestions: string[];
  spoilerLevel: number;
  revealedSolution: boolean;
  createdAt: string;
}

export interface ChatThreadView {
  id: string;
  problemId: string;
  problemTitle: string;
  title: string;
  maxSpoilerLevel: number;
  messages: ChatMessageView[];
  createdAt: string;
}

export interface ChatTurn {
  userMessage: ChatMessageView;
  reply: ChatMessageView;
}

// --------------------------------------------------------------- comparison

export interface ApproachOption {
  id: string;
  name: string;
  summary: string;
  timeComplexity: string;
  spaceComplexity: string;
  pros: string[];
  cons: string[];
  whenToPrefer: string;
  keyInsight: string;
  userApproach: boolean;
  optimal: boolean;
  pseudocode: string | null;
}

export interface SolutionComparison {
  userApproach: ApproachOption;
  alternatives: ApproachOption[];
  missingInsight: string;
  tradeoffSummary: string;
  recommendation: string;
  pseudocodeRevealed: boolean;
}

export interface ComparisonView {
  problemId: string;
  problemTitle: string;
  comparison: SolutionComparison;
}

// ----------------------------------------------------------------- progress

export interface Headline {
  problemsAttempted: number;
  reviewsRun: number;
  optimalSolutions: number;
  optimalRate: number;
  averageScore: number;
  scoreDelta: number;
  currentStreakDays: number;
  bookmarks: number;
}

export interface TopicMastery {
  topic: string;
  attempts: number;
  optimal: number;
  mastery: number;
  verdict: string;
  advice: string;
}

export interface MistakePattern {
  title: string;
  occurrences: number;
  severity: string;
  coaching: string;
}

export interface TrendPoint {
  date: string;
  averageScore: number;
  reviews: number;
}

export interface ComplexityBucket {
  complexity: string;
  count: number;
  optimalForItsProblems: boolean;
}

export interface HintDependency {
  hintsPerProblem: number;
  solutionsRevealed: number;
  independenceScore: number;
  verdict: string;
  advice: string;
}

export interface RecentActivity {
  reviewId: string;
  problemId: string;
  problemTitle: string;
  verdict: Verdict;
  score: number;
  at: string;
}

export interface ProgressSummary {
  headline: Headline;
  strongTopics: TopicMastery[];
  weakTopics: TopicMastery[];
  recurringMistakes: MistakePattern[];
  qualityTrend: TrendPoint[];
  complexityMix: ComplexityBucket[];
  hintDependency: HintDependency;
  recentActivity: RecentActivity[];
}

export interface Recommendation {
  id: string;
  kind: string;
  title: string;
  reason: string;
  action: string;
  practiceProblems: string[];
  concepts: string[];
  priority: number;
}

export interface RecommendationBundle {
  recommendations: Recommendation[];
  focusOfTheWeek: string;
}

export interface LearningReport {
  generatedFor: string;
  generatedAt: string;
  summary: ProgressSummary;
  recommendations: Recommendation[];
  markdown: string;
}

// ---------------------------------------------------------------- bookmarks

export interface BookmarkView {
  id: string;
  kind: string;
  problemId: string | null;
  problemTitle: string | null;
  referenceId: string | null;
  title: string;
  content: string;
  note: string | null;
  createdAt: string;
}

// --------------------------------------------------------------------- meta

export interface ProviderStatus {
  id: string;
  model: string;
  available: boolean;
  active: boolean;
}

export interface HintLevelInfo {
  level: number;
  name: string;
  title: string;
  intent: string;
  spoiler: boolean;
}

export interface PatternInfo {
  id: string;
  name: string;
  topics: string[];
  optimalTime: string;
  optimalSpace: string;
}

export interface MetaResponse {
  product: string;
  version: string;
  languages: string[];
  hintLadder: HintLevelInfo[];
  aiProviders: ProviderStatus[];
  patterns: PatternInfo[];
}

export interface SandboxInfo {
  provider: string;
  enabled: boolean;
  supportedLanguages: string[];
}

export interface ExecutionResult {
  status: 'OK' | 'DISABLED' | 'SIMULATED' | 'ERROR';
  stdout: string;
  stderr: string;
  durationMs: number;
  exitCode: number | null;
  testOutcomes: { name: string; passed: boolean; expected: string | null; actual: string | null; note: string }[];
  note: string;
}

// --------------------------------------------------------------- pagination

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors?: { field: string; message: string }[];
  path?: string;
  timestamp?: string;
}
