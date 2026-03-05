---
phase: 05-community-operating-system-expansion
plan: 04A
type: execution
wave: 1
depends_on:
  - "05-03"
  - "05-04"
files_expected:
  - apps/web-react/src/components/Layout.tsx
  - apps/web-react/src/views/Dashboard.tsx
  - apps/web-react/src/components/ui/CivicButton.tsx
  - apps/web-react/src/components/ui/CivicCard.tsx
  - apps/web-react/src/components/ui/CivicBadge.tsx
  - apps/web-react/src/components/ui/CivicToolbar.tsx
  - apps/web-react/src/components/ui/CivicActionBar.tsx
  - apps/web-react/src/components/ui/CivicPageHeader.tsx
  - apps/web-react/src/components/ui/CivicEmptyState.tsx
  - apps/web-react/src/styles/theme.scss
  - apps/web-react/src/styles/main.scss
  - apps/web-react/src/i18n.ts
  - docs/community/current-backlog.md
  - docs/community/community-operating-system-backlog.md
  - docs/CHANGELOG.md
  - .planning/STATE.md
requirements:
  - COS-02
  - COS-07
  - COS-08
autonomous: true
---

<objective>
Standardize the visual core of the product so the community operating system stops feeling generic and starts presenting a clear, pleasant, and accessible civic identity.

Purpose: create a shared shell, typographic direction, and component language that can support both simple and advanced experience modes without fragmenting UX quality.
Output: redesigned shell, updated dashboard hero, standardized shared primitives, bilingual copy alignment, and Docker-validated frontend build evidence.
</objective>
