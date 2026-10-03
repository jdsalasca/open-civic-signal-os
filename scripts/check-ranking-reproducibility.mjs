#!/usr/bin/env node
// Checks that the backend's ranking is reproducible, and records what it was.
//
// The point of this script is NOT to rank signals in Node. An earlier version of this repo had
// src/prioritize.mjs reimplementing the scoring formula in JavaScript, which meant the
// "reproducibility" check reproduced a hand-copied second implementation. AGENTS.md is explicit
// that the backend owns domain calculations, and a copy cannot detect that it has drifted: it
// happily keeps agreeing with an older formula long after the backend moved.
//
// Reproducibility here means one thing: for the same inputs and the same formula version, the
// backend returns byte-identical ordering. The backend is the single source of truth; this
// script observes it twice and compares.
//
// Usage:
//   node scripts/check-ranking-reproducibility.mjs --api http://localhost:8080 [--out docs/reproducibility]
//   node scripts/check-ranking-reproducibility.mjs --input capture.json   # re-verify a saved capture
//
// Exit codes: 0 reproducible, 1 not reproducible, 2 could not check (bad input, API unreachable).

import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { createHash } from 'node:crypto';

const args = parseArgs(process.argv.slice(2));

if (args.help) {
  printUsage();
  process.exit(0);
}

const outDir = args.out ?? 'docs/reproducibility';

if (args.input) {
  await verifySavedCapture(args.input);
} else {
  await checkLiveApi();
}

function parseArgs(argv) {
  const parsed = {};
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (token === '--help' || token === '-h') {
      parsed.help = true;
      continue;
    }
    if (!token.startsWith('--')) {
      fail(`Unexpected argument "${token}". Use --key value pairs; see --help.`, 2);
    }
    const key = token.slice(2);
    const value = argv[i + 1];
    if (value === undefined || value.startsWith('--')) {
      fail(`Missing value for --${key}.`, 2);
    }
    parsed[key] = value;
    i += 1;
  }
  return parsed;
}

function printUsage() {
  console.log(`Check that the backend ranking is reproducible.

  --api <url>       API base URL. Defaults to $API_BASE_URL or localhost:8080.
  --community-id <uuid>  Restrict the ranking to one community when it matters.
  --status <list>   Status filter, comma separated, e.g. NEW or NEW,IN_PROGRESS.
  --size <n>        How many ranked signals to compare. Defaults to 20.
  --out <dir>       Where to write the capture. Defaults to docs/reproducibility.
  --input <file>    Re-verify a previously written capture instead of calling the API.
  --help            Show this message.

Exit 0 means the two calls agreed byte for byte. Exit 1 means the ranking moved. Exit 2 means
the check could not run at all, which is deliberately not the same as passing.`);
}

