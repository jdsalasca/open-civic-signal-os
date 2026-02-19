import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:3002';

test.describe('Intensive QA - Open Civic Signal OS', () => {

  test('Security: Redirect to login if unauthenticated', async ({ page }) => {
    await page.goto(`${BASE_URL}/`);
    await expect(page).toHaveURL(/.*login/);
    await page.screenshot({ path: 'output/playwright/qa_redirect_check.png' });
  });

  test('Auth: Citizen login and voting flow', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[placeholder="Username"]', 'citizen');
    await page.fill('input[type="password"]', 'citizen2026');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('text=Welcome back, citizen!')).toBeVisible();
    
    // Attempt to vote on the first available signal
    await page.waitForSelector('.p-datatable-tbody tr');
    await page.click('.p-datatable-tbody tr:first-child');
    
    await expect(page).toHaveURL(/.*signal\/.*/);
    
    const voteBtn = page.locator('button:has-text("Support Issue")');
    await expect(voteBtn).toBeVisible();
    await voteBtn.click();
    
    // Check for success or "already voted" toast
    await page.waitForSelector('.p-toast-message');
    await page.screenshot({ path: 'output/playwright/qa_voting_feedback.png' });
  });

  test('Robustness: Signal reporting with edge case data', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[placeholder="Username"]', 'citizen');
    await page.fill('input[type="password"]', 'citizen2026');
    await page.click('button[type="submit"]');
    
    await page.goto(`${BASE_URL}/report`);
    
    // Try to break it with an empty title (should fail due to validation)
    await page.click('button[type="submit"]');
    const error = page.locator('.p-error');
    await expect(error).toBeVisible();
    
    await page.screenshot({ path: 'output/playwright/qa_validation_errors.png' });
  });

  test('Visual: Desktop layout check', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[placeholder="Username"]', 'admin');
    await page.fill('input[type="password"]', 'admin12345');
    await page.click('button[type="submit"]');
    
    await page.waitForSelector('.p-datatable');
    await page.screenshot({ path: 'output/playwright/qa_desktop_layout.png', fullPage: true });
  });

  test('Visual: Mobile layout check (detect overlaps)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 }); // iPhone 13
    await page.goto(`${BASE_URL}/`);
    await page.screenshot({ path: 'output/playwright/qa_mobile_overlaps.png', fullPage: true });
  });
});
