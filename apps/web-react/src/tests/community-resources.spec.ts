import { expect, test } from "@playwright/test";
import type {
  CommunityPermissionPolicy,
  CommunityResourceBoard,
} from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";
const resourceId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const bookingId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

test.describe("Community shared resources", () => {
  test("shows policy, exposes the approval queue, and records a decision", async ({ page }) => {
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
        scope: "BOOK_RESOURCES",
        allowedRoles: ["MEMBER", "MODERATOR", "COORDINATOR", "PUBLIC_SERVANT_LIAISON"],
        updatedBy: "coordinator",
        updatedAt: "2026-03-21T12:00:00",
      },
      {
        communityId,
        scope: "MANAGE_RESOURCES",
        allowedRoles: ["COORDINATOR", "PUBLIC_SERVANT_LIAISON"],
        updatedBy: "coordinator",
        updatedAt: "2026-03-21T12:00:00",
      },
    ];

    const pendingBooking = {
      id: bookingId,
      resourceId,
      resourceName: "Community hall",
      communityId,
      requesterId: "88888888-8888-8888-8888-888888888888",
      requesterName: "vecina",
      purpose: "Neighbourhood assembly",
      startsAt: "2026-04-03T19:00:00",
      endsAt: "2026-04-03T22:00:00",
      status: "PENDING_APPROVAL" as const,
      decisionNote: null,
      requestedAt: "2026-03-22T09:00:00",
      decidedAt: null,
      decidedByName: null,
      cancelledAt: null,
    };

    let board: CommunityResourceBoard = {
      communityId,
      communityName: "Los Rosales",
      resourceCount: 1,
      pendingApprovals: 1,
      resources: [
        {
          id: resourceId,
          communityId,
          name: "Community hall",
          description: "Seats 40 people with a projector.",
          locationLabel: "Community hall",
          requiresApproval: true,
          minNoticeHours: 24,
          maxBookingHours: 4,
          archived: false,
          createdBy: "33333333-3333-3333-3333-333333333333",
          createdAt: "2026-03-21T08:00:00",
          upcomingBlockingCount: 1,
          currentRequesterBookingId: null,
          currentRequesterBookingStatus: null,
          upcomingBookings: [pendingBooking],
        },
      ],
      myBookings: [],
      approvalsQueue: [pendingBooking],
    };

    let decisionPayload: Record<string, unknown> | null = null;

    await page.route(`**/api/communities/${communityId}/permissions`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(permissionPolicies),
      });
    });

    await page.route("**/api/community/resources/board?communityId=**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(board),
      });
    });

    await page.route("**/api/community/resources/bookings", async (route) => {
      decisionPayload = route.request().postDataJSON() as Record<string, unknown>;
      const approved = decisionPayload.approve === true;
      const decided = {
        ...pendingBooking,
        status: approved ? ("APPROVED" as const) : ("REJECTED" as const),
        decisionNote: String(decisionPayload.decisionNote ?? ""),
        decidedAt: "2026-03-22T10:00:00",
        decidedByName: "coordinator",
      };
      board = {
        ...board,
        pendingApprovals: 0,
        approvalsQueue: [],
        resources: board.resources.map((resource) => ({
          ...resource,
          upcomingBookings: approved ? [decided] : [],
          upcomingBlockingCount: approved ? 1 : 0,
        })),
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(decided),
      });
    });

    await page.goto("/communities/resources");

    const detail = page.getByTestId("community-resources-detail-card");
    await expect(page.getByTestId("community-resources-list-card")).toContainText("Community hall");
    await expect(detail).toContainText("Needs coordinator approval");
    await expect(detail).toContainText("Minimum notice: 24h");
    await expect(detail).toContainText("Maximum booking: 4h");
    await expect(detail).toContainText("vecina");

    const approvalRow = page.getByTestId(`community-resources-approval-${bookingId}`);
    await expect(approvalRow).toContainText("Neighbourhood assembly");

    await page.getByTestId(`community-resources-note-${bookingId}`).fill("Approved for the assembly");
    await page.getByTestId(`community-resources-approve-${bookingId}`).click();

    await expect.poll(() => decisionPayload).not.toBeNull();
    expect(decisionPayload).toMatchObject({
      communityId,
      bookingId,
      approve: true,
      decisionNote: "Approved for the assembly",
    });
    await expect(page.getByTestId("community-resources-approvals")).toContainText("No bookings are waiting");

    await expect(page.getByTestId("community-resources-request-card")).toBeVisible();
    await page.getByTestId("community-resources-purpose").fill("Community meeting");
    await expect(page.getByTestId("community-resources-create-card")).toBeVisible();
  });
});