# Task Pack: Frontend

## Inputs

- Story ID
- Target route/view
- UX acceptance criteria

## Implement

1. Build minimal UI change for story outcome.
2. Keep API consumption aligned with current contract.
3. Add/adjust loading, empty, and error states.

## Verify

1. `npm run build:web`
2. `npm run agent:ux:pw -- open http://localhost:5173 --headed`
3. `npm run agent:ux:pw -- snapshot`
4. `npm run agent:ux:pw -- screenshot output/playwright/<story>-<view>.png`

## Output

- Code diff
- UX Evidence (Playwright)
- Risks and rollback
