#!/usr/bin/env node

import process from "node:process";

const body = process.env.PR_BODY ?? "";
const changedFiles = (process.env.CHANGED_FILES ?? "")
  .split("\n")
  .map((f) => f.trim())
  .filter(Boolean);

const frontendTouched = changedFiles.some((file) => file.startsWith("apps/web-react/"));

if (!frontendTouched) {
  console.log("Frontend UX evidence check skipped: no frontend files changed.");
  process.exit(0);
}

const failures = [];
const uxSectionMatch = body.match(/## UX Evidence \(Playwright\)([\s\S]*)/i);

if (!uxSectionMatch) {
  failures.push("Missing section: ## UX Evidence (Playwright)");
} else {
  const section = uxSectionMatch[1].slice(0, 1200);
  const hasNa = /n\/a/i.test(section);
  const hasCommand = /agent:ux:pw|playwright-cli|npm run agent:ux:pw/i.test(section);
  const hasArtifact = /output\/playwright\//i.test(section);

  if (hasNa) {
    failures.push("Frontend changed, but UX Evidence is marked N/A.");
  }
  if (!hasCommand) {
    failures.push("UX Evidence must include Playwright command usage.");
  }
  if (!hasArtifact) {
    failures.push("UX Evidence must include artifact path under output/playwright/.");
  }
}

if (failures.length > 0) {
  console.error("Frontend UX evidence gate failed:\n");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log("Frontend UX evidence gate passed.");
