# Architecture notes

Why the system is shaped the way it is. Each section states a decision, the alternatives considered,
and what the decision costs.

---

## 1. The product constraint is enforced server-side

**Decision.** The hint ladder advances one rung per request, and the final rung requires
`revealSolution=true`. The chat re-evaluates the spoiler gate on every turn. Both rules live in
`HintService` and `ChatService`, not in the UI.

**Why.** "Never reveal the complete optimal solution by default" is the product. A rule enforced only
in the interface is a suggestion: any client, any curl, any future mobile app could bypass it. Put it
behind the API and it holds for every surface at once.

**The two-step gate.** A free-text "just tell me the answer" is treated as a *first ask*: the mentor
answers with the key observation and names the exact phrase that unlocks the rest. Only "reveal the
solution" (or the UI's explicit control) opens it. The narrow phrase list in `ChatService` is
deliberate — a loose matcher would make the gate decorative.

**Cost.** A learner in a hurry needs two messages instead of one. That is the intended friction.

---

## 2. Vertical slices, not layers

**Decision.** The backend is organised by domain (`identity`, `catalog`, `practice`, `intelligence`,
`progress`, `sandbox`) rather than by technical layer (`controllers`, `services`, `repositories`).
Slices talk to each other only through service interfaces.

**Why.** The brief asks for a "modular architecture suitable for future microservices". A layered
package tree makes extraction hard, because a single service class knows about five domains. With
slices, `intelligence` could be lifted into its own deployable by taking the package and the
`LlmClient` config with it; the seam is already where the split would go.

**Cost.** Some duplication across slices (each has its own DTO shapes) in exchange for the boundary
being real.

---

## 3. An LLM abstraction rather than Spring AI

**Decision.** A hand-written `LlmClient` interface with `AnthropicLlmClient`, `OpenAiLlmClient` and
`HeuristicLlmClient`, behind an `LlmGateway` that handles provider selection and fallback.

**Why.** The brief allows "Spring AI **or** an LLM abstraction layer". Spring AI would add a
fast-moving dependency and its own abstractions on top of ours, for a surface we use narrowly — one
completion call with structured output. A ~120-line interface plus two `RestClient` implementations
is easier to test (the heuristic client is a first-class implementation, not a mock), easier to
reason about, and pins the failure modes where we can see them.

**Cost.** No free access to Spring AI's ecosystem — embeddings, vector stores, advisors. None of
which this product needs today, and all of which sit behind the same seam if it ever does.

---

## 4. Structured JSON everywhere, with a tolerant parser

**Decision.** Every stage declares a Java record and asks the model for exactly that shape.
`Json.extractJson` recovers the first *balanced* JSON value from arbitrary model text, tracking
string literals and escapes so braces inside embedded code do not terminate the scan early.

**Why.** Panels render fields, not paragraphs. Structured output is what lets the complexity panel
show a per-construct breakdown rather than a blob. Models mostly comply, but "mostly" is not a
contract — so failure to parse degrades to the offline engine instead of surfacing an error.

**Cost.** Prompts are longer (each carries its schema), and a schema change touches the prompt, the
record and the offline engine together.

---

## 5. The offline engine is a real implementation

**Decision.** `heuristic` is the default provider. It performs genuine static analysis and consults a
curated pattern catalogue, and satisfies the same stage contracts as the hosted providers.

**Why.** Three things fall out of one decision:

1. The prototype is fully functional with no API key — the brief's "working end-to-end" requirement.
2. Hosted providers get a fallback that is better than an error page.
3. The test suite has deterministic output to assert against, so "does a quadratic solution get
   flagged as quadratic?" is a unit test rather than a manual check.

**How the analysis works.** `StaticCodeAnalyzer` strips comments and string literals first, so
keyword scanning cannot be fooled by prose or a code sample inside a docstring. Then:

- Loop depth is tracked with a block stack for brace languages and an indentation stack for Python,
  with comprehension nesting counted separately.
- Recursion requires a call *after the declaration* — where the declaration is a `def`/`function`
  keyword or a signature immediately followed by a body brace. A Java helper called above its own
  definition is not recursion, and getting this wrong turned an O(n²) solution into an "exponential"
  one before it was fixed.
- A branching recursion guarded by a visited set is linear, not exponential: the visited set is what
  bounds the work. Without this, every grid DFS is misreported as O(2ⁿ).
- Constraint reading prefers bounds on *size* over bounds on *values*, because `nums.length <= 10^4`
  determines the target complexity and `-10^9 <= nums[i] <= 10^9` does not.

