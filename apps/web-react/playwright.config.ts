import { defineConfig, devices } from '@playwright/test';
import { WEB_PORT } from './ports';

export default defineConfig({
  testDir: './src/tests',
  globalSetup: './playwright.global-setup.ts',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  // One worker everywhere, not only in CI. Left undefined locally, Playwright defaults to half the
  // cores and spins up that many browser contexts at once, which is what made the mobile-chrome
  // flakes move between unrelated specs: browserContext.newPage timing out after 30s, a resource
  // symptom that has nothing to do with any given test. One worker locally keeps verification
  // identical to CI instead of leaving the two to disagree.
  workers: 1,
  reporter: [['html', { outputFolder: '../../output/playwright-report' }]],
  use: {
    baseURL: process.env.BASE_URL || `http://localhost:${WEB_PORT}`,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
  ],
});
