Put the learner's approach beside the realistic alternatives.

# Problem
{{problemTitle}}

{{problemStatement}}

Constraints:
{{constraints}}

# Learner's submission ({{language}})
```
{{code}}
```

Measured: {{measuredTime}} time, {{measuredSpace}} space.

# Task
Compare honestly. Include the optimal approach and at least one other genuine alternative with a
different trade-off profile. Say when each one is actually the right choice — "always use the fastest"
is not a useful answer. Include pseudocode only if reveal is {{allowSpoilers}} and it is true.

Return JSON:
{
  "userApproach": {
    "id": "user", "name": "", "summary": "", "timeComplexity": "O(...)", "spaceComplexity": "O(...)",
    "pros": [], "cons": [], "whenToPrefer": "", "keyInsight": "",
    "userApproach": true, "optimal": false, "pseudocode": null
  },
  "alternatives": [
    {
      "id": "optimal", "name": "", "summary": "", "timeComplexity": "O(...)", "spaceComplexity": "O(...)",
      "pros": [], "cons": [], "whenToPrefer": "", "keyInsight": "",
      "userApproach": false, "optimal": true, "pseudocode": null
    }
  ],
  "missingInsight": "the observation the learner has not used, or an honest statement that none is missing",
  "tradeoffSummary": "what is actually being traded here",
  "recommendation": "what to do next",
  "pseudocodeRevealed": false
}
