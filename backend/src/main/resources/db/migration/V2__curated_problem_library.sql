-- Curated starter library.
-- Statements are original phrasings of well-known interview problems, so the library ships with the
-- product rather than depending on any platform's content.

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('69cb5b5b-2432-5f39-b049-dd6d59959f3a', 'Two Sum', 'two-sum', 'MANUAL', null, null, 'EASY',
        'You are given an array of integers nums and an integer target. Return the indices of the two numbers that add up to target.

Each input has exactly one valid answer, and you may not use the same element twice. The answer may be returned in any order.

Example 1:
Input: nums = [2,7,11,15], target = 9
Output: [0,1]
Explanation: nums[0] + nums[1] == 9, so the answer is [0, 1].

Example 2:
Input: nums = [3,2,4], target = 6
Output: [1,2]

Example 3:
Input: nums = [3,3], target = 6
Output: [0,1]

Constraints:
2 <= nums.length <= 10^4
-10^9 <= nums[i] <= 10^9
-10^9 <= target <= 10^9
Exactly one valid answer exists.

Follow up: can you do better than O(n^2)?',
        'Constraints:
2 <= nums.length <= 10^4
-10^9 <= nums[i] <= 10^9
-10^9 <= target <= 10^9
Exactly one valid answer exists.',
        '[{"input": "nums = [2,7,11,15], target = 9", "output": "[0,1]", "explanation": "nums[0] + nums[1] == 9, so the answer is [0, 1]."}, {"input": "nums = [3,2,4], target = 6", "output": "[1,2]"}, {"input": "nums = [3,3], target = 6", "output": "[0,1]"}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('69cb5b5b-2432-5f39-b049-dd6d59959f3a', 'Array');
insert into problem_topic (problem_id, topic) values ('69cb5b5b-2432-5f39-b049-dd6d59959f3a', 'Hash Table');

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('c5f6a0ef-f8b7-5e73-9408-9254dfffb923', 'Longest Substring Without Repeating Characters', 'longest-substring-without-repeating-characters', 'MANUAL', null, null, 'MEDIUM',
        'Given a string s, find the length of the longest contiguous substring that contains no repeated characters.

Note that a substring is contiguous; a subsequence is not. The answer must be a substring.

Example 1:
Input: s = "abcabcbb"
Output: 3
Explanation: The answer is "abc", with length 3.

Example 2:
Input: s = "bbbbb"
Output: 1
Explanation: The answer is "b", with length 1.

Example 3:
Input: s = "pwwkew"
Output: 3
Explanation: The answer is "wke". Note that "pwke" is a subsequence, not a substring.

Constraints:
0 <= s.length <= 5 * 10^4
s consists of English letters, digits, symbols and spaces.',
        'Constraints:
0 <= s.length <= 5 * 10^4
s consists of English letters, digits, symbols and spaces.',
        '[{"input": "s = \"abcabcbb\"", "output": "3", "explanation": "The answer is \"abc\", with length 3."}, {"input": "s = \"bbbbb\"", "output": "1", "explanation": "The answer is \"b\", with length 1."}, {"input": "s = \"pwwkew\"", "output": "3", "explanation": "The answer is \"wke\". Note that \"pwke\" is a subsequence, not a substring."}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('c5f6a0ef-f8b7-5e73-9408-9254dfffb923', 'String');
insert into problem_topic (problem_id, topic) values ('c5f6a0ef-f8b7-5e73-9408-9254dfffb923', 'Sliding Window');
insert into problem_topic (problem_id, topic) values ('c5f6a0ef-f8b7-5e73-9408-9254dfffb923', 'Hash Table');

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('2438dda8-6a8a-5b54-af78-d4b998a7a6a7', 'Container With Most Water', 'container-with-most-water', 'MANUAL', null, null, 'MEDIUM',
        'You are given an integer array height of length n, where height[i] is the height of a vertical line drawn at position i.

Pick two lines so that, together with the x-axis, they form a container holding the most water. Return the maximum amount of water the container can store. You may not slant the container.

Example 1:
Input: height = [1,8,6,2,5,4,8,3,7]
Output: 49
Explanation: The lines at index 1 and index 8 give a width of 7 and a height of min(8, 7) = 7.

Example 2:
Input: height = [1,1]
Output: 1

Constraints:
n == height.length
2 <= n <= 10^5
0 <= height[i] <= 10^4',
        'Constraints:
n == height.length
2 <= n <= 10^5
0 <= height[i] <= 10^4',
        '[{"input": "height = [1,8,6,2,5,4,8,3,7]", "output": "49", "explanation": "The lines at index 1 and index 8 give a width of 7 and a height of min(8, 7) = 7."}, {"input": "height = [1,1]", "output": "1"}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('2438dda8-6a8a-5b54-af78-d4b998a7a6a7', 'Array');
insert into problem_topic (problem_id, topic) values ('2438dda8-6a8a-5b54-af78-d4b998a7a6a7', 'Two Pointers');
insert into problem_topic (problem_id, topic) values ('2438dda8-6a8a-5b54-af78-d4b998a7a6a7', 'Greedy');

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('3b3eb962-1d14-5ad7-a209-a0409b8fd7fe', 'Koko Eating Bananas', 'koko-eating-bananas', 'MANUAL', null, null, 'MEDIUM',
        'There are n piles of bananas, where piles[i] is the number of bananas in the i-th pile. A guard returns in h hours.

Koko picks an eating speed of k bananas per hour. Each hour she chooses one pile and eats k bananas from it; if the pile has fewer than k bananas left she eats all of them and does not move on to another pile that hour.

Return the minimum integer k such that she can finish every pile within h hours.

Example 1:
Input: piles = [3,6,7,11], h = 8
Output: 4

Example 2:
Input: piles = [30,11,23,4,20], h = 5
Output: 30

Example 3:
Input: piles = [30,11,23,4,20], h = 6
Output: 23

Constraints:
1 <= piles.length <= 10^4
piles.length <= h <= 10^9
1 <= piles[i] <= 10^9',
        'Constraints:
1 <= piles.length <= 10^4
piles.length <= h <= 10^9
1 <= piles[i] <= 10^9',
        '[{"input": "piles = [3,6,7,11], h = 8", "output": "4"}, {"input": "piles = [30,11,23,4,20], h = 5", "output": "30"}, {"input": "piles = [30,11,23,4,20], h = 6", "output": "23"}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('3b3eb962-1d14-5ad7-a209-a0409b8fd7fe', 'Array');
insert into problem_topic (problem_id, topic) values ('3b3eb962-1d14-5ad7-a209-a0409b8fd7fe', 'Binary Search');

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('b41937f2-fd2d-5d16-a104-1f70d23e39bc', 'Subarray Sum Equals K', 'subarray-sum-equals-k', 'MANUAL', null, null, 'MEDIUM',
        'Given an array of integers nums and an integer k, return the total number of contiguous subarrays whose sum equals k.

The array may contain negative numbers, so a subarray sum does not grow monotonically as the subarray gets longer.

Example 1:
Input: nums = [1,1,1], k = 2
Output: 2

Example 2:
Input: nums = [1,2,3], k = 3
Output: 2
Explanation: The subarrays are [1,2] and [3].

Example 3:
Input: nums = [1,-1,0], k = 0
Output: 3

Constraints:
1 <= nums.length <= 2 * 10^4
-1000 <= nums[i] <= 1000
-10^7 <= k <= 10^7',
        'Constraints:
1 <= nums.length <= 2 * 10^4
-1000 <= nums[i] <= 1000
-10^7 <= k <= 10^7',
        '[{"input": "nums = [1,1,1], k = 2", "output": "2"}, {"input": "nums = [1,2,3], k = 3", "output": "2", "explanation": "The subarrays are [1,2] and [3]."}, {"input": "nums = [1,-1,0], k = 0", "output": "3"}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('b41937f2-fd2d-5d16-a104-1f70d23e39bc', 'Array');
insert into problem_topic (problem_id, topic) values ('b41937f2-fd2d-5d16-a104-1f70d23e39bc', 'Hash Table');
insert into problem_topic (problem_id, topic) values ('b41937f2-fd2d-5d16-a104-1f70d23e39bc', 'Prefix Sum');

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('a0633e71-6f4a-5830-a648-469947476334', 'Coin Change', 'coin-change', 'MANUAL', null, null, 'MEDIUM',
        'You are given an integer array coins representing coin denominations, and an integer amount.

Return the fewest number of coins needed to make up that amount. If the amount cannot be made up by any combination of the coins, return -1. You have an infinite supply of each denomination.

Example 1:
Input: coins = [1,2,5], amount = 11
Output: 3
Explanation: 11 = 5 + 5 + 1.

Example 2:
Input: coins = [2], amount = 3
Output: -1

Example 3:
Input: coins = [1], amount = 0
Output: 0

Constraints:
1 <= coins.length <= 12
1 <= coins[i] <= 2^31 - 1
0 <= amount <= 10^4',
        'Constraints:
1 <= coins.length <= 12
1 <= coins[i] <= 2^31 - 1
0 <= amount <= 10^4',
        '[{"input": "coins = [1,2,5], amount = 11", "output": "3", "explanation": "11 = 5 + 5 + 1."}, {"input": "coins = [2], amount = 3", "output": "-1"}, {"input": "coins = [1], amount = 0", "output": "0"}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('a0633e71-6f4a-5830-a648-469947476334', 'Array');
insert into problem_topic (problem_id, topic) values ('a0633e71-6f4a-5830-a648-469947476334', 'Dynamic Programming');

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('a8145bde-3d8e-5985-a5e7-2db67288f179', 'Number of Islands', 'number-of-islands', 'MANUAL', null, null, 'MEDIUM',
        'You are given an m x n grid where each cell is either "1" (land) or "0" (water). An island is a group of land cells connected horizontally or vertically; diagonals do not connect. Assume the grid is surrounded by water on all four sides.

Return the number of islands.

Example 1:
Input: grid = [["1","1","0"],["1","0","0"],["0","0","1"]]
Output: 2

Example 2:
Input: grid = [["1","1","1"],["0","1","0"],["1","1","1"]]
Output: 1

Constraints:
m == grid.length
n == grid[i].length
1 <= m, n <= 300
grid[i][j] is either "0" or "1".',
        'Constraints:
m == grid.length
n == grid[i].length
1 <= m, n <= 300
grid[i][j] is either "0" or "1".',
        '[{"input": "grid = [[\"1\",\"1\",\"0\"],[\"1\",\"0\",\"0\"],[\"0\",\"0\",\"1\"]]", "output": "2"}, {"input": "grid = [[\"1\",\"1\",\"1\"],[\"0\",\"1\",\"0\"],[\"1\",\"1\",\"1\"]]", "output": "1"}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('a8145bde-3d8e-5985-a5e7-2db67288f179', 'Graph');
insert into problem_topic (problem_id, topic) values ('a8145bde-3d8e-5985-a5e7-2db67288f179', 'Matrix');
insert into problem_topic (problem_id, topic) values ('a8145bde-3d8e-5985-a5e7-2db67288f179', 'BFS');
insert into problem_topic (problem_id, topic) values ('a8145bde-3d8e-5985-a5e7-2db67288f179', 'DFS');

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('b3bd4820-2459-5fe8-bfb8-cc4ac031241a', 'Daily Temperatures', 'daily-temperatures', 'MANUAL', null, null, 'MEDIUM',
        'Given an array temperatures where temperatures[i] is the temperature on day i, return an array answer such that answer[i] is the number of days you have to wait after day i to get a warmer temperature.

If no future day is warmer, set answer[i] to 0.

Example 1:
Input: temperatures = [73,74,75,71,69,72,76,73]
Output: [1,1,4,2,1,1,0,0]

Example 2:
Input: temperatures = [30,40,50,60]
Output: [1,1,1,0]

Constraints:
1 <= temperatures.length <= 10^5
30 <= temperatures[i] <= 100',
        'Constraints:
1 <= temperatures.length <= 10^5
30 <= temperatures[i] <= 100',
        '[{"input": "temperatures = [73,74,75,71,69,72,76,73]", "output": "[1,1,4,2,1,1,0,0]"}, {"input": "temperatures = [30,40,50,60]", "output": "[1,1,1,0]"}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('b3bd4820-2459-5fe8-bfb8-cc4ac031241a', 'Array');
insert into problem_topic (problem_id, topic) values ('b3bd4820-2459-5fe8-bfb8-cc4ac031241a', 'Stack');
insert into problem_topic (problem_id, topic) values ('b3bd4820-2459-5fe8-bfb8-cc4ac031241a', 'Monotonic Stack');

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('3c9b396c-64f6-5e98-8412-4575bad3e277', 'Kth Largest Element in an Array', 'kth-largest-element-in-an-array', 'MANUAL', null, null, 'MEDIUM',
        'Given an integer array nums and an integer k, return the k-th largest element in the array.

Note that this is the k-th largest in sorted order, not the k-th distinct element.

Example 1:
Input: nums = [3,2,1,5,6,4], k = 2
Output: 5

Example 2:
Input: nums = [3,2,3,1,2,4,5,5,6], k = 4
Output: 4

Constraints:
1 <= k <= nums.length <= 10^5
-10^4 <= nums[i] <= 10^4

Follow up: can you solve it without fully sorting the array?',
        'Constraints:
1 <= k <= nums.length <= 10^5
-10^4 <= nums[i] <= 10^4',
        '[{"input": "nums = [3,2,1,5,6,4], k = 2", "output": "5"}, {"input": "nums = [3,2,3,1,2,4,5,5,6], k = 4", "output": "4"}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('3c9b396c-64f6-5e98-8412-4575bad3e277', 'Array');
insert into problem_topic (problem_id, topic) values ('3c9b396c-64f6-5e98-8412-4575bad3e277', 'Heap');
insert into problem_topic (problem_id, topic) values ('3c9b396c-64f6-5e98-8412-4575bad3e277', 'Sorting');

insert into problem (id, title, slug, source, external_id, url, difficulty, statement, constraints_text, examples, owner_id, curated, created_at, updated_at)
values ('26b5a1a1-0f3c-5305-ba8a-772ef367e5a9', 'Valid Anagram', 'valid-anagram', 'MANUAL', null, null, 'EASY',
        'Given two strings s and t, return true if t is an anagram of s, and false otherwise.

An anagram uses exactly the same characters with exactly the same frequencies, in any order.

Example 1:
Input: s = "anagram", t = "nagaram"
Output: true

Example 2:
Input: s = "rat", t = "car"
Output: false

Constraints:
1 <= s.length, t.length <= 5 * 10^4
s and t consist of lowercase English letters.

Follow up: what would change if the inputs contained Unicode characters?',
        'Constraints:
1 <= s.length, t.length <= 5 * 10^4
s and t consist of lowercase English letters.',
        '[{"input": "s = \"anagram\", t = \"nagaram\"", "output": "true"}, {"input": "s = \"rat\", t = \"car\"", "output": "false"}]',
        null, true, current_timestamp, current_timestamp);
insert into problem_topic (problem_id, topic) values ('26b5a1a1-0f3c-5305-ba8a-772ef367e5a9', 'String');
insert into problem_topic (problem_id, topic) values ('26b5a1a1-0f3c-5305-ba8a-772ef367e5a9', 'Hash Table');
insert into problem_topic (problem_id, topic) values ('26b5a1a1-0f3c-5305-ba8a-772ef367e5a9', 'Sorting');
