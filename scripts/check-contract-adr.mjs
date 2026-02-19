import { execSync } from "node:child_process";

function sh(cmd) {
  return execSync(cmd, { stdio: ["ignore", "pipe", "pipe"] }).toString("utf8").trim();
}

const baseRef = process.env.GITHUB_BASE_REF || "origin/main";

try {
  sh("git fetch --no-tags --prune origin +refs/heads/*:refs/remotes/origin/*");
} catch {}

let diffFiles = "";
try {
  diffFiles = sh(`git diff --name-only ${baseRef}...HEAD`);
} catch {
  diffFiles = sh("git diff --name-only HEAD~1...HEAD");
}

const files = diffFiles.split("\n").map((x) => x.trim()).filter(Boolean);
const contractChanged = files.includes("packages/contracts/openapi.yaml");

if (!contractChanged) {
  console.log("No contract change detected; ADR gate passed.");
  process.exit(0);
}

const adrTouched = files.some((f) => f.startsWith("docs/architecture/ADR") || f === "docs/architecture/decision-index.md");

if (!adrTouched) {
  console.error("Contract changed but no ADR update found.");
  console.error("Required: update ADR file in docs/architecture/ (ADR-*.md) or decision-index.md");
  process.exit(1);
}

console.log("Contract change with ADR update detected; gate passed.");
