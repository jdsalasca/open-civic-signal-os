import { expect, test } from "@playwright/test";
import { mockAppBootstrap } from "./helpers/session";
import { mockHelpCenter } from "./helpers/dashboard";
import type { CommunityTrustMetrics } from "../types";

/**
 * The Community area shares one page-header pattern (title + subtitle + action
 * slot), a comfortable reading measure for long-form content, and must never
 * scroll horizontally at 360px. These checks run entirely against mocked routes
 * so they need the Vite server and nothing else.
 */
const communityId = "11111111-1111-1111-1111-111111111111";

const trustPayload: CommunityTrustMetrics = {
  communityId,
  communityName: "Los Rosales",
  communitySlug: "los-rosales",
  period: "LAST_30_DAYS",
  startDate: "2026-02-07",
  endDate: "2026-03-07",
  generatedAt: "2026-03-07T10:00:00",
  lastUpdatedAt: "2026-03-07T09:40:00",
  freshness: "20m ago",
  lowData: false,
  lowDataReason: null,
  cards: [
    {
      key: "resolution_rate",
      label: "Case closure rate",
      value: "50%",
      unit: "percentage",
      definition: "Share of issues reported during the selected period that are already closed or rejected.",
      formula: "resolved_or_rejected_signals_created_in_period / total_signals_created_in_period",
      supportingText: "2 of 4 reported issues have already reached a closed state.",
    },
  ],
  breakdowns: [],
};

async function openTrustMetrics(page: import("@playwright/test").Page) {
  await page.addInitScript((activeCommunityId) => {
    window.localStorage.setItem(
      "auth-storage",
      JSON.stringify({
        state: {
          accessToken: "consistency-token",
          userName: "consistency_user",
          activeRole: "COORDINATOR",
          rawRoles: ["COORDINATOR", "CITIZEN"],
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
              userId: "22222222-2222-2222-2222-222222222222",
              communityId: activeCommunityId,
              communityName: "Los Rosales",
              communitySlug: "los-rosales",
              parentCommunityId: null,
              breadcrumb: [{ id: activeCommunityId, name: "Los Rosales", slug: "los-rosales" }],
              role: "COORDINATOR",
              createdBy: "22222222-2222-2222-2222-222222222222",
              createdAt: "2026-03-07T08:00:00",
            },
          ],
          threadListStateByCommunity: {},
        },
        version: 0,
      })
    );
  }, communityId);

  await page.route("**/api/community/trust-metrics?**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(trustPayload) }));

  await mockAppBootstrap(page, communityId);
  await mockHelpCenter(page);

  await page.goto("/communities/trust");
  await expect(page.getByTestId("community-trust-overview")).toBeVisible();
}

test.describe("Community visual consistency", () => {
  test("community views share one page header with a title, subtitle and action slot", async ({ page }) => {
    await openTrustMetrics(page);

    const header = page.getByTestId("civic-page-header");
    await expect(header).toBeVisible();
    await expect(header).toHaveClass(/civic-page-header-with-actions/);
    await expect(header.getByTestId("page-header-title")).toContainText(/trust metrics/i);
    await expect(header.getByTestId("page-header-description")).toBeVisible();

    const actions = header.getByTestId("page-header-actions");
    await expect(actions).toBeVisible();
    // The action slot is the header's own child, not a sibling wrapper div.
    await expect(actions).toHaveCount(1);
  });

  test("long-form community content uses a comfortable reading measure", async ({ page }) => {
    await openTrustMetrics(page);

    const maxWidth = await page.evaluate(() => {
      const probe = document.createElement("p");
      probe.className = "community-reading-width";
      probe.textContent = "measure";
      document.body.appendChild(probe);
      const value = getComputedStyle(probe).maxWidth;
      probe.remove();
      return value;
    });

    // 44rem at the default 16px root.
    expect(maxWidth).toBe("704px");
  });

  test("community views do not scroll horizontally at 360px or 1440px", async ({ page }) => {
    await openTrustMetrics(page);

    for (const width of [360, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      await page.waitForTimeout(150);
      const { scrollWidth, clientWidth } = await page.evaluate(() => ({
        scrollWidth: document.documentElement.scrollWidth,
        clientWidth: document.documentElement.clientWidth,
      }));
      expect(scrollWidth, `horizontal overflow at ${width}px`).toBeLessThanOrEqual(clientWidth + 1);
    }
  });
});
