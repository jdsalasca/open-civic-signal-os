import { expect, test } from "@playwright/test";

import { seedAuthenticatedApp } from './helpers/session';
import { mockHelpCenter } from './helpers/dashboard';
import type { SignalAging, SignalMeta } from "../types";

test.describe("Dashboard case aging and SLA risk", () => {
  test("shows SLA counters, buckets, and the worst breaching case to staff", async ({ page }) => {
    const meta: SignalMeta = {
      totalSignals: 4,
      unresolvedSignals: 4,
      lastUpdatedAt: "2026-04-01T10:00:00",
      criticalScoreThreshold: 220,
    };

    const aging: SignalAging = {
      communityId: null,
      generatedAt: "2026-04-01T12:00:00",
      slaTargetDays: 30,
      unresolvedCount: 4,
      atRiskCount: 1,
      breachedCount: 1,
      medianAgeDays: 12,
      ageBuckets: [
        { bucket: "FRESH_0_7", count: 1 },
        { bucket: "AGING_7_14", count: 1 },
        { bucket: "STALE_14_PLUS", count: 1 },
        { bucket: "OVERDUE", count: 1 },
      ],
      atRiskSignals: [
        {
          id: "signal-breached",
          title: "Bridge safety inspection overdue",
          category: "infrastructure",
          status: "IN_PROGRESS",
          priorityScore: 190,
          ageDays: 45,
          slaTargetDays: 30,
          slaRisk: "BREACHED",
          daysOverTarget: 15,
          createdAt: "2026-02-15T09:00:00",
        },
        {
          id: "signal-risk",
          title: "Water supply interruption",
          category: "utilities",
          status: "NEW",
          priorityScore: 150,
          ageDays: 26,
          slaTargetDays: 30,
          slaRisk: "AT_RISK",
          daysOverTarget: 0,
          createdAt: "2026-03-06T09:00:00",
        },
      ],
      trend: [],
    };

    await seedAuthenticatedApp(page);

    await mockHelpCenter(page);


    await page.route("**/api/signals/meta*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(meta),
      });
    });

    await page.route("**/api/signals/prioritized*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 }),
      });
    });

    await page.route("**/api/signals/duplicates*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({}),
      });
    });

    await page.route("**/api/signals/aging*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(aging),
      });
    });

    await page.route("**/api/notifications/recent*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([]),
      });
    });

    await page.route("**/api/communities/my", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([]),
      });
    });

    await page.goto("/");

    const panel = page.getByTestId("dashboard-aging-panel");
    await expect(panel).toBeVisible();
    await expect(panel).toContainText("Case aging and SLA risk");
    await expect(panel).toContainText("SLA target: 30 days");
    await expect(panel).toContainText("0-7 days");
    await expect(panel).toContainText("Overdue: 1");

    const list = page.getByTestId("dashboard-aging-list");
    await expect(list).toContainText("Bridge safety inspection overdue");
    await expect(list).toContainText("Breached");
    await expect(list).toContainText("45 days old");
    await expect(list).toContainText("At risk");
  });

  test("hides the aging panel from citizens", async ({ page }) => {
    let agingCalls = 0;
    await seedAuthenticatedApp(page, "CITIZEN");
    await mockHelpCenter(page);

    await page.route("**/api/signals/meta*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ totalSignals: 0, unresolvedSignals: 0, lastUpdatedAt: null }),
      });
    });
    await page.route("**/api/signals/prioritized*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 }),
      });
    });
    await page.route("**/api/signals/aging*", async (route) => {
      agingCalls += 1;
      await route.fulfill({ status: 200, contentType: "application/json", body: "{}" });
    });
    await page.route("**/api/communities/my", async (route) => {
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([]) });
    });

    await page.goto("/");
    await expect(page.getByTestId("dashboard-aging-panel")).toHaveCount(0);
    expect(agingCalls).toBe(0);
  });
});