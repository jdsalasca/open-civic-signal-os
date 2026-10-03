// Exercises the generator end to end against a saved report fixture.
//
// The script is the thing a council meeting actually reads, so its own guarantees are worth
// pinning: same input produces the same bytes, and a report that would misrepresent the
// community stops the run instead of rendering.

import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const script = path.join(root, 'scripts', 'generate-transparency-report.mjs');

const validReport = {
  communityId: '11111111-2222-3333-4444-555555555555',
  communityName: 'Riverside District',
  period: {
    key: '2026-03',
    startDate: '2026-03-01',
    endDate: '2026-04-01',
    previous: { key: '2026-02', startDate: '2026-02-01', endDate: '2026-03-01', previous: null },
  },
  metrics: [
    { key: 'SIGNALS_REPORTED', label: 'Signals reported', value: 42, unit: 'count', previousValue: 31, delta: 11, direction: 'UP' },
    { key: 'MEDIAN_RESOLUTION_DAYS', label: 'Median days to resolve', value: 12, unit: 'days', previousValue: 19, delta: -7, direction: 'DOWN' },
  ],
  actioned: [
    { signalId: 'a', title: 'Streetlight out', status: 'RESOLVED', category: 'infrastructure', priorityScore: 70, locationLabel: 'Main', daysOpen: 5, reportedAt: '2026-03-02T09:00:00', resolvedAt: '2026-03-07T09:00:00' },
  ],
  unaddressed: [
    { signalId: 'b', title: 'Broken water valve', status: 'OPEN', category: 'utilities', priorityScore: 91, locationLabel: 'Calle 12', daysOpen: 20, reportedAt: '2026-03-03T09:00:00', resolvedAt: null },
  ],
  narrative: ['Riverside District transparency report for 2026-03.'],
  formulaVersion: 'v1',
  generatedAt: '2026-04-01T10:00:00',
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

function run(report) {
  const dir = mkdtempSync(path.join(tmpdir(), 'report-test-'));
  const input = path.join(dir, 'report.json');
  const out = path.join(dir, 'out');
  writeFileSync(input, JSON.stringify(report, null, 2));
  try {
    const stdout = execFileSync(
      process.execPath,
      [script, '--input', input, '--out', out],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] },
    );
    const stem = `transparency-report-${report.communityId}-${report.period.key}`;
    return {
      code: 0,
      stdout,
      files: {
        json: readFileSync(path.join(out, `${stem}.json`), 'utf8'),
        markdown: readFileSync(path.join(out, `${stem}.md`), 'utf8'),
      },
    };
  } catch (error) {
    return { code: error.status ?? 1, stderr: error.stderr ?? '' };
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
}

console.log('generate-transparency-report');

const first = run(validReport);
check('renders a valid report', first.code === 0, first.stderr);
check('reports both artifacts', first.code === 0 && first.stdout.includes('.json') && first.stdout.includes('.md'));
check('includes the unaddressed section', first.code === 0 && first.stdout.length > 0);

const second = run(validReport);
check(
  'produces byte-identical output across runs',
  second.code === 0
    && first.files.json === second.files.json
    && first.files.markdown === second.files.markdown,
  'the generated files differed between two identical runs',
);
// The "unchanged" log path only shows up when the second run sees the first run's files, so
// this needs one shared output directory rather than the per-call temp dirs above.
{
  const dir = mkdtempSync(path.join(tmpdir(), 'report-twice-'));
  const input = path.join(dir, 'report.json');
  const out = path.join(dir, 'out');
  writeFileSync(input, JSON.stringify(validReport, null, 2));
  execFileSync(process.execPath, [script, '--input', input, '--out', out], { encoding: 'utf8' });
  const rerunStdout = execFileSync(process.execPath, [script, '--input', input, '--out', out], { encoding: 'utf8' });
  check(
    'reports unchanged files on a rerun into the same directory',
    rerunStdout.includes('unchanged:') && !rerunStdout.includes('wrote:'),
    `expected only "unchanged:" lines, got: ${rerunStdout}`,
  );
  rmSync(dir, { recursive: true, force: true });
}

check('rejects a report with no previous period', run({ ...validReport, period: { key: '2026-03', startDate: '2026-03-01', endDate: '2026-04-01' } }).code === 1);
check('rejects a malformed period key', run({ ...validReport, period: { ...validReport.period, key: 'March' } }).code === 1);
check('rejects a metric with no comparison', run({ ...validReport, metrics: [{ key: 'X', label: 'X', value: 1, unit: 'count' }] }).code === 1);
check('rejects a report with no formula version', run({ ...validReport, formulaVersion: undefined }).code === 1);
check('fails fast on a missing input file', runMissingInput().code === 1);

function runMissingInput() {
  try {
    execFileSync(
      process.execPath,
      [script, '--input', path.join(tmpdir(), 'definitely-absent-report.json'), '--out', tmpdir()],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] },
    );
    return { code: 0 };
  } catch (error) {
    return { code: error.status ?? 1, stderr: error.stderr ?? '' };
  }
}

// Markdown content checks, using a kept directory so the files can be read back.
const keepDir = mkdtempSync(path.join(tmpdir(), 'report-md-'));
writeFileSync(path.join(keepDir, 'report.json'), JSON.stringify(validReport, null, 2));
execFileSync(process.execPath, [script, '--input', path.join(keepDir, 'report.json'), '--out', path.join(keepDir, 'out')], { encoding: 'utf8' });
const markdown = readFileSync(path.join(keepDir, 'out', 'transparency-report-11111111-2222-3333-4444-555555555555-2026-03.md'), 'utf8');
rmSync(keepDir, { recursive: true, force: true });

check('states the closed period', markdown.includes('2026-03-01 to 2026-04-01'));
check('names the unaddressed item', markdown.includes('Broken water valve'));
check('names the actioned item', markdown.includes('Streetlight out'));
check('reads a falling resolution time as improved', markdown.includes('-7 (improved)'));
check('leaves an ambiguous intake metric unframed', markdown.includes('| Signals reported | 42 count | 31 count | +11 |'));
check('does not claim rising intake is a win', !markdown.includes('+11 (improved)'));
check('cites the scoring formula', markdown.includes('Scoring formula: v1'));
check('claims reproducibility', markdown.includes('byte-identical'));

if (failures.length > 0) {
  console.error(`\n${failures.length} check(s) failed: ${failures.join(', ')}`);
  process.exit(1);
}
console.log('\nall checks passed');