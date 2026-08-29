#!/usr/bin/env bash
#
# Walks the core Revexa loop against a running API and fails loudly if any step misbehaves.
# Used by CI and handy locally:  API_BASE=http://localhost:8080/api/v1 ./scripts/smoke-test.sh
set -euo pipefail

API="${API_BASE:-http://localhost:8080/api/v1}"
EMAIL="smoke-$(date +%s)@revexa.ai"
PASSWORD="smoke-password-123"

say() { printf '\n\033[1m%s\033[0m\n' "$*"; }
fail() { printf '\033[31mFAIL: %s\033[0m\n' "$*" >&2; exit 1; }

# Reads a python subscript expression from argv so shell quoting never leaks into the program.
json() { python3 -c 'import sys,json;d=json.load(sys.stdin);print(eval("d"+sys.argv[1]))' "$1"; }

say "1. Public metadata is reachable without a token"
LADDER=$(curl -sf "$API/meta" | json "['hintLadder'].__len__()")
[ "$LADDER" = "6" ] || fail "expected a 6-rung hint ladder, got $LADDER"

say "2. Register"
AUTH=$(curl -sf -X POST "$API/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"displayName\":\"Smoke Test\",\"password\":\"$PASSWORD\"}")
TOKEN=$(echo "$AUTH" | json "['accessToken']")
[ -n "$TOKEN" ] || fail "no access token returned"
AUTH_HEADER="Authorization: Bearer $TOKEN"

say "3. Protected endpoints reject an anonymous caller"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$API/problems")
[ "$CODE" = "401" ] || fail "expected 401 without a token, got $CODE"

say "4. Import a problem through the PracticePlatformProvider abstraction"
IMPORT=$(curl -sf -X POST "$API/problems/import" -H "$AUTH_HEADER" -H 'Content-Type: application/json' -d '{
  "title": "Two Sum",
  "rawText": "Two Sum\n\nGiven an array of integers nums and an integer target, return the indices of the two numbers that add up to target.\n\nConstraints:\n2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9"
}')
PROBLEM_ID=$(echo "$IMPORT" | json "['problem']['id']")
PROVIDER=$(echo "$IMPORT" | json "['providerId']")
[ "$PROVIDER" = "manual" ] || fail "expected the manual provider, got $PROVIDER"

say "5. Review a deliberately quadratic solution"
REVIEW=$(curl -sf -X POST "$API/reviews" -H "$AUTH_HEADER" -H 'Content-Type: application/json' -d "{
  \"problemId\": \"$PROBLEM_ID\", \"language\": \"python\", \"force\": true,
  \"code\": \"def two_sum(nums, target):\n    for i in range(len(nums)):\n        for j in range(i + 1, len(nums)):\n            if nums[i] + nums[j] == target:\n                return [i, j]\n    return []\n\"
}")
TIME=$(echo "$REVIEW" | json "['result']['complexity']['time']")
OPTIMAL=$(echo "$REVIEW" | json "['result']['complexity']['optimalTime']")
BETTER=$(echo "$REVIEW" | json "['result']['optimization']['betterApproachExists']")
[ "$TIME" = "O(n^2)" ] || fail "expected O(n^2), got $TIME"
[ "$OPTIMAL" = "O(n)" ] || fail "expected an achievable O(n), got $OPTIMAL"
[ "$BETTER" = "True" ] || fail "expected a better approach to be detected"
echo "   measured $TIME against an achievable $OPTIMAL"

say "6. Review the optimal solution and confirm it is not 'improved' further"
OPT_REVIEW=$(curl -sf -X POST "$API/reviews" -H "$AUTH_HEADER" -H 'Content-Type: application/json' -d "{
  \"problemId\": \"$PROBLEM_ID\", \"language\": \"python\", \"force\": true,
  \"code\": \"def two_sum(nums, target):\n    if not nums:\n        return []\n    seen = {}\n    for i, x in enumerate(nums):\n        need = target - x\n        if need in seen:\n            return [seen[need], i]\n        seen[x] = i\n    return []\n\"
}")
VERDICT=$(echo "$OPT_REVIEW" | json "['result']['verdict']")
[ "$VERDICT" = "OPTIMAL" ] || fail "expected an OPTIMAL verdict, got $VERDICT"

say "7. The hint ladder advances one rung at a time"
SESSION=$(curl -sf -X POST "$API/hints/sessions" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d "{\"problemId\":\"$PROBLEM_ID\"}")
SESSION_ID=$(echo "$SESSION" | json "['id']")
for _ in 1 2 3 4 5; do
  LEVEL=$(curl -sf -X POST "$API/hints/sessions/$SESSION_ID/next" -H "$AUTH_HEADER" \
    -H 'Content-Type: application/json' -d '{"revealSolution":false}' | json "['currentLevel']")
done
[ "$LEVEL" = "5" ] || fail "expected to reach rung 5, got $LEVEL"

say "8. The solution rung stays gated without an explicit reveal"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/hints/sessions/$SESSION_ID/next" \
  -H "$AUTH_HEADER" -H 'Content-Type: application/json' -d '{"revealSolution":false}')
[ "$CODE" = "400" ] || fail "expected the solution rung to be refused, got $CODE"

REVEALED=$(curl -sf -X POST "$API/hints/sessions/$SESSION_ID/next" -H "$AUTH_HEADER" \
  -H 'Content-Type: application/json' -d '{"revealSolution":true}' | json "['solutionRevealed']")
[ "$REVEALED" = "True" ] || fail "an explicit reveal should unlock the final rung"

say "9. Chat withholds the solution until asked in so many words"
THREAD=$(curl -sf -X POST "$API/chat/threads" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d "{\"problemId\":\"$PROBLEM_ID\"}" | json "['id']")
GATED=$(curl -sf -X POST "$API/chat/threads/$THREAD/messages" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"message":"just tell me the answer, give me the full solution"}' | json "['reply']['revealedSolution']")
[ "$GATED" = "False" ] || fail "a loose request should not reveal the solution"

OPENED=$(curl -sf -X POST "$API/chat/threads/$THREAD/messages" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"message":"reveal the solution"}' | json "['reply']['revealedSolution']")
[ "$OPENED" = "True" ] || fail "an explicit reveal should be honoured"

say "10. Progress reflects the reviews just run"
REVIEWS=$(curl -sf "$API/progress/summary" -H "$AUTH_HEADER" | json "['headline']['reviewsRun']")
[ "$REVIEWS" = "2" ] || fail "expected 2 reviews on the dashboard, got $REVIEWS"

printf '\n\033[32mAll smoke checks passed.\033[0m\n'
