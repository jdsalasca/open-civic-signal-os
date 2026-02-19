import fs from "node:fs";

const required = [
  "docs/agent/functional-requirements.md",
  "docs/agent/technical-requirements.md",
  "docs/agent/integration-test-matrix.md",
  "docs/agent/security-abuse-checklist.md"
];

const missing = required.filter((p) => !fs.existsSync(p));
if (missing.length > 0) {
  console.error("Missing required agent context files:");
  for (const m of missing) console.error(`- ${m}`);
  process.exit(1);
}

console.log("Agent context check passed.");
