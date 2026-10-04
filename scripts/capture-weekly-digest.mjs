// Captures the weekly digest screen for visual review. Not a test: this exists to be looked at.
//
// Usage: node scripts/capture-weekly-digest.mjs <outDir>
import { chromium, devices } from 'playwright';
import { mkdirSync } from 'node:fs';

const outDir = process.argv[2] ?? 'docs/evidence/round-47';
const BASE = process.env.BASE_URL ?? 'http://127.0.0.1:5199';
mkdirSync(outDir, { recursive: true });

const COMMUNITY_ID = '11111111-2222-3333-4444-555555555555';

const digest = (over = {}) => ({
  version: 'v1',
  communityId: COMMUNITY_ID,
  communityName: 'Riverside District',
  week: { key: '2026-W13', startDate: '2026-03-23', endDate: '2026-03-30', previousKey: '2026-W12' },
  topUnresolved: [
    { signalId: 'sig-1', title: 'Water main break on Calle 12', category: 'utilities', status: 'NEW',
      locationLabel: 'Calle 12', priorityScore: 313, daysOpen: 4,
      whyRanked: 'urgency 5/5, impact 5/5, 900 people affected, 40 community votes' },
    { signalId: 'sig-2', title: 'Streetlight out on the main corridor', category: 'infrastructure',
      status: 'IN_PROGRESS', locationLabel: 'Avenida Central', priorityScore: 210, daysOpen: 11,
      whyRanked: 'urgency 4/5, impact 4/5, 320 people affected, 22 community votes' },
    { signalId: 'sig-3', title: 'Loose paving slab near the school gate', category: 'infrastructure',
      status: 'NEW', locationLabel: 'Escuela primaries', priorityScore: 96, daysOpen: 19,
      whyRanked: 'urgency 3/5, impact 3/5, 80 people affected, 9 community votes' },
  ],
  resolvedThisWeek: 3,
  rejectedThisWeek: 1,
  reportedThisWeek: 7,
  stillOpenTotal: 12,
  body: '# Riverside District: weekly digest 2026-W13\n\nWeek of 2026-03-23 to 2026-03-29.\n\n7 report(s) came in, 3 were resolved and 1 was rejected with a reason.\n12 remain open in total.\n\n## Still unresolved, highest priority first\n\n1. Water main break on Calle 12 (utilities)\n   score 313.0, open 4 day(s), urgency 5/5, impact 5/5, 900 people affected, 40 community votes\n2. Streetlight out on the main corridor (infrastructure)\n   score 210.0, open 11 day(s), urgency 4/5, impact 4/5, 320 people affected, 22 community votes\n3. Loose paving slab near the school gate (infrastructure)\n   score 96.0, open 19 day(s), urgency 3/5, impact 3/5, 80 people affected, 9 community votes',
  contentHash: 'abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789',
  published: false,
  publishedAt: null,
  generatedAt: '2026-03-30T06:00:00',
  deliveredToChannels: 0,
  ...over,
});

const schedule = [
  { communityId: COMMUNITY_ID, communityName: 'Riverside District', weekKey: '2026-W13',
    outcome: 'PREPARED', detail: '3 item(s), hash 9762e631fab5. Sealed; publishing sends this exact artifact and does not recompose it.', ranAt: '2026-03-30T06:00:00' },
  { communityId: COMMUNITY_ID, communityName: null, weekKey: '2026-W12', outcome: 'PREPARED',
    detail: '4 item(s), hash 5510aa22bc31. Sealed; publishing sends this exact artifact and does not recompose it.', ranAt: '2026-03-23T06:00:00' },
];

async function seed(page) {
  await page.addInitScript((id) => {
    localStorage.setItem('auth-storage', JSON.stringify({
      state: { accessToken: 't', userName: 'liaison', activeRole: 'PUBLIC_SERVANT',
        rawRoles: ['PUBLIC_SERVANT', 'CITIZEN'], isLoggedIn: true, isHydrated: true },
      version: 0,
    }));
    localStorage.setItem('community-storage', JSON.stringify({
      state: { activeCommunityId: id, memberships: [{ communityId: id, communityName: 'Riverside District',
        communitySlug: 'riverside-district', parentCommunityId: null,
        breadcrumb: [{ id, name: 'Riverside District', slug: 'riverside-district' }], role: 'COORDINATOR' }] },
      version: 0,
    }));
  }, COMMUNITY_ID);
}

async function stub(page, { digestBody, scheduleBody }) {
  // Registered last-wins, and deliberately scoped: a broad '**/api/**' catch-all also matched the
  // dev server's own module requests (/src/views/WeeklyDigestView.tsx) and served them JSON, which
  // broke the lazy import and left the screen blank.
  await page.route('**/api/community/weekly-digest/schedule-history*', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(scheduleBody) }));
  await page.route('**/api/community/weekly-digest?*', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(digestBody) }));
}

const shots = [
  { name: 'unpublished-desktop', width: 1440, height: 1000,
    state: { digestBody: digest(), scheduleBody: schedule } },
  { name: 'published-desktop', width: 1440, height: 1000,
    state: { digestBody: digest({ published: true, deliveredToChannels: 2,
      publishedAt: '2026-03-30T07:10:00' }), scheduleBody: schedule } },
  { name: 'unpublished-mobile', width: 390, height: 844,
    state: { digestBody: digest(), scheduleBody: schedule } },
  { name: 'no-signals-desktop', width: 1440, height: 1000,
    state: { digestBody: digest({ topUnresolved: [], resolvedThisWeek: 0, rejectedThisWeek: 0,
      reportedThisWeek: 0, stillOpenTotal: 0,
      body: '# Riverside District: weekly digest 2026-W13\n\nNo reports came in this week.' }),
      scheduleBody: [] } },
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
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  await seed(page);
  await stub(page, shot.state);
  await page.goto(`${BASE}/communities/weekly-digest`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${outDir}/${shot.name}.png`, fullPage: true });
  // fullPage does not reach below the fold here: the app scrolls an inner container, not the page.
  await page.evaluate(() => {
    const scroller = document.querySelector('main') ?? document.scrollingElement;
    if (scroller) scroller.scrollTop = scroller.scrollHeight;
  });
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${outDir}/${shot.name}-lower.png` });
  console.log(`${shot.name}: ${errors.length ? `CONSOLE ERRORS ${JSON.stringify(errors)}` : 'clean'}`);
  await context.close();
}
await browser.close();