// Exercises the offline dataset shape checker without a live backend.
//
// src/prioritize.mjs deliberately no longer scores anything (the backend owns the formula); what it
// still promises is a fast, offline check that the golden example dataset is well-formed. These
// checks pin that promise: good data passes, and every documented class of problem is named rather
// than skipped.

import { execFileSync } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const script = path.join(root, 'src', 'prioritize.mjs');

const validSignal = {
  id: 'sig-100',
  title: 'Streetlight out',
  category: 'infrastructure',
  urgency: 5,
  impact: 4,
  affectedPeople: 300,
  communityVotes: 20,
};

const failures = [];

function check(name, condition, detail) {
  if (condition) {
    console.log(`  ok  ${name}`);
  } else {
    failures.push(name);
    console.error(`  FAIL ${name}${detail ? `: ${detail}` : ''}`);
  }
}

function run(argv) {
  try {
    const stdout = execFileSync(process.execPath, [script, ...argv], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      cwd: root,
      timeout: 30000,
    });
    return { code: 0, stdout };
  } catch (error) {
    return { code: error.status ?? 1, stdout: error.stdout ?? '', stderr: error.stderr ?? '' };
  }
}

const dir = mkdtempSync(path.join(tmpdir(), 'prioritize-test-'));

function datasetWith(...signals) {
  const file = path.join(dir, `dataset-${Math.random().toString(16).slice(2)}.json`);
  writeFileSync(file, JSON.stringify(signals));
  return file;
}

console.log('src/prioritize.mjs');

// 1. The committed golden dataset passes, and the script says so without inventing scores.
{
  const result = run([]);
  check('accepts the committed example dataset', result.code === 0, result.stderr);
  check('reports the signal count', /5 signals, 5 unique ids/.test(result.stdout), result.stdout.trim());
  check('points at the backend for real ranking', /npm run ranking:check/.test(result.stdout));
  check('does not print invented scores', !/priorityScore/.test(result.stdout));
}

// 2. A valid subset with repeated categories still passes.
{
  const result = run([datasetWith(validSignal, { ...validSignal, id: 'sig-101', urgency: 0 })]);
  check('accepts a valid dataset with zero urgency', result.code === 0, result.stderr);
  check('counts unique ids', /2 signals, 2 unique ids/.test(result.stdout), result.stdout.trim());
}

// 3. An empty dataset is valid, not an error.
{
  const result = run([datasetWith()]);
  check('accepts an empty dataset', result.code === 0, result.stderr);
  check('reports zero signals', /0 signals, 0 unique ids/.test(result.stdout), result.stdout.trim());
}

// 4. Each documented shape problem is named.
{
  const duplicate = run([datasetWith(validSignal, { ...validSignal, title: 'Other' })]);
  check('rejects duplicate ids', duplicate.code !== 0 && /repeats id sig-100/.test(duplicate.stderr), duplicate.stderr.slice(0, 160));

  const missingId = run([datasetWith({ ...validSignal, id: undefined })]);
  check('rejects a signal with no id', missingId.code !== 0 && /has no id/.test(missingId.stderr));

  const nonObject = run([datasetWith(validSignal, 42)]);
  check('rejects a non-object entry', nonObject.code !== 0 && /is not an object/.test(nonObject.stderr));

  const blankTitle = run([datasetWith({ ...validSignal, title: '   ' })]);
  check('rejects a blank title', blankTitle.code !== 0 && /has no title/.test(blankTitle.stderr));

  const missingCategory = run([datasetWith({ ...validSignal, category: undefined })]);
  check('rejects a missing category', missingCategory.code !== 0 && /has no category/.test(missingCategory.stderr));

  const badUrgency = run([datasetWith({ ...validSignal, urgency: 'high' })]);
  check('rejects a non-numeric urgency', badUrgency.code !== 0 && /non-numeric or negative urgency/.test(badUrgency.stderr));

  const negativeVotes = run([datasetWith({ ...validSignal, communityVotes: -1 })]);
  check('rejects negative vote counts', negativeVotes.code !== 0 && /non-numeric or negative communityVotes/.test(negativeVotes.stderr));

  const infiniteReach = run([datasetWith({ ...validSignal, affectedPeople: Number.POSITIVE_INFINITY })]);
  check('rejects a non-finite reach', infiniteReach.code !== 0 && /non-numeric or negative affectedPeople/.test(infiniteReach.stderr));
}

// 5. Unreadable or wrongly shaped files fail with an explanation, not a stack-only crash.
{
  const invalidJson = path.join(dir, 'broken.json');
  writeFileSync(invalidJson, '{not json');
  const invalidJsonRun = run([invalidJson]);
  check('rejects a file that is not valid JSON', invalidJsonRun.code !== 0 && /not valid JSON/.test(invalidJsonRun.stderr), invalidJsonRun.stderr.slice(0, 160));

  const objectFile = path.join(dir, 'object.json');
  writeFileSync(objectFile, JSON.stringify({ signals: [] }));
  const objectRun = run([objectFile]);
  check('rejects a JSON object instead of an array', objectRun.code !== 0 && /must be an array of civic signals/.test(objectRun.stderr));

  const absentRun = run([path.join(dir, 'absent.json')]);
  check('fails on a missing file', absentRun.code !== 0, `exit ${absentRun.code}`);
}

rmSync(dir, { recursive: true, force: true });

if (failures.length > 0) {
  console.error(`\n${failures.length} check(s) failed: ${failures.join(', ')}`);
  process.exit(1);
}
console.log('\nall checks passed');
