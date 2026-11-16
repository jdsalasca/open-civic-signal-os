import { expect, test } from '@playwright/test';
import { DEFAULT_COMMUNITY_ID, seedAuthenticatedApp } from './helpers/session';

test.describe('Offline support', () => {
  test('registers the worker, caches the shell and warns while offline', async ({ page, context }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: DEFAULT_COMMUNITY_ID });
    await page.goto('/');

    const hasActiveWorker = await page.evaluate(async () => {
      if (!('serviceWorker' in navigator)) return false;
      const registration = await navigator.serviceWorker.ready;
      return Boolean(registration.active);
    });
    expect(hasActiveWorker).toBe(true);

    // The reload is the navigation the worker intercepts, and therefore the one that stores the shell.
    // Waiting for network idle lets the lazy feed chunks finish before the worker is asked to serve
    // the same route offline.
    await page.reload({ waitUntil: 'networkidle' });
    const cachedPaths = await page.evaluate(async () => {
      const paths: string[] = [];
      for (const key of await caches.keys()) {
        const cache = await caches.open(key);
        for (const request of await cache.keys()) {
          paths.push(new URL(request.url).pathname);
        }
      }
      return paths;
    });
    expect(cachedPaths).toContain('/offline.html');
    expect(cachedPaths).toContain('/index.html');

    await context.setOffline(true);
    await expect(page.getByTestId('offline-banner')).toBeVisible({ timeout: 10000 });

    // A route the worker never fetched still opens: the cached shell answers the navigation.
    const offlineResponse = await page.goto('/communities/feed');
    expect(offlineResponse?.status()).toBe(200);
  });
});
