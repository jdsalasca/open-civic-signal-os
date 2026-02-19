# API Java

Spring Boot backend for ingestion, prioritization, and public backlog APIs.

## Commands

```bash
./mvnw spring-boot:run
```

If Maven wrapper is not available yet, use:

```bash
mvn spring-boot:run
```

## Initial modules

- `ingest`: parse and validate community signals
- `priority`: deterministic scoring service
- `backlog`: query and status transitions
