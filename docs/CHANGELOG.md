# Changelog

## Unreleased

### Added
- **Full Auth System**: Migrated from memory to database-backed authentication using Spring Security and JPA. (#23)
- **User Registration**: New `/register` view using **React Hook Forms** with real-time validation and role selection. (#24)
- **SuperAdmin Role**: Introduced `ROLE_SUPER_ADMIN` (`admin` / `admin12345`) with full system access. (#23)
- **RBAC Refinement**: Granular control over UI elements and API endpoints for SuperAdmin, Staff, and Citizens. (#23)
- **Signal Detail View**: New dedicated view showing complete problem descriptions and granular score breakdowns for transparency. (#19)
- **Modern Visual Redesign**: Implemented a "Glassmorphism" aesthetic with a deep dark palette, blurred backgrounds, and interactive hover effects. (#21)
- **Automated Visual Audit**: Integrated Playwright for end-to-end visual verification and regression testing. (#20)
- **Standardized Error Handling**: Integrated `GlobalExceptionHandler` with a new `ApiError` format for consistent structured error responses. (#14)
- **PostgreSQL Persistence**: Replaced mock repositories with Spring Data JPA and real PostgreSQL connectivity. (#12)
- **Messaging Relay Service**: Added `NotificationService` to simulate and track outgoing relays to WhatsApp and Telegram. (#7)

### Fixed
- **Production Build Error**: Resolved `ReferenceError: React is not defined` by correctly configuring `@vitejs/plugin-react`. (#17)
- **API Mapping**: Resolved JPA column duplication errors in the `Signal` entity.
