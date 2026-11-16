import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';

test.describe('Offline support', () => {
  test('registers the worker, caches the shell and warns while offline', async ({ page, context }) => {
    await seedAuthenticatedApp(page, 'CITIZEN');
    await page.goto('/');

    const hasActiveWorker = await page.evaluate(async () => {
      if (!('serviceWorker' in navigator)) return false;
      const registration = await navigator.serviceWorker.ready;
      return Boolean(registration.active);
    });
    expect(hasActiveWorker).toBe(true);

    // The reload is the navigation the worker intercepts, and therefore the one that stores the shell.
    await page.reload();
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
