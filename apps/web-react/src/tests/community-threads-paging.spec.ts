import { expect, test } from '@playwright/test';
import type { Page, Request } from '@playwright/test';

type AuthContext = {
  token: string;
  sourceCommunityId: string;
  targetCommunityId: string;
  sourceCommunityName: string;
};

async function loginAsAdmin(page: Page) {
  await page.goto('/login');
  await page.getByTestId('login-username-input').fill('admin');
  await page.getByTestId('login-password-input').fill('admin12345');
  await page.getByTestId('login-submit-button').click();
  await page.waitForURL('**/');
}

async function buildCommunitiesAndThreads(page: Page): Promise<AuthContext> {
  const suffix = Date.now();
  const sourceCommunityName = `Paging Source ${suffix}`;
  const targetCommunityName = `Paging Target ${suffix}`;

  const token = await page.evaluate(() => {
    const raw = localStorage.getItem('auth-storage');
    return raw ? JSON.parse(raw).state?.accessToken : null;
  });
  if (!token) {
    throw new Error('Missing access token after login');
  }

  const headers = { Authorization: `Bearer ${token}` };
  const sourceSlug = `paging-source-${suffix}`;
  const targetSlug = `paging-target-${suffix}`;

  const sourceCreate = await page.request.post('/api/communities', {
    headers,
    data: {
      name: sourceCommunityName,
      slug: sourceSlug,
      description: 'Playwright paging source',
    },
  });
  expect(sourceCreate.ok()).toBeTruthy();

  const targetCreate = await page.request.post('/api/communities', {
    headers,
    data: {
      name: targetCommunityName,
      slug: targetSlug,
      description: 'Playwright paging target',
    },
  });
  expect(targetCreate.ok()).toBeTruthy();

  const membershipsRes = await page.request.get('/api/communities/my', { headers });
  expect(membershipsRes.ok()).toBeTruthy();
  const memberships = await membershipsRes.json();
  const sourceMembership = memberships.find((m: { communityName: string }) => m.communityName === sourceCommunityName);
  const targetMembership = memberships.find((m: { communityName: string }) => m.communityName === targetCommunityName);
  if (!sourceMembership?.communityId || !targetMembership?.communityId) {
    throw new Error('Unable to resolve source/target community memberships for test');
  }

  const switchRes = await page.request.post(`/api/communities/${sourceMembership.communityId}/switch`, {
    headers,
  });
  expect(switchRes.ok()).toBeTruthy();

  for (let i = 0; i < 11; i += 1) {
    const createThreadRes = await page.request.post('/api/community/threads', {
      headers,
      data: {
        sourceCommunityId: sourceMembership.communityId,
        targetCommunityId: targetMembership.communityId,
        title: `Paging Thread ${suffix}-${i + 1}`,
      },
    });
    expect(createThreadRes.ok()).toBeTruthy();
  }

  return {
    token,
    sourceCommunityId: sourceMembership.communityId,
    targetCommunityId: targetMembership.communityId,
    sourceCommunityName,
  };
}

async function createCommunityBlogPosts(page: Page, auth: AuthContext, count: number) {
  const headers = { Authorization: `Bearer ${auth.token}` };
  for (let i = 0; i < count; i += 1) {
    const createBlogRes = await page.request.post('/api/community/blog', {
      headers,
      data: {
        communityId: auth.sourceCommunityId,
        title: `Blog fanout baseline ${Date.now()}-${i + 1}`,
        content: `Performance validation entry ${i + 1}`,
        statusTag: 'IN_PROGRESS',
      },
    });
    expect(createBlogRes.ok()).toBeTruthy();
  }
}

