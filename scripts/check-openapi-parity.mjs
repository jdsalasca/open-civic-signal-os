#!/usr/bin/env node

// Compares the Spring controller route surface against packages/contracts/openapi.yaml.
// Wired into `agent:preflight` so contract drift fails the gate instead of being noticed
// by whoever reads the docs next.

import { execSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { join } from "node:path";

const CONTROLLER_DIR = join("apps", "api-java", "src", "main", "java", "org", "opencivic", "signalos", "web");
const OPENAPI = join("packages", "contracts", "openapi.yaml");

/**
 * Endpoints the contract intentionally does not describe. Each one needs a reason, so
 * adding to this list is a deliberate act rather than a silent omission.
 */
const INTENTIONALLY_UNDOCUMENTED = new Map([
  ["/api/auth/refresh", "token rotation endpoint, not part of the civic contract"],
  ["/api/auth/login", "authentication handshake, not part of the civic contract"],
  ["/api/auth/register", "authentication handshake, not part of the civic contract"],
  ["/api/auth/verify", "authentication handshake, not part of the civic contract"],
  ["/api/auth/resend-code", "authentication handshake, not part of the civic contract"],
  ["/api/auth/logout", "authentication handshake, not part of the civic contract"],
  ["/api/auth/me", "authentication handshake, not part of the civic contract"],
  ["/api/auth/profile/me", "authentication handshake, not part of the civic contract"],
  ["/api/test/email", "local-only email probe, never exposed in a deployment"],
]);

function listControllers() {
  return execSync(`git ls-files "${CONTROLLER_DIR}"`, { encoding: "utf8" })
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.endsWith(".java"));
}

function extractRoutes(source) {
  const baseMatch = source.match(/@RequestMapping\(\s*"([^"]*)"\s*\)/);
  const base = baseMatch ? baseMatch[1] : "";

  const routes = [];
  const methodPattern =
    /@(Get|Post|Put|Patch|Delete)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"[^)]*\))?/g;

  let match;
  while ((match = methodPattern.exec(source)) !== null) {
    const verb = match[1].toUpperCase();
    const subPath = (match[2] || "").replace(/\/+$/, "");
    const full = `${base}${subPath}` || "/";
    routes.push({ verb, path: full, line: source.slice(0, match.index).split("\n").length });
  }
  return routes;
}

function normalizeSpecPath(specPath) {
  return specPath.replace(/\{[^}]+\}/g, "{}");
}

function normalizeRoutePath(routePath) {
  return routePath.replace(/\{[^}]+\}/g, "{}");
}

const openapiText = readFileSync(OPENAPI, "utf8");
const documented = new Map();
// Path lines under `paths:` are indented two spaces and end with a colon.
const pathPattern = /^ {2}(\/[^\s:]*):\s*$/gm;
let pathMatch;
while ((pathMatch = pathPattern.exec(openapiText)) !== null) {
  const specPath = pathMatch[1];
  // Collect the verbs documented under this path.
  const slice = openapiText.slice(pathMatch.index, pathMatch.index + 4000);
  const verbs = new Set();
  const verbPattern = /^ {4}(get|post|put|patch|delete):\s*$/gm;
  let verbMatch;
  while ((verbMatch = verbPattern.exec(slice)) !== null) {
    verbs.add(verbMatch[1].toUpperCase());
  }
  documented.set(normalizeSpecPath(specPath), verbs);
}

const undocumented = [];
const controllers = listControllers();

// Guard against a silent false pass: if route extraction ever breaks, an empty
// controller list would report "passed" while checking nothing.
if (controllers.length === 0) {
  console.error("OpenAPI parity check failed: no controllers were found. The path glob is wrong.");
  process.exit(1);
}

let routesChecked = 0;
for (const file of controllers) {
  const source = readFileSync(file, "utf8");
  for (const route of extractRoutes(source)) {
    routesChecked++;
    const key = normalizeRoutePath(route.path);
    if (INTENTIONALLY_UNDOCUMENTED.has(key)) continue;
    if (!documented.has(key)) {
      undocumented.push(`${file}:${route.line} ${route.verb} ${route.path} [no contract path]`);
      continue;
    }
    if (!documented.get(key).has(route.verb)) {
      undocumented.push(`${file}:${route.line} ${route.verb} ${route.path} [verb not documented]`);
    }
  }
}

if (routesChecked === 0) {
  console.error("OpenAPI parity check failed: no routes were extracted from the controllers.");
  process.exit(1);
}

if (undocumented.length > 0) {
  console.error(`OpenAPI parity check failed: ${undocumented.length} active route(s) not described in the contract.\n`);
  for (const entry of undocumented) {
    console.error(`- ${entry}`);
  }
  console.error(
    "\nEither document the route in packages/contracts/openapi.yaml (plus an ADR if it changes the\n" +
      "contract), or add it to INTENTIONALLY_UNDOCUMENTED in scripts/check-openapi-parity.mjs with a reason."
  );
  process.exit(1);
}

console.log(
  `OpenAPI parity check passed: ${routesChecked} routes across ${controllers.length} controller sources, ` +
    `${documented.size} documented paths, ${INTENTIONALLY_UNDOCUMENTED.size} intentionally undocumented.`
);