async function checkLiveApi() {
  const base = (args.api ?? process.env.API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');
  const size = args.size === undefined ? 20 : Number.parseInt(args.size, 10);
  if (!Number.isInteger(size) || size < 1) {
    fail(`--size must be a positive integer, got ${args.size}`, 2);
  }

  const formula = await fetchJson(`${base}/api/signals/formula`, 'the published scoring formula');
  if (!formula || typeof formula.version !== 'string' || formula.version.length === 0) {
    fail('The API returned a formula without a version, so a reproducibility claim would be unanchored.', 2);
  }

  const first = await fetchRanking(base, size, 'first');
  const second = await fetchRanking(base, size, 'second');

  const firstHash = hashOrder(first);
  const secondHash = hashOrder(second);

  const capture = {
    formulaVersion: formula.version,
    formula: formula.formula,
    effectiveFrom: formula.effectiveFrom,
    cappedFactors: formula.cappedFactors,
    filters: { communityId: args['community-id'] ?? null, status: args.status ?? null, size },
    entryCount: first.length,
    orderingHash: firstHash,
    entries: first,
  };

  await mkdir(outDir, { recursive: true });
  const file = path.join(outDir, `ranking-${formula.version}.json`);
  const serialised = `${JSON.stringify(capture, null, 2)}\n`;
  if (existsSync(file)) {
    const current = await readFile(file, 'utf8');
    if (current !== serialised) {
      console.log(`  ranking changed since the last capture: ${file}`);
    } else {
      console.log(`  unchanged: ${file}`);
    }
  } else {
    console.log(`  wrote:     ${file}`);
  }
  await writeFile(file, serialised, 'utf8');

  console.log(`Formula version: ${formula.version}`);
  console.log(`Compared ${first.length} ranked entries across two calls.`);

  if (firstHash !== secondHash) {
    const at = first.findIndex((entry, index) => entry.id !== second[index]?.id);
    console.error('');
    console.error('Ranking is NOT reproducible: two identical calls returned different orders.');
    console.error(`  ordering hash: ${firstHash}`);
    console.error(`             vs ${secondHash}`);
    if (at >= 0) {
      console.error(`  first difference at position ${at + 1}:`);
      console.error(`    call 1: ${describe(first[at])}`);
      console.error(`    call 2: ${describe(second[at])}`);
    }
    console.error('');
    console.error('Until this is understood, do not cite a rank position from this endpoint.');
    process.exit(1);
  }

  console.log(`  ordering hash: ${firstHash}`);
  console.log('Ranking is reproducible across two calls.');
}

async function fetchRanking(base, size, label) {
  const url = new URL(`${base}/api/signals/prioritized`);
  url.searchParams.set('size', String(size));
  if (args['community-id']) {
    url.searchParams.set('communityId', args['community-id']);
  }
  if (args.status) {
    url.searchParams.set('status', args.status);
  }
  const page = await fetchJson(url, `the ranked backlog (${label} call)`);
  const content = page?.content;
  if (!Array.isArray(content)) {
    fail(`The ranked backlog did not return a content array (${label} call).`, 2);
  }
  if (content.length === 0) {
    fail(
      'The ranked backlog returned zero entries. An empty list is trivially reproducible and '
        + 'would prove nothing, so this is treated as a failed check rather than a pass.',
      2,
    );
  }
  return content.map((entry) => ({
    id: entry.id,
    priorityScore: entry.priorityScore,
    status: entry.status,
    category: entry.category,
    breakdown: entry.scoreBreakdown ?? null,
  }));
}

async function fetchJson(url, what) {
  let response;
  try {
    response = await fetch(url, { headers: { accept: 'application/json' } });
  } catch (error) {
    fail(`Could not reach ${what} at ${url}: ${error.message}`, 2);
  }
  if (!response.ok) {
    fail(`Could not read ${what}: ${response.status} ${response.statusText} at ${url}`, 2);
  }
  try {
    return await response.json();
  } catch (error) {
    fail(`${what} did not return JSON: ${error.message}`, 2);
  }
}

async function verifySavedCapture(file) {
  if (!existsSync(file)) {
    fail(`Capture not found: ${file}`, 2);
  }
  let capture;
  try {
    capture = JSON.parse(await readFile(file, 'utf8'));
  } catch (error) {
    fail(`Capture is not valid JSON: ${error.message}`, 2);
  }
  if (!capture.orderingHash || !Array.isArray(capture.entries)) {
    fail('Capture is missing an orderingHash or entries, so it cannot be verified.', 2);
  }
  const recomputed = hashOrder(capture.entries);
  if (recomputed !== capture.orderingHash) {
    console.error(`Capture ${file} does NOT match its own recorded hash.`);
    console.error(`  recorded:   ${capture.orderingHash}`);
    console.error(`  recomputed: ${recomputed}`);
    console.error('The file was edited after it was written.');
    process.exit(1);
  }
  console.log(`Capture ${file} matches its recorded hash (${capture.orderingHash}).`);
  console.log(`  formula version ${capture.formulaVersion}, ${capture.entries.length} entries.`);
}

/**
 * Hash over the ordering only.
 *
 * Score and id, deliberately: those are what a rank position means. Title and category are
 * excluded because editing a title should not make a ranking look unreproducible, and the
 * question here is whether the order held, not whether the prose was touched.
 */
function hashOrder(entries) {
  const canonical = entries
    .map((entry) => `${entry.id}:${entry.priorityScore}`)
    .join('\n');
  return createHash('sha256').update(canonical, 'utf8').digest('hex');
}

function describe(entry) {
  if (!entry) {
    return '(missing)';
  }
  return `id=${entry.id} score=${entry.priorityScore}`;
}

function fail(message, code = 1) {
  console.error(`ranking-reproducibility: ${message}`);
  process.exit(code);
}
