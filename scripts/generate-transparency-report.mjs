#!/usr/bin/env node
// Renders a closed-month transparency report to Markdown and JSON for publication.
//
// AGENTS.md requires every release to publish a "what changed for communities" summary, and
// this project will not publish a number it cannot reproduce. Two properties follow:
//
//   Idempotent. Running twice over the same period produces byte-identical output. The report
//   is diffed and committed alongside the data change that caused it, so a drifting figure
//   shows up as a reviewable diff instead of a silent correction.
//
//   Fail fast. A malformed period, an unreachable API, or a report missing its comparison
//   column stops the script. A half-rendered report that looks fine is worse than none.
//
// Usage:
//   node scripts/generate-transparency-report.mjs --period 2026-03 \
//     --community-id <uuid> --api http://localhost:8080 --token <token> --out docs/reports
//
// Or, against a saved report fixture, which is what CI and the golden dataset use:
//   node scripts/generate-transparency-report.mjs --input .tmp/report.json --out docs/reports

import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import path from 'node:path';

const args = parseArgs(process.argv.slice(2));

/**
 * Only directionally-meaningful measures get an improved/worse reading.
 *
 * Resolution time and the still-open count are genuinely better when lower. Rising intake is
 * not better or worse: it may mean more problems or more participation, and a transparency
 * report has no business guessing which. Those metrics get the bare delta and no framing,
 * because a renderer that invents a verdict is the thing this project exists to avoid.
 *
 * Declared before use because this module runs top-level code on load, so a later `const`
 * would still be in its temporal dead zone when rendering starts.
 */
const LOWER_IS_BETTER = new Set(['MEDIAN_RESOLUTION_DAYS', 'SIGNALS_STILL_OPEN']);

if (args.help) {
  printUsage();
  process.exit(0);
}

const outDir = args.out ?? 'docs/reports';
const period = args.period ?? previousMonth();

await mkdir(outDir, { recursive: true });

const report = args.input ? await loadFromFile(args.input) : await fetchFromApi();

validateReport(report, args.input ?? 'api');

const stem = `transparency-report-${report.communityId}-${report.period.key}`;

const json = stableStringify(report) + '\n';
const markdown = renderMarkdown(report);

await writeIfChanged(path.join(outDir, `${stem}.json`), json);
await writeIfChanged(path.join(outDir, `${stem}.md`), markdown);

console.log(`Report for ${report.communityName} / ${report.period.key}`);
console.log(`  ${report.metrics.length} metrics, ${report.actioned.length} actioned, ${report.unaddressed.length} unaddressed`);
console.log(`  ${path.join(outDir, `${stem}.json`)}`);
console.log(`  ${path.join(outDir, `${stem}.md`)}`);

function parseArgs(argv) {
  const parsed = {};
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (token === '--help' || token === '-h') {
      parsed.help = true;
      continue;
    }
    if (!token.startsWith('--')) {
      fail(`Unexpected argument "${token}". Use --key value pairs; see --help.`);
    }
    const key = token.slice(2);
    const value = argv[i + 1];
    if (value === undefined || value.startsWith('--')) {
      fail(`Missing value for --${key}.`);
    }
    parsed[key] = value;
    i += 1;
  }
  return parsed;
}

function printUsage() {
  console.log(`Generate a monthly transparency report.

  --period <YYYY-MM>       Calendar month to report. Defaults to the previous month.
  --community-id <uuid>    Community to report on. Required when calling the API.
  --api <url>              API base URL. Defaults to $VITE_API_BASE_URL or localhost:8080.
  --token <token>          Open-data token, when the community requires one.
  --input <file.json>      Render a saved report instead of calling the API.
  --out <dir>              Output directory. Defaults to docs/reports.
  --help                   Show this message.`);
}

