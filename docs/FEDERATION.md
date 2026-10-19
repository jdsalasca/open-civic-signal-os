# Federation Setup and Migration

How to run two or more Open Civic Signal OS instances as one civic federation, and how to adopt it on
an instance that already exists.

Design decisions live in `docs/architecture/ADR-20260321-federation-manifest.md` and
`docs/architecture/ADR-20260321-city-namespace-and-federated-backlog.md`. This page is the operator's
route.

## What federation is here

Each deployment owns its communities and their residents' reports. Federation lets one instance read
another's **published** data over HTTP, under a token the serving community issues. Nothing is
synchronised: there is no replication and no shared database.

That is a deliberate boundary. A federation that replicated reports would have to answer where a
resident's report physically lives and who can compel its disclosure. This one answers "who asked for
it, with which scope, and did the community still consent at the time".

## The one rule

**A community that does not enable open data serves nothing.** Turning the policy off stops the feed
immediately, for tokens issued before the change. Checked before the token is even validated, because
consent is the stronger claim.

## Setup

### 1. Read the manifest

```
GET https://<peer>/api/federation/manifest
```

Public, unauthenticated, carries no data. Read it before asking for anything:

- `contractVersion` — the shape this instance serves
- `supportedContractVersions` — what it can still read, so an older peer is not cut off
- `cities` — the cities it federates, each with a `federationKey`
- `citiesNotAdvertised` — how many communities exist but are not listed, because they have not opted in

Then confirm the peer can read you:

```
GET https://<peer>/api/federation/compatibility?peerContractVersion=v1
```

Compare `contractVersion` yourself. A peer at a version you cannot read should be refused, not parsed
optimistically — the mismatch otherwise surfaces as a parse error deep in somebody's integration
instead of as a clear refusal.

### 2. Address a city by its key

`federationKey`, never the community id. The id is local to one deployment and means nothing to a peer.
The key is stable for the life of the community, while a slug can be renamed under you.

### 3. Get a token from the community

A coordinator of the serving community issues it, scoped to what you need:

```
POST /api/community/exports/tokens
{ "communityId": "...", "label": "Peer instance", "scopes": ["EXPORT_PRIORITIZED_BACKLOG"], "rateLimitPerHour": 60 }
```

`scopes` is enforced per dataset. A token scoped to `EXPORT_SIGNALS` does **not** gain
`EXPORT_PRIORITIZED_BACKLOG`; an existing feed token does not silently acquire a new dataset.

Available scopes: `EXPORT_SIGNALS`, `EXPORT_PROPOSALS`, `EXPORT_VOTES`, `EXPORT_DECISIONS`,
`EXPORT_METRICS`, `EXPORT_PRIORITIZED_BACKLOG`.

The plaintext token is returned once. If it is lost, issue another and revoke the first.

### 4. Read

```
GET https://<peer>/api/open-data/{communityId}/prioritized_backlog
X-Api-Token: ocs_...
```

Response headers carry your budget: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.

Every row carries `cityKey`, `rank`, `priorityScore`, the four score components, `formulaVersion` and
`formulaExpression`. Render your own explanation of the rank from those; the platform does not send a
sentence in its own voice, so there is nothing to translate or disagree with.

`signalId` is local to the serving instance. Key your copy on `(cityKey, signalId)`.

## Migration path for an existing instance

### Single instance to a federation member

1. Deploy. Nothing changes: the feed stays as permissive as it already was.
2. Read your own manifest: `GET /api/federation/manifest`. Note `citiesNotAdvertised`.
3. For each community that wants to be federated, set its open-data policy to
   `AGGREGATED_PUBLIC` (or `AGGREGATED_AND_DECISIONS` to include the decision ledger).
4. Re-read the manifest. Communities that opted in now appear under `cities` with their
   `federationKey`; the rest are counted only.
5. Tell peers the keys. Keys are stable from here on.

### Behaviour change to plan for

The token feed now refuses a community whose policy is `DISABLED`. An instance whose communities never
set a policy will find every token read returning 403 until those policies are set. This is consent
doing its job, and it is the only intentional break here.

### Two existing instances merging into one federation

They do not merge their data. Each keeps its own communities and serves its own tokens; federation only
adds the ability to read across. Keys are per instance and per community, so two cities in different
instances never collide, and two cities in the same instance never collide either — both are covered
by a unique index.

## What this does not do

- **No replication and no shared database.**
- **No inbound.** A peer reads; nothing is pushed into this platform.
- **No mutual authentication yet.** Tokens are bearer credentials over TLS. Signing responses and
  pinning peers is governance work that has not been done, and `ADR-20260321-city-namespace-and-federated-backlog.md`
  says why it is not being invented inside a dataset definition.
- **No data residency answer.** Where a federated copy of a report physically lives, and who can compel
  its disclosure, is unresolved. This is the main thing to settle before a real cross-border
  deployment.
- **No cross-instance signal identity.** `signalId` is instance-local.
- **The backlog is top 50 by score.** Use `EXPORT_SIGNALS` for everything.