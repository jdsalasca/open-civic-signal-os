import { expect, test } from '@playwright/test';
import { DEFAULT_COMMUNITY_ID, seedAuthenticatedApp } from './helpers/session';
import { mockDashboardRoutes } from './helpers/dashboard';

const cid = DEFAULT_COMMUNITY_ID;

const feed = [
  {
    type: 'thread-update',
    id: 'thread-1',
    communityId: cid,
    title: 'Neighbors, let us clean the park on Saturday',
    summary: 'Meet at 8 a.m. at the entrance. I bring bags and gloves.',
    happenedAt: '2026-11-15T20:50:00',
    freshness: 'updated 7m ago',
    following: false,
    reactions: { '👍': 2 },
    viewerReaction: null,
    authorName: 'Sofía Ramírez',
    authorRole: 'CITIZEN',
    commentCount: 1,
    rootMessageId: 'm1',
  },
  {
    type: 'signal',
    id: 'signal-1',
    communityId: cid,
    title: 'Unsafe crosswalk on the main avenue',
    summary: 'Main Avenue and 3rd Street',
    happenedAt: '2026-11-15T18:00:00',
    freshness: 'updated 2h ago',
    confirmationCount: 0,
    viewerConfirmed: false,
    following: false,
    authorName: 'María Pérez',
    authorRole: 'CITIZEN',
    commentCount: 2,
  },
  {
    type: 'blog',
    id: 'blog-1',
    communityId: cid,
    title: 'New lighting for the park',
    summary: 'The city will install 12 lamps this week.',
    happenedAt: '2026-11-14T09:00:00',
    freshness: 'updated 1d ago',
    authorName: 'Equipo de la Alcaldía',
    authorRole: 'PUBLIC_SERVANT',
    commentCount: 1,
  },
];

