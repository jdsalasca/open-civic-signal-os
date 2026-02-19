import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function isSemver(value) {
  return /^\d+\.\d+\.\d+$/.test(value);
}

function assert(condition, message, errors) {
  if (!condition) {
    errors.push(message);
  }
}

function gatherPackageFiles(rootPackage) {
  const files = ["package.json"];
  const workspaces = rootPackage.workspaces ?? [];
  for (const ws of workspaces) {
    if (ws.endsWith("/*")) {
      const prefix = ws.slice(0, -2);
      const known = [
        `${prefix}/contracts/package.json`,
        `${prefix}/web-react/package.json`
      ];
      for (const candidate of known) {
        if (existsSync(candidate)) {
          files.push(candidate);
        }
      }
    } else {
      const candidate = `${ws}/package.json`;
      if (existsSync(candidate)) {
        files.push(candidate);
      }
    }
  }
  return [...new Set(files)];
}

function checkSemver(errors) {
  const rootPackage = readJson("package.json");
  const packageFiles = gatherPackageFiles(rootPackage);

  for (const file of packageFiles) {
    const pkg = readJson(file);
    assert(isSemver(pkg.version), `Invalid semver in ${file}: ${pkg.version}`, errors);
  }
}

function checkChangelog(errors) {
  const changelogPath = "docs/CHANGELOG.md";
  assert(existsSync(changelogPath), "Missing docs/CHANGELOG.md", errors);
  if (!existsSync(changelogPath)) {
    return;
  }

  const content = readFileSync(changelogPath, "utf8");
  assert(content.includes("## Unreleased"), "CHANGELOG must contain '## Unreleased' section", errors);

  const unreleasedSection = content.split("## Unreleased")[1] ?? "";
  const hasBullet = /^\s*-\s+/m.test(unreleasedSection);
  assert(hasBullet, "Unreleased section must include at least one bullet item", errors);
}

function checkCoverage(errors) {
  const rootPackage = readJson("package.json");
  const minCoverage = Number(rootPackage.releaseQuality?.minimumCoverage ?? 0);
  const summaryPath = join("coverage", "coverage-summary.json");

  assert(existsSync(summaryPath), "Missing coverage/coverage-summary.json (run coverage and commit summary)", errors);
  if (!existsSync(summaryPath)) {
    return;
  }

  const summary = readJson(summaryPath);
  const total = summary.total ?? {};
  const keys = ["lines", "statements", "functions", "branches"];

  for (const key of keys) {
    const pct = Number(total[key]?.pct ?? NaN);
    assert(Number.isFinite(pct), `Coverage metric missing: ${key}.pct`, errors);
    assert(pct >= minCoverage, `Coverage ${key}.pct=${pct} below minimum ${minCoverage}`, errors);
  }
}

function main() {
  const errors = [];
  checkSemver(errors);
  checkChangelog(errors);
  checkCoverage(errors);

  if (errors.length > 0) {
    console.error("Release quality gate failed:\n");
    for (const error of errors) {
      console.error(`- ${error}`);
    }
    process.exit(1);
  }

  console.log("Release quality gate passed.");
}

main();