test.describe('Community Threads pagination and status persistence (P1)', () => {
  test('threads list keeps paging/filter state and sends deterministic backend query params', async ({ page }) => {
    await loginAsAdmin(page);
    const auth = await buildCommunitiesAndThreads(page);

    await page.goto('/communities');
    await page.waitForLoadState('networkidle');
    await expect(page.getByText(auth.sourceCommunityName).first()).toBeVisible();

    await page.evaluate((sourceCommunityId: string) => {
      const raw = localStorage.getItem('community-storage');
      if (!raw) return;
      const parsed = JSON.parse(raw);
      parsed.state = parsed.state || {};
      parsed.state.activeCommunityId = sourceCommunityId;
      localStorage.setItem('community-storage', JSON.stringify(parsed));
    }, auth.sourceCommunityId);

    const initialThreadsResponse = page.waitForResponse((resp) => {
      const url = resp.url();
      return url.includes('/api/community/threads') && url.includes(`communityId=${auth.sourceCommunityId}`) && resp.ok();
    });
    await page.goto('/communities/threads');
    await initialThreadsResponse;

    const pageTwoResponse = page.waitForResponse((resp) => {
      const url = resp.url();
      return (
        url.includes('/api/community/threads') &&
        url.includes(`communityId=${auth.sourceCommunityId}`) &&
        url.includes('page=1') &&
        url.includes('size=10') &&
        resp.ok()
      );
    });
    await page.locator('.p-paginator-next').click();
    await pageTwoResponse;

    await page.goto('/communities/blog');
    await page.waitForLoadState('networkidle');
    const persistedPageResponse = page.waitForResponse((resp) => {
      const url = resp.url();
      return (
        url.includes('/api/community/threads') &&
        url.includes(`communityId=${auth.sourceCommunityId}`) &&
        url.includes('page=1') &&
        url.includes('size=10') &&
        resp.ok()
      );
    });
    await page.goto('/communities/threads');
    await persistedPageResponse;

    await page.getByTestId('threads-status-filter').click();
    await Promise.all([
      page.waitForResponse((resp) => {
        const url = resp.url();
        return (
          url.includes('/api/community/threads') &&
          url.includes(`communityId=${auth.sourceCommunityId}`) &&
          url.includes('status=STALE') &&
          url.includes('page=0') &&
          url.includes('size=10') &&
          resp.ok()
        );
      }),
      page.locator('.p-dropdown-item', { hasText: /Stale|Desactualizados/i }).click(),
    ]);

    await page.goto('/communities/feed');
    await page.waitForLoadState('networkidle');
    const persistedFilterResponse = page.waitForResponse((resp) => {
      const url = resp.url();
      return (
        url.includes('/api/community/threads') &&
        url.includes(`communityId=${auth.sourceCommunityId}`) &&
        url.includes('status=STALE') &&
        url.includes('page=0') &&
        url.includes('size=10') &&
        resp.ok()
      );
    });
    await page.goto('/communities/threads');
    await persistedFilterResponse;
  });

  test('community blog initial load uses batched comment count endpoint without per-post comment fan-out', async ({ page }) => {
    await loginAsAdmin(page);
    const auth = await buildCommunitiesAndThreads(page);
    await createCommunityBlogPosts(page, auth, 4);

    await page.evaluate((sourceCommunityId: string) => {
      const raw = localStorage.getItem('community-storage');
      if (!raw) return;
      const parsed = JSON.parse(raw);
      parsed.state = parsed.state || {};
      parsed.state.activeCommunityId = sourceCommunityId;
      localStorage.setItem('community-storage', JSON.stringify(parsed));
    }, auth.sourceCommunityId);

    let batchedCountRequests = 0;
    let perPostCommentRequests = 0;
    const onRequest = (request: Request) => {
      const url = request.url();
      if (request.method() !== 'GET') return;
      if (url.includes('/api/community/blog/comments/count')) {
        batchedCountRequests += 1;
      }
      if (/\/api\/community\/blog\/[^/]+\/comments(\?|$)/.test(url)) {
        perPostCommentRequests += 1;
      }
    };
    page.on('request', onRequest);

    await page.goto('/communities/blog');
    await page.waitForLoadState('networkidle');
    await expect(page.getByText(/Blog fanout baseline/i).first()).toBeVisible();

    page.off('request', onRequest);

    expect(batchedCountRequests).toBe(1);
    expect(perPostCommentRequests).toBe(0);
  });
});
