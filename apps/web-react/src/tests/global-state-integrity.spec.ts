import { expect, test } from '@playwright/test';

test.describe('Global State Integrity (P0)', () => {
  test('Buttons and route navigation must not reset language, theme, or community context', async ({ page }) => {
    const readCommunityState = async () =>
      page.evaluate(() => {
        const raw = localStorage.getItem('community-storage');
        if (!raw) {
          return { activeCommunityId: null as string | null, memberships: [] as Array<{ communityId: string }> };
        }
        const parsed = JSON.parse(raw).state ?? {};
        return {
          activeCommunityId: parsed.activeCommunityId ?? null,
          memberships: parsed.memberships ?? [],
        };
      });

    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();
    await page.waitForURL('**/');

    await page.goto('/settings');
    await page.locator('.p-selectbutton >> text=Español').click();
    await page.locator('.p-selectbutton >> text=Modo Claro').click();
    await expect(page.getByText('Configuración del Sistema')).toBeVisible();

    await page.goto('/report');
    await page.getByRole('button', { name: 'Descartar' }).click();
    await expect(page).toHaveURL(/.*\/$/);
    await expect(page.getByTestId('welcome-message')).toBeVisible();

    await page.goto('/settings');
    await expect(page.getByText('Configuración del Sistema')).toBeVisible();
    await expect(page.locator('.p-selectbutton .p-highlight').filter({ hasText: 'Modo Claro' })).toBeVisible();

    const initialState = await readCommunityState();
    const initialCommunityId = initialState.activeCommunityId;

    if (initialState.memberships.length > 1) {
      await page.evaluate(() => {
        const raw = localStorage.getItem('community-storage');
        if (!raw) return;
        const parsed = JSON.parse(raw);
        const memberships = parsed.state?.memberships ?? [];
        if (memberships.length < 2) return;
        parsed.state.activeCommunityId = memberships[1].communityId;
        parsed.state.membershipsLoadedAt = 0;
        localStorage.setItem('community-storage', JSON.stringify(parsed));
      });
      await page.reload();
    }

    const switchedState = await readCommunityState();
    const selectedCommunityId = switchedState.activeCommunityId ?? initialCommunityId;

    await page.goto('/communities/threads');
    await page.goto('/communities/blog');
    await page.goto('/communities/feed');
    await page.goto('/settings');

    await page.evaluate(() => {
      const raw = localStorage.getItem('community-storage');
      if (!raw) return;
      const parsed = JSON.parse(raw);
      parsed.state.membershipsLoadedAt = 0;
      localStorage.setItem('community-storage', JSON.stringify(parsed));
    });
    await page.goto('/communities/feed');

    const finalState = await readCommunityState();
    expect(finalState.activeCommunityId).toBe(selectedCommunityId);
  });
});
