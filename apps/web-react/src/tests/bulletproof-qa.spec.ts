import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:3002';

test.describe('Bulletproof QA - Open Civic Signal OS', () => {

  test('Security: Mandatory Login Redirection', async ({ page }) => {
    await page.goto(`${BASE_URL}/`);
    await page.waitForURL(/.*login/);
    await expect(page.locator('h1')).toContainText('Access Portal');
  });

  test('Auth: Full Citizen Lifecycle (Register -> Login -> Report -> Vote)', async ({ page }) => {
    const testUser = `qa_citizen_${Date.now()}`;
    
    // 1. Register
    await page.goto(`${BASE_URL}/register`);
    await page.fill('#username', testUser);
    await page.fill('#email', `${testUser}@example.com`);
    await page.fill('#password input', 'Password123!');
    await page.click('button[type="submit"]');
    
    await page.waitForURL(/.*login/);
    
    // 2. Login
    await page.fill('#username', testUser);
    await page.fill('#password input', 'Password123!');
    await page.click('button[type="submit"]');
    
    await page.waitForURL(`${BASE_URL}/`);
    await expect(page.locator('.text-5xl')).toContainText('Governance');

    // 3. Report Issue
    await page.click('text=Report New Issue');
    await page.waitForURL(/.*report/);
    await page.fill('#title', `QA Automation Signal ${testUser}`);
    await page.fill('#description', 'This is an automated test signal for quality assurance.');
    await page.click('button[type="submit"]');
    
    await page.waitForURL(`${BASE_URL}/`);
    
    // 4. Vote/Support
    // Wait for the table to load the new record
    await page.waitForSelector(`text=${testUser}`);
    await page.click(`text=${testUser}`);
    await page.waitForURL(/.*signal\/.*/);
    
    const supportBtn = page.locator('button:has-text("Support this Issue")');
    await supportBtn.click();
    
    await expect(page.locator('.p-toast-message')).toBeVisible();
  });

  test('Security: RBAC Enforcement (Citizen cannot access Moderation)', async ({ page }) => {
    // Login as standard citizen
    await page.goto(`${BASE_URL}/login`);
    await page.fill('#username', 'citizen');
    await page.fill('#password input', 'citizen2026');
    await page.click('button[type="submit"]');
    
    // Try to force navigate to moderation
    await page.goto(`${BASE_URL}/moderation`);
    await page.waitForURL(/.*unauthorized/);
    await expect(page.locator('h1')).toContainText('Access Denied');
  });
});
