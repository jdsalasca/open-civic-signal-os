import { test, expect } from '@playwright/test';

test('dashboard visual audit', async ({ page }) => {
  await page.goto('http://localhost:3002/');
  
  // Wait for the app to load
  await page.waitForSelector('.page');
  
  // Verify main title exists
  const title = page.locator('h1');
  await expect(title).toContainText('Open Civic Signal OS');
  
  // Take a screenshot for the report
  await page.screenshot({ path: 'dashboard-audit.png', fullPage: true });
  
  console.log('Visual audit complete. Screenshot saved as dashboard-audit.png');
});
