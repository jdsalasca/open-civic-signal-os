// Captures the signal detail screen for visual review. Not a test: this exists to be looked at.
//
// Usage: node scripts/capture-signal-detail.mjs <outDir>
//
// The detail screen is where a resident follows one case over time, so it carries the audit trail:
// who moved it, when, and from which state to which. That history is only worth anything if it reads
// as language rather than as database output, which is what these captures exist to check.
//
// Everything is mocked and the session is seeded in localStorage, so the run needs no backend. The
// axios response interceptor logs the user out on any 401 that survives a refresh, so every route
// the shared Layout fires has to be answered here or the page bounces back to /login.
import { chromium, devices } from 'playwright';
import { mkdirSync } from 'node:fs';

const outDir = process.argv[2] ?? 'docs/evidence/round-64';
// 5173 is the civic dev frontend. Do not silently accept another app on this port.
const BASE = process.env.BASE_URL ?? 'http://127.0.0.1:5173';
mkdirSync(outDir, { recursive: true });

const signalId = '22222222-2222-2222-2222-222222222222';

// The wire sends a Java LocalDateTime, i.e. no offset and no seconds.
const createdAt = '2026-04-01T10:00:00';

const detail = (status) => ({
  id: signalId,
  title: 'Broken pedestrian bridge railing',
  description: 'The railing is loose and children cross here every morning on the way to school.',
  category: 'infrastructure',
  status,
  assignedToUsername: 'liaison',
  locationLabel: 'Pedestrian bridge by the sports field',
  evidenceUrls: ['https://example.com/bridge-1.jpg'],
  priorityScore: 198,
  scoreBreakdown: { urgency: 120, impact: 60, affectedPeople: 8, communityVotes: 10 },
  communityVotes: 12,
  reactions: {},
  explainabilitySummary: {
    version: 'v1',
    topFactors: [
      { key: 'urgency', contribution: 120 },
      { key: 'impact', contribution: 60 },
    ],
    summary: 'Top drivers: urgency (120.0), impact (60.0)',
  },
});

const history = [
  {
    id: '55555555-5555-5555-5555-555555555555',
    signalId,
    eventType: 'CREATED',
    statusFrom: 'NEW',
    statusTo: 'NEW',
    changedBy: 'vecina',
    assignedToUsername: null,
    reason: 'Reported through the web form with two photos.',
    createdAt,
  },
  {
    id: '44444444-4444-4444-4444-444444444444',
    signalId,
    eventType: 'ASSIGNED',
    statusFrom: 'NEW',
    statusTo: 'NEW',
    changedBy: 'staff',
    assignedToUsername: 'liaison',
    reason: 'Mobility liaison will coordinate the repair.',
    createdAt,
  },
  {
    id: '33333333-3333-3333-3333-333333333333',
    signalId,
    eventType: 'STATUS_CHANGED',
    statusFrom: 'NEW',
    statusTo: 'IN_PROGRESS',
    changedBy: 'staff',
    assignedToUsername: null,
    reason: 'Inspection started this morning.',
    createdAt,
  },
];

const formula = {
  version: 'v1',
  formula: '(Urgency * 30) + (Impact * 25) + min(People/10, 30) + min(Votes/5, 15)',
  effectiveFrom: '2026-03-21',
  weights: [],
  cappedFactors: ['affectedPeople', 'communityVotes'],
  changeNote: 'Initial published formula.',
};

const shots = [
  { name: 'in-progress-desktop', width: 1440, height: 1000, status: 'IN_PROGRESS' },
  { name: 'in-progress-mobile', width: 390, height: 844, status: 'IN_PROGRESS' },
  { name: 'resolved-desktop', width: 1440, height: 1000, status: 'RESOLVED' },
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

  await page.addInitScript(() => {
    window.localStorage.setItem(
      'auth-storage',
      JSON.stringify({
        state: {
          accessToken: 'test-token',
          userName: 'liaison',
          activeRole: 'PUBLIC_SERVANT',
          rawRoles: ['PUBLIC_SERVANT'],
          isLoggedIn: true,
          isHydrated: true,
        },
        version: 0,
      }),
    );
  });

  const json = (body) => ({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
  // A seeded comment so the engagement panel shows its own timestamp. The comment list is the
  // densest place a resident reads someone else's timestamp, so it is worth looking at.
  const comments = [
    {
      id: '66666666-6666-6666-6666-666666666666',
      parentId: signalId,
      parentType: 'SIGNAL',
      authorId: '77777777-7777-7777-7777-777777777777',
      authorUsername: 'vecino',
      authorRole: 'CITIZEN',
      content: 'I walk past this bridge every morning to take my kids to school. Please treat it as urgent.',
      createdAt,
    },
  ];
  await page.route('**/api/auth/me', (route) =>
    route.fulfill(json({ username: 'liaison', role: 'PUBLIC_SERVANT', interfaceMode: 'ADVANCED' })));
  await page.route('**/api/communities/my', (route) => route.fulfill(json([])));
  await page.route(`**/api/signals/${signalId}/comments`, (route) => route.fulfill(json(comments)));
  await page.route(`**/api/signals/${signalId}/history`, (route) => route.fulfill(json(history)));
  await page.route(`**/api/signals/${signalId}`, (route) => route.fulfill(json(detail(shot.status))));
  await page.route('**/api/signals/formula', (route) => route.fulfill(json(formula)));

  await page.goto(`${BASE}/signal/${signalId}`, { waitUntil: 'domcontentloaded' });

  // Prove we are looking at this app before writing anything to disk.
  try {
    await page.waitForSelector('[data-testid="signal-detail-location-label"]', { timeout: 15000 });
  } catch {
    await context.close();
    throw new Error(`${BASE} did not render the signal detail. Refusing to capture ${shot.name}.`);
  }

  await page.waitForTimeout(1200);
  await page.screenshot({ path: `${outDir}/${shot.name}.png`, fullPage: true });

  // fullPage does not reach below the fold here: the app scrolls an inner container, not the page.
  // Two framed views rather than a single "bottom" frame: this screen is tall enough that scrolling
  // to scrollHeight frames the footer and skips the audit trail, which is the part under review.
  await page.locator('[data-testid="signal-detail-timeline"]').scrollIntoViewIfNeeded();
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${outDir}/${shot.name}-timeline.png` });

  // Anchored on the seeded comment rather than a selector: CivicEngagement carries no testid, and
  // the fixture text is the most stable handle available without adding one for tooling's sake.
  await page.getByText('Please treat it as urgent').scrollIntoViewIfNeeded();
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${outDir}/${shot.name}-lower.png` });

  console.log(`${shot.name}: ${errors.length ? `CONSOLE ERRORS ${JSON.stringify(errors)}` : 'clean'}`);
  if (failed.length) console.log(`${shot.name}: FAILED REQUESTS ${JSON.stringify(failed, null, 2)}`);
  await context.close();
}
await browser.close();