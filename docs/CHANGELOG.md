# Changelog

## Unreleased

### Added
- **Duplicate Signal Detection**: Added heuristics (Levenshtein distance and partial title matching) to identify potential duplicate community signals. (#8)
- **Backend Hot-Reloading**: Integrated `spring-boot-devtools` and updated Docker configuration to support real-time development updates.
- **Dockerized Infrastructure**: Added multi-stage Dockerfiles and `docker-compose.yml` for production-ready deployment (Ports: 3002 for Web, 8081 for API).
- **Weekly Top-10 Digest**: New sidebar visualization and API endpoint highlighting the top unresolved community needs. (#6)
- **Dashboard Filters & Lifecycle**: Added real-time filtering by Category and Status to the public dashboard. (#5)
- **UI Enhancements**: Integrated status-coded pills and visualized scoring breakdowns with tooltips in the signal table. (#5)
- **Explainable Scoring Breakdown**: Prioritized signals now include a breakdown of their score (urgency, impact, reach, community votes) for full transparency. (#4)
- **Multi-channel Ingest Adapters**: New ingestion pipeline supporting JSON, CSV, and Chat-like exports into the civic backlog. (#3)
- Added release quality gate workflow with semver, changelog, and coverage checks.
- Added agent-first release policy documentation.
