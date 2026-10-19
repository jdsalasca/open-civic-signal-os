# ADR-20260321: Federation Manifest

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #33, and the compatibility half of #11
- **Contract:** `packages/contracts/openapi.yaml` (`/api/federation/manifest`, `/compatibility`)

## Context

The open-data API already serves a community's backlog to a token holder. What it does not do is
tell a **peer instance** what it can consume and in what shape.

City-to-city compatibility fails in a specific way. Two instances running different versions will
each assume the other speaks their dialect, and the mismatch surfaces as a parse error deep in an
importer rather than as a clear "we do not speak the same version". By then the importer has already
written half a dataset.

## Decision

### The manifest is public and carries no data

`GET /api/federation/manifest` is `permitAll`. A peer has to read the contract **before** it can
decide whether to ask for a token, so requiring authentication would make the handshake impossible.

It lists capabilities, not records. Reading an export still requires a scoped token issued by the
serving community, exactly as before. Pinned by `theManifestShouldNotExposeAnyData`, which asserts no
`title` or `priorityScore` appears in the response.

### The contract version is explicit, and a mismatch is a refusal

`contractVersion` is `v1`, bumped when the shape of a federated export changes in a way a consumer
would notice. `supportedContractVersions` lists what this instance can still read, so an older peer
is not cut off the moment a new version ships.

`GET /api/federation/compatibility?peerContractVersion=v99` returns `readable: false` with the
reason: **a best-effort parse of an unrecognised shape would produce data that looks valid and is
not.** Pinned by `anUnrecognisedContractVersionShouldBeRefusedRatherThanGuessed`.

### The manifest does not promise that a peer speaks the same contract

This is the sentence the ADR exists for. A peer reading "contractVersion v1" could wrongly conclude
that any v1 peer is compatible. It says what **this** instance serves; it says nothing about the
other one.

`interpretation` states on every response that a consumer must read the peer's own manifest and
compare, because the alternative is the parse error described above. Pinned by
`theManifestShouldNotPromiseThatAPeerSpeaksTheSameContract`.

### The dataset list comes from the export centre, not a second copy

`CommunityOpenDataService.exportDefinitions()` is now exposed and the manifest maps over it. A peer
reading the manifest and a consumer reading the export centre cannot disagree about what exists.

Each entry carries the **scope** a consumer needs, so it knows which token to ask for.

## Consequences

- A peer can discover the contract before exchanging anything.
- A version mismatch is a clear refusal rather than a half-written import.
- The manifest cannot drift from the export centre.
- No data is exposed by the discovery endpoint.
- An older peer keeps working while a newer contract version is supported.

## Known limits

- **No client.** The manifest exists and is tested; nothing in this repository consumes a peer's
  manifest. A federation client is the other half of #11.
- **No peer registry.** An instance does not record which peers it trusts or has exchanged with.
- **No push.** This is pull-only discovery. A city cannot be notified that another published
  something.
- **No schema diff.** The manifest states a version number; it does not describe what changed between
  versions, so a consumer cannot tell whether a bump affects the fields it uses.
- **No signature.** The manifest is not signed, so a consumer cannot verify it came from the instance
  it claims to be. Same limitation as the backlog publication hash, and it matters more here because
  the peer is remote.
- **`supportedContractVersions` is a constant list.** Adding a version is a code change, which is
  correct but means a peer cannot be supported without a deploy.
- **No data residency or consent model.** Whether a community's data may leave its instance at all is
  a governance question this ADR does not answer, and it should be answered before a client is built.