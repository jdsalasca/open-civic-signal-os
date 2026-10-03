#!/usr/bin/env node

// Fails when a tracked file carries a committed secret or a known-insecure compose default.
// Wired into `agent:preflight` so the debt cannot come back silently.

import { execSync } from "node:child_process";
import { readFileSync } from "node:fs";

const RULES = [
  {
    id: "committed-jwt-secret",
    files: ["docker-compose.yml", "infra/docker-compose.yml", "infra/docker-compose.dev.yml", "infra/.env.example"],
    pattern: /JWT_SECRET:\s*["']?[A-Za-z0-9+/=_-]{16,}["']?\s*$/m,
    message: "JWT_SECRET is assigned a literal value. Use ${JWT_SECRET:?...} instead.",
  },
  {
    id: "committed-db-password",
    files: ["docker-compose.yml", "infra/docker-compose.yml", "infra/docker-compose.dev.yml", "infra/.env.example"],
    pattern: /POSTGRES_PASSWORD:\s*(?!\$\{)["']?[A-Za-z0-9]{6,}["']?/,
    message: "POSTGRES_PASSWORD is a literal. Use ${POSTGRES_PASSWORD:?...} instead.",
  },
  {
    id: "committed-smtp-password",
    files: [".env.example", "infra/.env.example"],
    pattern: /SMTP_PASSWORD:\s*(?!\$\{)["']?[A-Za-z0-9]{8,}["']?/,
    message: "SMTP_PASSWORD is a literal in a tracked example file.",
  },
  {
    id: "unpinned-mutable-image-tag",
    files: ["docker-compose.yml", "infra/docker-compose.yml", "infra/docker-compose.dev.yml"],
    // Postgres and the app images are pinned; an unpinned helper image is silent drift.
    pattern: /^\s*image:\s*axllent\/mailpit:latest\s*$/m,
    message: "axllent/mailpit:latest is unpinned. Use an explicit version tag.",
  },
];

const tracked = new Set(
  execSync("git ls-files", { encoding: "utf8" })
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean)
);

const problems = [];

for (const rule of RULES) {
  for (const file of rule.files) {
    if (!tracked.has(file)) continue;
    let contents;
    try {
      contents = readFileSync(file, "utf8");
    } catch {
      continue;
    }
    // An env-var reference is the safe form; only literals are findings.
    const offenders = contents
      .split("\n")
      .map((line, index) => ({ line: index + 1, text: line }))
      .filter(({ text }) => rule.pattern.test(text));

    for (const { line, text } of offenders) {
      problems.push(`${file}:${line} [${rule.id}] ${rule.message}\n    ${text.trim()}`);
    }
  }
}

// A tracked .env is always a finding regardless of content.
for (const file of tracked) {
  if (/^\.env$|(^|\/)\.env$/.test(file) && !file.endsWith(".example")) {
    problems.push(`${file} [tracked-dotenv] A real .env file is tracked. Only .env.example belongs in git.`);
  }
}

if (problems.length > 0) {
  console.error("Runtime secret policy check failed:\n");
  for (const problem of problems) {
    console.error(`- ${problem}`);
  }
  console.error("\nSee docs/architecture/ADR-20260321-runtime-secret-policy.md");
  process.exit(1);
}

console.log("Runtime secret policy check passed.");