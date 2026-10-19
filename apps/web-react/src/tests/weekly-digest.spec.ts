import { expect, test, type Page } from '@playwright/test';
import { mockAppBootstrap } from './helpers/session';
import { mockHelpCenter } from './helpers/dashboard';

/**
 * The digest view exists because the dashboard sidebar of the same name is not the digest.
 *
 * That sidebar sorts the signals already on screen and shows the top three. It does not know the
 * week, it does not carry the counts, and it cannot be published. This screen does, and the thing it
 * must make obvious is whether the week has been published: a prepared digest nobody published is
 * the failure mode the scheduler deliberately leaves to a person.
 */
async function seedSession(page: Page) {
  await page.addInitScript(() => {
    window.localStorage.setItem(
      'auth-storage',
      JSON.stringify({
        state: {
          accessToken: 'test-token',
          userName: 'liaison',
          activeRole: 'PUBLIC_SERVANT',
          rawRoles: ['PUBLIC_SERVANT', 'CITIZEN'],
          isLoggedIn: true,
          isHydrated: true,
        },
        version: 0,
      }),
    );
    window.localStorage.setItem(
      'community-storage',
      JSON.stringify({
        state: {
          activeCommunityId: '11111111-2222-3333-4444-555555555555',
          memberships: [
            {
              communityId: '11111111-2222-3333-4444-555555555555',
              communityName: 'Riverside District',
              communitySlug: 'riverside-district',
              parentCommunityId: null,
              breadcrumb: [
                {
                  id: '11111111-2222-3333-4444-555555555555',
                  name: 'Riverside District',
                  slug: 'riverside-district',
                },
              ],
              role: 'COORDINATOR',
            },
          ],
        },
        version: 0,
      }),
    );
  });
}

const digest = {
  version: 'v1',
  communityId: '11111111-2222-3333-4444-555555555555',
  communityName: 'Riverside District',
  week: {
    key: '2026-W13',
    startDate: '2026-03-23',
    endDate: '2026-03-30',
    previousKey: '2026-W12',
  },
  topUnresolved: [
    {
      signalId: 'sig-1',
      title: 'Water main break',
      category: 'utilities',
      status: 'NEW',
      locationLabel: 'Calle 12',
      priorityScore: 313.0,
      daysOpen: 4,
      whyRanked: 'urgency 5/5, impact 5/5, 900 people affected, 40 community votes',
    },
  ],
  resolvedThisWeek: 3,
  rejectedThisWeek: 1,
  reportedThisWeek: 7,
  stillOpenTotal: 12,
  body: '# Riverside District: weekly digest 2026-W13\n\n7 report(s) came in, 3 were resolved.',
  contentHash: 'abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789',
  published: false,
  publishedAt: null,
  generatedAt: '2026-03-30T06:00:00',
  deliveredToChannels: 0,
};

async function stubDigest(page: Page, overrides: Record<string, unknown> = {}) {
  await page.route('**/api/community/weekly-digest?*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ...digest, ...overrides }),
    });
  });
  await page.route('**/api/community/weekly-digest/schedule-history*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          communityId: '11111111-2222-3333-4444-555555555555',
          communityName: 'Riverside District',
          weekKey: '2026-W13',
          outcome: 'PREPARED',
          detail: 'Digest ready with 1 item(s). Publish it deliberately; the scheduler does not send.',
          ranAt: '2026-03-30T06:00:00',
        },
      ]),
    });
  });
}

test.describe('Weekly digest view', () => {
  test('shows the bulletin body and the counts', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);
    await stubDigest(page);

    await page.goto('/communities/weekly-digest');

    await expect(page.getByTestId('weekly-digest-body')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('weekly-digest-body')).toContainText('weekly digest 2026-W13');
    await expect(page.getByTestId('weekly-digest-week-badge')).toContainText('2026-W13');
    await expect(page.getByTestId('weekly-digest-reported')).toContainText('7');
    await expect(page.getByTestId('weekly-digest-resolved')).toContainText('3');
    await expect(page.getByTestId('weekly-digest-rejected')).toContainText('1');
    await expect(page.getByTestId('weekly-digest-open')).toContainText('12');
    // The hash is what lets a recipient verify what they received.
    await expect(page.getByTestId('weekly-digest-hash')).toContainText('abcdef0123456789');
  });

  test('an unpublished week says so and offers a publish control', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);
    await stubDigest(page, { published: false });

    await page.goto('/communities/weekly-digest');

    // A prepared digest nobody published is the failure mode the scheduler leaves to a person.
    await expect(page.getByTestId('weekly-digest-published-badge')).toContainText('Not published yet');
    await expect(page.getByTestId('weekly-digest-publish')).toBeVisible();
  });

  test('a published week says so and does not offer to publish again', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);
    await stubDigest(page, { published: true, deliveredToChannels: 2 });

    await page.goto('/communities/weekly-digest');

    await expect(page.getByTestId('weekly-digest-published-badge')).toContainText('Published');
    await expect(page.getByTestId('weekly-digest-delivered-badge')).toContainText('2');
    // The API refuses a second publish for the same week, so the control should not be offered.
    await expect(page.getByTestId('weekly-digest-publish')).toHaveCount(0);
  });

  test('the scheduler history shows what happened and why', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);
    await stubDigest(page);

    await page.goto('/communities/weekly-digest');

    await expect(page.getByTestId('weekly-digest-schedule-list')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('weekly-digest-run-2026-W13')).toContainText('Prepared');
    // The detail says the scheduler does not send, which is the accountability statement.
    await expect(page.getByTestId('weekly-digest-run-2026-W13')).toContainText('does not send');
  });

  test('publishing sends the week and reloads', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);
    await stubDigest(page, { published: false });

    let publishedUrl = '';
    await page.route('**/api/community/weekly-digest/publish*', async (route) => {
      publishedUrl = route.request().url();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ ...digest, published: true, deliveredToChannels: 1 }),
      });
    });

    await page.goto('/communities/weekly-digest');
    await expect(page.getByTestId('weekly-digest-publish')).toBeVisible({ timeout: 30000 });
    await page.getByTestId('weekly-digest-publish').click();

    await expect.poll(() => publishedUrl !== '', { timeout: 15000 }).toBe(true);
    // The week being published is the one on screen, not a default.
    expect(publishedUrl).toContain('week=2026-W13');
  });

  test('an unreachable API shows an unavailable state', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);
    await page.route('**/api/community/weekly-digest?*', async (route) => route.abort('failed'));
    await page.route('**/api/community/weekly-digest/schedule-history*', async (route) => route.abort('failed'));

    await page.goto('/communities/weekly-digest');
    await expect(page.getByTestId('weekly-digest-unavailable')).toBeVisible({ timeout: 30000 });
  });
});
