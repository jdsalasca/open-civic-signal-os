import { expect, test } from "@playwright/test";
import type { CommunityTrustMetrics } from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";

test.describe("Community trust metrics dashboard", () => {
  test("shows explainable trust cards and degrades cleanly for low-data communities", async ({ page }) => {
    let requestedPeriods: string[] = [];

    await page.addInitScript((activeCommunityId) => {
      window.localStorage.setItem(
        "auth-storage",
        JSON.stringify({
          state: {
            accessToken: "trust-token",
            userName: "metrics_user",
            activeRole: "CITIZEN",
            rawRoles: ["CITIZEN"],
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

    await page.route("**/api/communities/my", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            userId: "22222222-2222-2222-2222-222222222222",
            communityId,
            communityName: "Los Rosales",
            communitySlug: "los-rosales",
            parentCommunityId: null,
            breadcrumb: [{ id: communityId, name: "Los Rosales", slug: "los-rosales" }],
            role: "COORDINATOR",
            createdBy: "22222222-2222-2222-2222-222222222222",
            createdAt: "2026-03-07T08:00:00",
          },
        ]),
      });
    });

    await page.route("**/api/community/trust-metrics?**", async (route) => {
      const url = new URL(route.request().url());
      const period = url.searchParams.get("period") ?? "LAST_30_DAYS";
      requestedPeriods.push(period);

      const payload: CommunityTrustMetrics =
        period === "LAST_7_DAYS"
          ? {
              communityId,
              communityName: "Los Rosales",
              communitySlug: "los-rosales",
              period: "LAST_7_DAYS",
              startDate: "2026-03-01",
              endDate: "2026-03-07",
              generatedAt: "2026-03-07T10:00:00",
              lastUpdatedAt: "2026-03-07T09:40:00",
              freshness: "20m ago",
              lowData: true,
              lowDataReason: "Only one report and no finished execution tasks were recorded in this period yet.",
              cards: [
                {
                  key: "resolution_rate",
                  label: "Case closure rate",
                  value: "0%",
                  unit: "percentage",
                  definition: "Share of issues reported during the selected period that are already closed or rejected.",
                  formula: "resolved_or_rejected_signals_created_in_period / total_signals_created_in_period",
                  supportingText: "0 of 1 reported issues have reached a closed state.",
                },
              ],
              breakdowns: [
                {
                  key: "signals_by_status",
                  title: "Issue outcomes in this period",
                  description: "How newly reported issues are distributed across current lifecycle states.",
                  items: [],
                },
              ],
            }
          : {
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
                {
                  key: "participation_coverage",
                  label: "Participation coverage",
                  value: "67%",
                  unit: "percentage",
                  definition: "Share of community members who left a traceable contribution in this period.",
                  formula: "distinct_active_contributors_in_period / total_community_members",
                  supportingText: "6 of 9 known community members contributed.",
                },
              ],
              breakdowns: [
                {
                  key: "signals_by_status",
                  title: "Issue outcomes in this period",
                  description: "How newly reported issues are distributed across current lifecycle states.",
                  items: [
                    { label: "RESOLVED", value: 2, share: 50 },
                    { label: "NEW", value: 2, share: 50 },
                  ],
                },
                {
                  key: "tasks_by_stage",
                  title: "Execution tasks touched in this period",
                  description: "How active board tasks are distributed across TODO, IN PROGRESS, and DONE.",
                  items: [
                    { label: "DONE", value: 3, share: 60 },
                    { label: "IN PROGRESS", value: 2, share: 40 },
                  ],
                },
              ],
            };

      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(payload) });
    });

    await page.goto("http://127.0.0.1:5173/communities/trust");

    await expect(page.getByTestId("community-trust-overview")).toContainText("Los Rosales");
    await expect(page.getByTestId("community-trust-freshness")).toContainText("20m ago");
    await expect(page.getByTestId("community-trust-card-resolution_rate")).toContainText("50%");
    await expect(page.getByTestId("community-trust-card-participation_coverage")).toContainText("67%");
    await expect(page.getByTestId("community-trust-breakdown-signals_by_status")).toContainText("Issue outcomes in this period");

    await page.getByTestId("community-trust-period-filter").click();
    await page.getByText("Last 7 days", { exact: true }).click();

    await expect(page.getByTestId("community-trust-low-data")).toContainText("Only one report and no finished execution tasks");
    await expect(page.getByTestId("community-trust-breakdown-signals_by_status")).toContainText("Not enough data yet");
    await expect.poll(() => requestedPeriods).toContain("LAST_7_DAYS");
  });
});
