Review this submission the way a careful reviewer would: first understand it, then find what breaks.

# Problem
{{problemTitle}}

{{problemStatement}}

Constraints:
{{constraints}}

# Submission ({{language}})
```
{{code}}
```

# Task
Infer the intent behind the code — what the author noticed that led them here — then find real defects.
Only report findings you can point at in this code. Do not invent problems to fill the list.

Return JSON:
{
  "approach": {
    "name": "short label, e.g. 'Sort then two pointers'",
    "inferredIntuition": "the reasoning that plausibly led the author here, stated generously",
    "plainExplanation": "what the code does, in plain terms",
    "steps": ["the algorithm as ordered steps"],
    "dataStructures": ["structures actually used"],
    "patterns": ["algorithmic patterns present"],
    "language": "{{language}}"
  },
  "findings": [
    {
      "type": "CORRECTNESS | EDGE_CASE | PERFORMANCE | READABILITY",
      "severity": "CRITICAL | HIGH | MEDIUM | LOW",
      "title": "one line",
      "detail": "what goes wrong and when, concretely",
      "suggestion": "the smallest change that fixes it — no full rewrite",
      "line": null
    }
  ]
}
