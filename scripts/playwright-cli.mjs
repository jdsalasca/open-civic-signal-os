#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { spawnSync } from "node:child_process";

const args = process.argv.slice(2);
const root = process.cwd();
const outputDir = path.join(root, "output", "playwright");
fs.mkdirSync(outputDir, { recursive: true });

function run(cmd, cmdArgs, options = {}) {
  const result = spawnSync(cmd, cmdArgs, {
    stdio: "inherit",
    shell: process.platform === "win32",
    ...options
  });
  return result.status ?? 1;
}

function hasCommand(command) {
  const testArgs = process.platform === "win32" ? ["/c", "where", command] : ["-lc", `command -v ${command} >/dev/null 2>&1`];
  const cmd = process.platform === "win32" ? "cmd" : "bash";
  return spawnSync(cmd, testArgs, { stdio: "ignore" }).status === 0;
}

if (!hasCommand("npx")) {
  console.error("npx is required for Playwright CLI usage.\n");
  console.error("# Verify Node/npm are installed");
  console.error("node --version");
  console.error("npm --version\n");
  console.error("# If missing, install Node.js/npm, then:");
  console.error("npm install -g @playwright/cli@latest");
  console.error("playwright-cli --help");
  process.exit(1);
}

if (args.length === 0) {
  console.log("Usage:");
  console.log("  npm run agent:ux:pw -- <playwright-cli-args>");
  console.log("");
  console.log("Examples:");
  console.log("  npm run agent:ux:pw -- open http://localhost:5173 --headed");
  console.log("  npm run agent:ux:pw -- snapshot");
  console.log("  npm run agent:ux:pw -- screenshot output/playwright/dashboard.png");
  process.exit(0);
}

const codexHome = process.env.CODEX_HOME ?? path.join(process.env.USERPROFILE ?? process.env.HOME ?? "", ".codex");
const wrapper = path.join(codexHome, "skills", "playwright", "scripts", "playwright_cli.sh");

if (fs.existsSync(wrapper) && hasCommand("bash")) {
  const status = run("bash", [wrapper, ...args], { env: { ...process.env, CODEX_HOME: codexHome } });
  process.exit(status);
}

const status = run("npx", ["--yes", "--package", "@playwright/cli", "playwright-cli", ...args], {
  env: { ...process.env, PLAYWRIGHT_OUTPUT_DIR: outputDir }
});
process.exit(status);
