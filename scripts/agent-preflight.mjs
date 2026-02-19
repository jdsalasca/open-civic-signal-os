#!/usr/bin/env node

import { execSync } from "node:child_process";
import process from "node:process";

function run(command, options = {}) {
  console.log(`\n> ${command}`);
  execSync(command, { stdio: "inherit", shell: true, ...options });
}

function runWithOptionalJava(command, options = {}) {
  const strict = process.env.AGENT_PREFLIGHT_STRICT === "1";
  if (!strict) {
    try {
      execSync("mvn -v", { stdio: "ignore", shell: true });
    } catch {
      console.log("\nJava check skipped: Maven/Java environment is not ready in this shell.");
      return { ok: true, skipped: true };
    }
  }

  try {
    run(command, options);
    return { ok: true, skipped: false };
  } catch (error) {
    const message = String(error?.message ?? "");
    const envIssue =
      message.includes("JAVA_HOME") ||
      message.includes("'mvn' is not recognized") ||
      message.includes("not found");

    if (!strict && envIssue) {
      console.log("\nJava check skipped due to local environment blocker (set JAVA_HOME or install Maven).");
      return { ok: true, skipped: true };
    }
    throw error;
  }
}

function listChangedFiles() {
  const tracked = execSync("git diff --name-only HEAD", { encoding: "utf8", shell: true })
    .split("\n")
    .map((x) => x.trim())
    .filter(Boolean);

  const untracked = execSync("git ls-files --others --exclude-standard", { encoding: "utf8", shell: true })
    .split("\n")
    .map((x) => x.trim())
    .filter(Boolean);

  return [...new Set([...tracked, ...untracked])];
}

function hasPrefix(files, prefix) {
  return files.some((f) => f.startsWith(prefix));
}

function hasOneOf(files, prefixes) {
  return prefixes.some((prefix) => hasPrefix(files, prefix));
}

const files = listChangedFiles();
console.log("Detected changed files:", files.length);

run("npm run agent:context:check");
run("npm run backlog:current:check");

const frontendChanged = hasPrefix(files, "apps/web-react/");
const backendChanged = hasPrefix(files, "apps/api-java/");
const docsChanged = hasPrefix(files, "docs/");
const contractChanged = hasOneOf(files, ["packages/contracts/", "docs/architecture/"]);

if (frontendChanged) {
  run("npm run build:web");
  console.log("\nPlaywright evidence reminder:");
  console.log("Run: npm run agent:ux:pw -- open http://localhost:5173 --headed");
  console.log("Run: npm run agent:ux:pw -- snapshot");
  console.log("Run: npm run agent:ux:pw -- screenshot output/playwright/<name>.png");
}

if (backendChanged) {
  runWithOptionalJava("mvn -q test", { cwd: "apps/api-java" });
}

if (contractChanged) {
  run("npm run agent:adr:check");
}

if (docsChanged) {
  console.log("\nDocs changed: ensure links and examples remain valid.");
}

console.log("\nAgent preflight passed.");
process.exit(0);
