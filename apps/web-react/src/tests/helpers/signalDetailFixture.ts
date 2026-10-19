import type { Page } from '@playwright/test';
import { seedAuthenticatedApp } from './session';

/**
 * Shared fixture for the signal detail screen.
 *
 * AuthGuard only reads the persisted store, so seeding localStorage is enough to reach the route
 * without a backend. Two details are load-bearing and cost an afternoon to rediscover:
 *
 *  1. `isHydrated: true` has to be in the seeded state, or the guard renders its progress bar and
 *     the assertions wait on a page that will never arrive.
 *  2. Every route the shared Layout fires has to be answered. An unmocked request is not a harmless
 *     gap: it reaches the live backend, the 401 survives a refresh attempt, and axios calls
 *     `logout()`, which wipes the seeded store and bounces the run back to /login. `communities/my`
 *     is the one that bites, because it is fired on every authenticated page.
 */
export const signalId = '22222222-2222-2222-2222-222222222222';

// The wire sends a Java LocalDateTime, i.e. no offset and no seconds.
export const createdAt = '2026-04-01T10:00:00';

export async function mockSignalDetail(page: Page, status = 'IN_PROGRESS') {
  await seedAuthenticatedApp(page);

  await page.route(`**/api/signals/${signalId}/comments`, (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }));

    await page.route(`**/api/signals/${signalId}/history`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: '44444444-4444-4444-4444-444444444444',
            signalId,
            eventType: 'STATUS_CHANGED',
            statusFrom: 'NEW',
            statusTo: 'IN_PROGRESS',
            changedBy: 'staff',
            assignedToUsername: null,
            reason: 'Inspection started this morning',
            createdAt,
          },
        ]),
      }));

    await page.route(`**/api/signals/${signalId}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: signalId,
          title: 'Broken pedestrian bridge railing',
          description: 'The railing is loose and children cross here every morning.',
          category: 'infrastructure',
          status,
          assignedToUsername: 'liaison',
          locationLabel: 'Pedestrian bridge by the sports field',
          evidenceUrls: ['https://example.com/bridge-1.jpg'],
          priorityScore: 198,
          scoreBreakdown: { urgency: 120, impact: 60, affectedPeople: 8, communityVotes: 10 },
          communityVotes: 12,
          reactions: {},
          explainabilitySummary: {
            version: 'v1',
            topFactors: [
              { key: 'urgency', contribution: 120 },
              { key: 'impact', contribution: 60 },
            ],
            summary: 'Top drivers: urgency (120.0), impact (60.0)',
          },
        }),
      }));

    await page.route('**/api/signals/formula', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          version: 'v1',
          formula: '(Urgency * 30) + (Impact * 25) + min(People/10, 30) + min(Votes/5, 15)',
          effectiveFrom: '2026-03-21',
          weights: [],
          cappedFactors: ['affectedPeople', 'communityVotes'],
          changeNote: 'Initial published formula.',
        }),
      }));
}