# Story Acceptance Checklist

Use this checklist in every PR linked to a story.

## Identity

- [ ] Story ID included (`story:*`)
- [ ] Linked issue included (`#123`)
- [ ] Scope explicitly declared (backend/frontend/contracts/docs)

## Delivery

- [ ] Acceptance criteria implemented
- [ ] No unrelated scope creep
- [ ] Backward compatibility evaluated

## Verification

- [ ] Main project command executed
- [ ] Frontend build executed (if applicable)
- [ ] Backend build/tests executed (if applicable)
- [ ] Results summarized in PR

## Safety

- [ ] Risk section included
- [ ] Rollback plan included
- [ ] Follow-up tasks documented (if partial delivery)

## Docs and Contracts

- [ ] Docs updated for behavior change
- [ ] Contract/OpenAPI updated when payload/API changed
- [ ] Changelog updated when user-visible behavior changed