test.describe('Community social feed', () => {
  test('shows one timeline, filters it and publishes from the composer', async ({ page }) => {
    let publishPayload: Record<string, unknown> | null = null;
    let threadMessageCalls = 0;
    let published = false;
    const newThreadItem = {
      type: 'thread-update',
      id: 'new-thread',
      communityId: cid,
      title: 'A new community question',
      summary: 'This is the message body.',
      happenedAt: '2026-11-16T12:00:00',
      freshness: '',
      following: false,
      reactions: {},
      viewerReaction: null,
      authorName: 'liaison',
      authorRole: 'CITIZEN',
      commentCount: 0,
      rootMessageId: 'new-thread-message',
    };
    await page.route('**/api/community/threads', (route) => {
      publishPayload = route.request().postDataJSON() as Record<string, unknown>;
      published = true;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'new-thread',
          title: 'A new community question',
          updatedAt: '2026-11-16T12:00:00',
          messages: [
            {
              id: 'new-thread-message',
              threadId: 'new-thread',
              authorId: 'u1',
              authorName: 'liaison',
              sourceCommunityId: cid,
              content: 'This is the message body.',
              depth: 0,
              directReplyCount: 0,
              hidden: false,
              createdAt: '2026-11-16T12:00:00',
              reactions: {},
            },
          ],
        }),
      });
    });
    await page.route('**/api/community/threads/new-thread/messages', (route) => {
      threadMessageCalls += 1;
      return route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });
    await page.route('**/api/community/threads/thread-1/messages', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '{}' }));
    await page.route('**/api/community/threads/thread-1?*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'thread-1',
          title: feed[0].title,
          messages: [
            {
              id: 'm1', threadId: 'thread-1', authorId: 'u-sofia', authorName: 'Sofía Ramírez',
              sourceCommunityId: cid, content: feed[0].summary, depth: 0, directReplyCount: 1,
              hidden: false, createdAt: '2026-11-15T20:50:00', reactions: {},
            },
            {
              id: 'm2', threadId: 'thread-1', authorId: 'u-carlos', authorName: 'Carlos Ruiz',
              sourceCommunityId: cid, parentMessageId: 'm1', content: 'I can bring bags.',
              depth: 1, directReplyCount: 0, hidden: false, createdAt: '2026-11-15T21:05:00', reactions: {},
            },
          ],
        }),
      }));
    await page.route('**/api/community/threads?*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, size: 100, number: 0 }),
      }));
    await page.route('**/api/notifications/recent', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));

    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await mockDashboardRoutes(page);
    // After the shared helper so these payloads win.
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(published ? [newThreadItem, ...feed] : feed),
      }));
    await page.route('**/api/follows/**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ following: route.request().method() !== 'DELETE' }),
      }));
    await page.route('**/api/signals/signal-1/confirmations', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ signalId: 'signal-1', count: 1, confirmedByViewer: true }),
      }));
    await page.goto('/communities/feed');

    await expect(page.getByRole('heading', { level: 1 })).toContainText('Los Rosales');
    await expect(page.getByTestId('community-feed-timeline')).toBeVisible();
    await expect(page.locator('.social-feed-item')).toHaveCount(3);
    await expect(page.locator('.social-feed-item').first()).toContainText(feed[0].summary);

    // The card says who posted and how many comments it has, and official posts are marked.
    await expect(page.locator('.social-feed-item').filter({ hasText: 'Unsafe crosswalk' })).toContainText(
      'By María Pérez'
    );
    await expect(page.locator('.social-feed-item').filter({ hasText: 'New lighting' })).toContainText(
      'Equipo de la Alcaldía'
    );
    await expect(page.locator('.social-feed-item').filter({ hasText: 'New lighting' })).toContainText('Official');
    await expect(page.locator('.social-feed-item').filter({ hasText: 'clean the park' })).toContainText(
      'Comment · 1'
    );

    await page.getByTestId('community-feed-filter').selectOption('signal');
    await expect(page.locator('.social-feed-item')).toHaveCount(1);
    await expect(page.locator('.social-feed-type')).toHaveText('Report');

    await page.getByTestId('community-feed-filter').selectOption('all');
    await page.getByTestId('community-alerts-toggle').click();
    await expect(page.getByTestId('community-alerts-toggle')).toHaveAttribute('aria-pressed', 'true');
    await page.getByTestId('community-feed-composer').locator('button').first().click();
    await page.locator('#feed-post-title').fill('A new community question');
    await page.locator('#feed-post-body').fill('This is the message body.');
    await page.getByTestId('community-feed-publish').click();

    await expect(page.getByText('Published in the community feed.')).toBeVisible();
    await expect(page.locator('#feed-post-title')).toHaveCount(0);
    await expect(page.getByText('A new community question')).toBeVisible();
    await expect(page.locator('.social-feed-item')).toHaveCount(4);
    // Thread and body travel in one request: a second call could fail and leave an empty thread.
    expect(publishPayload).toMatchObject({
      title: 'A new community question',
      initialMessage: 'This is the message body.',
    });
    expect(threadMessageCalls).toBe(0);

    // Follow a report, confirm it and reply to a conversation from the feed itself.
    const signalCard = page.locator('.social-feed-item').filter({ hasText: 'Unsafe crosswalk' });
    await signalCard.getByTestId('community-feed-follow-signal-1').click();
    await expect(signalCard.getByTestId('community-feed-follow-signal-1')).toHaveAttribute('aria-pressed', 'true');
    await signalCard.getByTestId('community-feed-confirm-signal-1').click();
    await expect(signalCard.getByTestId('community-feed-confirm-signal-1')).toHaveAttribute('aria-pressed', 'true');
    await expect(signalCard.getByTestId('community-feed-confirm-signal-1')).toContainText('1');

    const threadCard = page.locator('.social-feed-item').filter({ hasText: 'clean the park' });
    await threadCard.getByTestId('community-feed-comment-thread-1').click();
    // The thread is read by id, so replies keep their authors instead of a generic label.
    await expect(threadCard.getByText('Carlos Ruiz')).toBeVisible();
    await expect(threadCard.getByText('I can bring bags.')).toBeVisible();
    await page.getByTestId('community-feed-reply-input-thread-1').fill('I will bring gloves.');
    await page.getByTestId('community-feed-reply-send-thread-1').click();
    await expect(page.getByText('Comment published.')).toBeVisible();
  });

  test('the citizen home is the community feed with report and opinion side by side', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));

    await page.goto('/');
    await expect(page.getByTestId('community-feed-composer')).toBeVisible();
    await expect(page.getByTestId('community-feed-report')).toBeVisible();
    await expect(page.getByTestId('community-feed-timeline')).toBeVisible();
  });

  test('a failed feed load shows a retry card, not an empty community', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) => route.abort('failed'));

    await page.goto('/');
    await expect(page.getByTestId('community-feed-error')).toBeVisible();
    await expect(page.getByTestId('community-feed-empty')).toHaveCount(0);
  });

  test('reads and publishes a comment on a report from the feed', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));
    await page.route('**/api/signals/signal-1/comments', (route) => {
      if (route.request().method() === 'POST') {
        return route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'comment-1',
            authorUsername: 'vecino',
            content: 'I walk there every day.',
            createdAt: '2026-11-16T12:00:00',
          }),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { id: 'comment-0', authorUsername: 'maria', content: 'Same here.', createdAt: '2026-11-15T19:00:00' },
        ]),
      });
    });

    await page.goto('/communities/feed');
    const card = page.locator('.social-feed-item').filter({ hasText: 'Unsafe crosswalk' });
    await expect(card.getByRole('button', { name: /Comment · 2/ })).toBeVisible();
    await card.getByTestId('community-feed-comment-signal-1').click();
    await expect(card.getByText('Same here.')).toBeVisible();

    await card.getByTestId('community-feed-reply-input-signal-1').fill('I walk there every day.');
    await card.getByTestId('community-feed-reply-send-signal-1').click();
    await expect(page.getByText('Comment published.')).toBeVisible();
    await expect(card.getByText('I walk there every day.')).toBeVisible();
    await expect(card.getByRole('button', { name: /Comment · 3/ })).toBeVisible();
  });

  test('a post from five hours ago is not shown as in the future', async ({ page }) => {
    const happenedAt = new Date(Date.now() - 5 * 3600 * 1000).toISOString().slice(0, 19);
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ ...feed[1], happenedAt }]),
      }));

    await page.goto('/communities/feed');
    const card = page.locator('.social-feed-item').first();
    await expect(card.locator('.social-feed-time')).toContainText('5 hours ago');
    await expect(card.locator('.social-feed-time')).not.toContainText('in 5 hours');
  });

  test('turns a URL in a summary into a safe link', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            ...feed[0],
            id: 'thread-link',
            summary: 'Details at https://example.com/cleanup.',
          },
        ]),
      }));

    await page.goto('/communities/feed');
    const link = page.getByRole('link', { name: 'https://example.com/cleanup' });
    await expect(link).toHaveAttribute('href', 'https://example.com/cleanup');
    await expect(link).toHaveAttribute('rel', /noopener/);
  });

  test('a network failure speaks the reader language instead of technical jargon', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));
    await page.route('**/api/signals/signal-1/react', (route) => route.abort('failed'));

    await page.goto('/communities/feed');
    await page.getByTestId('community-feed-react-trigger-signal-1').click();
    await page.getByTestId('community-feed-react-like-signal-1').click();
    await expect(page.getByText('No connection. Check your internet and try again.')).toBeVisible();
  });

  test('loads older posts with a cursor instead of stopping at the first page', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    const pageOf = (start: number) =>
      Array.from({ length: 20 }, (_, index) => ({
        type: 'signal',
        id: `paged-${start + index}`,
        communityId: cid,
        title: `Paged report ${start + index}`,
        summary: 'Summary',
        happenedAt: new Date(Date.UTC(2026, 10, 16, 20, 0, 0) - (start + index) * 60000)
          .toISOString()
          .slice(0, 19),
        freshness: '',
        confirmationCount: 0,
        viewerConfirmed: false,
        following: false,
        reactions: {},
        viewerReaction: null,
        authorName: 'Vecino',
        authorRole: 'CITIZEN',
        commentCount: 0,
      }));
    const firstPage = pageOf(0);
    let beforeParam: string | null = null;
    await page.route('**/api/community/feed*', (route) => {
      const before = new URL(route.request().url()).searchParams.get('before');
      beforeParam = before;
      const data = before ? pageOf(20).slice(0, 2) : firstPage;
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(data) });
    });

    await page.goto('/communities/feed');
    await expect(page.getByText('This feed shows the last 30 days of community activity.')).toBeVisible();
    await expect(page.getByTestId('community-feed-load-more')).toBeVisible();
    await expect(page.locator('.social-feed-item')).toHaveCount(20);

    // A filter with no matches in the first page is not the end of the feed: pagination stays.
    await page.getByTestId('community-feed-filter').selectOption('blog');
    await expect(page.getByTestId('community-feed-empty')).toBeVisible();
    await expect(page.getByTestId('community-feed-load-more')).toBeVisible();
    await page.getByTestId('community-feed-filter').selectOption('all');

    await page.getByTestId('community-feed-load-more').click();
    await expect(page.locator('.social-feed-item')).toHaveCount(22);
    await expect.poll(() => beforeParam).toBe(firstPage[19].happenedAt);
    // A short page is the end of the feed, so the control disappears instead of lying.
    await expect(page.getByTestId('community-feed-load-more')).toHaveCount(0);
  });

  test('shows the detected place and joins the nearby community in one click', async ({ page, context }) => {
    await context.grantPermissions(['geolocation']);
    await context.setGeolocation({ latitude: 5.5353, longitude: -73.3678 });
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    let joined = false;
    const nearby = {
      id: 'city-tunja',
      name: 'Tunja',
      slug: 'tunja',
      city: 'Tunja',
      region: 'Boyacá',
      country: 'Colombia',
      locality: null,
      neighborhood: null,
      memberCount: 4,
    };
    await page.route('**/api/places/reverse*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          neighborhood: 'Barrio Las Nieves',
          locality: 'Tunja',
          city: 'Tunja',
          region: 'Boyacá',
          country: 'Colombia',
        }),
      }));
    await page.route('**/api/communities/nearest*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(nearby) }));
    await page.route('**/api/communities/my', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            userId: 'u1',
            communityId: cid,
            communityName: 'Los Rosales',
            communitySlug: 'los-rosales',
            parentCommunityId: null,
            breadcrumb: [{ id: cid, name: 'Los Rosales', slug: 'los-rosales' }],
            role: 'MEMBER',
            createdBy: 'u1',
            createdAt: '2026-11-01T10:00:00',
          },
          ...(joined
            ? [{
                userId: 'u1',
                communityId: nearby.id,
                communityName: nearby.name,
                communitySlug: nearby.slug,
                parentCommunityId: null,
                breadcrumb: [{ id: nearby.id, name: nearby.name, slug: nearby.slug }],
                role: 'MEMBER',
                createdBy: 'u1',
                createdAt: '2026-11-16T10:00:00',
              }]
            : []),
        ]),
      }));
    await page.route('**/api/communities/city-tunja/join', (route) => {
      joined = true;
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id: nearby.id }) });
    });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));

    await page.goto('/communities/feed');
    await expect(page.getByTestId('community-feed-place')).toContainText('Barrio Las Nieves');
    await expect(page.getByTestId('community-feed-place')).toContainText('Tunja');

    const banner = page.getByTestId('community-feed-location-mismatch');
    await expect(banner).toContainText('You are in Tunja');
    await expect(banner).toContainText('Los Rosales');
    await banner.getByTestId('community-location-join').click();

    // Joining from the banner is one click and leaves the nearby community active.
    await expect(page.getByTestId('community-feed-location-mismatch')).toHaveCount(0);
    await expect(page.getByTestId('community-switch-dropdown')).toContainText('Tunja');
  });

  test('reacts to a conversation from the feed', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));
    await page.route('**/api/community/threads/thread-1/messages/m1/react', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ reactions: { '👍': 3 }, viewerReaction: '👍' }),
      }));

    await page.goto('/communities/feed');
    const card = page.locator('.social-feed-item').filter({ hasText: 'clean the park' });
    const trigger = card.getByTestId('community-feed-react-trigger-thread-1');
    await expect(trigger).toContainText('· 2');
    await trigger.click();
    await card.getByTestId('community-feed-react-like-thread-1').click();
    await expect(trigger).toContainText('Like');
    await expect(trigger).toContainText('· 3');
  });

  test('reacts on tap without waiting for the server', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));
    let release!: () => void;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    await page.route('**/api/signals/signal-1/react', async (route) => {
      await gate;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ reactions: { LIKE: 1 }, viewerReaction: 'LIKE' }),
      });
    });

    await page.goto('/communities/feed');
    const trigger = page.getByTestId('community-feed-react-trigger-signal-1');
    await trigger.click();
    const like = page.getByTestId('community-feed-react-like-signal-1');
    await like.click();
    // The count and pressed state change before the request resolves.
    await expect(trigger).toContainText('· 1');
    await expect(trigger).toContainText('Like');
    release();
  });

  test('focuses the composer and publishes once with Control+Enter', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    let calls = 0;
    let payload: Record<string, unknown> | null = null;
    let release!: () => void;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    await page.route('**/api/community/threads', async (route) => {
      calls += 1;
      payload = route.request().postDataJSON() as Record<string, unknown>;
      await gate;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ id: 'keyboard-thread', title: 'Keyboard post', updatedAt: '2026-11-17T10:00:00', messages: [] }),
      });
    });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));

    await page.goto('/communities/feed');
    await page.getByTestId('community-feed-composer').locator('button').first().click();
    await expect(page.locator('#feed-post-title')).toBeFocused();

    await page.locator('#feed-post-title').fill('Keyboard post');
    await page.locator('#feed-post-body').fill('Sent with Control and Enter.');
    await page.locator('#feed-post-body').press('Control+Enter');
    await page.locator('#feed-post-body').press('Control+Enter');

    // The second shortcut lands while the first request is still open and must be ignored.
    await expect.poll(() => calls).toBe(1);
    release();
    await expect(page.getByText('Published in the community feed.')).toBeVisible();
    expect(payload).toMatchObject({ title: 'Keyboard post', initialMessage: 'Sent with Control and Enter.' });
  });

  test('returns to the same feed position after visiting Mi Actividad', async ({ page }) => {
    const items = Array.from({ length: 20 }, (_, index) => ({
      type: 'signal',
      id: `scroll-${index}`,
      communityId: cid,
      title: `Scroll report ${index}`,
      summary: 'Summary',
      happenedAt: new Date(Date.UTC(2026, 10, 17, 10, 0, 0) - index * 60000).toISOString().slice(0, 19),
      freshness: '',
      confirmationCount: 0,
      viewerConfirmed: false,
      following: false,
      reactions: {},
      viewerReaction: null,
      authorName: 'Vecino',
      authorRole: 'CITIZEN',
      commentCount: 0,
    }));
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(items) }));
    await page.route('**/api/signals/mine*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, last: true }),
      }));

    await page.goto('/communities/feed');
    await expect(page.locator('.social-feed-item')).toHaveCount(20);
    await page.evaluate(() => {
      const scroller = document.querySelector('.app-main');
      if (scroller) scroller.scrollTop = 800;
    });
    await expect
      .poll(() => page.evaluate(() => document.querySelector('.app-main')?.scrollTop ?? 0))
      .toBeGreaterThan(700);

    await page.getByTestId('user-menu-toggle').click();
    await page.locator('.app-user-menu a[href="/mine"]').click();
    await expect(page).toHaveURL(/\/mine/);
    await page.goBack();

    await expect(page.locator('.social-feed-item')).toHaveCount(20);
    await expect
      .poll(() => page.evaluate(() => document.querySelector('.app-main')?.scrollTop ?? 0))
      .toBeGreaterThan(700);
  });

  test('likes a report from the feed and updates the count', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));
    await page.route('**/api/signals/signal-1/react', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ reactions: { LIKE: 3 }, viewerReaction: 'LIKE' }),
      }));

    await page.goto('/communities/feed');
    const card = page.locator('.social-feed-item').filter({ hasText: 'Unsafe crosswalk' });
    const trigger = card.getByTestId('community-feed-react-trigger-signal-1');
    await trigger.click();
    const like = card.getByTestId('community-feed-react-like-signal-1');
    await expect(like).toContainText('0');
    await like.click();
    await expect(trigger).toContainText('· 3');
    await expect(trigger).toContainText('Like');
  });

  test('votes up and down on a report', async ({ page }) => {
    await seedAuthenticatedApp(page, 'CITIZEN', { communityId: cid });
    await page.route('**/api/community/feed*', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));
    await page.route('**/api/signals/signal-1/react', (route) => {
      const type = (route.request().postDataJSON() as { type: string }).type;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ reactions: { [type]: 1 }, viewerReaction: type }),
      });
    });

    await page.goto('/communities/feed');
    const card = page.locator('.social-feed-item').filter({ hasText: 'Unsafe crosswalk' });
    const up = card.getByTestId('community-feed-upvote-signal-1');
    const down = card.getByTestId('community-feed-downvote-signal-1');

    await up.click();
    await expect(up).toHaveAttribute('aria-pressed', 'true');
    await expect(up).toContainText('1');

    await down.click();
    await expect(down).toHaveAttribute('aria-pressed', 'true');
    await expect(down).toContainText('1');
    await expect(up).toHaveAttribute('aria-pressed', 'false');
    await expect(up).toContainText('0');
  });
});
