import { expect, test } from '@playwright/test';

const communityId = '11111111-1111-1111-1111-111111111111';

test.describe('Community official announcements channel', () => {
  test('shows pinned announcements separately and supports archive search', async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem(
        'auth-storage',
        JSON.stringify({
          state: {
            accessToken: 'test-token',
            userName: 'servant',
            activeRole: 'PUBLIC_SERVANT',
            rawRoles: ['PUBLIC_SERVANT', 'CITIZEN'],
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
                userId: '22222222-2222-2222-2222-222222222222',
                communityId,
                communityName: 'Los Rosales',
                communitySlug: 'rosalistas',
                parentCommunityId: null,
                breadcrumb: [{ id: communityId, name: 'Los Rosales', slug: 'rosalistas' }],
                role: 'PUBLIC_SERVANT_LIAISON',
                createdBy: '22222222-2222-2222-2222-222222222222',
                createdAt: '2026-03-05T10:06:00'
              }
            ],
            threadListStateByCommunity: {}
          },
          version: 0
        })
      );
    });

    await page.route('**/api/community/blog/comments/count**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          'pinned-post': 0,
          'timeline-post': 0
        })
      });
    });

    await page.route('**/api/community/blog/archive**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'archived-post',
            communityId,
            authorId: '22222222-2222-2222-2222-222222222222',
            authorUsername: 'servant',
            authorRole: 'ROLE_PUBLIC_SERVANT,ROLE_CITIZEN',
            official: true,
            pinned: false,
            title: 'Archived transit reroute',
            content: 'Previous detour communication for the transit corridor.',
            statusTag: 'COMPLETED',
            reactions: {},
            viewerReaction: null,
            archivedBy: '22222222-2222-2222-2222-222222222222',
            archivedAt: '2026-03-01T08:00:00',
            publishedAt: '2026-02-26T08:00:00',
            updatedAt: '2026-03-01T08:00:00'
          }
        ])
      });
    });

    await page.route('**/api/community/blog?communityId=*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'pinned-post',
            communityId,
            authorId: '22222222-2222-2222-2222-222222222222',
            authorUsername: 'servant',
            authorRole: 'ROLE_PUBLIC_SERVANT,ROLE_CITIZEN',
            official: true,
            pinned: true,
            title: 'Water interruption notice',
            content: 'Official maintenance window this Saturday.',
            statusTag: 'PLANNED',
            reactions: {},
            viewerReaction: null,
            archivedBy: null,
            archivedAt: null,
            publishedAt: '2026-03-05T08:00:00',
            updatedAt: '2026-03-05T08:00:00'
          },
          {
            id: 'timeline-post',
            communityId,
            authorId: '22222222-2222-2222-2222-222222222222',
            authorUsername: 'servant',
            authorRole: 'ROLE_PUBLIC_SERVANT,ROLE_CITIZEN',
            official: true,
            pinned: false,
            title: 'Road work update',
            content: 'Repair crews resumed work today.',
            statusTag: 'IN_PROGRESS',
            reactions: {},
            viewerReaction: null,
            archivedBy: null,
            archivedAt: null,
            publishedAt: '2026-03-04T08:00:00',
            updatedAt: '2026-03-04T08:00:00'
          }
        ])
      });
    });

    await page.goto('http://127.0.0.1:5173/communities/blog');

    await expect(page.getByTestId('pinned-announcements-section')).toContainText('Water interruption notice');
    await expect(page.getByTestId('official-timeline-section')).toContainText('Road work update');
    await expect(page.getByTestId('official-archive-section')).toContainText('Archived transit reroute');

    await page.getByTestId('blog-archive-search-input').fill('reroute');
    await page.getByTestId('blog-archive-search-button').click();

    await expect(page.getByTestId('archived-blog-post-archived-post')).toContainText('Archived transit reroute');
    await expect(page.getByText('Official')).toBeVisible();
    await expect(page.getByText('Pinned')).toBeVisible();
  });
});
