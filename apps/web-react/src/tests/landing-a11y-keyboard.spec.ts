import { expect, test, type Page } from '@playwright/test';

/**
 * Keyboard-only smoke test for the public landing page, with no backend anywhere.
 *
 * <p>The directory request is answered by a route stub, so the page renders its steady state and the
 * only thing under test is the keyboard path: the first Tab must land on a skip link that is
 * actually on screen, Enter must move focus into the main landmark, and every following stop must
 * keep a visible focus indicator. A keyboard visitor who cannot see where they are is locked out
 * just as effectively as one facing a login wall.
 */
const cities = [
  {
    id: 'city-valle',
    name: 'Villa Verde',
    slug: 'villa-verde',
    description: 'A growing district community.',
    country: 'Colombia',
    region: 'Antioquia',
    parentCommunityId: null,
    memberCount: 3,
    createdAt: '2026-03-01T10:00:00',
  },
  {
    id: 'city-lomas',
    name: 'Las Lomas',
    slug: 'las-lomas',
    description: null,
    country: 'Mexico',
    region: null,
    parentCommunityId: null,
    memberCount: 1,
    createdAt: '2026-03-02T10:00:00',
  },
];

async function stubCityDirectory(page: Page) {
  await page.route('**/api/communities/public', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(cities),
    });
  });
}

type FocusState = {
  tag: string;
  testId: string | null;
  label: string;
  outlineVisible: boolean;
  boxShadowVisible: boolean;
};

async function readFocusState(page: Page): Promise<FocusState | null> {
  return page.evaluate(() => {
    const element = document.activeElement as HTMLElement | null;
    if (!element || element === document.body) {
      return null;
    }
    const style = getComputedStyle(element);
    const outlineWidth = Number.parseFloat(style.outlineWidth || '0');
    return {
      tag: element.tagName,
      testId: element.getAttribute('data-testid'),
      label: (element.textContent ?? '').trim().slice(0, 48),
      outlineVisible: style.outlineStyle !== 'none' && outlineWidth > 0,
      // PrimeReact components draw their focus ring as a box-shadow and zero the outline.
      boxShadowVisible: style.boxShadow !== 'none' && style.boxShadow.length > 0,
    };
  });
}

test.describe('Landing page keyboard accessibility', () => {
  test('first Tab reaches a visible skip link and Enter lands inside the main landmark', async ({ page }) => {
    await stubCityDirectory(page);
    await page.goto('/');
    await expect(page.getByTestId('landing-page')).toBeVisible({ timeout: 30000 });

    const skipLink = page.getByTestId('skip-to-content-link');
    await expect(skipLink).toHaveAttribute('href', '#main-content');

    await page.keyboard.press('Tab');
    await expect(skipLink).toBeFocused();

    // The link waits off-screen and slides in on focus. If it stayed hidden, the first Tab would
    // consume a stop that shows nothing. The top offset animates over 150ms, so poll instead of
    // reading the box once and racing the transition.
    await expect
      .poll(async () => (await skipLink.boundingBox())?.y ?? Number.NEGATIVE_INFINITY)
      .toBeGreaterThanOrEqual(0);
    const box = await skipLink.boundingBox();
    expect(box).not.toBeNull();
    const viewport = page.viewportSize();
    expect(box!.height).toBeGreaterThan(0);
    if (viewport) {
      expect(box!.y + box!.height).toBeLessThanOrEqual(viewport.height);
    }

    const skipFocus = await readFocusState(page);
    expect(skipFocus?.testId).toBe('skip-to-content-link');
    expect(skipFocus!.outlineVisible).toBe(true);

    await page.keyboard.press('Enter');
    await expect
      .poll(async () =>
        page.evaluate(() => {
          const main = document.querySelector('[data-testid="main-content"]');
          return Boolean(main && main.contains(document.activeElement));
        })
      )
      .toBe(true);

    // One landmark only: a second main would make the skip target ambiguous for screen readers.
    const mainCount = await page.evaluate(() => document.querySelectorAll('main').length);
    expect(mainCount).toBe(1);
  });

  test('every keyboard stop on the landing page keeps a visible focus indicator', async ({ page }) => {
    await stubCityDirectory(page);
    await page.goto('/');
    await expect(page.getByTestId('landing-city-card-city-valle')).toBeVisible({ timeout: 30000 });

    const stops: FocusState[] = [];
    for (let stop = 0; stop < 14; stop += 1) {
      await page.keyboard.press('Tab');
      const state = await readFocusState(page);
      if (!state) {
        // Focus left the document and moved to the browser chrome: the page has no more stops.
        break;
      }
      stops.push(state);
      expect(
        state.outlineVisible || state.boxShadowVisible,
        `tab stop ${stop + 1} (${state.tag} ${state.testId ?? state.label}) shows no focus indicator`
      ).toBe(true);
    }

    // The walk must reach interactive landing content, not stop after a stray element.
    expect(stops.length).toBeGreaterThanOrEqual(6);
    expect(stops.some((stop) => stop.testId === 'landing-login-cta')).toBe(true);
  });
});
