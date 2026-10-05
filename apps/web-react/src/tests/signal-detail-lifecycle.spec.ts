import { expect, test } from '@playwright/test';
import { mockSignalDetail, signalId } from './helpers/signalDetailFixture';

/**
 * The signal detail screen is where a resident follows one case over time, so it is the screen
 * where machine vocabulary hurts most: the header badge, the timeline badge and the transition
 * sentence all published the raw lifecycle enum, and the timeline timestamp changed shape with
 * the reader's browser locale.
 *
 * Fixture and its two load-bearing details live in helpers/signalDetailFixture.ts.
 */
test.describe('Signal detail speaks plain language', () => {
  test.beforeEach(async ({ page }) => {
    await mockSignalDetail(page);
  });

  test('the header badge spells out the lifecycle state', async ({ page }) => {
    await page.goto(`/signal/${signalId}`);
    await expect(page.getByTestId('signal-detail-location-label')).toBeVisible({ timeout: 30000 });

    await expect(page.getByTestId('signal-detail-status-badge')).toHaveText('In progress');

    // IN_PROGRESS is an identifier for the API, not something a resident can act on.
    await expect(page.getByText('IN_PROGRESS')).toHaveCount(0);
  });

  test('the timeline transition reads as two states, not two identifiers', async ({ page }) => {
    await page.goto(`/signal/${signalId}`);
    await expect(page.getByTestId('signal-timeline-entry-0')).toBeVisible({ timeout: 30000 });

    await expect(page.getByTestId('signal-timeline-entry-0')).toContainText('From New to In progress');
    await expect(page.getByTestId('signal-timeline-entry-0')).not.toContainText('IN_PROGRESS');
  });
});

test.describe('Signal detail timestamps do not depend on the reader browser locale', () => {
  // Same defect as the public backlog freshness stamp: toLocaleString with no argument renders
  // from the browser's default locale, so the same audit trail reads "4/1/2026, 10:00:00 AM" to one
  // resident and "1.4.2026, 10:00:00" to the next.
  test.use({ locale: 'de-DE' });

  test('renders the audit trail the same way under a non-English locale', async ({ page }) => {
    await mockSignalDetail(page);

    await page.goto(`/signal/${signalId}`);
    await expect(page.getByTestId('signal-timeline-entry-0')).toBeVisible({ timeout: 30000 });

    await expect(page.getByTestId('signal-timeline-entry-0')).toContainText('2026-04-01 10:00');
    await expect(page.getByTestId('signal-timeline-entry-0')).not.toContainText('a. m.');
  });
});