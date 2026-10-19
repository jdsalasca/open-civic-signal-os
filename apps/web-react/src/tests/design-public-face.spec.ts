import { expect, test, type Page } from '@playwright/test';

/**
 * The public face: the first screen a stranger sees.
 *
 * <p>These assertions are about the promise the front door makes before anyone signs in. The hero
 * has to show what the product is (a neighbourhood raising its signals), the two calls to action
 * have to be the two real paths in (start a city, or find yours), and the search field has to be
 * usable rather than a magnifier sitting on top of the placeholder. Nothing here needs a session,
 * so the whole suite runs against the static app.
 */
const cities = [
  {
    id: 'city-valle',
    name: 'Villa Verde',
    slug: 'villa-verde',
    description: 'A growing district community.',
    country: 'Colombia',
    region: 'Antioquia',
    parentCommunityId: null,
    memberCount: 3,
    createdAt: '2026-03-01T10:00:00',
  },
  {
    id: 'city-lomas',
    name: 'Las Lomas',
    slug: 'las-lomas',
    description: null,
    country: 'Mexico',
    region: null,
    parentCommunityId: null,
    memberCount: 1,
    createdAt: '2026-03-02T10:00:00',
  },
];

async function mockCityDirectory(page: Page) {
  await page.route('**/api/communities/public', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(cities) }),
  );
}

test.describe('Public landing', () => {
  test('opens with a hero, two real ways in, how it works and a trust strip', async ({ page }) => {
    await mockCityDirectory(page);
    await page.goto('/');

    await expect(page.getByTestId('landing-hero')).toBeVisible({ timeout: 30000 });

    const heroIllustration = page.getByTestId('landing-hero-illustration');
    await expect(heroIllustration).toBeVisible();
    expect((await heroIllustration.getAttribute('alt'))?.trim().length ?? 0).toBeGreaterThan(0);

    await expect(page.getByTestId('landing-create-city')).toBeVisible();
    await expect(page.getByTestId('landing-find-city')).toBeVisible();

    const steps = page.getByTestId('landing-step');
    await expect(steps).toHaveCount(3);

    await expect(page.getByTestId('landing-trust-strip')).toBeVisible();
  });

  test('the search magnifier never sits on top of the placeholder text', async ({ page }) => {
    await mockCityDirectory(page);
    await page.goto('/');

    const icon = page.getByTestId('landing-search-icon');
    const input = page.getByTestId('landing-search-input');
    await expect(input).toBeVisible({ timeout: 30000 });

    const iconBox = await icon.boundingBox();
    const inputBox = await input.boundingBox();
    const paddingLeft = await input.evaluate((el) => parseFloat(getComputedStyle(el).paddingLeft));

    expect(iconBox).not.toBeNull();
    expect(inputBox).not.toBeNull();
    // The placeholder starts where the input's own padding ends. The icon has to finish first.
    expect(iconBox!.x + iconBox!.width).toBeLessThanOrEqual(inputBox!.x + paddingLeft + 1);
  });

  test('the empty directory invites a first city with an illustration', async ({ page }) => {
    await page.route('**/api/communities/public', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
    );
    await page.goto('/');

    await expect(page.getByTestId('landing-empty-state')).toBeVisible({ timeout: 30000 });
    const illustration = page.getByTestId('landing-empty-illustration');
    await expect(illustration).toBeVisible();
    expect((await illustration.getAttribute('alt'))?.trim().length ?? 0).toBeGreaterThan(0);
  });

  test('serves the Spanish front door when the visitor chose Spanish', async ({ page }) => {
    await mockCityDirectory(page);
    await page.addInitScript(() =>
      window.localStorage.setItem('settings-storage', JSON.stringify({ state: { language: 'es' }, version: 0 })),
    );
    await page.goto('/');

    await expect(page.getByTestId('landing-hero')).toContainText('Tu ciudad', { timeout: 30000 });
    await expect(page.getByTestId('landing-find-city')).toContainText('Encuentra tu ciudad');
  });
});

test.describe('State illustrations', () => {
  test('a missing coordinate shows the paper map illustration', async ({ page }) => {
    await page.goto('/a-coordinate-that-does-not-exist');

    const illustration = page.getByTestId('not-found-illustration');
    await expect(illustration).toBeVisible({ timeout: 30000 });
    expect((await illustration.getAttribute('alt'))?.trim().length ?? 0).toBeGreaterThan(0);
  });

  test('a forbidden sector shows the locked door illustration', async ({ page }) => {
    await page.goto('/unauthorized');

    const illustration = page.getByTestId('unauthorized-illustration');
    await expect(illustration).toBeVisible({ timeout: 30000 });
    expect((await illustration.getAttribute('alt'))?.trim().length ?? 0).toBeGreaterThan(0);
  });
});

test.describe('Auth form polish', () => {
  test('login keeps explicit labels on both fields', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByTestId('login-card')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('login-username-input')).toBeVisible();
    await expect(page.getByTestId('login-password-input')).toBeVisible();
  });

  test('registration explains the password rule and the city picker', async ({ page }) => {
    await mockCityDirectory(page);
    await page.goto('/register');

    await expect(page.getByTestId('register-password-hint')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('register-city-help')).toBeVisible();
    await expect(page.getByTestId('register-city-select')).toBeVisible();
  });
});