async function fetchFromApi() {
  if (!args['community-id']) {
    fail('--community-id is required when generating from the API.');
  }
  const base = (args.api ?? process.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');
  const url = new URL(`${base}/api/community/transparency-report`);
  url.searchParams.set('communityId', args['community-id']);
  if (args.period) {
    url.searchParams.set('period', args.period);
  }

  const headers = { accept: 'application/json' };
  if (args.token) {
    headers.authorization = `Bearer ${args.token}`;
  }

  let response;
  try {
    response = await fetch(url, { headers });
  } catch (error) {
    fail(`Could not reach the API at ${url}: ${error.message}`);
  }
  if (!response.ok) {
    fail(`API returned ${response.status} ${response.statusText} for ${url}`);
  }
  return response.json();
}

async function loadFromFile(file) {
  if (!existsSync(file)) {
    fail(`Input file not found: ${file}`);
  }
  try {
    return JSON.parse(await readFile(file, 'utf8'));
  } catch (error) {
    fail(`Input file is not valid JSON: ${error.message}`);
  }
}

/**
 * Fail fast rather than publishing a plausible-looking report. Every check here corresponds to
 * a way the output could misrepresent the community without looking broken.
 */
function validateReport(candidate, source) {
  const failWith = (message) => fail(`${source}: ${message}`);

  if (!candidate || typeof candidate !== 'object') {
    failWith('report must be a JSON object');
  }
  if (!candidate.communityId) failWith('missing communityId');
  if (!candidate.communityName) failWith('missing communityName');

  const period = candidate.period;
  if (!period || typeof period !== 'object') failWith('missing period');
  if (!/^\d{4}-\d{2}$/.test(period.key ?? '')) {
    failWith(`period.key must look like YYYY-MM, got ${JSON.stringify(period.key)}`);
  }
  if (!period.startDate || !period.endDate) failWith('period is missing its bounds');
  // A report without the previous month cannot answer "compared to when?", which is the only
  // reason a month-over-month figure is worth publishing.
  if (!period.previous || !period.previous.key) {
    failWith('period.previous is missing, so no month-over-month comparison can be rendered');
  }

  if (!Array.isArray(candidate.metrics) || candidate.metrics.length === 0) {
    failWith('metrics must be a non-empty array');
  }
  for (const metric of candidate.metrics) {
    if (!metric.key) failWith('a metric is missing its key');
    if (typeof metric.value !== 'number') {
      failWith(`metric ${metric.key} has a non-numeric value`);
    }
    if (metric.previousValue === undefined || metric.previousValue === null) {
      failWith(`metric ${metric.key} has no previousValue to compare against`);
    }
  }

  if (!Array.isArray(candidate.actioned)) failWith('actioned must be an array');
  if (!Array.isArray(candidate.unaddressed)) failWith('unaddressed must be an array');
  if (!candidate.formulaVersion) {
    failWith('missing formulaVersion, so the figures cannot be tied to a scoring formula');
  }
}

/**
 * Only directionally-meaningful measures get an improved/worse reading; see LOWER_IS_BETTER
 * above. The bare delta is the honest reading for anything ambiguous.
 */
function describeTrend(metric) {
  if (metric.delta === 0) return 'unchanged';
  const change = `${metric.delta > 0 ? '+' : ''}${metric.delta}`;
  if (!LOWER_IS_BETTER.has(metric.key)) {
    return change;
  }
  const improved = metric.delta < 0;
  return `${change} (${improved ? 'improved' : 'worse'})`;
}

function renderMarkdown(report) {
  const lines = [];
  const { period } = report;

  lines.push(`# ${report.communityName}: transparency report for ${period.key}`);
  lines.push('');
  lines.push(`Period: ${period.startDate} to ${period.endDate} (exclusive). Compared with ${period.previous.key}.`);
  lines.push(`Scoring formula: ${report.formulaVersion}. Generated by \`scripts/generate-transparency-report.mjs\`.`);
  lines.push('');
  lines.push('## What changed for communities this month');
  lines.push('');

  for (const line of report.narrative ?? []) {
    lines.push(`- ${line}`);
  }
  lines.push('');

  lines.push('## Figures');
  lines.push('');
  lines.push('| Measure | This month | Previous | Change |');
  lines.push('| --- | --- | --- | --- |');

  for (const metric of report.metrics) {
    lines.push(`| ${metric.label} | ${metric.value} ${metric.unit} | ${metric.previousValue} ${metric.unit} | ${describeTrend(metric)} |`);
  }
  lines.push('');

  if (report.actioned.length > 0) {
    lines.push(`## Actioned (${report.actioned.length})`);
    lines.push('');
    lines.push('| Report | Status | Days open |');
    lines.push('| --- | --- | --- |');
    for (const item of report.actioned) {
      lines.push(`| ${item.title} | ${item.status} | ${item.daysOpen} |`);
    }
    lines.push('');
  }

  // The unaddressed list is the part a resident uses to judge whether the institution is
  // keeping up. Omitting it would make this a marketing document rather than an audit.
  lines.push(`## Still unaddressed (${report.unaddressed.length})`);
  lines.push('');
  if (report.unaddressed.length === 0) {
    lines.push('Every report raised in this period was resolved or rejected with a reason.');
  } else {
    lines.push('| Report | Status | Priority | Days open |');
    lines.push('| --- | --- | --- | --- |');
    for (const item of report.unaddressed) {
      lines.push(`| ${item.title} | ${item.status} | ${item.priorityScore} | ${item.daysOpen} |`);
    }
  }
  lines.push('');

  lines.push('## Reproducibility');
  lines.push('');
  lines.push('Every figure above is derived from the closed period above. Re-running the generator');
  lines.push('for the same period produces byte-identical output, so any difference you see is a data');
  lines.push('change rather than a change in how the report is computed.');
  lines.push('');

  return lines.join('\n');
}

/** Stable key order so an unchanged report is a byte-identical file and diffs cleanly. */
function stableStringify(value) {
  return `${JSON.stringify(sortKeys(value), null, 2)}\n`;
}

function sortKeys(value) {
  if (Array.isArray(value)) {
    return value.map(sortKeys);
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.keys(value).sort().map((key) => [key, sortKeys(value[key])]),
    );
  }
  return value;
}

async function writeIfChanged(file, contents) {
  if (existsSync(file)) {
    const current = await readFile(file, 'utf8');
    if (current === contents) {
      console.log(`  unchanged: ${file}`);
      return;
    }
  }
  await writeFile(file, contents, 'utf8');
  console.log(`  wrote:     ${file}`);
}

function previousMonth() {
  const now = new Date();
  const year = now.getUTCFullYear();
  const month = now.getUTCMonth();
  const previous = new Date(Date.UTC(year, month - 1, 1));
  return previous.toISOString().slice(0, 7);
}

function fail(message) {
  console.error(`transparency-report: ${message}`);
  process.exit(1);
}