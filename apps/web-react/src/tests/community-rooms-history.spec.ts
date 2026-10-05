import { expect, test } from "@playwright/test";
import type { CommunityRoomDetail, CommunityRoomSummary } from "../types";

/**
 * A capped message list has to be visible as capped.
 *
 * The API bounds a room read at 50 messages by default and says so: `messageCount` is the true total
 * and `hasMoreMessages` states that the array is a page. That honesty is worthless if the UI renders
 * the page as though it were the room, so these tests pin what a reader is actually told.
 *
 * A coordinator deciding whether a group is still active is reading this screen. "Nothing after
 * March" and "we only fetched the last 50" are different facts and must not look the same.
 */

const communityId = "11111111-1111-1111-1111-111111111111";
const roomId = "55555555-5555-5555-5555-555555555555";
const userId = "33333333-3333-3333-3333-333333333333";

function message(index: number) {
  return {
    id: `message-${index}`,
    roomId,
    authorId: userId,
    authorName: "coordinator",
    body: `Field note ${index}`,
    createdAt: "2026-03-20T10:00:00",
    mentionedUserIds: [],
    mentionsCurrentUser: false,
  };
}

/**
 * The room list picks its own first room, so the session only needs a community and a user role that
 * actually exists. `PUBLIC_SERVANT` keeps this a non-admin reader; the coordinator rights come from the
 * community membership, not from the app-level role.
 */
async function seedSession(page: import("@playwright/test").Page) {
  await page.addInitScript(
    ([id, member]) => {
      localStorage.setItem(
        "auth-storage",
        JSON.stringify({
          state: {
            accessToken: "test-token",
            userName: "liaison",
            activeRole: "PUBLIC_SERVANT",
            rawRoles: ["PUBLIC_SERVANT"],
            isLoggedIn: true,
            isHydrated: true,
          },
          version: 0,
        }),
      );
      localStorage.setItem(
        "community-storage",
        JSON.stringify({
          state: {
            activeCommunityId: id,
            membershipsLoadedAt: Date.now(),
            memberships: [
              {
                userId: member,
                communityId: id,
                communityName: "Riverside District",
                communitySlug: "riverside-district",
                parentCommunityId: null,
                breadcrumb: [{ id, name: "Riverside District", slug: "riverside-district" }],
                role: "COORDINATOR",
                createdBy: member,
                createdAt: "2026-03-01T09:00:00",
              },
            ],
            threadListStateByCommunity: {},
          },
          version: 0,
        }),
      );
    },
    [communityId, userId],
  );
}

function summary(messageCount: number): CommunityRoomSummary {
  return {
    id: roomId,
    communityId,
    projectBoardId: null,
    name: "Night patrol working group",
    topic: "Coordinate the lighting audit walk",
    createdBy: userId,
    createdAt: "2026-03-01T09:00:00",
    archived: false,
    messageCount,
    unreadMentionCount: 0,
    muted: false,
    lastActivityAt: "2026-03-20T10:00:00",
  };
}

function detail(over: Partial<CommunityRoomDetail> = {}): CommunityRoomDetail {
  return {
    id: roomId,
    communityId,
    projectBoardId: null,
    name: "Night patrol working group",
    topic: "Coordinate the lighting audit walk",
    createdBy: userId,
    createdAt: "2026-03-01T09:00:00",
    archived: false,
    muted: false,
    mutedAt: null,
    messageCount: 0,
    hasMoreMessages: false,
    unreadMentionCount: 0,
    messages: [],
    ...over,
  };
}

test.describe("A truncated room history says so", () => {
  test("shows what is loaded out of the total when older messages exist", async ({ page }) => {
    await seedSession(page);
    await page.route("**/api/communities/**/permissions", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );
    await page.route("**/api/community/rooms/workspace?communityId=**", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ rooms: [summary(1204)], totalRooms: 1, mentionInbox: { rooms: [] } }),
      }),
    );
    await page.route(`**/api/community/rooms/${roomId}?communityId=**`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          detail({
            messageCount: 1204,
            hasMoreMessages: true,
            messages: Array.from({ length: 50 }, (_, index) => message(index)),
          }),
        ),
      }),
    );

    await page.goto("/communities/rooms");

    // Both numbers, because one alone is misleading: 50 alone hides that there are 1,204.
    await expect(page.getByTestId("community-rooms-history-note")).toBeVisible({ timeout: 30000 });
    await expect(page.getByTestId("community-rooms-history-note")).toContainText("50");
    await expect(page.getByTestId("community-rooms-history-note")).toContainText("1204");
  });

  test("loads older messages on request rather than refusing to page", async ({ page }) => {
    await seedSession(page);
    await page.route("**/api/communities/**/permissions", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );
    await page.route("**/api/community/rooms/workspace?communityId=**", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ rooms: [summary(1204)], totalRooms: 1, mentionInbox: { rooms: [] } }),
      }),
    );

    let requestedLimit: string | null = null;
    await page.route(`**/api/community/rooms/${roomId}?communityId=**`, (route) => {
      const url = new URL(route.request().url());
      requestedLimit = url.searchParams.get("limit");
      const limit = Number(requestedLimit ?? 50);
      const total = 1204;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          detail({
            messageCount: total,
            hasMoreMessages: limit < total,
            messages: Array.from({ length: Math.min(limit, total) }, (_, index) => message(index)),
          }),
        ),
      });
    });

    await page.goto("/communities/rooms");

    await expect(page.getByTestId("community-rooms-load-older")).toBeVisible({ timeout: 30000 });
    await page.getByTestId("community-rooms-load-older").click();

    // The request must actually raise the limit; a button that re-fetches the same page is a control
    // that lies.
    await expect.poll(() => requestedLimit).not.toBe("50");
    await expect.poll(() => Number(requestedLimit ?? 0)).toBeGreaterThan(50);
  });

  test("says nothing about paging when the whole room is loaded", async ({ page }) => {
    await seedSession(page);
    await page.route("**/api/communities/**/permissions", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
    );
    await page.route("**/api/community/rooms/workspace?communityId=**", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ rooms: [summary(12)], totalRooms: 1, mentionInbox: { rooms: [] } }),
      }),
    );
    await page.route(`**/api/community/rooms/${roomId}?communityId=**`, (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          detail({
            messageCount: 12,
            hasMoreMessages: false,
            messages: Array.from({ length: 12 }, (_, index) => message(index)),
          }),
        ),
      }),
    );

    await page.goto("/communities/rooms");

    await expect(page.getByTestId("community-rooms-message-list")).toBeVisible({ timeout: 30000 });
    // Offering to load more of a room that fits would train people to distrust the control.
    await expect(page.getByTestId("community-rooms-load-older")).toHaveCount(0);
    await expect(page.getByTestId("community-rooms-history-note")).toHaveCount(0);
  });
});