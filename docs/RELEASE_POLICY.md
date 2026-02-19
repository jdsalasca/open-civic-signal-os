# Release Policy

## Objective

Every release must pass a quality gate before publishing.

## Mandatory checks

- Semver validity across package manifests.
- Changelog updated under `## Unreleased`.
- Coverage summary present and above configured threshold.

## Coverage configuration

Coverage threshold is controlled from `package.json`:

```json
"releaseQuality": {
  "minimumCoverage": 0
}
```

Raise this value progressively as test depth increases.

## Release workflow

1. Update changelog unreleased section.
2. Ensure coverage summary is up to date.
3. Run `npm run quality:release`.
4. Create release PR/tag only after gate passes.
