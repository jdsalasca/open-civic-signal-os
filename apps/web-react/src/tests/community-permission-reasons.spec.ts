import { expect, test } from '@playwright/test';

test.describe('Community creation permission reasons', () => {
  test('shows backend permission reason inline for thread and blog creation', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();
    await page.waitForURL('**/');

    await page.route('**/api/community/threads', async (route) => {
      if (route.request().method() !== 'POST') {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Security clearance insufficient for this sector.' }),
      });
    });

    await page.route('**/api/community/blog', async (route) => {
      if (route.request().method() !== 'POST') {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Security clearance insufficient for this sector.' }),
      });
    });

    await page.goto('/communities/threads');
    await page.getByTestId('thread-title-input').fill('Permission reason thread test');
    await page.getByTestId('thread-target-dropdown').click();
    await page.locator('.p-dropdown-item').first().click();
    await expect(page.getByTestId('create-thread-button')).toBeEnabled();
    await page.getByTestId('create-thread-button').click();
    await expect(page.getByTestId('thread-create-permission-reason')).toContainText('Security clearance insufficient');

    await page.goto('/communities/blog');
    await page.getByTestId('blog-title-input').fill('Permission reason blog test');
    await page.getByTestId('blog-content-input').fill('Permission reason blog content test');
    await expect(page.getByTestId('publish-blog-button')).toBeEnabled();
    await page.getByTestId('publish-blog-button').click();
    await expect(page.getByTestId('blog-create-permission-reason')).toContainText('Security clearance insufficient');
  });
});
