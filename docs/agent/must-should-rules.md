# MUST and SHOULD Rules

This file is the low-cognitive-load policy for agents.

## MUST (non-negotiable)

1. Keep one objective per PR.
2. Do not commit generated artifacts (`dist`, `test-results`, local screenshots unless requested).
3. Run `npm run agent:preflight` before push.
4. If frontend changed, include Playwright UX evidence in PR.
5. If backend changed, run Java tests (`mvn -q test`).
6. If contract changed, keep OpenAPI/ADR aligned in same PR.
7. Include rollback and migration impact in PR (`none` if none).
8. Do not bypass CI checks.
9. Do not introduce malformed JSON/markdown.
10. Do not ship behavior changes without tests.

## SHOULD (strong recommendation)

1. Prefer small PRs over broad batches.
2. Prefer explicit names and stable interfaces.
3. Add one anti-pattern note in docs for risky features.
4. Keep examples copy/paste runnable.
5. Raise ambiguity early instead of guessing.
