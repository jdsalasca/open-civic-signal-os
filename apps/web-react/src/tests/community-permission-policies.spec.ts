import { expect, test } from '@playwright/test';

const rootCommunity = {
  id: '11111111-1111-1111-1111-111111111111',
  name: 'Los Rosales',
  slug: 'rosalistas',
  description: 'Community coordination space for Los Rosales district.',
  parentCommunityId: null,
  createdAt: '2026-03-05T10:00:00'
};

test.describe('Community permission policies', () => {
  test('coordinator can edit and save community policy matrix', async ({ page }) => {
    let savedPayload: unknown = null;

    await page.addInitScript((communityId) => {
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
      window.localStorage.setItem(
        'community-storage',
        JSON.stringify({
          state: {
            activeCommunityId: communityId,
            membershipsLoadedAt: Date.now(),
            memberships: [
              {
                userId: '33333333-3333-3333-3333-333333333333',
                communityId,
                communityName: 'Los Rosales',
                communitySlug: 'rosalistas',
                parentCommunityId: null,
                breadcrumb: [{ id: communityId, name: 'Los Rosales', slug: 'rosalistas' }],
                role: 'COORDINATOR',
                createdBy: '33333333-3333-3333-3333-333333333333',
                createdAt: '2026-03-05T10:06:00'
              }
            ],
            threadListStateByCommunity: {}
          },
          version: 0
        })
      );
    }, rootCommunity.id);

    await page.route('**/api/communities/tree', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ ...rootCommunity, children: [] }])
      });
    });

    await page.route('**/api/communities/my', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            userId: '33333333-3333-3333-3333-333333333333',
            communityId: rootCommunity.id,
            communityName: rootCommunity.name,
            communitySlug: rootCommunity.slug,
            parentCommunityId: null,
            breadcrumb: [{ id: rootCommunity.id, name: rootCommunity.name, slug: rootCommunity.slug }],
            role: 'COORDINATOR',
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
        body: JSON.stringify([rootCommunity])
      });
    });

    await page.route(`**/api/communities/${rootCommunity.id}/permissions`, async (route) => {
      if (route.request().method() === 'PUT') {
        savedPayload = route.request().postDataJSON();
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              communityId: rootCommunity.id,
              scope: 'CREATE_PROPOSAL',
              allowedRoles: ['MEMBER', 'MODERATOR', 'COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
              updatedBy: '33333333-3333-3333-3333-333333333333',
              updatedAt: '2026-03-05T11:00:00'
            },
            {
              communityId: rootCommunity.id,
              scope: 'CREATE_THREAD',
              allowedRoles: ['COORDINATOR'],
              updatedBy: '33333333-3333-3333-3333-333333333333',
              updatedAt: '2026-03-05T11:00:00'
            },
            {
              communityId: rootCommunity.id,
              scope: 'ADD_THREAD_MESSAGE',
              allowedRoles: ['MEMBER', 'MODERATOR', 'COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
              updatedBy: '33333333-3333-3333-3333-333333333333',
              updatedAt: '2026-03-05T11:00:00'
            },
            {
              communityId: rootCommunity.id,
              scope: 'MODERATE_THREAD_MESSAGE',
              allowedRoles: ['MODERATOR', 'COORDINATOR'],
              updatedBy: '33333333-3333-3333-3333-333333333333',
              updatedAt: '2026-03-05T11:00:00'
            },
            {
              communityId: rootCommunity.id,
              scope: 'CREATE_OFFICIAL_UPDATE',
              allowedRoles: ['COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
              updatedBy: '33333333-3333-3333-3333-333333333333',
              updatedAt: '2026-03-05T11:00:00'
            },
            {
              communityId: rootCommunity.id,
              scope: 'UPDATE_OFFICIAL_UPDATE',
              allowedRoles: ['COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
              updatedBy: '33333333-3333-3333-3333-333333333333',
              updatedAt: '2026-03-05T11:00:00'
            },
            {
              communityId: rootCommunity.id,
              scope: 'MANAGE_MEMBERSHIPS',
              allowedRoles: ['COORDINATOR'],
              updatedBy: '33333333-3333-3333-3333-333333333333',
              updatedAt: '2026-03-05T11:00:00'
            },
            {
              communityId: rootCommunity.id,
              scope: 'MANAGE_PERMISSION_POLICIES',
              allowedRoles: ['COORDINATOR'],
              updatedBy: '33333333-3333-3333-3333-333333333333',
              updatedAt: '2026-03-05T11:00:00'
            },
            {
              communityId: rootCommunity.id,
              scope: 'VIEW_SENSITIVE_DATA',
              allowedRoles: ['COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
              updatedBy: '33333333-3333-3333-3333-333333333333',
              updatedAt: '2026-03-05T11:00:00'
            }
          ])
        });
        return;
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            communityId: rootCommunity.id,
            scope: 'CREATE_PROPOSAL',
            allowedRoles: ['MEMBER', 'MODERATOR', 'COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
            updatedBy: '33333333-3333-3333-3333-333333333333',
            updatedAt: '2026-03-05T10:10:00'
          },
          {
            communityId: rootCommunity.id,
            scope: 'CREATE_THREAD',
            allowedRoles: ['MEMBER', 'MODERATOR', 'COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
            updatedBy: '33333333-3333-3333-3333-333333333333',
            updatedAt: '2026-03-05T10:10:00'
          },
          {
            communityId: rootCommunity.id,
            scope: 'ADD_THREAD_MESSAGE',
            allowedRoles: ['MEMBER', 'MODERATOR', 'COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
            updatedBy: '33333333-3333-3333-3333-333333333333',
            updatedAt: '2026-03-05T10:10:00'
          },
          {
            communityId: rootCommunity.id,
            scope: 'MODERATE_THREAD_MESSAGE',
            allowedRoles: ['MODERATOR', 'COORDINATOR'],
            updatedBy: '33333333-3333-3333-3333-333333333333',
            updatedAt: '2026-03-05T10:10:00'
          },
          {
            communityId: rootCommunity.id,
            scope: 'CREATE_OFFICIAL_UPDATE',
            allowedRoles: ['COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
            updatedBy: '33333333-3333-3333-3333-333333333333',
            updatedAt: '2026-03-05T10:10:00'
          },
          {
            communityId: rootCommunity.id,
            scope: 'UPDATE_OFFICIAL_UPDATE',
            allowedRoles: ['COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
            updatedBy: '33333333-3333-3333-3333-333333333333',
            updatedAt: '2026-03-05T10:10:00'
          },
          {
            communityId: rootCommunity.id,
            scope: 'MANAGE_MEMBERSHIPS',
            allowedRoles: ['COORDINATOR'],
            updatedBy: '33333333-3333-3333-3333-333333333333',
            updatedAt: '2026-03-05T10:10:00'
          },
          {
            communityId: rootCommunity.id,
            scope: 'MANAGE_PERMISSION_POLICIES',
            allowedRoles: ['COORDINATOR'],
            updatedBy: '33333333-3333-3333-3333-333333333333',
            updatedAt: '2026-03-05T10:10:00'
          },
          {
            communityId: rootCommunity.id,
            scope: 'VIEW_SENSITIVE_DATA',
            allowedRoles: ['COORDINATOR', 'PUBLIC_SERVANT_LIAISON'],
            updatedBy: '33333333-3333-3333-3333-333333333333',
            updatedAt: '2026-03-05T10:10:00'
          }
        ])
      });
    });

    await page.goto('http://127.0.0.1:5173/communities');

    await expect(page.getByTestId('community-permission-card')).toContainText('Community policies');
    await page.getByTestId('community-permission-toggle-CREATE_THREAD-MEMBER').click();
    await page.getByTestId('community-permission-save-button').click();

    await expect.poll(() => savedPayload).not.toBeNull();
    expect(savedPayload).toMatchObject({
      policies: expect.arrayContaining([
        expect.objectContaining({
          scope: 'CREATE_THREAD',
          allowedRoles: ['MODERATOR', 'COORDINATOR', 'PUBLIC_SERVANT_LIAISON']
        })
      ])
    });
  });
});
