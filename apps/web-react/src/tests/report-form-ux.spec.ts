import { test, expect } from '@playwright/test';

test('Report form wizard supports structured intake', async ({ page }) => {
  const uniqueSuffix = `${Date.now()}-${test.info().project.name}`;

  // Login with deterministic seeded credentials.
  await page.goto('/login');
  await page.getByTestId('login-username-input').fill('citizen');
  await page.getByTestId('login-password-input').fill('citizen123');
  await page.getByTestId('login-submit-button').click();
  await page.waitForURL('**/');
  
  // Navigate to report
  await page.goto('/report');
  await expect(page).toHaveURL(/\/report$/);
  await expect(page.getByTestId('report-wizard-step-1')).toBeVisible();

  // Step 1: basics
  await page.getByTestId('report-title-input').fill(`Dangerous Pothole on Main St ${uniqueSuffix}`);
  await page.getByTestId('report-location-input').fill('Main street entrance near block 4');
  await page.getByTestId('report-category-dropdown').click();
  await page.locator('.p-dropdown-item').first().click();
  await page.getByTestId('report-next-button').click();

  // Step 2: evidence and context
  await expect(page.getByTestId('report-wizard-step-2')).toBeVisible();
  await page.getByTestId('report-description-textarea').fill('There is a massive pothole that is damaging cars and causing traffic jams. It needs immediate attention.');
  await page.getByTestId('report-evidence-input-0').fill('https://example.com/evidence-pothole-1.jpg');
  await page.getByTestId('report-add-evidence-button').click();
  await page.getByTestId('report-evidence-input-1').fill('https://example.com/evidence-pothole-2.jpg');
  await page.getByTestId('report-next-button').click();

  // Step 3: keep default scoring and submit
  await expect(page.getByTestId('report-wizard-step-3')).toBeVisible();

  // Submit
  await expect(page.getByTestId('report-submit-button')).toBeEnabled();
  await page.getByTestId('report-submit-button').click();

  // Expect explicit post-submit trust loop instead of a blind redirect.
  await expect(page.getByTestId('report-success-card')).toBeVisible();
  await expect(page.getByTestId('report-success-step-review')).toBeVisible();
  await expect(page.getByTestId('report-success-step-priority')).toBeVisible();
  await expect(page.getByTestId('report-success-step-followup')).toBeVisible();
  await expect(page.getByTestId('report-success-go-mine')).toBeVisible();
});
