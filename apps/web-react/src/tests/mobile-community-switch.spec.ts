import { expect, test } from '@playwright/test';
import { DEFAULT_COMMUNITY_ID, seedAuthenticatedApp } from './helpers/session';

const secondCommunityId = '22222222-2222-2222-2222-222222222222';

const json = (body: unknown) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

const membership = (communityId: string, name: string) => ({
  userId: 'u1',
  communityId,
  communityName: name,
  communitySlug: name.toLowerCase().replace(/\s+/g, '-'),
  parentCommunityId: null,
  breadcrumb: [{ id: communityId, name, slug: name.toLowerCase().replace(/\s+/g, '-') }],
  role: 'MEMBER',
  createdBy: 'u1',
  createdAt: '2026-11-01T10:00:00',
});

test.use({ viewport: { width: 390, height: 844 } });

test('switches community from the mobile drawer', async ({ page }) => {
  await seedAuthenticatedApp(page, 'CITIZEN', { communityId: DEFAULT_COMMUNITY_ID });
  await page.route('**/api/communities/my', (route) =>
    route.fulfill(json([
      membership(DEFAULT_COMMUNITY_ID, 'Los Rosales'),
      membership(secondCommunityId, 'Tunja'),
    ])));
  await page.route(`**/api/communities/${secondCommunityId}/switch`, (route) =>
    route.fulfill(json({ activeCommunityId: secondCommunityId })));
  await page.route('**/api/community/feed*', (route) => route.fulfill(json([])));

  await page.goto('/');
  await page.getByTestId('mobile-menu-toggle').click();
  const select = page.getByTestId('community-switch-mobile');
  await expect(select).toBeVisible();
  await select.selectOption(secondCommunityId);
  await expect(select).toHaveValue(secondCommunityId);
});
