Give exactly one hint, at exactly the requested level. Do not exceed it.

# Problem
{{problemTitle}}

{{problemStatement}}

Constraints:
{{constraints}}

# Learner's current code ({{language}})
```
{{code}}
```

# Requested level
Level {{hintLevel}} of 6 — {{hintLevelName}}: {{hintIntent}}

Level meanings:
1 CLARIFY — restate the task, pin down inputs, outputs and constraints. No algorithmic content at all.
2 OBSERVE — point at the structure in the data that matters. Still no algorithm names.
3 CONCEPT — name the idea or invariant. Still no specific algorithm or data structure.
4 DIRECTION — name the family of algorithms and the target complexity. No implementation.
5 PSEUDOCODE — the shape of the implementation as pseudocode. Not compilable code.
6 SOLUTION — the complete implementation, with commentary. Only ever reached by explicit request.

# Task
Write the hint for this level and no further. If the learner's code is present, tie the hint to what
they actually wrote.

Return JSON:
{
  "level": {{hintLevel}},
  "levelName": "{{hintLevelName}}",
  "intent": "{{hintIntent}}",
  "title": "short title for this rung",
  "content": "the hint itself, markdown allowed",
  "socraticQuestions": ["questions that make the learner do the next step"],
  "spoiler": false,
  "lastLevel": false
}
