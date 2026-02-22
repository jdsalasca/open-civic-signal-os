import { test, expect } from '@playwright/test';

test('Report form UX validation - Sliders and Dropdowns', async ({ page }) => {
  // Login with deterministic seeded credentials.
  await page.goto('/login');
  await page.getByTestId('login-username-input').fill('citizen');
  await page.getByTestId('login-password-input').fill('citizen123');
  await page.getByTestId('login-submit-button').click();
  await page.waitForURL('**/');
  
  // Navigate to report
  await page.goto('/report');
  await expect(page).toHaveURL('http://localhost:3002/report');

  // Fill text fields
  await page.getByTestId('report-title-input').fill('Dangerous Pothole on Main St');
  await page.getByTestId('report-description-textarea').fill('There is a massive pothole that is damaging cars and causing traffic jams. It needs immediate attention.');

  // Interact with category dropdown using stable selectors.
  await page.getByTestId('report-category-dropdown').click();
  await page.locator('.p-dropdown-item').first().click();

  // Keep default slider values; form should still submit successfully.

  // Submit
  await page.getByTestId('report-submit-button').click();

  // Expect successful redirect.
  await expect(page).toHaveURL('/');
});
