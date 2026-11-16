import { expect, test } from '@playwright/test';
import { DEFAULT_COMMUNITY_ID, seedAuthenticatedApp } from './helpers/session';
import { mockDashboardRoutes } from './helpers/dashboard';

const cid = DEFAULT_COMMUNITY_ID;

const feed = [
  {
    type: 'thread-update',
    id: 'thread-1',
    communityId: cid,
    title: 'Neighbors, let us clean the park on Saturday',
    summary: 'Meet at 8 a.m. at the entrance. I bring bags and gloves.',
    happenedAt: '2026-11-15T20:50:00',
    freshness: 'updated 7m ago',
  },
  {
    type: 'signal',
    id: 'signal-1',
    communityId: cid,
    title: 'Unsafe crosswalk on the main avenue',
    summary: 'Main Avenue and 3rd Street',
    happenedAt: '2026-11-15T18:00:00',
    freshness: 'updated 2h ago',
  },
  {
    type: 'blog',
    id: 'blog-1',
    communityId: cid,
    title: 'New lighting for the park',
    summary: 'The city will install 12 lamps this week.',
    happenedAt: '2026-11-14T09:00:00',
    freshness: 'updated 1d ago',
  },
];

test.describe('Community social feed', () => {
  test('shows one timeline, filters it and publishes from the composer', async ({ page }) => {
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));
    await page.route('**/api/community/threads', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id: 'new-thread' }) }));
    await page.route('**/api/community/threads/new-thread/messages', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '{}' }));
    await page.route('**/api/notifications/recent', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));

    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await mockDashboardRoutes(page);
    await page.goto('/communities/feed');

    await expect(page.getByRole('heading', { level: 1 })).toContainText('Los Rosales');
    await expect(page.getByTestId('community-feed-timeline')).toBeVisible();
    await expect(page.locator('.social-feed-item')).toHaveCount(3);
    await expect(page.locator('.social-feed-item').first()).toContainText(feed[0].summary);

    await page.getByTestId('community-feed-filter-signal').click();
    await expect(page.locator('.social-feed-item')).toHaveCount(1);
    await expect(page.locator('.social-feed-type')).toHaveText('Report');

    await page.getByTestId('community-feed-filter-all').click();
    await page.getByTestId('community-feed-composer').locator('button').first().click();
    await page.locator('#feed-post-title').fill('A new community question');
    await page.locator('#feed-post-body').fill('This is the message body.');
    await page.getByTestId('community-feed-publish').click();

    await expect(page.getByText('Published in the community feed.')).toBeVisible();
    await expect(page.locator('#feed-post-title')).toHaveCount(0);
    await expect(page.locator('.social-feed-item')).toHaveCount(3);
  });
});
