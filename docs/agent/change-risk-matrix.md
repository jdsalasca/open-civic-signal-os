# Change Risk Matrix

Use before implementation.

| Change Type | Example | Risk | Required Controls |
| --- | --- | --- | --- |
| Docs only | guide updates | Low | link check |
| Internal refactor | no API changes | Medium | regression tests |
| API payload change | response fields change | High | OpenAPI + migration notes |
| Core logic update | scoring rules | High | deterministic tests + reproducibility check |
| Security/policy change | auth/policy changes | Critical | review + audit notes + rollback |

## Rule

If risk is High or Critical, split into smaller PRs unless impossible.
