import type { Page } from '@playwright/test';

/**
 * The two requests every authenticated screen fires before it renders anything of its own, and the
 * seeded session that gets it there.
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

/** Persist a session directly. AuthGuard only reads the store, so no login request is needed. */
export async function seedSession(page: Page, role = 'PUBLIC_SERVANT') {
  await page.addInitScript((activeRole) => {
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
  }, role);
}

const json = (body: unknown) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

/**
 * Answer the routes the shared Layout fires. Domain routes still belong to each spec.
 *
 * `communities/my` returns an empty list on purpose: a community store that hydrates an active
 * community changes which routes the view calls, and a spec about the dashboard should not depend on
 * that. A spec that needs a community should pass its own membership.
 */
export async function mockAppBootstrap(page: Page) {
  await page.route('**/api/auth/me', (route) =>
    route.fulfill(json({ username: 'liaison', role: 'PUBLIC_SERVANT', interfaceMode: 'ADVANCED' })));
  await page.route('**/api/communities/my', (route) => route.fulfill(json([])));
}

/** Seed a session and answer the bootstrap routes: the one-liner a spec needs to reach its view. */
export async function seedAuthenticatedApp(page: Page, role = 'PUBLIC_SERVANT') {
  await seedSession(page, role);
  await mockAppBootstrap(page);
}