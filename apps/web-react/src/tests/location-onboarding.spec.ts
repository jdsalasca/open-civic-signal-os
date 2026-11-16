import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';

const json = (body: unknown) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

const city = {
  id: 'city-bogota',
  name: 'Bogotá',
  slug: 'bogota',
  country: 'Colombia',
  region: 'Cundinamarca',
  city: 'Bogotá',
  locality: null,
  neighborhood: null,
  memberCount: 2,
};

test.describe('Location-first onboarding', () => {
  test('a new account is placed on the nearest city and sees the full hierarchy', async ({ page }) => {
    await page.context().grantPermissions(['geolocation']);
    await page.context().setGeolocation({ latitude: 4.6533, longitude: -74.0836 });

    let joined = false;
    await seedAuthenticatedApp(page, 'CITIZEN');
    await page.route('**/api/communities/my', (route) =>
      route.fulfill(json(joined
        ? [{
            userId: 'u1',
            communityId: city.id,
            communityName: city.name,
            communitySlug: city.slug,
            parentCommunityId: null,
            breadcrumb: [{ id: city.id, name: city.name, slug: city.slug }],
            role: 'MEMBER',
            createdBy: 'u1',
            createdAt: '2026-11-16T12:00:00',
          }]
        : [])));
    await page.route('**/api/communities/public', (route) => route.fulfill(json([city])));
    await page.route('**/api/communities/nearest*', (route) => route.fulfill(json(city)));
    await page.route('**/api/places/reverse*', (route) =>
      route.fulfill(json({
        neighborhood: 'La Candelaria',
        locality: 'Santa Fe',
        city: 'Bogotá',
        region: 'Cundinamarca',
        country: 'Colombia',
      })));
    await page.route('**/api/communities/*/join', (route) => {
      joined = true;
      return route.fulfill(json({ id: city.id }));
    });
    await page.route('**/api/community/feed*', (route) => route.fulfill(json([])));

    await page.goto('/');
    await expect(page).toHaveURL(/\/onboarding/);
    await expect(page.getByTestId('onboarding-nearest')).toContainText('Bogotá');
    const hierarchy = page.getByTestId('onboarding-detected-place');
    await expect(hierarchy).toContainText('Barrio');
    await expect(hierarchy).toContainText('La Candelaria');
    await expect(hierarchy).toContainText('Localidad');
    await expect(hierarchy).toContainText('Santa Fe');
    await expect(hierarchy).toContainText('Municipio');
    await expect(hierarchy).toContainText('Departamento');
    await expect(hierarchy).toContainText('Cundinamarca');

    await page.getByTestId('onboarding-join-nearest-city-bogota').click();
    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByTestId('community-feed-composer')).toBeVisible();
  });
});
