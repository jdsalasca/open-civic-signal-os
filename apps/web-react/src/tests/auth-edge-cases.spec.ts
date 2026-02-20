import { test, expect } from '@playwright/test';

test.describe('Auth Edge Cases (P1)', () => {
  
  test('Should return 409 Conflict when registering duplicate email', async ({ page }) => {
    const duplicateEmail = 'opencivicadmin@yopmail.com'; // Already exists from seeding
    const uniqueUser = `user_${Date.now()}`;

    await page.goto('/register');
    await page.getByTestId('register-username-input').fill(uniqueUser);
    await page.getByTestId('register-email-input').fill(duplicateEmail);
    await page.getByTestId('register-password-input').fill('SecurePass123!');
    await page.getByTestId('register-confirm-password-input').fill('SecurePass123!');
    
    // Trigger registration
    await page.getByTestId('register-submit-button').click();

    // Verify error toast or message (Backend returns 409)
    // We expect the GlobalExceptionHandler message: "Data integrity violation: Identity or resource already exists."
    await expect(page.getByText(/Identity already registered/i).or(page.getByText(/Data integrity violation/i))).toBeVisible();
    
    // Ensure we are still on register page
    await expect(page).toHaveURL(/.*register/);
  });
});
