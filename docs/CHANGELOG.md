# Changelog

## Unreleased

### Added
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
- **Container Accessibility**: Corrected Docker Compose dependencies to ensure the `civic-web` container starts reliably.
- **Production Build Error**: Resolved `ReferenceError: React is not defined` by correctly configuring `@vitejs/plugin-react`. (#17)
- **API Mapping**: Resolved JPA column duplication errors in the `Signal` entity.
