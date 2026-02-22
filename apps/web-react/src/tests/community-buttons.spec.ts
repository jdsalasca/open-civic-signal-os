import { expect, test } from '@playwright/test';

test.describe('Community Buttons UX (P0)', () => {
  test('Community actions avoid dead clicks and execute end-to-end', async ({ page }) => {
    const suffix = Date.now();
    const communityName = `Playwright Community ${suffix}`;
    const communitySlug = `pw-${suffix}`;
    const threadTitle = `Playwright Thread ${suffix}`;
    const threadMessage = `Message ${suffix}`;
    const blogTitle = `Blog ${suffix}`;

    await page.goto('/login');
    await page.getByTestId('login-username-input').fill('admin');
    await page.getByTestId('login-password-input').fill('admin12345');
    await page.getByTestId('login-submit-button').click();

    await page.waitForURL('**/');
    await page.goto('/communities');

    await page.getByTestId('open-create-community-button').click();
    await page.locator('#comm-name').fill(communityName);
    await page.locator('#comm-slug').fill(communitySlug);
    await page.locator('#comm-desc').fill('Created by Playwright for E2E validation');
    await page.getByTestId('create-community-submit-button').click();
    await expect(page.getByText(communityName).first()).toBeVisible();

    await page.goto('/communities/threads');
    await expect(page.getByTestId('create-thread-button')).toBeDisabled();

    await page.getByTestId('thread-title-input').fill(threadTitle);
    await page.getByTestId('thread-target-dropdown').click();
    await page.locator('.p-dropdown-item').first().click();

    await expect(page.getByTestId('create-thread-button')).toBeEnabled();
    await page.getByTestId('create-thread-button').click();
    await expect(page.getByText(threadTitle)).toBeVisible();

    await page.getByTestId('thread-select-dropdown').click();
    await page.locator('.p-dropdown-item', { hasText: threadTitle }).first().click();
    await page.getByTestId('thread-message-input').fill(threadMessage);
    await expect(page.getByTestId('send-thread-message-button')).toBeEnabled();
    await page.getByTestId('send-thread-message-button').click();
    await expect(page.getByText(threadMessage)).toBeVisible();

    await page.goto('/communities/blog');
    await expect(page.getByTestId('publish-blog-button')).toBeDisabled();
    await page.getByTestId('blog-title-input').fill(blogTitle);
    await page.getByTestId('blog-content-input').fill('Playwright blog message');
    await expect(page.getByTestId('publish-blog-button')).toBeEnabled();
    await page.getByTestId('publish-blog-button').click();
    await expect(page.getByText(blogTitle)).toBeVisible();
  });
});
