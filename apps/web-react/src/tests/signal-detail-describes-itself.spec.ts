import { expect, test } from '@playwright/test';
import { mockSignalDetail, seedSession, signalId } from './helpers/signalDetailFixture';

/**
 * A screen has to say what it is.
 *
 * The case screen named a card below itself twice and then claimed to be the dashboard:
 *
 *   eyebrow     "Priority Rank"      the same words as the card that shows 198
 *   subtitle    "Intelligence Context" the same words as the card directly underneath
 *   topbar      "What needs attention today / Home"  a dashboard framing, on one case
 *
 * None of it was wrong at the level of a string; all of it was wrong as an orientation. Someone
 * arriving from a notification had no way to tell whether they were reading a case or a list.
 *
 * Every assertion below is a negative on purpose. A duplicated or misframed label cannot be caught
 * by asserting the right text is present, only by asserting the wrong text is gone.
 */
test.describe('The case screen describes itself', () => {
  test.beforeEach(async ({ page }) => {
    await seedSession(page);
    await mockSignalDetail(page);
  });

  test('the eyebrow names the case instead of repeating a card below it', async ({ page }) => {
    await page.goto(`/signal/${signalId}`);
    await expect(page.getByTestId('signal-detail-location-label')).toBeVisible({ timeout: 30000 });

    const eyebrow = page.getByTestId('page-header-eyebrow');

    // "Priority Rank" is the title of the card further down that renders the number 198.
    await expect(eyebrow).not.toContainText('Priority Rank');
    // What kind of case this is: the category is real information and appears nowhere else up here.
    await expect(eyebrow).toContainText('Infrastructure');
  });

  test('the header does not restate the card immediately below it', async ({ page }) => {
    await page.goto(`/signal/${signalId}`);
    await expect(page.getByTestId('signal-detail-location-label')).toBeVisible({ timeout: 30000 });

    // Anchor on the title first: "no description" is also what an absent header looks like, so
    // without this the assertion would pass without ever looking at the screen.
    await expect(page.getByTestId('page-header-title')).toContainText('Broken pedestrian bridge railing');

    // "Intelligence Context" is the title of the first card under the header. Repeating it as the
    // subtitle says nothing the card has not already said one line lower.
    await expect(page.getByTestId('page-header-description')).toHaveCount(0);
  });

  test('the topbar stops presenting a single case as the dashboard', async ({ page }) => {
    await page.goto(`/signal/${signalId}`);
    await expect(page.getByTestId('signal-detail-location-label')).toBeVisible({ timeout: 30000 });

    // The title was falling through to nav.insights, whose value is literally "Home".
    await expect(page.getByTestId('app-topbar-title')).toHaveText('Case detail');
    await expect(page.getByTestId('app-topbar-title')).not.toHaveText('Home');

    // "What needs attention today" is a dashboard framing; on a case it describes nothing.
    await expect(page.getByTestId('app-topbar-label')).toHaveCount(0);
  });
});

test.describe('The topbar still frames the pages it is meant to frame', () => {
  // The fix must not flatten every screen. This is the dashboard, the one route whose framing line
  // the change could plausibly have swallowed.
  test('the dashboard keeps its "what needs attention today" framing', async ({ page }) => {
    await seedSession(page, 'CITIZEN');
    await mockSignalDetail(page);
    // Every route the dashboard fires. One unmocked request is not a harmless gap: it 401s against
    // the live backend and axios logs the session out before the topbar ever renders.
    for (const path of ['notifications/recent', 'notifications/relay/top-10', 'signals/duplicates']) {
      await page.route(`**/api/${path}`, (route) =>
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }));
    }
    await page.route('**/api/signals/meta', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ totalSignals: 0, unresolvedSignals: 0, lastUpdatedAt: null, criticalScoreThreshold: 220 }),
      }));
    await page.route('**/api/signals/prioritized*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [], totalPages: 0, totalElements: 0, number: 0, size: 20, first: true, last: true }),
      }));

    await page.goto('/');
    await expect(page.getByTestId('app-topbar-label')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('app-topbar-label')).toContainText('What needs attention today');
    await expect(page.getByTestId('app-topbar-title')).toHaveText('Home');
  });
});