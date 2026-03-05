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

  test('Should surface verify recovery path when email delivery fails', async ({ page }) => {
    const username = `degraded_${Date.now()}`;
    const email = `${username}@example.com`;

    await page.route('**/auth/register', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Registration successful, but verification email could not be delivered.',
          username,
          emailDeliveryStatus: 'FAILED',
          supportEmail: 'support@open-civic.local',
          deliveryFailureReason: 'smtp down'
        })
      });
    });

    await page.route('**/auth/resend-code', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Could not deliver verification email. Retry later or contact support.',
          emailDeliveryStatus: 'FAILED',
          supportEmail: 'support@open-civic.local',
          deliveryFailureReason: 'smtp down'
        })
      });
    });

    await page.goto('/register');
    await page.getByTestId('register-username-input').fill(username);
    await page.getByTestId('register-email-input').fill(email);
    await page.getByTestId('register-password-input').fill('SecurePass123!');
    await page.getByTestId('register-confirm-password-input').fill('SecurePass123!');
    await page.getByTestId('register-submit-button').click();

    await expect(page).toHaveURL(/.*verify/);
    await expect(page.getByText(/verification email could not be delivered/i)).toBeVisible();
    await expect(page.getByText(/support@open-civic.local/i)).toBeVisible();

    await page.getByRole('button', { name: /resend/i }).click();
    await expect(page.getByText(/could not deliver verification email/i)).toBeVisible();
  });

  test('Should show onboarding progress cues from register to verify', async ({ page }) => {
    const username = `guided_${Date.now()}`;
    const email = `${username}@example.com`;

    await page.route('**/auth/register', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Registration successful.',
          username,
          emailDeliveryStatus: 'SENT',
        })
      });
    });

    await page.goto('/register');
    await expect(page.getByTestId('onboarding-progress-register')).toContainText(/step 1 of 3|paso 1 de 3/i);
    await page.getByTestId('register-username-input').fill(username);
    await page.getByTestId('register-email-input').fill(email);
    await page.getByTestId('register-password-input').fill('SecurePass123!');
    await page.getByTestId('register-confirm-password-input').fill('SecurePass123!');
    await page.getByTestId('register-submit-button').click();

    await expect(page).toHaveURL(/.*verify/);
    await expect(page.getByTestId('onboarding-progress-verify')).toContainText(/step 2 of 3|paso 2 de 3/i);
  });
});
