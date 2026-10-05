// Captures the public backlog for visual review. Not a test: this exists to be looked at.
//
// Usage: node scripts/capture-public-backlog.mjs <outDir>
//
// The public backlog is the screen that answers "what has the municipality actually committed to",
// and it is reachable without an account. That makes it the screen where a layout problem costs the
// most trust: a visitor who cannot tell what is a signal, what is a score, and how fresh the data is
// leaves with a worse impression than one who was never told.
//
// Three states, because the states are where public screens usually fall apart: populated, no session,
// and the API unreachable.
import { chromium, devices } from 'playwright';
import { mkdirSync } from 'node:fs';

const outDir = process.argv[2] ?? 'docs/evidence/round-63';
// 5173 is the civic dev frontend. Do not silently accept another app on this port: capturing a
// stranger's UI and filing it as backlog evidence is worse than failing here.
const BASE = process.env.BASE_URL ?? 'http://127.0.0.1:5173';
mkdirSync(outDir, { recursive: true });

const signals = [
  {
    id: 'sig-1',
    title: 'Water main break on Calle 12',
    description: 'No supply since Tuesday. Three blocks are affected and the clinic is included.',
    category: 'utilities',
    status: 'NEW',
    priorityScore: 313.0,
    scoreBreakdown: { urgency: 150, impact: 125, affectedPeople: 30, communityVotes: 8 },
    explainabilitySummary: {
      version: 'v1',
      topFactors: [],
      summary: 'Urgency and impact are both high, and 900 people are affected.',
    },
    latitude: null,
    longitude: null,
    createdAt: '2026-04-01T08:00:00',
    authorUsername: 'vecina',
    commentsCount: 4,
    viewerHasVoted: false,
    communityVotes: 8,
    affectedPeople: 900,
    locationLabel: 'Calle 12',
    sourceChannel: 'WEB_FORM',
    sourceRef: null,
    transformationVersion: 'v1',
  },
  {
    id: 'sig-2',
    title: 'Streetlight out on the main corridor',
    description: 'Dark for three nights between the school and the bus stop.',
    category: 'infrastructure',
    status: 'IN_PROGRESS',
    priorityScore: 150.0,
    scoreBreakdown: { urgency: 90, impact: 75, affectedPeople: 20, communityVotes: 4 },
    explainabilitySummary: { version: 'v1', topFactors: [], summary: 'High urgency reported by residents.' },
    latitude: null,
    longitude: null,
    createdAt: '2026-04-02T08:00:00',
    authorUsername: 'vecino',
    commentsCount: 1,
    viewerHasVoted: false,
    communityVotes: 4,
    affectedPeople: 320,
    locationLabel: 'Avenida Central',
    sourceChannel: 'WHATSAPP',
    sourceRef: null,
    transformationVersion: 'v1',
  },
  {
    id: 'sig-3',
    title: 'Loose paving slab near the school gate',
    description: 'A trip hazard where children queue at pickup.',
    category: 'infrastructure',
    status: 'RESOLVED',
    priorityScore: 96.0,
    scoreBreakdown: { urgency: 60, impact: 50, affectedPeople: 8, communityVotes: 0 },
    explainabilitySummary: { version: 'v1', topFactors: [], summary: 'Low reported urgency.' },
    latitude: null,
    longitude: null,
    createdAt: '2026-03-19T08:00:00',
    authorUsername: 'juan',
    commentsCount: 0,
    viewerHasVoted: false,
    communityVotes: 0,
    affectedPeople: 80,
    locationLabel: 'Escuela primaria',
    sourceChannel: 'WEB_FORM',
    sourceRef: null,
    transformationVersion: 'v1',
  },
];

const meta = {
  totalSignals: 210,
  unresolvedSignals: 140,
  lastUpdatedAt: '2026-04-01T10:00:00',
  criticalScoreThreshold: 220,
};

const formula = {
  version: 'v1',
  formula: '(Urgency * 30) + (Impact * 25) + min(People/10, 30) + min(Votes/5, 15)',
  effectiveFrom: '2026-03-21',
  weights: [],
  cappedFactors: ['affectedPeople', 'communityVotes'],
  changeNote: 'Initial published formula.',
};

const shots = [
  {
    name: 'populated-desktop',
    width: 1440,
    height: 1000,
    state: 'ok',
  },
  {
    name: 'populated-mobile',
    width: 390,
    height: 844,
    state: 'ok',
  },
  {
    name: 'api-unavailable-desktop',
    width: 1440,
    height: 1000,
    state: 'down',
  },
  {
    name: 'empty-desktop',
    width: 1440,
    height: 1000,
    state: 'empty',
  },
];

const browser = await chromium.launch();
for (const shot of shots) {
  const context = await browser.newContext({
    viewport: { width: shot.width, height: shot.height },
    ...(shot.width < 500 ? { ...devices['Pixel 7'] } : {}),
    colorScheme: 'light',
  });
  const page = await context.newPage();
  const errors = [];
  const failed = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  page.on('response', (r) => { if (r.status() >= 400) failed.push(`${r.status()} ${r.url()}`); });

  if (shot.state === 'down') {
    await page.route('**/api/signals/**', (route) => route.abort('failed'));
  } else {
    const content = shot.state === 'empty' ? [] : signals;
    await page.route('**/api/signals/prioritized*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content, totalPages: shot.state === 'empty' ? 0 : 1, totalElements: content.length, number: 0, size: 20, first: true, last: true, empty: content.length === 0 }),
      }));
    await page.route('**/api/signals/meta', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(meta) }));
    await page.route('**/api/signals/formula', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(formula) }));
  }

  await page.goto(`${BASE}/backlog`, { waitUntil: 'domcontentloaded' });

  // Prove we are looking at this app before writing anything to disk.
  try {
    await page.waitForSelector('.public-backlog-shell', { timeout: 15000 });
  } catch {
    await context.close();
    throw new Error(`${BASE} did not render the public backlog. Refusing to capture ${shot.name}.`);
  }

  await page.waitForTimeout(1200);
  await page.screenshot({ path: `${outDir}/${shot.name}.png`, fullPage: true });

  // fullPage does not reach below the fold here: the app scrolls an inner container, not the page.
  await page.evaluate(() => {
    const scroller = document.querySelector('main') ?? document.scrollingElement;
    if (scroller) scroller.scrollTop = scroller.scrollHeight;
  });
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${outDir}/${shot.name}-lower.png` });

  console.log(`${shot.name}: ${errors.length ? `CONSOLE ERRORS ${JSON.stringify(errors)}` : 'clean'}`);
  if (failed.length) console.log(`${shot.name}: FAILED REQUESTS ${JSON.stringify(failed, null, 2)}`);
  await context.close();
}
await browser.close();