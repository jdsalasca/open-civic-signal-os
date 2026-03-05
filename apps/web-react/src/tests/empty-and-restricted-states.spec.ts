import { expect, test } from '@playwright/test';

test.describe('Action-oriented empty and restricted states', () => {
  test('community hub and unauthorized state explain the situation and offer a next action', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('citizen');
    await page.getByTestId('login-password-input').fill('citizen123');
    await page.getByTestId('login-submit-button').click();
    await page.waitForURL('**/');

    await page.goto('/communities');
    await expect(
      page.getByText('Community Hub').or(page.getByText('Centro de Comunidades'))
    ).toBeVisible();
    await expect(
      page.getByText('Join a community, create one if needed').or(
        page.getByText('Únase a una comunidad, cree una si hace falta')
      )
    ).toBeVisible();

    await page.goto('/moderation');
    await expect(page).toHaveURL(/.*(unauthorized|forbidden)/);
    await expect(page.getByTestId('unauthorized-card')).toBeVisible();
    await expect(page.getByTestId('unauthorized-guidance')).toBeVisible();
    await expect(page.getByTestId('unauthorized-go-settings')).toBeVisible();
    await expect(page.getByTestId('unauthorized-go-home')).toBeVisible();
  });
});
