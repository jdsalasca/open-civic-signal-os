import { expect, test } from '@playwright/test';

const rootCommunity = {
  id: '11111111-1111-1111-1111-111111111111',
  name: 'Los Rosales',
  slug: 'rosalistas',
  description: 'Community coordination space for Los Rosales district.',
  parentCommunityId: null,
  createdAt: '2026-03-05T10:00:00'
};

const childCommunity = {
  id: '22222222-2222-2222-2222-222222222222',
  name: 'Los Rosales / Building A',
  slug: 'rosalistas-building-a',
  description: 'Nested working space for Building A residents.',
  parentCommunityId: rootCommunity.id,
  createdAt: '2026-03-05T10:05:00'
};

test.describe('Community hierarchy hub', () => {
  test('renders breadcrumb context and nested tree with user-friendly role labels', async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem(
        'auth-storage',
        JSON.stringify({
          state: {
            accessToken: 'test-token',
            userName: 'admin',
            activeRole: 'SUPER_ADMIN',
            rawRoles: ['SUPER_ADMIN', 'PUBLIC_SERVANT', 'CITIZEN'],
            isLoggedIn: true,
            isHydrated: true
          },
          version: 0
        })
      );
    });

    await page.route('**/api/communities/tree', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            ...rootCommunity,
            children: [
              {
                ...childCommunity,
                children: []
              }
            ]
          }
        ])
      });
    });

    await page.route('**/api/communities/my', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            userId: '33333333-3333-3333-3333-333333333333',
            communityId: childCommunity.id,
            communityName: childCommunity.name,
            communitySlug: childCommunity.slug,
            parentCommunityId: rootCommunity.id,
            breadcrumb: [
              { id: rootCommunity.id, name: rootCommunity.name, slug: rootCommunity.slug },
              { id: childCommunity.id, name: childCommunity.name, slug: childCommunity.slug }
            ],
            role: 'MEMBER',
            createdBy: '33333333-3333-3333-3333-333333333333',
            createdAt: '2026-03-05T10:06:00'
          }
        ])
      });
    });

    await page.route('**/api/communities', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([rootCommunity, childCommunity])
      });
    });

    await page.route('**/api/communities/*/switch', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ status: 'active' })
      });
    });

    await page.goto('http://127.0.0.1:5173/communities');

    const breadcrumbCard = page.getByTestId('active-community-breadcrumb-card');
    await expect(breadcrumbCard).toContainText('Los Rosales');
    await expect(breadcrumbCard).toContainText('Building A');
    await expect(breadcrumbCard).toContainText('Member');

    const treeView = page.getByTestId('community-tree-view');
    await expect(treeView).toContainText('Los Rosales');
    await expect(treeView).toContainText('rosalistas-building-a');
    await expect(treeView.getByRole('button', { name: 'Active context' })).toBeVisible();

    await expect(page.getByTestId(`membership-role-dropdown-${childCommunity.id}`)).toContainText('Member');
    await expect(page.getByTestId('join-role-dropdown')).toContainText('Member');
  });
});
