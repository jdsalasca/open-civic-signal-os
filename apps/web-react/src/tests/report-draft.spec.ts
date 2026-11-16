import { expect, test } from '@playwright/test';
import { seedAuthenticatedApp } from './helpers/session';

test.describe('Report draft', () => {
  test('restores a draft after a reload and discards it on request', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN');
    await page.goto('/report');

    await page.getByTestId('report-title-input').fill('Broken streetlight on 5th');
    await page.getByTestId('report-location-input').fill('5th avenue, block 2');
    await page.reload();

    await expect(page.getByTestId('report-draft-notice')).toBeVisible();
    await expect(page.getByTestId('report-title-input')).toHaveValue('Broken streetlight on 5th');

    await page.getByTestId('report-draft-discard').click();
    await expect(page.getByTestId('report-draft-notice')).toBeHidden();
    await expect(page.getByTestId('report-title-input')).toHaveValue('');

    await page.reload();
    await expect(page.getByTestId('report-draft-notice')).toBeHidden();
  });

  test('does not keep or restore an empty form', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN');
    await page.goto('/report');

    await page.getByTestId('report-title-input').fill('temporary');
    await page.getByTestId('report-title-input').fill('');
    await page.reload();

    await expect(page.getByTestId('report-draft-notice')).toBeHidden();
  });
});
