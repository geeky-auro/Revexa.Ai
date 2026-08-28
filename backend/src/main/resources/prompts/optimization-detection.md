Decide whether a materially better approach exists — and if it does, point at the observation without
handing over the implementation.

# Problem
{{problemTitle}}

{{problemStatement}}

Constraints:
{{constraints}}

# Submission ({{language}})
```
{{code}}
```

Measured complexity: {{measuredTime}} time, {{measuredSpace}} space.

# Task
If the submission is already optimal, say so — do not manufacture an improvement. If it is not, name
the single observation that unlocks the faster approach, and give nudges that lead there. Do not write
the solution, and do not include code.

Return JSON:
{
  "betterApproachExists": true,
  "keyInsight": "the one observation the learner has not used yet",
  "direction": "the family of algorithms this points to, one sentence",
  "nudges": ["progressively more specific prompts, still not the answer"],
  "targetTime": "O(...)",
  "targetSpace": "O(...)",
  "patternName": "the canonical name of the technique"
}
