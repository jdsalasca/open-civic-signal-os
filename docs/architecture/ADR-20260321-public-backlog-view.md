# ADR-20260321: A Public Backlog Readable Without An Account

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** the dashboard half of GitHub #2
- **No contract change:** the endpoints it reads were already public and documented

## Context

`SecurityConfig` already served `/api/signals/prioritized`, `/top-10`, `/meta` and `/formula` with
`permitAll`. The ranked backlog was public data. The only way to *look at* it, though, was the
dashboard at `/`, which sits inside `AuthGuard`.

So the platform published its backlog through an API almost nobody would call by hand, and asked
everyone else to register before they could see whether registering was worth it. AGENTS.md says
inclusion is a default; requiring an account to look at public data is the opposite of that.

## Decision

### A route outside the auth guard, reading only public endpoints

`/backlog` renders `PublicBacklog`, which calls `signals/prioritized`, `signals/meta` and
`signals/formula` and nothing else. No auth header, no session, no redirect.

This is the whole risk of the feature, and it is subtle: a page reachable without a session that
fires even one authenticated request will redirect a stranger to a login screen, and the feature
silently stops existing for exactly the people it was built for. The Playwright test asserts on the
outgoing request headers for that reason — `authorization` absent, no `accessToken` cookie — because
asserting on rendered content could not catch it.

Tested paths:

- A stranger with no stored session sees the list, not a login form.
- Each item shows the formula's terms, so the order is checkable.
- The formula and its version appear above the list.
- The freshness stamp appears, because AGENTS.md requires every dashboard to expose data freshness.
- An unreachable API shows an unavailable state, **not** a login prompt.
- A link back to sign in exists, so it is not a dead end.

### The formula is shown before the list, not after

A resident deciding whether to trust a ranking should be able to read the rule before reading the
result. Putting the formula in a footer implies it is a disclaimer; putting it first makes it the
premise.

### The reason is labelled as score terms, not as counts

`scoreBreakdown` values are already **weighted and capped**. For a report affecting 900 people, the
`affectedPeople` term is `30` — the cap — not `900`.

My first version rendered that as "900 people affected", which was wrong in a specific and
misleading way: a page whose purpose is to make the ranking auditable was inventing a headcount
from a score contribution. It now reads "score terms: urgency 150, impact 125, people 30, votes 8",
which is exactly what those numbers are.

Found by the test asserting on the text. The test now pins the honest label.

### The primitives gained `data-testid` forwarding

`CivicBadge` and `CivicEmptyState` accepted no extra DOM attributes, so tagging an asserted element
required a wrapper div that existed only to hold an attribute. Both now forward rest props to their
root.

This is a small change to shared components, made deliberately: AGENTS.md asks for stable test hooks
and centralized primitives, and a wrapper purely for the test is neither.

## Consequences

- A resident can see what the platform tracks before registering.
- The order is checkable without an account, because the formula and its version are on the page.
- The page cannot silently degrade into a login wall; the test fails if it does.
- Shared UI primitives are now taggable, which removes a reason for per-view wrappers.

## Known limits

- **Read-only and global.** No community selector, no filters, no paging. It shows the top 20 of the
  global backlog. Per-community public views need a scoping decision the API does not yet make
  public.
- **Not low-bandwidth optimised beyond being simple.** AGENTS.md asks for readability in low-bandwidth
  scenarios; this is a plain list with no images, but it is not measured against a throttled profile.
- No public signal detail page, so a resident cannot open an item. That needs a public read of
  `/api/signals/{id}` and a decision about what it may expose.
- The dashboard at `/` still requires an account, so authenticated community views and the public
  view remain two different pages with duplicated rendering.
- No `robots.txt` or meta-robots decision. A public backlog is indexable, which is likely desirable
  but has not been decided.