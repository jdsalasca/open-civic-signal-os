import { expect, test } from "@playwright/test";
import type {
  CommunityActivityAttendanceStatus,
  CommunityActivityBoard,
  CommunityPermissionPolicy,
} from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";
const activityId = "66666666-6666-6666-6666-666666666666";
const signupId = "77777777-7777-7777-7777-777777777777";

test.describe("Community volunteer activities", () => {
  test("shows the signup window, blocks a full activity, and records attendance", async ({ page }) => {
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
        scope: "JOIN_ACTIVITIES",
        allowedRoles: ["MEMBER", "MODERATOR", "COORDINATOR", "PUBLIC_SERVANT_LIAISON"],
        updatedBy: "coordinator",
        updatedAt: "2026-03-21T12:00:00",
      },
      {
        communityId,
        scope: "MANAGE_ACTIVITIES",
        allowedRoles: ["COORDINATOR", "PUBLIC_SERVANT_LIAISON"],
        updatedBy: "coordinator",
        updatedAt: "2026-03-21T12:00:00",
      },
    ];

    const openActivity: CommunityActivityBoard["activities"][number] = {
      id: activityId,
      communityId,
      title: "Lighting audit walk",
      description: "Walk the main corridor and record broken lamps.",
      locationLabel: "Community hall",
      startsAt: "2026-04-02T19:00:00",
      endsAt: "2026-04-02T21:00:00",
      signupOpensAt: "2026-03-21T09:00:00",
      signupClosesAt: "2026-04-01T19:00:00",
      signupCapacity: 2,
      organizerId: "33333333-3333-3333-3333-333333333333",
      organizerName: "coordinator",
      createdAt: "2026-03-21T08:00:00",
      cancelled: false,
      signupWindowState: "OPEN",
      signupOpen: true,
      fullReason: null,
      confirmedCount: 1,
      fillRatePercent: 50,
      currentVolunteerSignupId: null,
      currentVolunteerStatus: null,
      roster: [
        {
          signupId,
          volunteerId: "88888888-8888-8888-8888-888888888888",
          volunteerName: "vecina",
          status: "CONFIRMED",
          attendanceStatus: "PENDING",
          createdAt: "2026-03-21T09:30:00",
          cancelledAt: null,
          attendedAt: null,
        },
      ],
    };

    let board: CommunityActivityBoard = {
      communityId,
      communityName: "Los Rosales",
      openActivities: 1,
      myUpcomingSignups: 0,
      activities: [openActivity],
    };

    let attendanceCalls = 0;
    let signupCalls = 0;

    await page.route(`**/api/communities/${communityId}/permissions`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(permissionPolicies),
      });
    });

    await page.route("**/api/community/activities/board?communityId=**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(board),
      });
    });

    await page.route(`**/api/community/activities/${activityId}/signups?communityId=**`, async (route) => {
      signupCalls += 1;
      const leaving = route.request().method() === "DELETE";
      board = {
        ...board,
        myUpcomingSignups: leaving ? 0 : 1,
        activities: [
          {
            ...openActivity,
            confirmedCount: leaving ? 1 : 2,
            fillRatePercent: leaving ? 50 : 100,
            currentVolunteerSignupId: leaving ? null : "99999999-9999-9999-9999-999999999999",
            currentVolunteerStatus: leaving ? null : "CONFIRMED",
            signupWindowState: leaving ? "OPEN" : "FULL",
            signupOpen: leaving,
            fullReason: leaving ? null : "This activity already reached its published capacity.",
          },
        ],
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          activityId,
          communityId,
          signupId: "99999999-9999-9999-9999-999999999999",
          status: "CONFIRMED",
          message: "signup_confirmed",
        }),
      });
    });

    await page.route(`**/api/community/activities/${activityId}/attendance?communityId=**`, async (route) => {
      attendanceCalls += 1;
      const payload = route.request().postDataJSON() as {
        signupId: string;
        attendanceStatus: CommunityActivityAttendanceStatus;
      };
      board = {
        ...board,
        activities: [
          {
            ...board.activities[0],
            roster: board.activities[0].roster.map((volunteer) =>
              volunteer.signupId === payload.signupId
                ? { ...volunteer, attendanceStatus: payload.attendanceStatus, attendedAt: "2026-04-02T21:30:00" }
                : volunteer
            ),
          },
        ],
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          activityId,
          communityId,
          signupId: payload.signupId,
          status: "CONFIRMED",
          message: `attendance_${payload.attendanceStatus.toLowerCase()}`,
        }),
      });
    });

    await page.goto("/communities/activities");

    await expect(page.getByTestId("community-activities-list-card")).toContainText("Lighting audit walk");
    await expect(page.getByTestId("community-activities-stats-grid")).toContainText("Open activities");

    const activityCard = page.getByTestId(`community-activity-${activityId}`);
    await expect(activityCard).toContainText("Signups open");
    await expect(activityCard).toContainText("50%");
    await expect(activityCard).toContainText("1/2");

    await page.getByTestId(`community-activity-signup-${activityId}`).click();
    await expect.poll(() => signupCalls).toBe(1);
    await expect(activityCard).toContainText("Full");

    await page.getByTestId(`community-activity-attended-${signupId}`).click();
    await expect.poll(() => attendanceCalls).toBe(1);
    await expect(page.getByTestId(`community-activity-roster-${activityId}`)).toContainText("Attended");

    await expect(page.getByTestId("community-activities-create-card")).toBeVisible();
    await page.getByTestId("community-activities-title").fill("Bridge inspection");
    await page.getByTestId("community-activities-description").fill("Coordinate the bridge safety audit");
    await page.getByTestId("community-activities-location").fill("Community hall");
    await expect(page.getByTestId("community-activities-capacity")).toBeVisible();
  });
});