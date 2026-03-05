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
    shell: false,
    ...options
  });
  return result.status ?? 1;
}

function isLikelyScreenshotPath(value) {
  if (!value) return false;
  const normalized = value.replace(/\\/g, "/").toLowerCase();
  return normalized.endsWith(".png") || normalized.endsWith(".jpg") || normalized.endsWith(".jpeg");
}

function resolveOutputPath(targetPath) {
  if (path.isAbsolute(targetPath)) {
    return targetPath;
  }
  return path.resolve(root, targetPath);
}

function findLatestGeneratedScreenshot(dirPath) {
  if (!fs.existsSync(dirPath)) return null;
  const files = fs
    .readdirSync(dirPath)
    .filter((file) => /^page-.*\.(png|jpg|jpeg)$/i.test(file))
    .map((file) => {
      const fullPath = path.join(dirPath, file);
      const stat = fs.statSync(fullPath);
      return { fullPath, mtimeMs: stat.mtimeMs };
    })
    .sort((a, b) => b.mtimeMs - a.mtimeMs);
  return files[0]?.fullPath ?? null;
}

function buildNpxArgs(playwrightArgs) {
  return ["--yes", "--package", "@playwright/cli", "playwright-cli", ...playwrightArgs];
}

function runNpx(playwrightArgs) {
  const npxArgs = buildNpxArgs(playwrightArgs);
  return isWindows
    ? run("cmd", ["/c", "npx", ...npxArgs], {
        env: { ...process.env, PLAYWRIGHT_OUTPUT_DIR: outputDir }
      })
    : run("npx", npxArgs, {
        env: { ...process.env, PLAYWRIGHT_OUTPUT_DIR: outputDir }
      });
}

function hasCommand(command) {
  const testArgs = process.platform === "win32" ? ["/c", "where", command] : ["-lc", `command -v ${command} >/dev/null 2>&1`];
  const cmd = process.platform === "win32" ? "cmd" : "bash";
  return spawnSync(cmd, testArgs, { stdio: "ignore" }).status === 0;
}

const isWindows = process.platform === "win32";

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

if (!isWindows && fs.existsSync(wrapper) && hasCommand("bash")) {
  const status = run("bash", [wrapper, ...args], { env: { ...process.env, CODEX_HOME: codexHome } });
  if (status === 0) {
    process.exit(0);
  }
  console.warn("Playwright wrapper failed; falling back to npx @playwright/cli.");
}

let effectiveArgs = args;
let screenshotTargetPath = null;
if (args[0] === "screenshot" && isLikelyScreenshotPath(args[1])) {
  const target = resolveOutputPath(args[1]);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  screenshotTargetPath = target;
  effectiveArgs = ["screenshot"];
}

const generatedDir = path.join(root, ".playwright-cli");
const previousGeneratedScreenshot = screenshotTargetPath ? findLatestGeneratedScreenshot(generatedDir) : null;
const status = runNpx(effectiveArgs);
if (status !== 0) {
  console.error("Playwright CLI invocation failed.");
  process.exit(status);
}

if (screenshotTargetPath) {
  const latestGeneratedScreenshot = findLatestGeneratedScreenshot(generatedDir);
  const shouldCopy = latestGeneratedScreenshot && latestGeneratedScreenshot !== previousGeneratedScreenshot;
  if (!shouldCopy) {
    console.error("Could not locate generated screenshot to copy into requested path.");
    process.exit(1);
  }
  fs.copyFileSync(latestGeneratedScreenshot, screenshotTargetPath);
  console.log(`Saved screenshot to ${screenshotTargetPath}`);
}
process.exit(status);
