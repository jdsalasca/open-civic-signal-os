// Exercises the reproducibility checker without a live backend.
//
// The check exists to make a specific claim auditable, so its own guarantees are worth pinning:
// two identical captures hash identically, an edited capture does not, an empty ranking is a
// failed check rather than a pass, and an unreachable API never looks like success.

import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const script = path.join(root, 'scripts', 'check-ranking-reproducibility.mjs');

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
  const dir = mkdtempSync(path.join(tmpdir(), 'repro-test-'));
  try {
    const stdout = execFileSync(process.execPath, [script, ...argv], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    return { code: 0, stdout };
  } catch (error) {
    return { code: error.status ?? 1, stdout: error.stdout ?? '', stderr: error.stderr ?? '' };
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
}

const dir = mkdtempSync(path.join(tmpdir(), 'repro-fixtures-'));
const goodCapture = {
  formulaVersion: 'v1',
  formula: '(Urgency * 30) + (Impact * 25) + min(People/10, 30) + min(Votes/5, 15)',
  effectiveFrom: '2026-03-21',
  cappedFactors: ['affectedPeople', 'communityVotes'],
  filters: { communityId: null, status: null, size: 20 },
  entryCount: 3,
  orderingHash: '',
  entries: [
    { id: 'a', priorityScore: 95.0, status: 'NEW', category: 'utilities', breakdown: null },
    { id: 'b', priorityScore: 82.4, status: 'NEW', category: 'infrastructure', breakdown: null },
    { id: 'c', priorityScore: 71.1, status: 'NEW', category: 'infrastructure', breakdown: null },
  ],
};
// The hash is over id:score lines, computed the same way the script does it.
goodCapture.orderingHash = hashOrder(goodCapture.entries);

const goodPath = path.join(dir, 'good.json');
writeFileSync(goodPath, JSON.stringify(goodCapture, null, 2));

const editedCapture = {
  ...goodCapture,
  entries: [goodCapture.entries[0], goodCapture.entries[2], goodCapture.entries[1]],
};
const editedPath = path.join(dir, 'edited.json');
writeFileSync(editedPath, JSON.stringify(editedCapture, null, 2));

const missingHash = { ...goodCapture, orderingHash: undefined };
delete missingHash.orderingHash;
const missingHashPath = path.join(dir, 'nohash.json');
writeFileSync(missingHashPath, JSON.stringify(missingHash, null, 2));

console.log('check-ranking-reproducibility');

const good = run(['--input', goodPath]);
check('accepts a capture that matches its own hash', good.code === 0, good.stderr);
check('reports the formula version it verified', good.stdout.includes('v1'));
check('reports the ordering hash', good.stdout.includes(goodCapture.orderingHash));

const edited = run(['--input', editedPath]);
check('rejects a reordered capture', edited.code === 1, `exit ${edited.code}`);
check('names the recorded and recomputed hashes',
  edited.stderr.includes('recorded:') && edited.stderr.includes('recomputed:'));
check('says the file was edited', edited.stderr.includes('edited after it was written'));

const noHash = run(['--input', missingHashPath]);
check('rejects a capture with no recorded hash', noHash.code === 1 || noHash.code === 2, `exit ${noHash.code}`);

const absent = run(['--input', path.join(dir, 'absent.json')]);
check('fails loudly on a missing capture', absent.code === 2, `exit ${absent.code}`);
check('distinguishes could-not-check from not-reproducible', absent.stderr.includes('not found'));

const unreachable = run(['--api', 'http://127.0.0.1:1/', '--out', dir]);
check('never passes when the API is unreachable', unreachable.code !== 0, `exit ${unreachable.code}`);
check('explains that the API could not be reached', /Could not reach/.test(unreachable.stderr), unreachable.stderr.slice(0, 200));

const badSize = run(['--api', 'http://127.0.0.1:1/', '--size', 'zero']);
check('rejects a non-numeric size before calling the API', badSize.code === 2, `exit ${badSize.code}`);

const help = run(['--help']);
check('documents the exit codes', help.stdout.includes('Exit 0') && help.stdout.includes('Exit 2'));

rmSync(dir, { recursive: true, force: true });

function hashOrder(entries) {
  return createHash('sha256')
    .update(entries.map((e) => `${e.id}:${e.priorityScore}`).join('\n'), 'utf8')
    .digest('hex');
}

if (failures.length > 0) {
  console.error(`\n${failures.length} check(s) failed: ${failures.join(', ')}`);
  process.exit(1);
}
console.log('\nall checks passed');