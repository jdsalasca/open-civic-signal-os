// Captures the community rooms screen for visual review. Not a test: this exists to be looked at.
//
// Usage: node scripts/capture-community-rooms.mjs <outDir>
import { chromium, devices } from 'playwright';
import { mkdirSync } from 'node:fs';

const outDir = process.argv[2] ?? 'docs/evidence/round-51';
const BASE = process.env.BASE_URL ?? 'http://127.0.0.1:5199';
mkdirSync(outDir, { recursive: true });

const COMMUNITY_ID = '11111111-1111-1111-1111-111111111111';
const ROOM_ID = '55555555-5555-5555-5555-555555555555';
const USER_ID = '33333333-3333-3333-3333-333333333333';

const summary = (messageCount) => ({
  id: ROOM_ID,
  communityId: COMMUNITY_ID,
  projectBoardId: null,
  name: 'Night patrol working group',
  topic: 'Coordinate the lighting audit walk',
  createdBy: USER_ID,
  createdAt: '2026-03-01T09:00:00',
  archived: false,
  messageCount,
  unreadMentionCount: 0,
  muted: false,
  lastActivityAt: '2026-03-20T10:00:00',
});

const message = (index) => ({
  id: `message-${index}`,
  roomId: ROOM_ID,
  authorId: USER_ID,
  authorName: index % 2 === 0 ? 'coordinator' : 'vecina',
  body:
    index % 2 === 0
      ? `Field note ${index}: the lighting audit starts at the corner of Calle 12.`
      : `Field note ${index}: confirmed, I can walk the block after 7pm.`,
  createdAt: `2026-03-20T${String(9 + (index % 9)).padStart(2, '0')}:00:00`,
  mentionedUserIds: [],
  mentionsCurrentUser: false,
});

async function seed(page) {
  await page.addInitScript(([id, member]) => {
    localStorage.setItem('auth-storage', JSON.stringify({
      state: { accessToken: 't', userName: 'liaison', activeRole: 'PUBLIC_SERVANT',
        rawRoles: ['PUBLIC_SERVANT'], isLoggedIn: true, isHydrated: true },
      version: 0,
    }));
    localStorage.setItem('community-storage', JSON.stringify({
      state: { activeCommunityId: id, membershipsLoadedAt: Date.now(), memberships: [{
        userId: member, communityId: id, communityName: 'Riverside District',
        communitySlug: 'riverside-district', parentCommunityId: null,
        breadcrumb: [{ id, name: 'Riverside District', slug: 'riverside-district' }],
        role: 'COORDINATOR', createdBy: member, createdAt: '2026-03-01T09:00:00' }],
        threadListStateByCommunity: {} },
      version: 0,
    }));
  }, [COMMUNITY_ID, USER_ID]);
}

async function stub(page, { detailBody }) {
  // Registered last-wins, and deliberately scoped: a broad '**/api/**' catch-all also matched the
  // dev server's own module requests and served them JSON, which broke the lazy import.
  // The shared layout asks who am I on every screen. Without this it proxies to a backend that is not
  // running and logs a 500 that has nothing to do with the screen under review.
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '{}' }));
  await page.route('**/api/communities/*/permissions', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
  await page.route('**/api/community/rooms/mentions/read?*', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '0' }));
  await page.route('**/api/community/rooms/workspace?*', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json',
      body: JSON.stringify({ rooms: [summary(detailBody.messageCount)], totalRooms: 1,
        mentionInbox: { rooms: [] } }) }));
  await page.route(`**/api/community/rooms/${ROOM_ID}?*`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(detailBody) }));
  // The screen opens an EventSource. Close it immediately so the page settles instead of holding the
  // connection open forever, and so the capture does not depend on networkidle (an open stream never
  // reaches it).
  await page.route(`**/api/community/rooms/${ROOM_ID}/stream?*`, (route) =>
    route.fulfill({ status: 204, body: '' }));
}

const truncated = (count) => ({
  id: ROOM_ID,
  communityId: COMMUNITY_ID,
  projectBoardId: null,
  name: 'Night patrol working group',
  topic: 'Coordinate the lighting audit walk',
  createdBy: USER_ID,
  createdAt: '2026-03-01T09:00:00',
  archived: false,
  muted: false,
  mutedAt: null,
  messageCount: 1204,
  hasMoreMessages: true,
  unreadMentionCount: 0,
  messages: Array.from({ length: count }, (_, index) => message(index)),
});

const complete = {
  ...truncated(12),
  messageCount: 12,
  hasMoreMessages: false,
};

const shots = [
  { name: 'truncated-desktop', width: 1440, height: 1000, state: { detailBody: truncated(50) } },
  { name: 'truncated-mobile', width: 390, height: 844, state: { detailBody: truncated(50) } },
  { name: 'complete-desktop', width: 1440, height: 1000, state: { detailBody: complete } },
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
  // Console text says "500" but not which call. An unstubbed endpoint proxied to a backend that is
  // not running looks exactly like a UI bug in the screenshot, so name the URL.
  page.on('response', (r) => { if (r.status() >= 400) failed.push(`${r.status()} ${r.url()}`); });
  await seed(page);
  await stub(page, shot.state);
  await page.goto(`${BASE}/communities/rooms`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('[data-testid="community-rooms-message-list"]', { timeout: 30000 });
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${outDir}/${shot.name}.png`, fullPage: true });
  // fullPage does not reach below the fold here: the app scrolls an inner container, not the page.
  await page.evaluate(() => {
    const scroller = document.querySelector('main') ?? document.scrollingElement;
    if (scroller) scroller.scrollTop = scroller.scrollHeight;
  });
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${outDir}/${shot.name}-lower.png` });
  // The truncation note sits at the top of a message list inside an inner scroller, so neither the
  // full-page shot nor the bottom shot frames it. Scroll the control itself into view.
  const note = page.locator('[data-testid="community-rooms-history-note"]');
  if (await note.count()) {
    await note.scrollIntoViewIfNeeded();
    await page.waitForTimeout(400);
    await page.screenshot({ path: `${outDir}/${shot.name}-history-note.png` });
  }
  console.log(`${shot.name}: ${errors.length ? `CONSOLE ERRORS ${JSON.stringify(errors)}` : 'clean'}`);
  if (failed.length) console.log(`${shot.name}: FAILED REQUESTS ${JSON.stringify(failed, null, 2)}`);
  await context.close();
}
await browser.close();