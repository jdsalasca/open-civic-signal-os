import { expect, test, type Page } from '@playwright/test';
import { mockAppBootstrap } from './helpers/session';
import { mockHelpCenter } from './helpers/dashboard';

/**
 * The duplicate review screen has two jobs, and the second one is easy to get wrong.
 *
 * It must let a person decide. And it must send back **exactly the candidates it displayed**,
 * because the backend stores that payload verbatim as the record of what the decision was based on.
 * A screen that recomputed or trimmed the list would make the audit trail describe something the
 * reviewer never saw.
 */
async function seedSession(page: Page) {
  await page.addInitScript(() => {
    window.localStorage.setItem(
      'auth-storage',
      JSON.stringify({
        state: {
          accessToken: 'test-token',
          userName: 'moderator',
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
              role: 'MODERATOR',
            },
          ],
        },
        version: 0,
      }),
    );
  });
}

const suggestion = {
  targetSignalId: 'sig-target',
  targetTitle: 'Streetlight out on the main corridor',
  targetCategory: 'infrastructure',
  candidates: [
    {
      signalId: 'sig-dup',
      title: 'Streetlight out on Main Corridor',
      category: 'infrastructure',
      status: 'NEW',
      reportedAt: '2026-03-02T09:00:00',
      similarity: 1.0,
    },
  ],
  threshold: 0.75,
  latestDecision: null,
  reviewedAt: null,
};

test.describe('Duplicate review', () => {
  test('shows the suggestion with the score that justified it', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);

    await page.route('**/api/signals/merge-review/suggestions*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([suggestion]),
      });
    });
    await page.route('**/api/signals/merge-review/history*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/communities/merge-review');

    await expect(page.getByTestId('merge-review-list')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('merge-target-title-0')).toContainText('Streetlight out on the main corridor');
    await expect(page.getByTestId('merge-candidate-sig-dup')).toContainText('Streetlight out on Main Corridor');
    // A reviewer deciding whether two reports are the same complaint needs the score, not just a flag.
    await expect(page.getByTestId('merge-similarity-sig-dup')).toContainText('1.00');
  });

  test('approving sends back exactly the candidates it displayed', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);

    let decisionBody: Record<string, unknown> | null = null;
    await page.route('**/api/signals/merge-review/decisions*', async (route) => {
      decisionBody = JSON.parse(route.request().postData() ?? '{}');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          decisionId: 'd1',
          targetSignalId: 'sig-target',
          targetTitle: 'Streetlight out on the main corridor',
          decision: 'APPROVED',
          suggestedCount: 1,
          mergedTargetSignalId: 'sig-target',
          mergedTitle: 'Streetlight out on the main corridor',
          decidedAt: '2026-03-10T10:00:00',
          note: 'Same streetlight, same week.',
        }),
      });
    });
    await page.route('**/api/signals/merge-review/suggestions*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([suggestion]),
      });
    });
    await page.route('**/api/signals/merge-review/history*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/communities/merge-review');
    await expect(page.getByTestId('merge-review-list')).toBeVisible({ timeout: 30000 });

    await page.getByTestId('merge-note-0').fill('Same streetlight, same week.');
    await page.getByTestId('merge-approve-0').click();

    await expect.poll(() => decisionBody !== null, { timeout: 15000 }).toBe(true);
    expect(decisionBody!.decision).toBe('APPROVED');
    expect(decisionBody!.targetSignalId).toBe('sig-target');
    expect(decisionBody!.threshold).toBe(0.75);
    expect(decisionBody!.note).toBe('Same streetlight, same week.');

    // The audit trail depends on this: the payload must be the candidates as displayed.
    const sent = decisionBody!.suggestedSimilarities as Array<Record<string, unknown>>;
    expect(sent).toHaveLength(1);
    expect(sent[0].signalId).toBe('sig-dup');
    expect(sent[0].similarity).toBe(1.0);
    expect(sent[0].title).toBe('Streetlight out on Main Corridor');
  });

  test('rejecting records a decision without claiming a merge', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);

    let decisionBody: Record<string, unknown> | null = null;
    await page.route('**/api/signals/merge-review/decisions*', async (route) => {
      decisionBody = JSON.parse(route.request().postData() ?? '{}');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          decisionId: 'd2',
          targetSignalId: 'sig-target',
          targetTitle: null,
          decision: 'REJECTED',
          suggestedCount: 1,
          mergedTargetSignalId: null,
          mergedTitle: null,
          decidedAt: '2026-03-10T10:00:00',
          note: 'Different blocks.',
        }),
      });
    });
    await page.route('**/api/signals/merge-review/suggestions*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([suggestion]),
      });
    });
    await page.route('**/api/signals/merge-review/history*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/communities/merge-review');
    await expect(page.getByTestId('merge-review-list')).toBeVisible({ timeout: 30000 });

    await page.getByTestId('merge-reject-0').click();

    await expect.poll(() => decisionBody !== null, { timeout: 15000 }).toBe(true);
    expect(decisionBody!.decision).toBe('REJECTED');
  });

  test('a suggestion already decided stays visible carrying its decision', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);

    await page.route('**/api/signals/merge-review/suggestions*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ ...suggestion, latestDecision: 'REJECTED' }]),
      });
    });
    await page.route('**/api/signals/merge-review/history*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/communities/merge-review');

    // Hiding it would make the queue look permanently unfinished every time someone correctly
    // said "these are not the same".
    await expect(page.getByTestId('merge-latest-decision-0')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('merge-latest-decision-0')).toContainText('Kept separate');
  });

  test('an empty queue says so rather than showing a broken list', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);

    await page.route('**/api/signals/merge-review/suggestions*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });
    await page.route('**/api/signals/merge-review/history*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/communities/merge-review');
    await expect(page.getByTestId('merge-review-empty')).toBeVisible({ timeout: 30000 });
  });

  test('the history records what was decided', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);

    await page.route('**/api/signals/merge-review/suggestions*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });
    await page.route('**/api/signals/merge-review/history*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            decisionId: 'd3',
            targetSignalId: 'sig-target',
            targetTitle: null,
            decision: 'APPROVED',
            suggestedCount: 1,
            mergedTargetSignalId: 'sig-target',
            mergedTitle: null,
            decidedAt: '2026-03-10T10:00:00',
            note: 'Same streetlight.',
          },
        ]),
      });
    });

    await page.goto('/communities/merge-review');
    await expect(page.getByTestId('merge-review-history')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('merge-history-d3')).toContainText('Merged');
    await expect(page.getByTestId('merge-history-d3')).toContainText('Same streetlight.');
  });

  test('an unreachable queue shows an unavailable state, not a broken list', async ({ page }) => {
    await seedSession(page);
    await mockAppBootstrap(page, "11111111-2222-3333-4444-555555555555");
    await mockHelpCenter(page);

    await page.route('**/api/signals/merge-review/suggestions*', async (route) => route.abort('failed'));
    await page.route('**/api/signals/merge-review/history*', async (route) => route.abort('failed'));

    await page.goto('/communities/merge-review');
    await expect(page.getByTestId('merge-review-unavailable')).toBeVisible({ timeout: 30000 });
  });
});
