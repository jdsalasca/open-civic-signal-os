import { expect, test } from "@playwright/test";
import { mockAppBootstrap } from './helpers/session';
import { mockHelpCenter } from './helpers/dashboard';
import type {
  CommunityIntegrationCenter,
  CommunityPermissionPolicy,
} from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";
const integrationId = "cccccccc-cccc-cccc-cccc-cccccccccccc";
const deliveryId = "dddddddd-dddd-dddd-dddd-dddddddddddd";

test.describe("Community outbound integrations", () => {
  test("surfaces a failed delivery, retries it, and connects a new channel", async ({ page }) => {
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
        scope: "MANAGE_INTEGRATIONS",
        allowedRoles: ["COORDINATOR", "PUBLIC_SERVANT_LIAISON"],
        updatedBy: "coordinator",
        updatedAt: "2026-03-22T12:00:00",
      },
    ];

    const failedDelivery = {
      id: deliveryId,
      integrationId,
      integrationName: "Neighbourhood board feed",
      channel: "WEBHOOK",
      eventType: "OFFICIAL_ANNOUNCEMENT",
      referenceId: "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
      status: "FAILED" as const,
      attempts: 1,
      lastError: "Endpoint returned HTTP 500",
      createdAt: "2026-03-22T10:00:00",
      completedAt: "2026-03-22T10:00:01",
    };

    let center: CommunityIntegrationCenter = {
      communityId,
      communityName: "Los Rosales",
      supportedChannels: ["WEBHOOK", "EMAIL_DIGEST", "CALENDAR_FEED", "MAP_LINK"],
      integrationCount: 1,
      pendingDeliveries: 0,
      failedDeliveries: 1,
      integrations: [
        {
          id: integrationId,
          communityId,
          channel: "WEBHOOK",
          name: "Neighbourhood board feed",
          targetUri: "https://example.org/hooks/civic",
          enabled: true,
          autoRetry: true,
          dispatchable: true,
          createdBy: "33333333-3333-3333-3333-333333333333",
          createdAt: "2026-03-21T08:00:00",
          lastAttemptAt: "2026-03-22T10:00:00",
          lastSuccessAt: null,
          consecutiveFailures: 1,
          pendingDeliveries: 0,
          failedDeliveries: 1,
        },
      ],
      recentDeliveries: [failedDelivery],
    };

    let createPayload: Record<string, unknown> | null = null;

    await page.route(`**/api/communities/${communityId}/permissions`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(permissionPolicies),
      });
    });

    await page.route("**/api/community/integrations/center?communityId=**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(center),
      });
    });

    await page.route(`**/api/community/integrations/deliveries/${deliveryId}/retry?communityId=**`, async (route) => {
      const delivered = { ...failedDelivery, status: "DELIVERED" as const, attempts: 2, lastError: null };
      center = {
        ...center,
        failedDeliveries: 0,
        integrations: center.integrations.map((integration) => ({
          ...integration,
          consecutiveFailures: 0,
          failedDeliveries: 0,
          lastSuccessAt: "2026-03-22T11:00:00",
        })),
        recentDeliveries: [delivered],
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(delivered),
      });
    });

    await page.route("**/api/community/integrations", async (route) => {
      if (route.request().method() !== "POST") {
        await route.fallback();
        return;
      }
      createPayload = route.request().postDataJSON() as Record<string, unknown>;
      const created = {
        ...center.integrations[0],
        id: "ffffffff-ffff-ffff-ffff-ffffffffffff",
        name: String(createPayload.name),
        channel: createPayload.channel as typeof center.integrations[0]["channel"],
        targetUri: String(createPayload.targetUri),
        consecutiveFailures: 0,
        failedDeliveries: 0,
      };
      center = { ...center, integrationCount: 2, integrations: [created, ...center.integrations] };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(created),
      });
    });

    await mockAppBootstrap(page, communityId);

    await mockHelpCenter(page);

    await page.goto("/communities/integrations");

    await expect(page.getByTestId("community-integrations-list-card")).toContainText("Neighbourhood board feed");
    await expect(page.getByTestId("community-integrations-stats-grid")).toContainText("Failed");

    const deliveryRow = page.getByTestId(`community-integration-delivery-${deliveryId}`);
    await expect(deliveryRow).toContainText("Endpoint returned HTTP 500");

    await page.getByTestId(`community-integration-retry-${deliveryId}`).click();
    await expect(page.getByTestId(`community-integration-delivery-${deliveryId}`)).toContainText("Delivered");
    await expect(page.getByTestId("community-integrations-deliveries")).not.toContainText("HTTP 500");

    await page.getByTestId("community-integrations-name").fill("Calendar mirror");
    await page.getByTestId("community-integrations-target").fill("https://example.org/calendar");
    await page.getByTestId("community-integrations-secret").fill("a-sufficiently-long-secret");
    await page.getByTestId("community-integrations-create-submit").click();

    await expect.poll(() => createPayload).not.toBeNull();
    expect(createPayload).toMatchObject({
      communityId,
      channel: "WEBHOOK",
      name: "Calendar mirror",
      targetUri: "https://example.org/calendar",
      secret: "a-sufficiently-long-secret",
    });
    await expect(page.getByTestId("community-integrations-list-card")).toContainText("Calendar mirror");
  });
});