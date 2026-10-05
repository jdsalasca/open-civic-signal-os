import type { Page } from '@playwright/test';

/**
 * The requests every authenticated screen fires before it renders anything of its own, and the
 * seeded state that gets it there.
 *
 * This exists because of a failure mode that is invisible from the view under test. An unmocked
 * request is not a harmless gap: it reaches the live backend, the 401 survives a refresh attempt,
 * and `axios.ts` calls `logout()`. The seeded session is then wiped and the run lands on /login,
 * which reads as "the screen is broken" or "this needs a seeded backend" when neither is true.
 *
 * `auth/me` is the trap that catches people. `App.tsx` wraps that call in a try/catch and keeps the
 * local preference when it fails, so it looks handled - but the axios response interceptor runs first
 * and logs the session out before the catch is reached.
 */

export type SeededDataMode = 'full' | 'field';

/**
 * Persist a session directly. AuthGuard only reads the store, so no login request is needed.
 *
 * `dataMode` writes the settings store as well, because field mode is a promise about bytes and a
 * test that does not seed it is not testing field mode.
 */
export async function seedSession(page: Page, role = 'PUBLIC_SERVANT', dataMode?: SeededDataMode) {
  await page.addInitScript(
    ({ activeRole, mode }) => {
      window.localStorage.setItem(
        'auth-storage',
        JSON.stringify({
          state: {
            accessToken: 'test-token',
            userName: 'liaison',
            activeRole,
            rawRoles: activeRole === 'PUBLIC_SERVANT' ? ['PUBLIC_SERVANT', 'CITIZEN'] : [activeRole],
            isLoggedIn: true,
            isHydrated: true,
          },
          version: 0,
        }),
      );
      if (mode) {
        window.localStorage.setItem(
          'settings-storage',
          JSON.stringify({
            state: { language: 'en', theme: 'dark', interfaceMode: 'advanced', dataMode: mode },
            version: 0,
          }),
        );
      }
    },
    { activeRole: role, mode: dataMode },
  );
}

const json = (body: unknown) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

export const DEFAULT_COMMUNITY_ID = '11111111-1111-1111-1111-111111111111';

/**
 * Answer the routes the shared Layout fires. Domain routes still belong to each spec.
 *
 * `communities/my` returning an empty list is the default because a spec about the dashboard should
 * not depend on which routes a community-scoped view calls. But an empty list also makes the store
 * clear its active community, and a community-scoped view then renders nothing: that reads as a
 * broken component rather than as a missing fixture. Pass `communityId` for those.
 */
export async function mockAppBootstrap(page: Page, communityId?: string) {
  await page.route('**/api/auth/me', (route) =>
    route.fulfill(json({ username: 'liaison', role: 'PUBLIC_SERVANT', interfaceMode: 'ADVANCED' })));

  const memberships = communityId
    ? [
        {
          userId: '33333333-3333-3333-3333-333333333333',
          communityId,
          communityName: 'Los Rosales',
          communitySlug: 'los-rosales',
          parentCommunityId: null,
          breadcrumb: [{ id: communityId, name: 'Los Rosales', slug: 'los-rosales' }],
          role: 'MEMBER',
          createdBy: '33333333-3333-3333-3333-333333333333',
          createdAt: '2026-03-05T10:00:00',
        },
      ]
    : [];

  // No second call is needed: setMemberships keeps the current selection when it is still valid and
  // otherwise picks the first membership, so returning one is what makes a community-scoped view
  // render anything at all.
  await page.route('**/api/communities/my', (route) => route.fulfill(json(memberships)));
}

/** Seed a session and answer the bootstrap routes: the one-liner a spec needs to reach its view. */
export async function seedAuthenticatedApp(
  page: Page,
  role = 'PUBLIC_SERVANT',
  opts: { dataMode?: SeededDataMode; communityId?: string } = {},
) {
  await seedSession(page, role, opts.dataMode);
  await mockAppBootstrap(page, opts.communityId);
}