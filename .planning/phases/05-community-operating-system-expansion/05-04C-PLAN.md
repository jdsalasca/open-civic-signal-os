---
phase: 05-community-operating-system-expansion
plan: 04C
type: execution
wave: 1
depends_on:
  - "05-04B"
files_expected:
  - apps/web-react/src/views/Dashboard.tsx
  - apps/web-react/src/components/Layout.tsx
  - apps/web-react/src/styles/main.scss
  - apps/web-react/src/i18n.ts
  - apps/web-react/src/tests/dashboard-guided-home.spec.ts
requirements:
  - COS-07
  - COS-08
autonomous: true
---

<objective>
Push the visual core refresh into a true community-first home instead of a generic signals dashboard.

Purpose: make the first screen feel habitable, legible, and centered on the active community, while keeping simple mode calmer and more directive.
Output: a community hub layer inside the dashboard, stronger shell affordances toward the communities workspace, EN/ES copy updates, and Docker-validated UX evidence.
</objective>
