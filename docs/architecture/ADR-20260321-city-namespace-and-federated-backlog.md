# ADR-20260321: City Namespace and Federated Backlog

- Status: accepted
- Date: 2026-03-21
- Relates to: `ADR-20260321-federation-manifest.md`

## Context

The manifest shipped describing *what shape* this instance serves. It did not say *which cities*, and
the open-data export URL embeds `community.id` — a UUID minted by one deployment. A peer therefore
could not bootstrap: to ask for a city's backlog it had to already know an id only that deployment
could mint.

Two obvious fixes, both rejected:

- **Reuse the slug.** Public and human-readable, but mutable. A renamed city silently breaks every
  peer holding it, with no error on either side.
- **Let a peer pass a community UUID.** Simple, and it makes the federation key indistinguishable from
  an internal identifier, which is exactly what it should not be.

## Decision

### A stable, opaque federation key per community

`communities.federation_key`, 24 hex characters, unique, never null. Minted by
`Community.assignFederationKey` on persist, so a community created by a test, a seed or an admin path
is addressable too. Backfilled for existing rows from the id's own hex rather than a hash function,
because the migration has to run on PostgreSQL and on the H2 the tests use.

A lifecycle callback rather than a line in the creation service: the key is an invariant of the
entity, and leaving it to one service means a null column that only surfaces when a peer tries to use
it.

### The manifest lists the cities it federates

`federationKey`, `name`, `openDataPolicy` per city, plus `citiesNotAdvertised`.

**Opted-out communities are counted, not named.** The manifest is unauthenticated, so listing a
community that has not enabled open data would publish its existence to anyone who asked. The count
distinguishes "this instance federates nothing" from "this instance hides some", without disclosing
which.

### `PRIORITIZED_BACKLOG` as an export type, not a new endpoint

A bespoke `/api/federation/backlog` would have re-implemented token scoping, the anonymization gate,
the access log, rate limiting and CSV rendering. Adding a sixth export type gets all of that for free,
on the path that is already audited.

Appended to the enum rather than inserted: `CommunityOpenDataTokenScope` pairs with the export type
by ordinal, so inserting a value would silently repoint every later token scope at the wrong export.

### Rows carry the formula's inputs, not a second "why"

Each row carries `cityKey`, `rank`, `priorityScore`, the four components, `formulaVersion` and
`formulaExpression`. The digest renders a "why" sentence in this platform's voice; a peer renders its
own. A second prose rationale here would be a second thing to keep in step with the formula — the
failure `PrioritizationFormula` already exists to prevent.

`unresolved` is decided against the `SignalStatus` enum rather than a set of status strings, for the
same reason. **An unrecognised status counts as open**: a public backlog should not quietly drop a
resident's report because its status string is one this version has not heard of.

### Withdrawing consent stops the feed

`readWithToken` now checks the community's current open-data policy before validating the token. The
policy is the stronger claim: a community that withdraws open data expects its data to stop crossing
the instance boundary whether or not a peer still holds a token issued earlier. Validating only the
token meant a consent withdrawal changed nothing for anyone already scraping.

**This is a behaviour change.** A community whose policy is `DISABLED` now receives 403 on the token
feed. An existing test was relying on the old behaviour — its fixture never enabled open data — so the
change surfaced immediately rather than in production.

## Consequences

- Two cities coexist without conflict, and `FederationCityBacklogIT` asks the cross-city question
  directly: does anything from the other city appear in a city's backlog?
- `PRIORITIZED_BACKLOG` needs its own token scope. A token scoped to `EXPORT_SIGNALS` does not gain
  the dataset, so an existing feed token does not silently acquire a new one.
- Signal ids in the export remain instance-local. The city key is the real namespace; a peer keys rows
  on `(cityKey, signalId)`. A content-addressed cross-instance signal id is future work.
- Top 50 by score. `SIGNALS` remains the way to get everything.

## Alternatives rejected

- **A separate federation controller.** Duplicates an audited path to serve a dataset that fits it.
- **Hashing the whole backlog export as a signature.** That is a mutual-authentication decision, and
  it belongs with the governance work on peer trust rather than inside a dataset definition.