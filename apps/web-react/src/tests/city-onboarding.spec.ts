import { expect, test, type Page } from '@playwright/test';

/**
 * Registering has to leave a person attached to a city, and the front door has to show the cities
 * that exist before anyone commits to registering.
 *
 * <p>These tests assert on the requests as much as the screen. The failure that matters is silent:
 * a registration form that shows a city picker but posts without the chosen city, or a landing page
 * that lists cities by calling an authenticated endpoint and therefore shows a login wall to the
 * stranger it was built for.
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

async function mockCityDirectory(page: Page, opts: { abort?: boolean } = {}) {
  const headers: Record<string, string>[] = [];
  await page.route('**/api/communities/public', async (route) => {
    headers.push(route.request().headers());
    if (opts.abort) {
      await route.abort('failed');
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(cities),
    });
  });
  return headers;
}

test.describe('Public city landing', () => {
  test('lists cities with member counts and a way in, without a session', async ({ page }) => {
    const requestHeaders = await mockCityDirectory(page);

    // No stored session: a stranger arriving at the root URL.
    await page.goto('/');

    await expect(page.getByTestId('landing-page')).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId('landing-city-card-city-valle')).toContainText('Villa Verde');
    await expect(page.getByTestId('landing-city-members-city-valle')).toContainText('3 members');
    await expect(page.getByTestId('landing-city-members-city-lomas')).toContainText('1 member');
    await expect(page.getByTestId('landing-register-cta')).toBeVisible();
    await expect(page.getByTestId('landing-create-city')).toBeVisible();

    // The directory is public, so the read must not carry a credential.
    for (const headers of requestHeaders) {
      expect(headers['authorization']).toBeUndefined();
    }
  });

  test('joining a city carries the chosen city into registration', async ({ page }) => {
    await mockCityDirectory(page);

    await page.goto('/');
    await expect(page.getByTestId('landing-city-card-city-valle')).toBeVisible({ timeout: 30000 });
    await page.getByTestId('landing-join-city-valle').click();

    await expect(page).toHaveURL(/\/register\?communityId=city-valle/);
  });

  test('an unreachable directory shows an unavailable note, not a login prompt', async ({ page }) => {
    await mockCityDirectory(page, { abort: true });

    await page.goto('/');
    await expect(page.getByTestId('landing-load-error')).toBeVisible({ timeout: 30000 });
    expect(page.url()).toContain('/');
  });
});

test.describe('Registration attaches the account to a city', () => {
  test('posts the selected existing city', async ({ page }) => {
    await mockCityDirectory(page);
    let registerBody: Record<string, unknown> | null = null;
    await page.route('**/auth/register', async (route) => {
      registerBody = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'ok', username: 'new_citizen', emailDeliveryStatus: 'SENT' }),
      });
    });

    await page.goto('/register?communityId=city-valle');
    await page.getByTestId('register-username-input').fill('new_citizen');
    await page.getByTestId('register-email-input').fill('new_citizen@example.com');
    await page.getByTestId('register-password-input').fill('SecurePass123!');
    await page.getByTestId('register-confirm-password-input').fill('SecurePass123!');
    await page.getByTestId('register-submit-button').click();

    await expect(page).toHaveURL(/\/verify/);
    expect(registerBody).not.toBeNull();
    expect(registerBody!.communityId).toBe('city-valle');
    expect(registerBody!.communityName).toBeUndefined();
  });

  test('founds a new city and posts its name and country', async ({ page }) => {
    await mockCityDirectory(page);
    let registerBody: Record<string, unknown> | null = null;
    await page.route('**/auth/register', async (route) => {
      registerBody = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'ok', username: 'founder', emailDeliveryStatus: 'SENT' }),
      });
    });

    await page.goto('/register?mode=create');
    await page.getByTestId('register-city-mode-create').click();
    await page.getByTestId('register-city-name-input').fill('Villa Nueva');
    await page.getByTestId('register-city-country-input').fill('Peru');
    await page.getByTestId('register-city-region-input').fill('Lima');
    await page.getByTestId('register-username-input').fill('founder');
    await page.getByTestId('register-email-input').fill('founder@example.com');
    await page.getByTestId('register-password-input').fill('SecurePass123!');
    await page.getByTestId('register-confirm-password-input').fill('SecurePass123!');
    await page.getByTestId('register-submit-button').click();

    await expect(page).toHaveURL(/\/verify/);
    expect(registerBody).not.toBeNull();
    expect(registerBody!.communityName).toBe('Villa Nueva');
    expect(registerBody!.communityCountry).toBe('Peru');
    expect(registerBody!.communityRegion).toBe('Lima');
    expect(registerBody!.communityId).toBeUndefined();
  });

  test('registers without a city when the person skips it', async ({ page }) => {
    await mockCityDirectory(page);
    let registerBody: Record<string, unknown> | null = null;
    await page.route('**/auth/register', async (route) => {
      registerBody = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'ok', username: 'loner', emailDeliveryStatus: 'SENT' }),
      });
    });

    await page.goto('/register');
    await page.getByTestId('register-username-input').fill('loner');
    await page.getByTestId('register-email-input').fill('loner@example.com');
    await page.getByTestId('register-password-input').fill('SecurePass123!');
    await page.getByTestId('register-confirm-password-input').fill('SecurePass123!');
    await page.getByTestId('register-submit-button').click();

    await expect(page).toHaveURL(/\/verify/);
    expect(registerBody).not.toBeNull();
    expect(registerBody!.communityId).toBeUndefined();
    expect(registerBody!.communityName).toBeUndefined();
  });
});
