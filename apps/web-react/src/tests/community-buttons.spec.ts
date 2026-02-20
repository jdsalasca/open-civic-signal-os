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
    await page.getByPlaceholder('Name').fill(communityName);
    await page.getByPlaceholder('Slug').fill(communitySlug);
    await page.getByPlaceholder('Description').fill('Created by Playwright for E2E validation');
    await page.getByTestId('create-community-submit-button').click();
    await expect(page.getByText('Community created')).toBeVisible();

    await page.goto('/communities/threads');
    await expect(page.getByTestId('create-thread-button')).toBeDisabled();

    await page.getByPlaceholder('Thread title').fill(threadTitle);
    await page.locator('.p-card').filter({ hasText: 'Open Thread' }).locator('.p-dropdown').click();
    await page.locator('.p-dropdown-item', { hasText: /Central Hub|Los rosales/ }).first().click();

    await expect(page.getByTestId('create-thread-button')).toBeEnabled();
    await page.getByTestId('create-thread-button').click();
    await expect(page.getByText('Thread created')).toBeVisible();

    await page.locator('.p-card').filter({ hasText: 'Post Message' }).locator('.p-dropdown').click();
    await page.locator('.p-dropdown-item', { hasText: threadTitle }).click();
    await page.getByPlaceholder('Message').fill(threadMessage);
    await expect(page.getByTestId('send-thread-message-button')).toBeEnabled();
    await page.getByTestId('send-thread-message-button').click();
    await expect(page.getByText('Message sent')).toBeVisible();

    await page.goto('/communities/blog');
    await expect(page.getByTestId('publish-blog-button')).toBeDisabled();
    await page.getByPlaceholder('Title').fill(blogTitle);
    await page.getByPlaceholder('What changed for this community?').fill('Playwright blog message');
    await expect(page.getByTestId('publish-blog-button')).toBeEnabled();
    await page.getByTestId('publish-blog-button').click();
    await expect(page.getByText('Update published')).toBeVisible();
  });
});
