# PR Review Checklist

## Scope integrity

- [ ] PR maps to exactly one main issue objective.
- [ ] No unrelated refactors included.

## Backend and contracts

- [ ] Backend logic follows product rules.
- [ ] OpenAPI updated if API changed.
- [ ] Response fields match frontend usage.

## Frontend quality

- [ ] Handles loading/error/empty states.
- [ ] No hidden business logic drift from backend.
- [ ] Basic UX remains clear on desktop/mobile.

## Reliability

- [ ] Commands pass:
  - [ ] `npm run prioritize`
  - [ ] `npm run build:web`
  - [ ] Java build (or blocker documented)

## Trust and transparency

- [ ] Priority explanations remain visible.
- [ ] No opaque scoring behavior introduced.

## Release hygiene

- [ ] Docs updated.
- [ ] Issue linked and status labels updated.
