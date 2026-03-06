import { expect, test } from "@playwright/test";

const communityId = "11111111-1111-1111-1111-111111111111";
const signalId = "22222222-2222-2222-2222-222222222222";

test.describe("Community proposals", () => {
  test("creates a structured proposal and renders comparable detail sections", async ({ page }) => {
    let savedPayload: Record<string, unknown> | null = null;
    const proposals = [
      {
        id: "33333333-3333-3333-3333-333333333333",
        communityId,
        authorId: "44444444-4444-4444-4444-444444444444",
        authorUsername: "coordinator",
        relatedSignalId: signalId,
        relatedSignalTitle: "Unsafe pedestrian crossing",
        title: "Install raised crossing near the school",
        templateKey: "STANDARD_COMMUNITY_PROPOSAL",
        status: "PROPOSED",
        problemStatement: "Families cross at peak hours without physical traffic calming.",
        proposedSolution: "Build a raised crossing with paint and signage at the school gate.",
        estimatedCost: "COP 18M for civil works and signage.",
        beneficiariesSummary: "Students, parents, nearby residents, and daily drivers.",
        supportingLinks: ["https://example.com/existing-proposal"],
        createdAt: "2026-03-05T10:00:00",
        updatedAt: "2026-03-05T10:00:00",
      },
    ];

    await page.addInitScript(() => {
      window.localStorage.setItem(
        "auth-storage",
        JSON.stringify({
          state: {
            accessToken: "test-token",
            userName: "admin",
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
            activeCommunityId: communityId,
            membershipsLoadedAt: Date.now(),
            memberships: [
              {
                userId: "44444444-4444-4444-4444-444444444444",
                communityId,
                communityName: "Los Rosales",
                communitySlug: "los-rosales",
                parentCommunityId: null,
                breadcrumb: [{ id: communityId, name: "Los Rosales", slug: "los-rosales" }],
                role: "COORDINATOR",
                createdBy: "44444444-4444-4444-4444-444444444444",
                createdAt: "2026-03-05T09:00:00",
              },
            ],
            threadListStateByCommunity: {},
          },
          version: 0,
        })
      );
    });

    await page.route("**/api/communities/my", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            userId: "44444444-4444-4444-4444-444444444444",
            communityId,
            communityName: "Los Rosales",
            communitySlug: "los-rosales",
            parentCommunityId: null,
            breadcrumb: [{ id: communityId, name: "Los Rosales", slug: "los-rosales" }],
            role: "COORDINATOR",
            createdBy: "44444444-4444-4444-4444-444444444444",
            createdAt: "2026-03-05T09:00:00",
          },
        ]),
      });
    });

    await page.route("**/api/signals/prioritized**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          content: [
            {
              id: signalId,
              title: "Unsafe pedestrian crossing",
              description: "Crossing needs intervention.",
              category: "SAFETY",
              status: "NEW",
              priorityScore: 210,
              scoreBreakdown: {
                urgency: 4,
                impact: 4,
                affectedPeople: 60,
                communityVotes: 12,
              },
              communityVotes: 12,
              reactions: {},
              explainabilitySummary: {
                version: "v1",
                topFactors: [],
                summary: "High urgency around the school gate.",
              },
            },
          ],
          totalElements: 1,
          totalPages: 1,
          size: 20,
          number: 0,
        }),
      });
    });

    await page.route("**/api/community/proposals?communityId=**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(proposals),
      });
    });

    await page.route("**/api/community/proposals", async (route) => {
      savedPayload = route.request().postDataJSON() as Record<string, unknown>;
      proposals.unshift({
        id: "55555555-5555-5555-5555-555555555555",
        communityId,
        authorId: "44444444-4444-4444-4444-444444444444",
        authorUsername: "admin",
        relatedSignalId: signalId,
        relatedSignalTitle: "Unsafe pedestrian crossing",
        title: String(savedPayload.title),
        templateKey: "STANDARD_COMMUNITY_PROPOSAL",
        status: "PROPOSED",
        problemStatement: String(savedPayload.problemStatement),
        proposedSolution: String(savedPayload.proposedSolution),
        estimatedCost: String(savedPayload.estimatedCost),
        beneficiariesSummary: String(savedPayload.beneficiariesSummary),
        supportingLinks: savedPayload.supportingLinks as string[],
        createdAt: "2026-03-05T11:00:00",
        updatedAt: "2026-03-05T11:00:00",
      });
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(proposals[0]),
      });
    });

    await page.goto("http://127.0.0.1:5173/communities/proposals");

    await page.getByTestId("proposal-title-input").fill("Raised crossing and signage at school entrance");
    await page.getByTestId("proposal-problem-input").fill("Students and caregivers cross at peak hours without physical slowing measures at the main gate.");
    await page.getByTestId("proposal-solution-input").fill("Build a raised crossing, repaint lane markings, and add school-zone signage with reflective paint.");
    await page.getByTestId("proposal-cost-input").fill("COP 18M for construction, paint, and signs plus one week of traffic management.");
    await page.getByTestId("proposal-beneficiaries-input").fill("Students, families, nearby residents, and drivers using the corridor each day.");
    await page.getByTestId("proposal-link-input-0").fill("https://example.com/supporting-reference");
    await page.getByTestId("proposal-related-signal-select").click();
    await page.getByText("Unsafe pedestrian crossing", { exact: true }).click();
    await page.getByTestId("proposal-submit-button").click();

    await expect.poll(() => savedPayload).not.toBeNull();
    expect(savedPayload).toMatchObject({
      communityId,
      relatedSignalId: signalId,
      title: "Raised crossing and signage at school entrance",
    });

    const detailCard = page.getByTestId("community-proposal-detail-card");
    await expect(detailCard).toContainText("Problem to solve");
    await expect(detailCard).toContainText("Proposed solution");
    await expect(detailCard).toContainText("Estimated cost or effort");
    await expect(detailCard).toContainText("Unsafe pedestrian crossing");
    await expect(page.getByTestId("community-proposal-link-0")).toHaveAttribute("href", "https://example.com/supporting-reference");
  });
});
