import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

const requiredFiles = [
  "AGENTS.md",
  "GEMINI_INSTRUCTIONS.md",
  ".github/pull_request_template.md",
  "docs/DEFINITION_OF_DONE.md",
  "docs/IMPLEMENTATION_PLAYBOOK.md",
  "docs/PR_REVIEW_CHECKLIST.md",
  "docs/STACK.md",
  "docs/MONOREPO.md",
  "docs/ENGINEERING_STANDARDS.md",
  "docs/BACKEND_EXCELLENCE.md",
  "docs/UX_EXCELLENCE.md",
  "docs/TEST_STRATEGY.md",
  "docs/SECURITY_BASELINE.md",
  "docs/OBSERVABILITY.md",
  "docs/kanban.md",
  "packages/contracts/openapi.yaml"
];

const forbiddenPaths = [
  "apps/web-react/dist"
];

const errors = [];

for (const path of requiredFiles) {
  if (!existsSync(join(process.cwd(), path))) {
    errors.push(`Missing required file: ${path}`);
  }
}

for (const path of forbiddenPaths) {
  if (existsSync(join(process.cwd(), path))) {
    errors.push(`Forbidden committed path exists: ${path}`);
  }
}

const agents = readFileSync(join(process.cwd(), "AGENTS.md"), "utf8");
if (!agents.includes("Agent Operations")) {
  errors.push("AGENTS.md must include 'Agent Operations' section.");
}

const prTemplate = readFileSync(join(process.cwd(), ".github/pull_request_template.md"), "utf8");
if (!prTemplate.includes("## Verification")) {
  errors.push("PR template must include verification section.");
}

if (errors.length > 0) {
  console.error("Agent gate failed:\n");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

console.log("Agent gate passed.");
