import process from "node:process";

const title = process.env.PR_TITLE ?? "";
const body = process.env.PR_BODY ?? "";

const requiredSections = [
  "## Issue",
  "## Story ID",
  "## Scope",
  "## Verification",
  "## Risks / Notes",
  "## Rollback Plan",
  "## UX Evidence (Playwright)"
];

const missingSections = requiredSections.filter((section) => !body.includes(section));
const hasIssueReference = /#\d+/.test(body);
const hasStoryReference = /story:/i.test(body) || /\b[A-Z]{2,}-P\d-\d{3}\b/.test(body);
const hasVerificationSignal = /Main script passes|Frontend build passes|Java tests pass|Java build passes/i.test(body);
const hasUxEvidence = /Playwright|UX Evidence|N\/A/i.test(body);

const failures = [];
if (!title.trim()) failures.push("PR title is empty.");
if (!hasIssueReference) failures.push("PR body must reference at least one issue number (#123).");
if (!hasStoryReference) failures.push("PR body must include a Story ID (e.g. story:XYZ-P0-001).");
if (!hasVerificationSignal) failures.push("PR body must include verification checklist items.");
if (!hasUxEvidence) failures.push("PR body must include UX evidence section details (Playwright) or justified N/A.");
if (missingSections.length > 0) failures.push(`Missing sections: ${missingSections.join(", ")}`);

if (failures.length > 0) {
  console.error("Agent PR hygiene check failed:\n");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log("Agent PR hygiene check passed.");
