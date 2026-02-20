import { test, expect } from '@playwright/test';

test.describe('Auth Edge Cases (P1)', () => {
  
  test('Should return 409 Conflict when registering duplicate email', async ({ page }) => {
    const duplicateEmail = 'admin-signalos@yopmail.com'; // Already exists from seeding
    const uniqueUser = `user_${Date.now()}`;

    await page.goto('/register');
    await page.locator('#username-input').fill(uniqueUser);
    await page.locator('#email-input').fill(duplicateEmail);
    await page.locator('#password-input').fill('SecurePass123!');
    
    // Trigger registration
    await page.getByTestId('register-submit-button').click();

    // Verify error toast or message (Backend returns 409)
    // We expect the GlobalExceptionHandler message: "Data integrity violation: Identity or resource already exists."
    await expect(page.getByText(/Identity already registered/i).or(page.getByText(/Data integrity violation/i))).toBeVisible();
    
    // Ensure we are still on register page
    await expect(page).toHaveURL(/.*register/);
  });
});
