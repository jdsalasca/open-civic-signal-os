import { expect, test } from '@playwright/test';

const communityId = '11111111-1111-1111-1111-111111111111';
const secondCommunityId = '22222222-2222-2222-2222-222222222222';

test.describe('Community geospatial map', () => {
  test('loads community map and sends real API filters for local and cross-community heat', async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem(
        'auth-storage',
        JSON.stringify({
          state: {
            accessToken: 'test-token',
            userName: 'mapper',
            activeRole: 'CITIZEN',
            rawRoles: ['CITIZEN'],
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
            activeCommunityId: '11111111-1111-1111-1111-111111111111',
            memberships: [
              {
                userId: '33333333-3333-3333-3333-333333333333',
                communityId: '11111111-1111-1111-1111-111111111111',
                communityName: 'Los Rosales',
                communitySlug: 'los-rosales',
                parentCommunityId: null,
                breadcrumb: [{ id: '11111111-1111-1111-1111-111111111111', name: 'Los Rosales', slug: 'los-rosales' }],
                role: 'MEMBER',
                createdBy: '33333333-3333-3333-3333-333333333333',
                createdAt: '2026-03-05T10:00:00'
              },
              {
                userId: '33333333-3333-3333-3333-333333333333',
                communityId: '22222222-2222-2222-2222-222222222222',
                communityName: 'Campus Norte',
                communitySlug: 'campus-norte',
                parentCommunityId: null,
                breadcrumb: [{ id: '22222222-2222-2222-2222-222222222222', name: 'Campus Norte', slug: 'campus-norte' }],
                role: 'MODERATOR',
                createdBy: '33333333-3333-3333-3333-333333333333',
                createdAt: '2026-03-05T10:00:00'
              }
            ],
            membershipsLoadedAt: Date.now(),
            threadListStateByCommunity: {}
          },
          version: 0
        })
      );
    });

    let lastMapQuery = '';
    let lastHeatQuery = '';

    await page.route('**/api/communities/my', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            userId: '33333333-3333-3333-3333-333333333333',
            communityId,
            communityName: 'Los Rosales',
            communitySlug: 'los-rosales',
            parentCommunityId: null,
            breadcrumb: [{ id: communityId, name: 'Los Rosales', slug: 'los-rosales' }],
            role: 'MEMBER',
            createdBy: '33333333-3333-3333-3333-333333333333',
            createdAt: '2026-03-05T10:00:00'
          },
          {
            userId: '33333333-3333-3333-3333-333333333333',
            communityId: secondCommunityId,
            communityName: 'Campus Norte',
            communitySlug: 'campus-norte',
            parentCommunityId: null,
            breadcrumb: [{ id: secondCommunityId, name: 'Campus Norte', slug: 'campus-norte' }],
            role: 'MODERATOR',
            createdBy: '33333333-3333-3333-3333-333333333333',
            createdAt: '2026-03-05T10:00:00'
          }
        ])
      });
    });

    await page.route('**/api/signals/map?*', async (route) => {
      lastMapQuery = route.request().url();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          communityId,
          communityName: 'Los Rosales',
          communitySlug: 'los-rosales',
          generatedAt: '2026-03-05T14:00:00',
          freshness: 'updated just now',
          filters: { category: 'SAFETY', statuses: ['NEW'], fromDate: '2026-03-01', toDate: '2026-03-05' },
          availableCategories: ['INFRASTRUCTURE', 'SAFETY'],
          availableStatuses: ['IN_REVIEW', 'NEW'],
          mappedSignalsCount: 2,
          unmappedSignalsCount: 1,
          cumulativeHeatScore: 510,
          points: [
            {
              signalId: 'aaaaaaa1-1111-1111-1111-111111111111',
              communityId,
              communityName: 'Los Rosales',
              title: 'Unsafe crossing',
              category: 'SAFETY',
              status: 'NEW',
              locationLabel: 'Main avenue',
              latitude: 4.61,
              longitude: -74.08,
              priorityScore: 250,
              heatWeight: 290,
              createdAt: '2026-03-05T13:00:00'
            },
            {
              signalId: 'aaaaaaa2-1111-1111-1111-111111111111',
              communityId,
              communityName: 'Los Rosales',
              title: 'Street light outage',
              category: 'SAFETY',
              status: 'IN_REVIEW',
              locationLabel: 'North gate',
              latitude: 4.612,
              longitude: -74.079,
              priorityScore: 210,
              heatWeight: 220,
              createdAt: '2026-03-05T12:00:00'
            }
          ],
          clusters: [
            {
              clusterKey: '230:120',
              communityId,
              communityName: 'Los Rosales',
              latitude: 4.611,
              longitude: -74.0795,
              signalCount: 2,
              cumulativePriorityScore: 460,
              primaryCategory: 'SAFETY',
              topSignalId: 'aaaaaaa1-1111-1111-1111-111111111111',
              topSignalTitle: 'Unsafe crossing'
            }
          ]
        })
      });
    });

    await page.route('**/api/signals/map/heat?*', async (route) => {
      lastHeatQuery = route.request().url();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          generatedAt: '2026-03-05T14:00:00',
          freshness: 'updated just now',
          filters: { category: 'SAFETY', statuses: ['NEW'], fromDate: '2026-03-01', toDate: '2026-03-05' },
          availableCategories: ['INFRASTRUCTURE', 'SAFETY'],
          availableStatuses: ['IN_REVIEW', 'NEW'],
          visibleCommunitiesCount: 2,
          totalMappedSignalsCount: 3,
          totalHeatScore: 720,
          communities: [
            {
              communityId,
              communityName: 'Los Rosales',
              communitySlug: 'los-rosales',
              latitude: 4.611,
              longitude: -74.0795,
              mappedSignalsCount: 2,
              unmappedSignalsCount: 1,
              cumulativeHeatScore: 510,
              averagePriorityScore: 230,
              topCategory: 'SAFETY',
              topSignalId: 'aaaaaaa1-1111-1111-1111-111111111111',
              topSignalTitle: 'Unsafe crossing'
            },
            {
              communityId: secondCommunityId,
              communityName: 'Campus Norte',
              communitySlug: 'campus-norte',
              latitude: 4.702,
              longitude: -74.033,
              mappedSignalsCount: 1,
              unmappedSignalsCount: 0,
              cumulativeHeatScore: 210,
              averagePriorityScore: 210,
              topCategory: 'INFRASTRUCTURE',
              topSignalId: 'bbbbbbb1-2222-2222-2222-222222222222',
              topSignalTitle: 'Bridge crack'
            }
          ]
        })
      });
    });

    await page.goto('http://127.0.0.1:5173/communities/map');

    await expect(page.getByTestId('community-map-community-card')).toContainText('Unsafe crossing');
    await expect(page.getByTestId('community-map-heat-card')).toContainText('Campus Norte');

    await page.getByTestId('community-map-category-filter').click();
    await page.getByRole('option', { name: 'SAFETY' }).click();
    await page.getByTestId('community-map-status-filter').click();
    await page.getByRole('option', { name: 'NEW' }).click();
    await page.getByTestId('community-map-from-date-filter').fill('2026-03-01');
    await page.getByTestId('community-map-to-date-filter').fill('2026-03-05');
    await page.getByTestId('community-map-apply-filters').click();

    await expect.poll(() => lastMapQuery).toContain(`communityId=${communityId}`);
    await expect.poll(() => lastMapQuery).toContain('category=SAFETY');
    await expect.poll(() => lastMapQuery).toContain('status=NEW');
    await expect.poll(() => lastMapQuery).toContain('fromDate=2026-03-01');
    await expect.poll(() => lastMapQuery).toContain('toDate=2026-03-05');
    await expect.poll(() => lastHeatQuery).toContain('category=SAFETY');
    await expect.poll(() => lastHeatQuery).toContain('status=NEW');
  });
});
