import { expect, test } from "@playwright/test";
import type { CommunityDecision, CommunityPermissionPolicy } from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";
const decisionId = "22222222-2222-2222-2222-222222222222";
const proposalId = "33333333-3333-3333-3333-333333333333";
const governanceId = "44444444-4444-4444-4444-444444444444";
const projectId = "55555555-5555-5555-5555-555555555555";

test.describe("Community decision ledger", () => {
  test("records decisions with linked proposal, governance, and execution context", async ({ page }) => {
    let createPayload: Record<string, unknown> | null = null;
    const policies: CommunityPermissionPolicy[] = [
      {
        communityId,
        scope: "MANAGE_DECISION_LEDGER",
        allowedRoles: ["COORDINATOR", "PUBLIC_SERVANT_LIAISON"],
      },
    ];

    let decisions: CommunityDecision[] = [
      {
        id: decisionId,
        communityId,
        linkedProposalId: proposalId,
        linkedProposalTitle: "Safer school crossing rollout",
        governanceDocumentId: governanceId,
        governanceDocumentTitle: "Assembly act approving the crossing rollout",
        projectBoardId: projectId,
        projectBoardTitle: "Crossing execution board",
        decidedBy: "66666666-6666-6666-6666-666666666666",
        decidedByUsername: "coordinator",
        executionOwnerId: "77777777-7777-7777-7777-777777777777",
        executionOwnerUsername: "liaison",
        decisionType: "APPROVAL",
        decisionStatus: "IN_EXECUTION",
        approvalBasisType: "GOVERNANCE_RECORD",
        approvalBasisSummary: "Assembly minutes recorded unanimous approval and delegated delivery follow-through to the liaison team.",
        title: "Approve the safer crossing rollout",
        summary: "The assembly approved the rollout and linked it to the active execution board.",
        decidedAt: "2026-03-07T09:00:00",
        effectiveDate: "2026-03-10",
        createdAt: "2026-03-07T09:00:00",
        updatedAt: "2026-03-07T09:00:00",
      },
    ];

    await page.addInitScript((activeCommunityId) => {
      window.localStorage.setItem(
        "auth-storage",
        JSON.stringify({
          state: {
            accessToken: "test-token",
            userName: "coordinator",
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
                userId: "66666666-6666-6666-6666-666666666666",
                communityId: activeCommunityId,
                communityName: "Los Rosales",
                communitySlug: "los-rosales",
                parentCommunityId: null,
                breadcrumb: [{ id: activeCommunityId, name: "Los Rosales", slug: "los-rosales" }],
                role: "COORDINATOR",
                createdBy: "66666666-6666-6666-6666-666666666666",
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
            userId: "66666666-6666-6666-6666-666666666666",
            communityId,
            communityName: "Los Rosales",
            communitySlug: "los-rosales",
            parentCommunityId: null,
            breadcrumb: [{ id: communityId, name: "Los Rosales", slug: "los-rosales" }],
            role: "COORDINATOR",
            createdBy: "66666666-6666-6666-6666-666666666666",
            createdAt: "2026-03-07T08:00:00",
          },
        ]),
      });
    });

    await page.route("**/api/communities/*/permissions", async (route) => {
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(policies) });
    });

    await page.route("**/api/community/proposals?communityId=**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            id: proposalId,
            communityId,
            authorId: "66666666-6666-6666-6666-666666666666",
            authorUsername: "coordinator",
            relatedSignalId: null,
            relatedSignalTitle: null,
            title: "Safer school crossing rollout",
            templateKey: "STANDARD_COMMUNITY_PROPOSAL",
            status: "PROPOSED",
            problemStatement: "Families need a safer crossing.",
            proposedSolution: "Approve signage, paint, and volunteer shifts.",
            estimatedCost: "COP 18M",
            beneficiariesSummary: "Students and families.",
            supportingLinks: [],
            createdAt: "2026-03-07T08:00:00",
            updatedAt: "2026-03-07T08:00:00",
          },
        ]),
      });
    });

    await page.route("**/api/community/governance?communityId=**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            id: governanceId,
            communityId,
            createdBy: "66666666-6666-6666-6666-666666666666",
            authorUsername: "coordinator",
            title: "Assembly act approving the crossing rollout",
            summary: "Minutes approving the rollout.",
            documentType: "MINUTES",
            visibility: "COMMUNITY",
            tags: ["assembly"],
            currentVersionNumber: 1,
            currentVersion: null,
            versions: [],
            createdAt: "2026-03-07T08:00:00",
            updatedAt: "2026-03-07T08:00:00",
          },
        ]),
      });
    });

    await page.route("**/api/community/projects?communityId=**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            id: projectId,
            communityId,
            linkedProposalId: proposalId,
            linkedProposalTitle: "Safer school crossing rollout",
            ownerId: "77777777-7777-7777-7777-777777777777",
            ownerUsername: "liaison",
            title: "Crossing execution board",
            summary: "Track signage, volunteer shifts, and paint delivery.",
            dueDate: "2026-03-20",
            taskCounts: { todo: 1, inProgress: 0, done: 0 },
            tasks: [],
            createdAt: "2026-03-07T08:00:00",
            updatedAt: "2026-03-07T08:00:00",
          },
        ]),
      });
    });

    await page.route("**/api/community/decisions?**", async (route) => {
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(decisions) });
    });

    await page.route("**/api/community/decisions", async (route) => {
      createPayload = route.request().postDataJSON() as Record<string, unknown>;
      const created: CommunityDecision = {
        ...decisions[0],
        id: "88888888-8888-8888-8888-888888888888",
        title: String(createPayload.title),
        summary: String(createPayload.summary),
        decisionType: String(createPayload.decisionType) as CommunityDecision["decisionType"],
        decisionStatus: String(createPayload.decisionStatus) as CommunityDecision["decisionStatus"],
        approvalBasisType: String(createPayload.approvalBasisType) as CommunityDecision["approvalBasisType"],
        approvalBasisSummary: String(createPayload.approvalBasisSummary),
        executionOwnerUsername: String(createPayload.executionOwnerUsername),
      };
      decisions = [created, ...decisions];
      await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(created) });
    });

    await page.goto("http://127.0.0.1:5173/communities/decisions");

    await expect(page.getByTestId("community-decision-detail-card")).toContainText("Approve the safer crossing rollout");
    await expect(page.getByTestId("community-decision-related-card")).toContainText("Assembly act approving the crossing rollout");

    await page.getByTestId("decision-title-input").fill("Approve Saturday volunteer deployment");
    await page.getByTestId("decision-summary-input").fill("The committee approved Saturday deployment and linked it to the active execution board for follow-through.");
    await page.getByTestId("decision-proposal-select").click();
    await page.getByText("Safer school crossing rollout", { exact: true }).click();
    await page.getByTestId("decision-governance-select").click();
    await page.getByText("Assembly act approving the crossing rollout", { exact: true }).click();
    await page.getByTestId("decision-project-select").click();
    await page.getByText("Crossing execution board", { exact: true }).click();
    await page.getByTestId("decision-execution-owner-input").fill("liaison");
    await page.getByTestId("decision-basis-summary-input").fill("Assembly minutes and board ownership were reviewed before recording this execution decision.");
    await page.getByTestId("decision-submit-button").click();

    await expect.poll(() => createPayload).not.toBeNull();
    expect(createPayload).toMatchObject({
      communityId,
      linkedProposalId: proposalId,
      governanceDocumentId: governanceId,
      projectBoardId: projectId,
      executionOwnerUsername: "liaison",
      title: "Approve Saturday volunteer deployment",
    });

    await expect(page.getByTestId("community-decision-detail-card")).toContainText("Approve Saturday volunteer deployment");
    await expect(page.getByTestId("community-decision-detail-card")).toContainText("Assembly minutes and board ownership were reviewed");
  });
});
