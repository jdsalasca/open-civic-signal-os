# Changelog

## Unreleased

### Added
- **List-level Explainability Summary Contract**: Added backend-owned `explainabilitySummary` to prioritized signal responses and surfaced "why ranked here" snippets directly in dashboard rows, with API + Playwright coverage. (`story:OCS-P1-020`)
- **RBAC Hardening & Testing**: Reinforced Role-Based Access Control at the API level and implemented dedicated integration tests (`SecurityRBAC_IT`) to ensure unauthorized access is blocked. (#43)
- **Database Schema Validation**: Enforced strict schema validation (`ddl-auto: validate`) using Flyway as the source of truth for database migrations. (#42)
- **Standardized Logging**: Replaced legacy console outputs with professional SLF4J logging across all backend services for improved observability. (#41)
- **Build-Time Type Safety**: Integrated `tsc --noEmit` into the frontend build process to ensure zero type errors in production. (#40)
- **Batch Deduplication**: Refactored the duplicate signal detection to use a rolling window strategy, improving performance for large datasets. (#39)
- **Typed Domain Exceptions**: Introduced `ResourceNotFoundException`, `ConflictException`, and others for semantic API error responses. (#3)
- **Flyway Migrations**: Initialized professional database schema management. (#9)
- **Persistent Notifications**: Notification history is now stored in PostgreSQL, replacing the previous in-memory list. (#36)
- **Optimized Ranking**: Top-10 ranking logic has been moved to the database level for improved scalability and performance. (#37)
- **Automated Abuse Detection**: Signals with suspicious patterns (e.g., high urgency with low impact) are now automatically flagged for review. (#30)
- **Moderation Queue**: Dedicated view for Staff and SuperAdmins to approve or reject flagged signals with internal notes. (#30)
- **Infrastructure Secrets Management**: Migrated sensitive credentials to a unified `.env` file, removing hardcoded values from Docker Compose. (#31)
- **Login-First Policy**: Updated routing to force authentication before accessing the dashboard or reporting issues. (#29)
- **Governance Insights UI**: Major dashboard enhancement with real-time category distribution charts and high-quality visual hierarchy. (#28)
- **Citizen Voting System**: Users can now "Support" issues from the detail view. Each vote increases the community support factor and automatically recalculates the global priority score. (#27)
- **Premium UI Overhaul**: Replaced custom CSS with a complete **PrimeReact** design system integration. (#26)
- **Advanced State Management**: Integrated **Zustand** for global authentication and persistent UI state. (#25)
- **Modern API Integration**: Centralized API communication using **Axios** with interceptors for JWT Bearer tokens and 401 Auto-refresh. (#25)
- **Full Auth System**: Migrated from memory to database-backed authentication using Spring Security and JPA. (#23)
- **User Registration**: New `/register` view using **React Hook Forms** with real-time validation. (#24)
- **SuperAdmin Role**: Introduced `ROLE_SUPER_ADMIN` (`admin` / `admin12345`) with full system access. (#23)
- **Signal Detail View**: New dedicated view showing complete problem descriptions and granular score breakdowns. (#19)
- **Automated Visual Audit**: Integrated Playwright for end-to-end visual verification and regression testing. (#20)

### Fixed
- **Role-Aware Guided Home**: Dashboard hero and guidance card now adapt to citizen, moderator, and public-servant intent so each user sees one clearer next step instead of generic messaging. (`story:OCS-P1-024`)
- **Dashboard Choice Overload**: The first dashboard viewport now centers one primary report action and moves secondary tools into a lower-emphasis action surface to reduce first-run confusion. (`story:OCS-P1-023`)
- **Keyboard Navigation Baseline**: The skip link now moves focus into the main content region, and mobile navigation exposes clearer ARIA state for keyboard and assistive-tech users. (`story:OCS-P1-019`)
- **Playwright Wrapper Reliability (Windows)**: `agent:ux:pw screenshot <path>.png` now works with explicit output paths and saves artifacts in the requested location, matching documented workflow expectations.
- **Community Permission Transparency**: Blog and thread creation now surface backend `403` permission reasons inline in the UI (EN/ES) to avoid opaque action failures for unauthorized roles. (`P1 trust-critical`)
- **Double Prefix Bug**: Resolved issue where API requests hit `/api/api/*`. (#UX-001)
- **Contrast Compliance**: Hardened visual tokens to meet WCAG AA standards across all components. (#UX-007)
- **State Hydration**: Improved AuthGuard to wait for store rehydration, preventing false-positive logouts.
- **Async Robustness**: Added `@Async` to EmailService with structured error logging. (#38)
- **Null Safety**: Hardened `mergedFrom` collection initialization in Signal entity. (#P1-08)
- **Container Accessibility**: Corrected Docker Compose dependencies to ensure the `civic-web` container starts reliably.
- **Production Build Error**: Resolved `ReferenceError: React is not defined` by correctly configuring `@vitejs/plugin-react`. (#17)
