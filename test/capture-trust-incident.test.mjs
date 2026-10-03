// Exercises the incident capture without a live API.
//
// The protocol says capture before fixing. A capture script that silently half-succeeds would
// undermine the one thing it exists for, so the properties under test are about honesty: an
// unreachable probe is recorded as failed rather than omitted, and a bundle is never written for an
// unlabelled incident.

import { execFileSync, spawn } from 'node:child_process';
import { mkdtempSync, readFileSync, readdirSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const script = path.join(root, 'scripts', 'capture-trust-incident.mjs');

const failures = [];

function check(name, condition, detail) {
  if (condition) {
    console.log(`  ok  ${name}`);
  } else {
    failures.push(name);
    console.error(`  FAIL ${name}${detail ? `: ${detail}` : ''}`);
  }
}

function run(argv, outDir) {
  try {
    const stdout = execFileSync(process.execPath, [script, ...argv, '--out', outDir], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      timeout: 60000,
    });
    return { code: 0, stdout };
  } catch (error) {
    return { code: error.status ?? 1, stdout: error.stdout ?? '', stderr: error.stderr ?? '' };
  }
}

function bundleFiles(dir) {
  try {
    return readdirSync(dir).filter((f) => f.endsWith('.json'));
  } catch {
    return [];
  }
}

/**
 * Async variant.
 *
 * execFileSync blocks the event loop, so a stub server in this same process can never answer the
 * child's requests: the happy path would time out and look like a failure of the script.
 */
function runAsync(argv) {
  return new Promise((resolve) => {
    const child = spawn(process.execPath, [script, ...argv], { stdio: ['ignore', 'pipe', 'pipe'] });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.stderr.on('data', (chunk) => { stderr += chunk; });
    child.on('close', (code) => resolve({ code: code ?? 1, stdout, stderr }));
  });
}

console.log('capture-trust-incident');

// 1. Unlabelled must not produce a bundle.
{
  const dir = mkdtempSync(path.join(tmpdir(), 'incident-a-'));
  const result = run(['--api', 'http://127.0.0.1:1/'], dir);
  check('refuses to write an unlabelled bundle', result.code === 1, `exit ${result.code}`);
  check('says why the label is required', /--label is required/.test(result.stderr), result.stderr.slice(0, 160));
  check('wrote nothing when it refused', bundleFiles(dir).length === 0);
  rmSync(dir, { recursive: true, force: true });
}

// 2. An unreachable API still writes a bundle, and records the failure rather than hiding it.
{
  const dir = mkdtempSync(path.join(tmpdir(), 'incident-b-'));
  const result = run(['--label', 'Backlog showed stale ranks', '--api', 'http://127.0.0.1:1/'], dir);
  check('writes a bundle even when the API is unreachable', result.code === 0, `exit ${result.code} ${result.stderr}`);
  const files = bundleFiles(dir);
  check('produced exactly one file', files.length === 1, `got ${files.length}`);

  if (files.length === 1) {
    const bundle = JSON.parse(readFileSync(path.join(dir, files[0]), 'utf8'));
    check('records the label', bundle.label === 'Backlog showed stale ranks');
    check('records a capture timestamp', typeof bundle.capturedAt === 'string');
    check('has a schema version', bundle.schemaVersion === 'v1');
    const probes = Object.values(bundle.probes ?? {});
    check('attempted every probe', probes.length >= 4, `got ${probes.length}`);
    check(
      'marks unreachable probes as failed rather than omitting them',
      probes.every((probe) => probe.status === 'failed'),
      JSON.stringify(probes.map((p) => p.status)),
    );
    check(
      'explains what an incomplete bundle means',
      probes.every((probe) => typeof probe.note === 'string' && probe.note.length > 0),
    );
    check(
      'tells the operator the bundle is incomplete',
      /incomplete/.test(result.stdout),
      result.stdout,
    );
  }
  rmSync(dir, { recursive: true, force: true });
}

// 3. A missing --label value is rejected rather than silently becoming "undefined".
{
  const dir = mkdtempSync(path.join(tmpdir(), 'incident-c-'));
  const result = run(['--label'], dir);
  check('rejects a flag with no value', result.code === 1, `exit ${result.code}`);
  rmSync(dir, { recursive: true, force: true });
}

