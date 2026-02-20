import { test, expect } from '@playwright/test';

test('Report form UX validation - Sliders and Dropdowns', async ({ page }) => {
  // Login flow first (mocked or seeded user)
  await page.goto('/login');
  await page.fill('input[placeholder="Username"]', 'citizen_jane');
  await page.fill('input[placeholder="Password"]', 'password');
  await page.click('button[type="submit"]');
  
  // Navigate to report
  await page.click('[data-testid="report-issue-button"]');
  await expect(page).toHaveURL('/report');

  // Fill text fields
  await page.fill('input#title', 'Dangerous Pothole on Main St');
  await page.fill('textarea#description', 'There is a massive pothole that is damaging cars and causing traffic jams. It needs immediate attention.');

  // Interact with Dropdown (PrimeReact structure)
  await page.click('#category'); // Open dropdown
  await page.click('li[aria-label="Safety"]'); // Select option

  // Interact with Sliders
  // PrimeReact sliders are divs with role="slider" usually, or inputs hidden.
  // We can click on the track to change value.
  const urgencySlider = page.locator('.field:has-text("Urgency") .p-slider');
  await urgencySlider.click({ position: { x: 200, y: 5 } }); // Click towards right to increase

  const impactSlider = page.locator('.field:has-text("Impact") .p-slider');
  await impactSlider.click({ position: { x: 10, y: 5 } }); // Click left (low)

  // Submit
  await page.click('[data-testid="report-submit-button"]');

  // Expect redirect or success toast
  await expect(page).toHaveURL('/');
  await expect(page.locator('.p-toast')).toBeVisible();
});