**Cost.** It is a heuristic, and it says so: every complexity figure carries a confidence score and
the breakdown that produced it. It will misjudge unusual code. That is why the confidence number is
on screen rather than buried.

**Extending it.** Add an entry to `knowledge/patterns.json` — bounds, key insight, six-rung ladder,
Socratic questions, alternatives, practice problems. No code change.

---

## 6. Platform integration without scraping

**Decision.** `PracticePlatformProvider` with manual-entry, LeetCode-link and browser-extension
implementations. The registry picks the most specific provider that can read the payload, and manual
entry is the guaranteed fallback — so an import never fails for want of a provider.

**Why.** LeetCode has no public problem API, and scraping it would breach their terms and break on
the next markup change. The legitimate paths are: what the user can paste, what a URL reveals
(slug and title), and what an extension can capture inside the user's own authenticated session.

Each provider publishes a `Capabilities` record that the UI renders verbatim — including
`automaticImport: false`. The interface makes the limits visible instead of letting a user discover
them by hitting a wall. A test asserts that anything claiming automatic import also requires user
authorisation.

---

## 7. Shared core package

**Decision.** `packages/core` holds domain types, the API client, and the mentor rules the UI needs
(ladder labels, complexity ordering, language definitions). It imports nothing from `next`, `react`
or the DOM, and is consumed as TypeScript source via `transpilePackages`.

**Why.** The brief asks that core logic not be coupled to the web frontend. The client injects both
`fetch` and a `TokenStore`, so the same file works in a browser (`localStorage`), a VS Code extension
(`SecretStorage`) and React Native (keychain). The transparent single-flight token refresh lives
there too, rather than being reimplemented per surface.

**Cost.** A workspace and a `transpilePackages` entry. No build step — the source is the artifact.

---

## 8. One schema for local and production

**Decision.** Flyway migrations written in the SQL subset shared by PostgreSQL and H2's PostgreSQL
compatibility mode; H2 in-memory for local runs, Postgres in Docker.

**Why.** The usual shortcut — `ddl-auto: create-drop` locally, Flyway in production — means the
schema you develop against is not the schema you deploy. Here `ddl-auto: validate` runs against the
migrated schema locally, so a drift between entity and migration fails at startup on a laptop rather
than in a deployment.

**Cost.** A narrower SQL dialect (no Postgres-specific types) and one gotcha worth recording: `@Lob`
on a `String` makes Hibernate expect `CLOB`, which does not match a `text` column in H2's PG mode.
Dropping `@Lob` and keeping `columnDefinition = "text"` validates on both.

---

## 9. Rate limiting and caching that degrade rather than fail

**Decision.** `RateLimiter` has in-memory (Caffeine) and Redis implementations, chosen by
`revexa.cache.mode`. The Redis limiter falls back to the local one when Redis is unreachable. The
same applies to the AI result cache.

**Why.** A cache outage should never take the API with it. Three tiers exist because the endpoints
are genuinely different: anonymous auth traffic is keyed by IP and tightly capped, AI endpoints are
expensive and capped per user, and everything else is cheap.

---

## 10. Deliberate omissions

| Omitted | Why | Where it plugs in |
| --- | --- | --- |
| Real code execution | Safe untrusted execution is infrastructure, not app logic. The simulated sandbox says so in every response rather than implying a verdict it did not compute. | `CodeExecutionSandbox` |
| httpOnly refresh cookies | Prototype-appropriate; `localStorage` keeps the API stateless and the demo simple. | `BrowserTokenStore` (one class) |
| Streaming responses | The offline engine answers in milliseconds; streaming matters once a hosted model is the default. | `LlmClient` would gain a `stream` method |
| Multi-tenant / org accounts | Out of scope for a single-learner prototype. | `identity` slice |

---

## Request flow: a review

```
POST /api/v1/reviews
  → RateLimitFilter        (AI tier, per user)
  → ReviewService          (resolve or create the submission, scope it to the caller)
  → StageContextFactory    (problem + code + language → one immutable StageContext)
  → ReviewPipeline
      ├─ understanding ─┐
      ├─ analysis ──────┼─ parallel on the aiExecutor pool
      └─ complexity ────┘
      └─ optimisation    (needs the measured complexity, so it runs after)
      → score, verdict, mentor note, next steps
  → persist the structured payload verbatim + promote scalars to columns for the dashboard
  → ReviewView
```

The payload is stored verbatim so an old review re-renders exactly as it was produced; the scalar
columns exist because the progress dashboard aggregates over them, and re-parsing ten JSON blobs to
draw one chart would be the wrong trade.