// 4. The slug is derived from the label, so the file is findable by description.
{
  const dir = mkdtempSync(path.join(tmpdir(), 'incident-d-'));
  run(['--label', 'PII appeared in the open data export', '--api', 'http://127.0.0.1:1/'], dir);
  const files = bundleFiles(dir);
  check(
    'names the file after the label',
    files.length === 1 && files[0].includes('pii-appeared-in-the-open-data-export'),
    files.join(','),
  );
  rmSync(dir, { recursive: true, force: true });
}

// 5. --help documents the exit codes.
{
  const dir = mkdtempSync(path.join(tmpdir(), 'incident-e-'));
  const result = run(['--help'], dir);
  check('documents both exit codes', /Exit 0/.test(result.stdout) && /Exit 1/.test(result.stdout));
  rmSync(dir, { recursive: true, force: true });
}

// 6. The happy path against a stub API, so "it captures" is observed rather than assumed.
{
  const { createServer } = await import('node:http');
  const server = createServer((req, res) => {
    res.setHeader('content-type', 'application/json');
    if (req.url.startsWith('/api/signals/formula')) {
      res.end(JSON.stringify({ version: 'v1', formula: '(u*30)+(i*25)', effectiveFrom: '2026-03-21', cappedFactors: ['x'], changeNote: 'initial' }));
    } else if (req.url.startsWith('/api/signals/meta')) {
      res.end(JSON.stringify({ totalSignals: 210, unresolvedSignals: 140, lastUpdatedAt: '2026-04-01T10:00:00', criticalScoreThreshold: 220 }));
    } else if (req.url.startsWith('/api/signals/prioritized')) {
      res.end(JSON.stringify({
        content: [
          { id: 'sig-a', title: 'Water main break', priorityScore: 313 },
          { id: 'sig-b', title: 'Streetlight out', priorityScore: 150 },
        ],
        totalElements: 2,
      }));
    } else if (req.url.startsWith('/api/community/freshness')) {
      res.end(JSON.stringify([
        { communityId: 'c1', communityName: 'Riverside', sources: [{ key: 'REPORTS', verdict: 'DORMANT' }, { key: 'DECISIONS', verdict: 'FRESH' }] },
      ]));
    } else {
      res.statusCode = 404;
      res.end('{}');
    }
  });

  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  const dir = mkdtempSync(path.join(tmpdir(), 'incident-f-'));
  try {
    const result = await runAsync(['--label', 'Ranking looked wrong for W13', '--api', `http://127.0.0.1:${port}`, '--out', dir]);
    check('captures against a reachable API', result.code === 0, `exit ${result.code} ${result.stderr}`);
    check('reports no failed probes', /probes FAILED/.test(result.stdout) === false, result.stdout);

    const files = bundleFiles(dir);
    const bundle = JSON.parse(readFileSync(path.join(dir, files[0]), 'utf8'));

    check('captured the formula version', bundle.probes.formula.version === 'v1');
    check('captured the counts', bundle.probes.meta.unresolvedSignals === 140);
    check('captured a real ordering hash', typeof bundle.probes.ranking.orderingHash === 'string'
      && bundle.probes.ranking.orderingHash.length === 64, JSON.stringify(bundle.probes.ranking));
    check('captured the top titles', (bundle.probes.ranking.topTitles ?? []).join(',') === 'Water main break,Streetlight out');
    check(
      'captured which surfaces are stale',
      (bundle.probes.freshness.staleSurfaces ?? []).some((s) => s.includes('REPORTS:DORMANT')),
      JSON.stringify(bundle.probes.freshness),
    );
    check(
      'did not report a healthy surface as stale',
      !(bundle.probes.freshness.staleSurfaces ?? []).some((s) => s.includes('DECISIONS')),
      JSON.stringify(bundle.probes.freshness),
    );
  } finally {
    server.close();
    rmSync(dir, { recursive: true, force: true });
  }
}

if (failures.length > 0) {
  console.error(`\n${failures.length} check(s) failed: ${failures.join(', ')}`);
  process.exit(1);
}
console.log('\nall checks passed');