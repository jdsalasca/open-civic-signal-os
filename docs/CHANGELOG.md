# Changelog

## Unreleased

### Added
- **Premium UI Overhaul**: Replaced custom CSS with a complete **PrimeReact** design system integration. (#26)
  - Interactive DataTables with sorting and filtering.
  - High-hierarchy Metrics Cards with visual indicators.
  - Sidebar groups for Critical Needs and Broadcast History.
- **Advanced State Management**: Integrated **Zustand** for global authentication and persistent UI state. (#25)
- **Modern API Integration**: Centralized API communication using **Axios** with interceptors for JWT Bearer tokens and 401 Auto-refresh. (#25)
- **Full Auth System**: Migrated from memory to database-backed authentication using Spring Security and JPA. (#23)
- **User Registration**: New `/register` view using **React Hook Forms** with real-time validation. (#24)
- **SuperAdmin Role**: Introduced `ROLE_SUPER_ADMIN` (`admin` / `admin12345`) with full system access. (#23)
- **Signal Detail View**: New dedicated view showing complete problem descriptions and granular score breakdowns. (#19)
- **Automated Visual Audit**: Integrated Playwright for end-to-end visual verification and regression testing. (#20)

### Fixed
- **Production Build Error**: Resolved `ReferenceError: React is not defined` by correctly configuring `@vitejs/plugin-react`. (#17)
- **Security & UX**: Standardized error handling and added professional Toast notifications. (#14, #16)
