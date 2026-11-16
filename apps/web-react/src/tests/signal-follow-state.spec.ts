import { expect, test } from '@playwright/test';
import { mockSignalDetail, signalId } from './helpers/signalDetailFixture';

const json = (body: unknown) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

test.describe('Follow state on a case', () => {
  test('a failed lookup offers a retry instead of claiming the case is not followed', async ({ page }) => {
    let failing = true;
    await mockSignalDetail(page);
    await page.route(`**/api/follows/SIGNAL/${signalId}`, (route) =>
      failing ? route.abort('failed') : route.fulfill(json({ following: true })));

    await page.goto(`/signal/${signalId}`);
    const follow = page.getByTestId('signal-follow-button');
    await expect(follow).toContainText('Retry follow', { timeout: 30000 });
    await expect(follow).not.toHaveAttribute('aria-pressed', 'false');

    failing = false;
    await follow.click();
    await expect(follow).toContainText('Following');
    await expect(follow).toHaveAttribute('aria-pressed', 'true');
  });
});
