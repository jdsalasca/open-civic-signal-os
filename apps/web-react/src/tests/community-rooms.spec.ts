import { expect, test } from "@playwright/test";
import type {
  CommunityPermissionPolicy,
  CommunityRoomDetail,
  CommunityRoomWorkspace,
} from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";
const roomId = "55555555-5555-5555-5555-555555555555";

test.describe("Community coordination rooms", () => {
  test("posts mentions, surfaces the audit trail, and mutes per user", async ({ page }) => {
    await page.addInitScript((activeCommunityId) => {
      window.localStorage.setItem(
        "auth-storage",
        JSON.stringify({
          state: {
            accessToken: "test-token",
            userName: "coordinator",
            activeRole: "SUPER_ADMIN",
            rawRoles: ["SUPER_ADMIN", "PUBLIC_SERVANT", "CITIZEN"],
            isLoggedIn: true,
            isHydrated: true,
          },
          version: 0,
        })
      );
      window.localStorage.setItem(
        "community-storage",
        JSON.stringify({
          state: {
            activeCommunityId,
            membershipsLoadedAt: Date.now(),
            memberships: [
              {
                userId: "33333333-3333-3333-3333-333333333333",
                communityId: activeCommunityId,
                communityName: "Los Rosales",
                communitySlug: "los-rosales",
                parentCommunityId: null,
                breadcrumb: [{ id: activeCommunityId, name: "Los Rosales", slug: "los-rosales" }],
                role: "COORDINATOR",
                createdBy: "33333333-3333-3333-3333-333333333333",
                createdAt: "2026-03-01T09:00:00",
              },
            ],
            threadListStateByCommunity: {},
          },
          version: 0,
        })
      );
    }, communityId);

    const permissionPolicies: CommunityPermissionPolicy[] = [
      {
        communityId,
        scope: "POST_ROOM_MESSAGE",
        allowedRoles: ["MEMBER", "MODERATOR", "COORDINATOR", "PUBLIC_SERVANT_LIAISON"],
        updatedBy: "coordinator",
        updatedAt: "2026-03-20T12:00:00",
      },
      {
        communityId,
        scope: "MANAGE_ROOMS",
        allowedRoles: ["COORDINATOR", "PUBLIC_SERVANT_LIAISON"],
        updatedBy: "coordinator",
        updatedAt: "2026-03-20T12:00:00",
      },
    ];

    let workspace: CommunityRoomWorkspace = {
      communityId,
      communityName: "Los Rosales",
      mentionableUsernames: ["coordinator", "vecina"],
      rooms: [
        {
          id: roomId,
          communityId,
          projectBoardId: null,
          name: "Night patrol working group",
          topic: "Coordinate the lighting audit walk",
          createdBy: "33333333-3333-3333-3333-333333333333",
          createdAt: "2026-03-20T09:00:00",
          archived: false,
          messageCount: 0,
          unreadMentionCount: 0,
          muted: false,
          lastActivityAt: "2026-03-20T09:00:00",
        },
      ],
      mentionInbox: { communityId, unreadTotal: 0, items: [] },
    };

    let detail: CommunityRoomDetail = {
      id: roomId,
      communityId,
      projectBoardId: null,
      name: "Night patrol working group",
      topic: "Coordinate the lighting audit walk",
      createdBy: "33333333-3333-3333-3333-333333333333",
      createdAt: "2026-03-20T09:00:00",
      archived: false,
      muted: false,
      mutedAt: null,
      messageCount: 0,
      hasMoreMessages: false,
      unreadMentionCount: 0,
      messages: [],
    };

    let messagePayload: Record<string, unknown> | null = null;
    let muteCalls = 0;

    await page.route(`**/api/communities/${communityId}/permissions`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(permissionPolicies),
      });
    });

    await page.route("**/api/community/rooms/workspace?communityId=**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(workspace),
      });
    });

    await page.route(`**/api/community/rooms/${roomId}?communityId=**`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(detail),
      });
    });

    await page.route("**/api/community/rooms/messages", async (route) => {
      messagePayload = route.request().postDataJSON() as Record<string, unknown>;
      const body = String(messagePayload.body);
      const posted = {
        id: "message-1",
        roomId,
        authorId: "33333333-3333-3333-3333-333333333333",
        authorName: "coordinator",
        body,
        createdAt: "2026-03-20T10:00:00",
        mentionedUserIds: ["77777777-7777-7777-7777-777777777777"],
        mentionsCurrentUser: false,
      };
      detail = { ...detail, messageCount: 1, messages: [posted] };
      workspace = { ...workspace, rooms: [{ ...workspace.rooms[0], messageCount: 1 }] };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(posted),
      });
    });

    await page.route(`**/api/community/rooms/${roomId}/mute?communityId=**`, async (route) => {
      muteCalls += 1;
      const muted = new URL(route.request().url()).searchParams.get("muted") === "true";
      detail = { ...detail, muted, mutedAt: muted ? "2026-03-20T10:05:00" : null };
      workspace = {
        ...workspace,
        rooms: [{ ...workspace.rooms[0], muted }],
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ roomId, communityId, muted, mutedAt: detail.mutedAt }),
      });
    });

    await page.route("**/api/community/rooms/mentions/read?communityId=**", async (route) => {
      workspace = {
        ...workspace,
        mentionInbox: {
          communityId,
          unreadTotal: 0,
          items: [
            {
              id: "mention-1",
              roomId,
              roomName: "Night patrol working group",
              messageId: "message-1",
              messagePreview: "@vecina please confirm the 7pm walk start",
              mentionedUserId: "77777777-7777-7777-7777-777777777777",
              mentionedBy: "33333333-3333-3333-3333-333333333333",
              mentionedByName: "coordinator",
              createdAt: "2026-03-20T10:00:00",
              readAt: "2026-03-20T10:06:00",
            },
          ],
        },
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(workspace.mentionInbox),
      });
    });

    await page.goto("/communities/rooms");

    await expect(page.getByTestId("community-rooms-list-card")).toContainText("Night patrol working group");
    await expect(page.getByTestId("community-rooms-stats-grid")).toContainText("Rooms");

    await page.getByTestId("community-rooms-message-input").fill("@vecina please confirm the 7pm walk start");
    await page.getByTestId("community-rooms-send").click();

    await expect.poll(() => messagePayload).not.toBeNull();
    expect(messagePayload).toMatchObject({
      communityId,
      roomId,
      body: "@vecina please confirm the 7pm walk start",
    });
    await expect(page.getByTestId("community-rooms-message-list")).toContainText("@vecina please confirm the 7pm walk start");

    await page.getByTestId("community-rooms-mute-toggle").click();
    await expect.poll(() => muteCalls).toBe(1);
    await expect(page.getByTestId("community-rooms-mute-toggle")).toContainText("Unmute room");

    await page.getByTestId("community-rooms-name").fill("Bridge inspection");
    await page.getByTestId("community-rooms-topic").fill("Coordinate the bridge safety audit");
    await expect(page.getByTestId("community-rooms-create-card")).toBeVisible();
  });
});