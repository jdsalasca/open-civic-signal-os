import process from "node:process";

const prTitle = process.env.PR_TITLE ?? "";
const prBody = process.env.PR_BODY ?? "";
const minScore = Number(process.env.MIN_AGENT_SCORE ?? "8");

const checks = [
  {
    name: "Issue linkage",
    score: /#\d+/.test(prBody) ? 2 : 0,
    hint: "Add a linked issue reference like #123."
  },
  {
    name: "Story ID clarity",
    score: /story:/i.test(prBody) || /\b[A-Z]{2,}-P\d-\d{3}\b/.test(prBody) ? 2 : 0,
    hint: "Include Story ID in PR body."
  },
  {
    name: "Scope declaration",
    score: /## Scope/i.test(prBody) ? 2 : 0,
    hint: "Add explicit scope section (backend/frontend/contracts/docs)."
  },
  {
    name: "Verification evidence",
    score: /## Verification/i.test(prBody) && /(main script|build|test)/i.test(prBody) ? 2 : 0,
    hint: "Include verification commands and outcomes."
  },
  {
    name: "Risk and rollback",
    score: /## Risks?\s*\/\s*Notes/i.test(prBody) && /## Rollback Plan/i.test(prBody) ? 2 : 0,
    hint: "Add Risks/Notes and Rollback Plan sections."
  },
  {
    name: "Contract/docs alignment",
    score: /contracts|openapi|docs/i.test(prBody) ? 2 : 1,
    hint: "Mention contract/docs impact explicitly."
  }
];

const total = checks.reduce((acc, c) => acc + c.score, 0);
const lines = [];
lines.push(`# Agent Evaluation`);
lines.push("");
lines.push(`- PR title: ${prTitle || "(missing)"}`);
lines.push(`- Score: ${total}/12`);
lines.push(`- Threshold: ${minScore}`);
lines.push("");
lines.push("## Breakdown");
for (const c of checks) {
  lines.push(`- ${c.name}: ${c.score}/2`);
}
lines.push("");
lines.push("## Required Fixes");
const missing = checks.filter((c) => c.score < 2);
if (missing.length === 0) {
  lines.push("- None");
} else {
  for (const m of missing) {
    lines.push(`- ${m.hint}`);
  }
}

if (process.env.GITHUB_STEP_SUMMARY) {
  const fs = await import("node:fs/promises");
  await fs.appendFile(process.env.GITHUB_STEP_SUMMARY, lines.join("\n") + "\n", "utf8");
}

if (!prTitle.trim()) {
  console.error("PR title is missing.");
  process.exit(1);
}

if (total < minScore) {
  console.error(`Agent evaluation score ${total} is below threshold ${minScore}.`);
  process.exit(1);
}

console.log(`Agent evaluation passed with score ${total}/12.`);
