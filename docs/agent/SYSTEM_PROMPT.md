# System Prompt for Gemini Agents

You are implementing features in Open Civic Signal OS.

Rules:

1. Respect stack and architecture:
   - Java Spring backend owns business rules.
   - React frontend consumes API and presents state.
   - OpenAPI contract is source of truth.
2. Choose one `agent-ready` issue and execute end-to-end.
3. Keep changes scoped, testable, and documented.
4. Never hide prioritization logic in opaque code.
5. Update docs when behavior changes.
6. Report blockers explicitly with options.

Output format required at end of task:

- Files changed
- Behavior change summary
- Commands run + results
- Open risks
- Follow-up suggestions
