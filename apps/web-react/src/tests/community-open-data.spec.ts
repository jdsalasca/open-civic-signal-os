import { expect, test } from "@playwright/test";
import type {
  CommunityOpenDataCenter,
  CommunityOpenDataToken,
  CommunityPermissionPolicy,
} from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";

test.describe("Community open-data center", () => {
  test("creates scoped tokens, downloads datasets, and revokes token access", async ({ page }) => {
    let createPayload: Record<string, unknown> | null = null;
    let exportRequests = 0;

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
        scope: "MANAGE_OPEN_DATA_EXPORTS",
        allowedRoles: ["COORDINATOR", "PUBLIC_SERVANT_LIAISON"],
        updatedBy: "coordinator",
        updatedAt: "2026-03-05T12:00:00",
      },
    ];

    let center: CommunityOpenDataCenter = {
      communityId,
      communityName: "Los Rosales",
      defaultRateLimitPerHour: 120,
      datasets: [
        {
          resource: "SIGNALS",
          description: "Community issue registry with priority and intake context.",
          formats: ["CSV", "JSON"],
          externalPath: `/api/open-data/${communityId}/signals`,
        },
        {
          resource: "METRICS",
          description: "Trust metrics cards exported across standard reporting periods.",
          formats: ["CSV", "JSON"],
          externalPath: `/api/open-data/${communityId}/metrics`,
        },
      ],
      tokens: [],
      recentAccessLogs: [],
    };

    await page.route("**/api/communities/my", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            userId: "33333333-3333-3333-3333-333333333333",
            communityId,
            communityName: "Los Rosales",
            communitySlug: "los-rosales",
            parentCommunityId: null,
            breadcrumb: [{ id: communityId, name: "Los Rosales", slug: "los-rosales" }],
            role: "COORDINATOR",
            createdBy: "33333333-3333-3333-3333-333333333333",
            createdAt: "2026-03-01T09:00:00",
          },
        ]),
      });
    });

    await page.route(`**/api/communities/${communityId}/permissions`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(permissionPolicies),
      });
    });

    await page.route("**/api/community/exports/center?communityId=**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(center),
      });
    });

    await page.route("**/api/community/exports/tokens", async (route) => {
      createPayload = route.request().postDataJSON() as Record<string, unknown>;
      const createdToken: CommunityOpenDataToken = {
        id: "44444444-4444-4444-4444-444444444444",
        label: String(createPayload.label),
        tokenPrefix: "ocs_44444444-4444-",
        scopes: (createPayload.scopes as CommunityOpenDataToken["scopes"]) ?? [],
        rateLimitPerHour: Number(createPayload.rateLimitPerHour),
        active: true,
        createdAt: "2026-03-06T12:00:00",
        lastUsedAt: null,
        revokedAt: null,
      };
      center = {
        ...center,
        tokens: [createdToken, ...center.tokens],
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          token: createdToken,
          plainToken: "ocs_44444444-4444-4444-4444-444444444444_secret",
        }),
      });
    });

    await page.route("**/api/community/exports/signals?communityId=**", async (route) => {
      exportRequests += 1;
      center = {
        ...center,
        recentAccessLogs: [
          {
            id: `log-${exportRequests}`,
            accessChannel: "USER_EXPORT",
            exportType: "SIGNALS",
            format: "CSV",
            actorUsername: "coordinator",
            tokenLabel: null,
            note: "Privileged community export downloaded through export center.",
            createdAt: "2026-03-06T12:05:00",
          },
          ...center.recentAccessLogs,
        ],
      };
      await route.fulfill({
        status: 200,
        contentType: "text/csv",
        headers: {
          "Content-Disposition": `attachment; filename="community_${communityId}_signals.csv"`,
        },
        body: "id,title\n1,Streetlight outage\n",
      });
    });

    await page.route(/.*\/api\/community\/exports\/tokens\/.*communityId=.*/, async (route) => {
      const tokenId = route.request().url().split("/tokens/")[1].split("?")[0];
      center = {
        ...center,
        tokens: center.tokens.map((token) =>
          token.id === tokenId
            ? {
                ...token,
                active: false,
                revokedAt: "2026-03-06T12:10:00",
              }
            : token
        ),
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(center.tokens[0]),
      });
    });

    await page.goto("http://127.0.0.1:5173/communities/open-data");

    await expect(page.getByTestId("community-open-data-datasets-card")).toContainText("Signals");

    await page.getByTestId("community-open-data-token-label").fill("Transparency portal");
    await page.getByTestId("community-open-data-rate-limit").fill("24");
    await page.getByTestId("community-open-data-token-scope-EXPORT_METRICS").click();
    await page.getByTestId("community-open-data-create-token-button").click();

    await expect.poll(() => createPayload).not.toBeNull();
    expect(createPayload).toMatchObject({
      communityId,
      label: "Transparency portal",
      rateLimitPerHour: 24,
      scopes: ["EXPORT_SIGNALS", "EXPORT_METRICS"],
    });

    await expect(page.getByTestId("community-open-data-plain-token-panel")).toContainText("Copy this token now");
    await expect(page.getByTestId("community-open-data-plain-token-panel")).toContainText("ocs_44444444-4444-4444-4444-444444444444_secret");
    await expect(page.getByTestId("community-open-data-tokens-card")).toContainText("Transparency portal");

    const download = page.waitForEvent("download");
    await page.getByTestId("community-open-data-download-signals-csv").click();
    const downloadArtifact = await download;
    expect(downloadArtifact.suggestedFilename()).toBe(`community_${communityId}_signals.csv`);
    await expect(page.getByTestId("community-open-data-logs-card")).toContainText("Privileged community export downloaded through export center.");

    await page.getByTestId("community-open-data-revoke-token-44444444-4444-4444-4444-444444444444").click();
    await expect(page.getByTestId("community-open-data-tokens-card")).toContainText("Revoked");
  });
});
