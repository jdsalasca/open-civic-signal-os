import { test, expect } from '@playwright/test';

test.describe('Signal OS - Infrastructure Stress Test (P0)', () => {
  
  test('Concurrent Registration Stress', async ({ page }) => {
    const runId = `${Date.now()}_${Math.floor(Math.random() * 1_000_000)}`;
    
    // Simulate multiple registration attempts rapidly
    for (let i = 0; i < 5; i++) {
      await page.goto('/register');
      await page.getByTestId('register-username-input').fill(`stressuser_${runId}_${i}`);
      await page.getByTestId('register-email-input').fill(`stress_${runId}_${i}@example.com`);
      await page.getByTestId('register-password-input').fill('Password123!');
      await page.getByTestId('register-confirm-password-input').fill('Password123!');
      
      // Submit without waiting for the previous one to finish fully
      await page.getByTestId('register-submit-button').click();
    }
    
    // The last one should at least land on verify
    await expect(page).toHaveURL(/\/verify/);
  });

  test('Auth Code Brute Force Mitigation (Visual)', async ({ page }) => {
    await page.goto('/register');
    const user = `brute_${Date.now()}_${Math.floor(Math.random() * 1_000_000)}`;
    await page.getByTestId('register-username-input').fill(user);
    await page.getByTestId('register-email-input').fill(`${user}@example.com`);
    await page.getByTestId('register-password-input').fill('Password123!');
    await page.getByTestId('register-confirm-password-input').fill('Password123!');
    await page.getByTestId('register-submit-button').click();

    await expect(page).toHaveURL(/\/verify/);

    // Rapidly enter wrong codes and ensure verification does not complete.
    const input = page.locator('#verify-code');
    for (let i = 0; i < 3; i++) {
      await input.fill('000000');
      await page.click('button[type="submit"]');
      await expect(page).toHaveURL(/\/verify/);
      await expect(page.locator('#verify-code')).toBeVisible();
    }
  });
});
