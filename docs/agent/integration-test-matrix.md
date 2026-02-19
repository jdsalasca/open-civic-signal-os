# Integration Test Matrix

| Area | Scenario | Expected Result | Priority |
| --- | --- | --- | --- |
| Contract | valid request | schema-compliant response | P0 |
| Contract | invalid request | structured validation error | P0 |
| Core logic | same input replay | deterministic output | P0 |
| Persistence/replay | duplicate event | no duplicate side effect | P0 |
| Frontend integration | consume latest payload | render without runtime errors | P1 |
| Export/report | output generation | required fields present | P1 |
| Security | malformed input attempt | safe rejection | P0 |
| Observability | failed flow | traceable diagnostics present | P1 |

Rule: PR verification should reference rows from this matrix.
