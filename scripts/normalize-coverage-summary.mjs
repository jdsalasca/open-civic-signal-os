// Rewrites coverage/coverage-summary.json so file keys are relative to the repository root.
//
// c8/istanbul emit absolute paths in json-summary, which makes the committed summary depend on the
// machine that generated it and turns every refresh into a full-file diff. The release gate only
// reads `total`, but the per-file section stays useful for humans, so it is kept — just portable.

import { readFileSync, writeFileSync } from "node:fs";
import { relative, sep } from "node:path";

const summaryPath = "coverage/coverage-summary.json";
const summary = JSON.parse(readFileSync(summaryPath, "utf8"));
const root = process.cwd();

const normalized = { total: summary.total };
for (const [file, metrics] of Object.entries(summary)) {
  if (file === "total") {
    continue;
  }
  normalized[relative(root, file).split(sep).join("/")] = metrics;
}

writeFileSync(summaryPath, `${JSON.stringify(normalized, null, 2)}\n`, "utf8");
const files = Object.keys(normalized).length - 1;
console.log(`coverage/coverage-summary.json: ${files} file(s), paths relative to the repository root.`);
