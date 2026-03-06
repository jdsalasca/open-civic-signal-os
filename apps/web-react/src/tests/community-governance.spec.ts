import { expect, test } from "@playwright/test";
import type { GovernanceDocument } from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";
const documentId = "22222222-2222-2222-2222-222222222222";

test.describe("Community governance library", () => {
  test("creates, filters, and versions governance documents", async ({ page }) => {
    let createPayload: Record<string, unknown> | null = null;
    let versionPayload: Record<string, unknown> | null = null;

    const document: GovernanceDocument = {
      id: documentId,
      communityId,
      createdBy: "33333333-3333-3333-3333-333333333333",
      authorUsername: "coordinator",
      title: "Community coexistence agreement",
      summary: "Operating agreement for the Los Rosales coexistence committee.",
      documentType: "AGREEMENT",
      visibility: "COMMUNITY",
      tags: ["coexistence", "committee"],
      currentVersionNumber: 1,
      currentVersion: {
        id: "44444444-4444-4444-4444-444444444444",
        documentId,
        createdBy: "33333333-3333-3333-3333-333333333333",
        authorUsername: "coordinator",
        versionNumber: 1,
        content: "Initial agreement text for the coexistence committee.",
        changeSummary: "Initial publication for community review.",
        sourceUrl: "https://example.com/agreement/v1",
        effectiveDate: "2026-03-01",
        meetingDate: "2026-02-27",
        createdAt: "2026-03-01T12:00:00",
      },
      versions: [
        {
          id: "44444444-4444-4444-4444-444444444444",
          documentId,
          createdBy: "33333333-3333-3333-3333-333333333333",
          authorUsername: "coordinator",
          versionNumber: 1,
          content: "Initial agreement text for the coexistence committee.",
          changeSummary: "Initial publication for community review.",
          sourceUrl: "https://example.com/agreement/v1",
          effectiveDate: "2026-03-01",
          meetingDate: "2026-02-27",
          createdAt: "2026-03-01T12:00:00",
        },
      ],
      createdAt: "2026-03-01T12:00:00",
      updatedAt: "2026-03-01T12:00:00",
    };

    let documents: GovernanceDocument[] = [document];

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

    await page.route("**/api/community/governance?communityId=**", async (route) => {
      const url = new URL(route.request().url());
      const query = url.searchParams.get("query");
      const documentType = url.searchParams.get("documentType");
      const filtered = documents.filter((item) => {
        const matchesQuery = query
          ? [item.title, item.summary, ...item.tags].join(" ").toLowerCase().includes(query.toLowerCase())
          : true;
        const matchesType = documentType ? item.documentType === documentType : true;
        return matchesQuery && matchesType;
      });
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(filtered),
      });
    });

    await page.route("**/api/community/governance", async (route) => {
      createPayload = route.request().postDataJSON() as Record<string, unknown>;
      const created: GovernanceDocument = {
        ...document,
        id: "55555555-5555-5555-5555-555555555555",
        title: String(createPayload.title),
        summary: String(createPayload.summary),
        documentType: String(createPayload.documentType) as GovernanceDocument["documentType"],
        visibility: String(createPayload.visibility) as GovernanceDocument["visibility"],
        tags: (createPayload.tags as string[]) ?? [],
        currentVersionNumber: 1,
        currentVersion: {
          id: "66666666-6666-6666-6666-666666666666",
          documentId: "55555555-5555-5555-5555-555555555555",
          createdBy: "33333333-3333-3333-3333-333333333333",
          authorUsername: "coordinator",
          versionNumber: 1,
          content: String(createPayload.content),
          changeSummary: String(createPayload.changeSummary),
          sourceUrl: (createPayload.sourceUrl as string | null) ?? null,
          effectiveDate: (createPayload.effectiveDate as string | null) ?? null,
          meetingDate: (createPayload.meetingDate as string | null) ?? null,
          createdAt: "2026-03-06T10:30:00",
        },
        versions: [
          {
            id: "66666666-6666-6666-6666-666666666666",
            documentId: "55555555-5555-5555-5555-555555555555",
            createdBy: "33333333-3333-3333-3333-333333333333",
            authorUsername: "coordinator",
            versionNumber: 1,
            content: String(createPayload.content),
            changeSummary: String(createPayload.changeSummary),
            sourceUrl: (createPayload.sourceUrl as string | null) ?? null,
            effectiveDate: (createPayload.effectiveDate as string | null) ?? null,
            meetingDate: (createPayload.meetingDate as string | null) ?? null,
            createdAt: "2026-03-06T10:30:00",
          },
        ],
        createdAt: "2026-03-06T10:30:00",
        updatedAt: "2026-03-06T10:30:00",
      };
      documents = [created, ...documents];
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(created),
      });
    });

    await page.route("**/api/community/governance/*/versions", async (route) => {
      versionPayload = route.request().postDataJSON() as Record<string, unknown>;
      const updated: GovernanceDocument = {
        ...documents[0],
        currentVersionNumber: 2,
        currentVersion: {
          id: "77777777-7777-7777-7777-777777777777",
          documentId: documents[0].id,
          createdBy: "33333333-3333-3333-3333-333333333333",
          authorUsername: "coordinator",
          versionNumber: 2,
          content: String(versionPayload.content),
          changeSummary: String(versionPayload.changeSummary),
          sourceUrl: (versionPayload.sourceUrl as string | null) ?? null,
          effectiveDate: (versionPayload.effectiveDate as string | null) ?? null,
          meetingDate: (versionPayload.meetingDate as string | null) ?? null,
          createdAt: "2026-03-06T11:00:00",
        },
        versions: [
          {
            id: "77777777-7777-7777-7777-777777777777",
            documentId: documents[0].id,
            createdBy: "33333333-3333-3333-3333-333333333333",
            authorUsername: "coordinator",
            versionNumber: 2,
            content: String(versionPayload.content),
            changeSummary: String(versionPayload.changeSummary),
            sourceUrl: (versionPayload.sourceUrl as string | null) ?? null,
            effectiveDate: (versionPayload.effectiveDate as string | null) ?? null,
            meetingDate: (versionPayload.meetingDate as string | null) ?? null,
            createdAt: "2026-03-06T11:00:00",
          },
          ...documents[0].versions,
        ],
        updatedAt: "2026-03-06T11:00:00",
      };
      documents = [updated, ...documents.slice(1)];
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(updated),
      });
    });

    await page.goto("http://127.0.0.1:5173/communities/governance");

    await page.getByTestId("governance-title-input").fill("Community coexistence framework 2026");
    await page.getByTestId("governance-summary-input").fill("Shared framework for coexistence, meeting rhythm, and committee escalation rules.");
    await page.getByTestId("governance-tags-input").fill("coexistence, minutes, 2026");
    await page.getByTestId("governance-content-input").fill("This governance framework defines community coexistence norms, the committee operating cycle, and the escalation path for unresolved conflicts during 2026.");
    await page.getByTestId("governance-change-summary-input").fill("Initial publication for community reference.");
    await page.getByTestId("governance-source-url-input").fill("https://example.com/governance/framework-v1");
    await page.getByTestId("governance-effective-date-input").fill("2026-03-10");
    await page.getByTestId("governance-meeting-date-input").fill("2026-03-05");
    await page.getByTestId("governance-submit-button").click();

    await expect.poll(() => createPayload).not.toBeNull();
    expect(createPayload).toMatchObject({
      communityId,
      title: "Community coexistence framework 2026",
      tags: ["coexistence", "minutes", "2026"],
    });

    const detailCard = page.getByTestId("governance-detail-card");
    await expect(detailCard).toContainText("Community coexistence framework 2026");
    await expect(detailCard).toContainText("Current version v1");
    await expect(detailCard).toContainText("coexistence");

    await page.getByTestId("governance-query-input").fill("coexistence");
    await page.getByTestId("governance-type-filter").click();
    await page.getByText("Agreement", { exact: true }).click();
    await page.getByTestId("governance-apply-filters").click();

    await expect(page.getByTestId("governance-list-card")).toContainText("Community coexistence framework 2026");

    await page.getByTestId("governance-version-content-input").fill("This governance framework now includes a formal mediation stage before escalation to the wider committee.");
    await page.getByTestId("governance-version-change-summary-input").fill("Added mediation step before escalation.");
    await page.getByTestId("governance-version-source-url-input").fill("https://example.com/governance/framework-v2");
    await page.getByTestId("governance-version-effective-date-input").fill("2026-03-15");
    await page.getByTestId("governance-version-submit-button").click();

    await expect.poll(() => versionPayload).not.toBeNull();
    expect(versionPayload).toMatchObject({
      changeSummary: "Added mediation step before escalation.",
    });
    await expect(detailCard).toContainText("Current version v2");
    await expect(page.getByTestId("governance-versions-card")).toContainText("Version 2");
  });
});
