# UX Quality Bar (Mandatory)

Visual experience quality is treated as 70% of perceived product value.

## Minimum UX Acceptance

- Clear hierarchy and readable typography.
- Responsive behavior for desktop and mobile.
- Accessible contrast for primary actions and key status signals.
- No broken navigation states or dead-end pages.
- Empty/loading/error states are explicit.

## Mandatory Playwright Evidence

For frontend-impacting changes:

- Run Playwright CLI flow audit.
- Save artifacts under `output/playwright/`.
- Include evidence in PR section `UX Evidence (Playwright)`.

## Failure Conditions

Reject PR if:

- Frontend changed and no Playwright evidence exists.
- Primary user path is broken.
- Visual regression introduces unreadable or inaccessible UI.
