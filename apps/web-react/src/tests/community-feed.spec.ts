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
    authorName: 'Sofía Ramírez',
    authorRole: 'CITIZEN',
    commentCount: null,
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
    await page.route('**/api/community/threads', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id: 'new-thread' }) }));
    await page.route('**/api/community/threads/new-thread/messages', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '{}' }));
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
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(feed) }));
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

    await page.getByTestId('community-feed-filter-signal').click();
    await expect(page.locator('.social-feed-item')).toHaveCount(1);
    await expect(page.locator('.social-feed-type')).toHaveText('Report');

    await page.getByTestId('community-feed-filter-all').click();
    await page.getByTestId('community-alerts-toggle').click();
    await expect(page.getByTestId('community-alerts-toggle')).toContainText('Alerts on');
    await page.getByTestId('community-feed-composer').locator('button').first().click();
    await page.locator('#feed-post-title').fill('A new community question');
    await page.locator('#feed-post-body').fill('This is the message body.');
    await page.getByTestId('community-feed-publish').click();

    await expect(page.getByText('Published in the community feed.')).toBeVisible();
    await expect(page.locator('#feed-post-title')).toHaveCount(0);
    await expect(page.locator('.social-feed-item')).toHaveCount(3);

    // Follow a report, confirm it and reply to a conversation from the feed itself.
    const signalCard = page.locator('.social-feed-item').filter({ hasText: 'Unsafe crosswalk' });
    await signalCard.getByTestId('community-feed-follow-signal-1').click();
    await expect(signalCard.getByTestId('community-feed-follow-signal-1')).toContainText('Following');
    await signalCard.getByText('I saw it too').click();
    await expect(signalCard.getByText(/Confirmed · 1/)).toBeVisible();

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
    await page.getByTestId('community-feed-react-like-signal-1').click();
    await expect(page.getByText('No connection. Check your internet and try again.')).toBeVisible();
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
    const like = card.getByTestId('community-feed-react-like-signal-1');
    await expect(like).toContainText('Like');
    await like.click();
    await expect(like).toContainText('· 3');
    await expect(like).toHaveAttribute('aria-pressed', 'true');
  });
});
