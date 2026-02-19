#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const backlogPath = path.join(root, "docs", "community", "current-backlog.md");

if (!fs.existsSync(backlogPath)) {
  console.error("Missing required backlog file: docs/community/current-backlog.md");
  process.exit(1);
}

const content = fs.readFileSync(backlogPath, "utf8");
const requiredSections = [
  "# Current Backlog",
  "## Current State",
  "## Now (Next 7 Days)",
  "## Next (7-21 Days)",
  "## P0 Execution Queue (Agent Order)",
  "## Definition of Ready for Any New Story"
];

const issues = [];

for (const section of requiredSections) {
  if (!content.includes(section)) {
    issues.push(`Missing section: ${section}`);
  }
}

const queueItems = [...content.matchAll(/^\d+\.\s+`story:/gm)].length;
if (queueItems < 5) {
  issues.push("P0 Execution Queue must include at least 5 story items.");
}

const hasDate = /\(\d{4}-\d{2}-\d{2}\)/.test(content);
if (!hasDate) {
  issues.push("Header must include an ISO date, e.g. (2026-02-19).");
}

if (issues.length > 0) {
  console.error("Current backlog check failed:");
  for (const issue of issues) {
    console.error(`- ${issue}`);
  }
  process.exit(1);
}

console.log("Current backlog check passed.");
