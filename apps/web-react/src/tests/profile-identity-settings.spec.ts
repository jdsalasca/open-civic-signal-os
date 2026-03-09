import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.getByTestId('login-username-input').fill('admin');
  await page.getByTestId('login-password-input').fill('admin12345');
  await page.getByTestId('login-submit-button').click();
  await page.waitForURL('**/');
}

test.describe('Profile Identity Settings', () => {
  test('user can save civic profile fields and visibility preferences', async ({ page }) => {
    await login(page);

    await page.goto('/settings');
    await expect(page.getByTestId('profile-settings-card')).toBeVisible();

    await page.getByTestId('profile-display-name-input').fill('Admin Civic Lead');
    await page.getByTestId('avatar-preset-harbor-light').click();
    await page.getByTestId('profile-affiliations-input').fill('Central Campus, Block A');
    await page.getByTestId('profile-affiliations-input').press('Enter');
    await page.getByTestId('profile-bio-input').fill('Coordinates community follow-up and accountability updates.');

    await page.getByTestId('profile-civic-role-select').locator('.p-dropdown').click();
    await page.getByText('Authority', { exact: true }).click();

    await page.getByTestId('profile-visibility-select').locator('.p-dropdown').click();
    await page.getByText('Shared community only', { exact: true }).click();

    await page.getByTestId('affiliation-visibility-select').locator('.p-dropdown').click();
    await page.getByText('Admins only', { exact: true }).click();

    await page.getByTestId('activity-visibility-select').locator('.p-dropdown').click();
    await page.getByText('Shared community only', { exact: true }).click();

    await page.getByTestId('save-profile-button').click();
    await expect(page.getByText('Public identity updated.')).toBeVisible();

    await page.reload();
    await expect(page.getByTestId('profile-display-name-input')).toHaveValue('Admin Civic Lead');
    await expect(page.getByTestId('profile-affiliations-chip-list')).toContainText('Block A');
    await expect(page.getByTestId('profile-affiliations-chip-list')).toContainText('Central Campus');
    await expect(page.getByTestId('profile-bio-input')).toHaveValue('Coordinates community follow-up and accountability updates.');
    await expect(page.getByTestId('profile-settings-card')).toContainText('Shared community only');
    await expect(page.getByTestId('profile-settings-card')).toContainText('Admins only');
    await expect(page.getByTestId('profile-settings-card')).toContainText('Authority');
    await expect(page.getByTestId('privacy-center-card')).toContainText('Sensitive-data access log');
    await expect(page.getByTestId('privacy-center-card')).toContainText('Shared community only');
    await expect(page.getByTestId('avatar-preset-harbor-light')).toHaveClass(/avatar-preset-option-selected/);
    await expect(page.getByTestId('profile-achievements-card')).toContainText('Ten incidents reported');
    await expect(page.getByTestId('settings-community-memberships-card')).toContainText('Community belonging');
    await expect(page.getByTestId('settings-community-memberships-card')).toContainText('Communities');
  });
});
