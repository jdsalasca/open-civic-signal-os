// Exercises the multi-channel ingest adapter without a live backend.
//
// Ingest is where raw community input becomes stored signals, so its own guarantees are worth
// pinning: ids are deterministic per (source, payload) so re-runs never duplicate, field mapping
// follows the documented fallbacks, and malformed input fails fast instead of silently dropping
// records.

import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const script = path.join(root, 'src', 'ingest.mjs');

const failures = [];

function check(name, condition, detail) {
  if (condition) {
    console.log(`  ok  ${name}`);
  } else {
    failures.push(name);
    console.error(`  FAIL ${name}${detail ? `: ${detail}` : ''}`);
  }
}

function run(argv, cwd = root) {
  try {
    const stdout = execFileSync(process.execPath, [script, ...argv], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      cwd,
    });
    return { code: 0, stdout };
  } catch (error) {
    return { code: error.status ?? 1, stdout: error.stdout ?? '', stderr: error.stderr ?? '' };
  }
}

function readJson(file) {
  return JSON.parse(readFileSync(file, 'utf8'));
}

const dir = mkdtempSync(path.join(tmpdir(), 'ingest-test-'));

console.log('src/ingest.mjs');

// 1. JSON adapters map fields with the documented fallbacks and defaults.
{
  const input = path.join(dir, 'input.json');
  const target = path.join(dir, 'target-a.json');
  writeFileSync(input, JSON.stringify([
    { text: 'Broken bench in the plaza', urgency: '4', impact: 3, reach: 120, votes: '9' },
    { subject: 'Bus route missing', urgency: 2, impact: 2, affectedPeople: 50, votes: 1, category: 'transport', status: 'IN_REVIEW' },
    {},
  ]));

  const result = run([input, 'json', target]);
  check('accepts a json input', result.code === 0, result.stderr);

  const signals = readJson(target);
  check('wrote one signal per record', signals.length === 3, `got ${signals.length}`);

  const first = signals[0];
  check('takes the title from text when title is absent', first.title === 'Broken bench in the plaza');
  check('coerces string numbers', first.urgency === 4 && first.communityVotes === 9);
  check('falls back to reach for affectedPeople', first.affectedPeople === 120);
  check('defaults category and status', first.category === 'uncategorized' && first.status === 'NEW');
  check('labels the source in metadata', first.metadata.source === 'json');
  check('stamps the transform version', first.metadata.transformVersion === 'ingest-v2');
  check('records when the signal was ingested', typeof first.metadata.ingestedAt === 'string');

  const second = signals[1];
  check('takes the title from subject when title and text are absent', second.title === 'Bus route missing');
  check('keeps explicit category and status', second.category === 'transport' && second.status === 'IN_REVIEW');

  const third = signals[2];
  check('gives an empty record safe defaults', third.title === 'Untitled Signal'
    && third.urgency === 1 && third.impact === 1 && third.affectedPeople === 0 && third.communityVotes === 0);

  check('derives a deterministic id from source and payload', /^sig-json-[0-9a-f]{12}$/.test(first.id), first.id);
  check('stores the id as the fingerprint', first.metadata.fingerprint === first.id);
}

// 2. Re-running must not duplicate: the same input yields byte-identical output.
{
  const input = path.join(dir, 'input.json');
  const target = path.join(dir, 'target-b.json');
  const args = [input, 'json', target];

  run(args);
  const firstBytes = readFileSync(target, 'utf8');
  const rerun = run(args);
  const secondBytes = readFileSync(target, 'utf8');

  check('re-run succeeds', rerun.code === 0, rerun.stderr);
  check('re-run is idempotent', firstBytes === secondBytes);
  check('re-run reports the deduplication', /\(3 deduplicated\)/.test(rerun.stdout), rerun.stdout.trim());
}

// 3. CSV inputs are parsed column by column.
{
  const input = path.join(dir, 'input.csv');
  const target = path.join(dir, 'target-c.csv.json');
  writeFileSync(input, [
    'title,urgency,impact,affectedPeople,votes,category',
    'Pothole on 5th,4,3,80,12,roads',
    'Stray dogs near market,2,1,,3,',
  ].join('\n'));

  const result = run([input, 'csv', target]);
  check('accepts a csv input', result.code === 0, result.stderr);

  const signals = readJson(target);
  check('parses every data row', signals.length === 2, `got ${signals.length}`);
  check('maps csv columns', signals[0].title === 'Pothole on 5th' && signals[0].category === 'roads' && signals[0].urgency === 4);
  check('defaults empty numeric cells', signals[1].affectedPeople === 0);
  check('labels csv as the source', signals[1].metadata.source === 'csv');
}

// 4. Malformed input fails fast instead of being dropped.
{
  const badColumns = path.join(dir, 'bad-columns.csv');
  writeFileSync(badColumns, 'title,urgency\nOnly title,1,extra\n');
  const badColumnsRun = run([badColumns, 'csv', path.join(dir, 'out-1.json')]);
  check('rejects a csv row with the wrong column count', badColumnsRun.code !== 0, `exit ${badColumnsRun.code}`);
  check('names the expected column count', /expected 2 columns, got 3/.test(badColumnsRun.stderr), badColumnsRun.stderr.slice(0, 160));

  const headerOnly = path.join(dir, 'header-only.csv');
  writeFileSync(headerOnly, 'title,urgency\n');
  const headerOnlyRun = run([headerOnly, 'csv', path.join(dir, 'out-2.json')]);
  check('rejects a csv with no data rows', headerOnlyRun.code !== 0 && /at least one data row/.test(headerOnlyRun.stderr));

  const badNumber = path.join(dir, 'bad-number.csv');
  writeFileSync(badNumber, 'title,urgency\nSlow issue,soon\n');
  const badNumberRun = run([badNumber, 'csv', path.join(dir, 'out-3.json')]);
  check('rejects a non-numeric urgency', badNumberRun.code !== 0 && /Invalid numeric value for 'urgency'/.test(badNumberRun.stderr), badNumberRun.stderr.slice(0, 160));

  const missing = run([path.join(dir, 'does-not-exist.json')]);
  check('rejects a missing input file', missing.code === 1, `exit ${missing.code}`);
  check('explains the expected usage', /Usage: node src\/ingest\.mjs/.test(missing.stderr), missing.stderr.slice(0, 160));
}

// 5. A target file that is not an array is refused rather than overwritten.
{
  const input = path.join(dir, 'input.json');
  const target = path.join(dir, 'not-an-array.json');
  writeFileSync(target, JSON.stringify({ signals: [] }));
  const result = run([input, 'json', target]);
  check('refuses a non-array target', result.code !== 0, `exit ${result.code}`);
  check('says the target must contain an array', /must contain an array/.test(result.stderr), result.stderr.slice(0, 160));
}

// 6. Non-csv channels fall through to json parsing, including single objects and chat exports.
{
  const input = path.join(dir, 'chat.json');
  const target = path.join(dir, 'target-chat.json');
  writeFileSync(input, JSON.stringify({ text: 'Loud music at night', urgency: 3, impact: 2 }));
  const result = run([input, 'chat', target]);
  check('wraps a single json object into an array', result.code === 0 && readJson(target).length === 1, result.stderr);
  check('labels the chat channel', readJson(target)[0].metadata.source === 'chat');
}

rmSync(dir, { recursive: true, force: true });

if (failures.length > 0) {
  console.error(`\n${failures.length} check(s) failed: ${failures.join(', ')}`);
  process.exit(1);
}
console.log('\nall checks passed');
