import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:3002';

test.describe('Bulletproof QA - Debugging', () => {

  test('Auth: Detailed Citizen Lifecycle with Logging', async ({ page }) => {
    // Capture browser console logs
    page.on('console', msg => console.log(`[BROWSER] ${msg.type()}: ${msg.text()}`));
    page.on('request', request => console.log(`[REQUEST] ${request.method()} ${request.url()}`));
    page.on('response', response => console.log(`[RESPONSE] ${response.status()} ${response.url()}`));

    const testUser = `debug_user_${Date.now()}`;
    
    // 1. Register
    console.log('--- STARTING REGISTRATION ---');
    await page.goto(`${BASE_URL}/register`);
    await page.fill('#username', testUser);
    await page.fill('#email', `${testUser}@example.com`);
    await page.fill('#password input', 'Password123!');
    await page.click('button[type="submit"]');
    
    // Wait for the redirect to login
    await page.waitForURL(/.*login/, { timeout: 10000 });
    console.log('--- REGISTRATION SUCCESSFUL ---');
    
    // 2. Login
    console.log('--- STARTING LOGIN ---');
    await page.fill('#username', testUser);
    await page.fill('#password input', 'Password123!');
    await page.click('button[type="submit"]');
    
    // Wait for the dashboard load
    await page.waitForURL(`${BASE_URL}/`, { timeout: 10000 });
    console.log('--- LOGIN SUCCESSFUL ---');
    
    await expect(page.locator('.text-5xl')).toContainText('Governance');

    // 3. Report Issue
    console.log('--- STARTING REPORT ---');
    await page.click('text=Report New Issue');
    await page.waitForURL(/.*report/);
    await page.fill('#title', `QA Debug Signal ${testUser}`);
    await page.fill('#description', 'This is a debug test signal.');
    await page.click('button[type="submit"]');
    
    await page.waitForURL(`${BASE_URL}/`, { timeout: 10000 });
    console.log('--- REPORT SUCCESSFUL ---');
    
    // 4. Verification in Table
    console.log('--- STARTING VERIFICATION ---');
    await page.waitForSelector(`text=${testUser}`, { timeout: 10000 });
    console.log('--- SIGNAL FOUND IN TABLE ---');
  });
});
