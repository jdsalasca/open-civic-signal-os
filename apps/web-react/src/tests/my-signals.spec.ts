import { expect, test } from '@playwright/test';
import { DEFAULT_COMMUNITY_ID, seedAuthenticatedApp } from './helpers/session';

const signal = (id: string, status: string, votes: number) => ({
  id,
  title: `Report ${id}`,
  description: 'Description',
  category: 'SAFETY',
  urgency: 3,
  impact: 3,
  affectedPeople: 10,
  communityVotes: votes,
  priorityScore: 42,
  status,
  createdAt: '2026-11-10T10:00:00',
});

test.describe('My reports', () => {
  test('follows every page so the history and its totals are complete', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: DEFAULT_COMMUNITY_ID });
    await page.route('**/api/signals/mine*', (route) => {
      const pageNumber = Number(new URL(route.request().url()).searchParams.get('page') ?? 0);
      const body =
        pageNumber === 0
          ? { content: [signal('a', 'RESOLVED', 3)], totalElements: 2, totalPages: 2, last: false }
          : { content: [signal('b', 'IN_PROGRESS', 4)], totalElements: 2, totalPages: 2, last: true };
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
    });

    await page.goto('/mine');

    await expect(page.getByTestId('mine-signals-table')).toContainText('Report a');
    await expect(page.getByTestId('mine-signals-table')).toContainText('Report b');
    await expect(page.getByTestId('mine-metric-total')).toContainText('2');
    await expect(page.getByTestId('mine-metric-resolved')).toContainText('1');
    await expect(page.getByTestId('mine-metric-votes')).toContainText('7');
  });

  test('an unreachable API is an error with retry, not an empty history', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: DEFAULT_COMMUNITY_ID });
    await page.route('**/api/signals/mine*', (route) => route.abort('failed'));

    await page.goto('/mine');

    await expect(page.getByTestId('mine-signals-error')).toBeVisible();
    await expect(page.getByText('You have not submitted any reports yet')).toHaveCount(0);
  });
});
