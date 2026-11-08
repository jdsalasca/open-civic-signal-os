# ADR-20261108: City, Locality and Neighborhood on the Community

- **Status:** Accepted
- **Date:** 2026-11-08
- **Decides:** GitHub #138
- **Contract:** `packages/contracts/openapi.yaml` (`Community` schema)

## Context

Registration asked for a city name, a country and a region. That is enough to list a city, not
enough to find a barrio: two people reporting the same place wrote "Bogotá", "BOGOTA" and
"  bogota ", and the platform stored three communities that look different and mean the same. The
directory then asked a newcomer to guess which spelling was the real one.

The community is the unit that owns signals and memberships, and the existing `parentCommunityId`
already models nesting. A separate place catalog would add a second tree, a second set of joins and
a second duplicate rule before there is a need for one.

## Decision

### Three nullable fields on `communities`

`city`, `locality` and `neighborhood`. A community can be a city-wide space, a locality or a
single barrio; the visible name is the most specific level provided (barrio → localidad → ciudad).
Country and region stay as they were. Non-geographic spaces (a cross-community hub, a building)
keep all three null and behave exactly as before.

### Normalization is a backend responsibility

`PlaceNames.normalize` trims, collapses whitespace, applies Title Case with Spanish connectors and
keeps accents for display (`Bogotá`). It rejects names longer than 120 characters, names with
characters outside `letters, digits, space . , ' ’ ( ) # & -`, and names without a letter. The UI
never decides what is valid.

### Duplicates are decided on a normalized key

`place_key` is `slug(city)|slug(locality)|slug(neighborhood)`, where the slug folds accents and
case (`Bogotá` and `BOGOTA` both become `bogota`). A unique index plus an application check make a
second community for the same place a **409** with the existing community name, instead of a
directory full of near-duplicates. Rows created before this ADR have `place_key = null` and are
outside the rule; the database allows multiple nulls.

### The registration seeds, it does not build a second tree

The person who registers a place becomes its `COORDINATOR`, as before. Locality and neighborhood
are optional; a city without them remains a valid community.

## Consequences

- A barrio can be registered once and joined by everyone else.
- "Bogotá / Chapinero / El Chicó" and "BOGOTA / chapinero / el chico" collide as intended.
- Search on the landing page and the registration selector match city, locality, barrio, region and
  country.
- Existing communities are untouched and keep their current names.

## Known limits

- **One place per community.** A community cannot span two barrios. That is deliberate for v1; a
  multi-barrio scope would be a different entity.
- **No canonical gazetteer.** The key normalizes spelling, not meaning: "El Chicó" and "El Chico"
  (without the accent) collide, but "Barrio Chicó" does not. A real catalog would need external
  identifiers.
- **No geocoding.** Coordinates stay on the signal, not on the community.
- **No moderation of place names.** A coordinator can still create a place with a misleading but
  well-formed name; the duplicate key is the only automatic control.
