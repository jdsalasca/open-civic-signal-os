---
phase: 05-community-operating-system-expansion
plan: 04B
type: execution
wave: 1
depends_on:
  - "05-04A"
files_expected:
  - apps/api-java/src/main/java/org/opencivic/signalos/domain/InterfaceMode.java
  - apps/api-java/src/main/java/org/opencivic/signalos/domain/User.java
  - apps/api-java/src/main/java/org/opencivic/signalos/web/AuthController.java
  - apps/api-java/src/main/java/org/opencivic/signalos/web/dto/UpdateUserProfileRequest.java
  - apps/api-java/src/main/java/org/opencivic/signalos/web/dto/UserProfileResponse.java
  - apps/api-java/src/main/resources/db/migration/V16__User_Interface_Mode.sql
  - apps/api-java/src/test/java/org/opencivic/signalos/security/UserProfileVisibilityIT.java
  - apps/web-react/src/store/useSettingsStore.ts
  - apps/web-react/src/views/Settings.tsx
  - apps/web-react/src/components/Layout.tsx
  - apps/web-react/src/views/Dashboard.tsx
  - apps/web-react/src/App.tsx
  - apps/web-react/src/main.tsx
  - apps/web-react/src/types.ts
  - apps/web-react/src/tests/interface-mode-settings.spec.ts
  - packages/contracts/openapi.yaml
  - docs/architecture/ADR-20260305-interface-mode-profile-contract.md
requirements:
  - COS-01
  - COS-07
  - COS-08
autonomous: true
---

<objective>
Turn simple-vs-advanced experience mode into a persisted product capability instead of a visual aspiration.

Purpose: keep first-time members in a calmer default shell while preserving deeper workspace tools for advanced users and institutional operators.
Output: backend-owned interface mode preference, updated profile contract, settings controls, shell/dashboard behavior changes, and Docker-validated frontend build evidence.
</objective>
