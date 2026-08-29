import type { ComplexityAnalysis, Severity, Verdict } from './types.ts';

/**
 * The mentoring rules, expressed once.
 *
 * The backend enforces these (a client cannot talk its way past the hint gate), but every surface
 * needs the same vocabulary to render them — so the labels, ordering and spoiler rules live here
 * instead of being retyped in each app.
 */

export const HINT_LEVELS = [
  { level: 1, name: 'CLARIFY', title: 'Clarify the problem', short: 'Clarify', spoiler: false },
  { level: 2, name: 'OBSERVE', title: 'Find the observation', short: 'Observe', spoiler: false },
  { level: 3, name: 'CONCEPT', title: 'Conceptual hint', short: 'Concept', spoiler: false },
  { level: 4, name: 'DIRECTION', title: 'Algorithmic direction', short: 'Direction', spoiler: false },
  { level: 5, name: 'PSEUDOCODE', title: 'Pseudocode', short: 'Pseudocode', spoiler: true },
  { level: 6, name: 'SOLUTION', title: 'Full solution', short: 'Solution', spoiler: true },
] as const;

export const MAX_HINT_LEVEL = 6;
export const SOLUTION_LEVEL = 6;

export function hintLevel(level: number) {
  return HINT_LEVELS.find((l) => l.level === level) ?? HINT_LEVELS[0];
}

/** The solution rung always needs a deliberate act, never a default. */
export function requiresExplicitReveal(nextLevel: number): boolean {
  return nextLevel >= SOLUTION_LEVEL;
}

// ------------------------------------------------------------- complexity

const COMPLEXITY_RANK: Record<string, number> = {
  'O(1)': 0,
  'O(log n)': 10,
  'O(sqrt n)': 20,
  'O(n)': 30,
  'O(n log k)': 35,
  'O(n log n)': 40,
  'O(n log^2 n)': 45,
  'O(n^2)': 50,
  'O(n^2 log n)': 55,
  'O(n^3)': 60,
  'O(n^4)': 70,
  'O(n * 2^n)': 85,
  'O(2^n)': 90,
  'O(n!)': 100,
};

export function complexityRank(complexity: string): number {
  return COMPLEXITY_RANK[complexity] ?? 30;
}

export function isBetterComplexity(candidate: string, baseline: string): boolean {
  return complexityRank(candidate) < complexityRank(baseline);
}

/** How far from optimal, as a 0-1 fraction, for progress bars and colour ramps. */
export function complexityGap(analysis: Pick<ComplexityAnalysis, 'time' | 'optimalTime'>): number {
  const gap = complexityRank(analysis.time) - complexityRank(analysis.optimalTime);
  return Math.max(0, Math.min(1, gap / 60));
}

// ---------------------------------------------------------------- verdicts

export const VERDICT_LABELS: Record<Verdict, string> = {
  OPTIMAL: 'Optimal',
  SOLID: 'Solid',
  IMPROVABLE: 'Improvable',
  INEFFICIENT: 'Too slow',
  RISKY: 'Correctness risk',
};

export const VERDICT_DESCRIPTIONS: Record<Verdict, string> = {
  OPTIMAL: 'Matches the best known bound for this problem shape.',
  SOLID: 'Sound approach with a narrow gap to the best version.',
  IMPROVABLE: 'Correct, but a materially better approach exists.',
  INEFFICIENT: 'Will exceed the time limit at the stated constraints.',
  RISKY: 'A correctness issue will fail before performance ever matters.',
};

export const SEVERITY_ORDER: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

export function severityWeight(severity: Severity): number {
  const index = SEVERITY_ORDER.indexOf(severity);
  return index === -1 ? SEVERITY_ORDER.length : index;
}

export function sortFindings<T extends { severity: Severity }>(findings: T[]): T[] {
  return [...findings].sort((a, b) => severityWeight(a.severity) - severityWeight(b.severity));
}

// --------------------------------------------------------------- languages

export interface LanguageOption {
  id: string;
  label: string;
  monacoId: string;
  starter: string;
}

/**
 * The languages the workspace offers. `monacoId` keeps the editor's grammar in step with the id the
 * backend analyser uses, so a language is added in exactly one place.
 */
export const LANGUAGES: LanguageOption[] = [
  {
    id: 'python',
    label: 'Python',
    monacoId: 'python',
    starter: 'class Solution:\n    def solve(self, nums):\n        # your approach here\n        return None\n',
  },
  {
    id: 'java',
    label: 'Java',
    monacoId: 'java',
    starter: 'class Solution {\n    public int solve(int[] nums) {\n        // your approach here\n        return 0;\n    }\n}\n',
  },
  {
    id: 'cpp',
    label: 'C++',
    monacoId: 'cpp',
    starter:
      '#include <vector>\nusing namespace std;\n\nclass Solution {\npublic:\n    int solve(vector<int>& nums) {\n        // your approach here\n        return 0;\n    }\n};\n',
  },
  {
    id: 'javascript',
    label: 'JavaScript',
    monacoId: 'javascript',
    starter: 'function solve(nums) {\n  // your approach here\n  return null;\n}\n',
  },
  {
    id: 'typescript',
    label: 'TypeScript',
    monacoId: 'typescript',
    starter: 'function solve(nums: number[]): number | null {\n  // your approach here\n  return null;\n}\n',
  },
  { id: 'go', label: 'Go', monacoId: 'go', starter: 'func solve(nums []int) int {\n\t// your approach here\n\treturn 0\n}\n' },
  {
    id: 'rust',
    label: 'Rust',
    monacoId: 'rust',
    starter: 'impl Solution {\n    pub fn solve(nums: Vec<i32>) -> i32 {\n        // your approach here\n        0\n    }\n}\n',
  },
  {
    id: 'kotlin',
    label: 'Kotlin',
    monacoId: 'kotlin',
    starter: 'class Solution {\n    fun solve(nums: IntArray): Int {\n        // your approach here\n        return 0\n    }\n}\n',
  },
  {
    id: 'csharp',
    label: 'C#',
    monacoId: 'csharp',
    starter: 'public class Solution {\n    public int Solve(int[] nums) {\n        // your approach here\n        return 0;\n    }\n}\n',
  },
];

export function languageOption(id: string): LanguageOption {
  return LANGUAGES.find((l) => l.id === id) ?? LANGUAGES[0]!;
}

// ------------------------------------------------------------- formatting

export function formatRelativeTime(iso: string, now: Date = new Date()): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) {
    return '';
  }
  const seconds = Math.round((now.getTime() - then) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  if (days < 30) return `${days}d ago`;
  const months = Math.round(days / 30);
  return months < 12 ? `${months}mo ago` : `${Math.round(months / 12)}y ago`;
}

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');
}
