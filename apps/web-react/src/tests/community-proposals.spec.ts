import { expect, test } from "@playwright/test";
import type { CommunityProposalDeliberation, CommunityProposalVoting } from "../types";

const communityId = "11111111-1111-1111-1111-111111111111";
const signalId = "22222222-2222-2222-2222-222222222222";

test.describe("Community proposals", () => {
  test("creates a structured proposal and renders comparable detail sections with typed deliberation", async ({ page }) => {
    let savedPayload: Record<string, unknown> | null = null;
    let deliberationPayload: Record<string, unknown> | null = null;
    let moderatedPayload: Record<string, unknown> | null = null;
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
        voteMode: "YES_NO",
        resultVisibility: "COMMUNITY",
        eligibilityRule: "VERIFIED_MEMBERS",
        votingOpensAt: "2026-03-05T10:00:00",
        votingClosesAt: "2026-03-20T18:00:00",
        createdAt: "2026-03-05T10:00:00",
        updatedAt: "2026-03-05T10:00:00",
      },
    ];
    let voting: CommunityProposalVoting = {
      proposalId: proposals[0].id,
      config: {
        voteMode: "YES_NO",
        resultVisibility: "COMMUNITY",
        eligibilityRule: "VERIFIED_MEMBERS",
        votingOpensAt: "2026-03-05T10:00:00",
        votingClosesAt: "2026-03-20T18:00:00",
      },
      openForVoting: true,
      canCurrentUserVote: true,
      blockedReason: null,
      currentUserVote: null,
      tally: {
        visible: true,
        visibilityReason: null,
        totalBallots: 0,
        distinctVoters: 0,
        turnoutPercentage: 0,
        forVotes: 0,
        againstVotes: 0,
        averageScore: null,
        scoreDistribution: [],
      },
      auditSummary: {
        acceptedVotes: 0,
        duplicateBlockedAttempts: 0,
        eligibilityBlockedAttempts: 0,
        closedWindowBlockedAttempts: 0,
      },
    };
    let deliberation: CommunityProposalDeliberation = {
      proposalId: proposals[0].id,
      counts: {
        pros: 1,
        cons: 0,
        questions: 0,
        evidence: 1,
        visibleEntries: 2,
        hiddenEntries: 0,
      },
      entries: [
        {
          id: "66666666-6666-6666-6666-666666666666",
          proposalId: proposals[0].id,
          authorId: "44444444-4444-4444-4444-444444444444",
          authorUsername: "coordinator",
          entryType: "PRO",
          content: "Families already documented repeated close calls during school entry hours.",
          supportingLink: null,
          hidden: false,
          moderationReason: null,
          hiddenByUsername: null,
          hiddenAt: null,
          createdAt: "2026-03-05T10:10:00",
          updatedAt: "2026-03-05T10:10:00",
        },
        {
          id: "77777777-7777-7777-7777-777777777777",
          proposalId: proposals[0].id,
          authorId: "44444444-4444-4444-4444-444444444444",
          authorUsername: "coordinator",
          entryType: "EVIDENCE",
          content: "Traffic memo with incident log from the school committee.",
          supportingLink: "https://example.com/evidence/memo",
          hidden: false,
          moderationReason: null,
          hiddenByUsername: null,
          hiddenAt: null,
          createdAt: "2026-03-05T10:20:00",
          updatedAt: "2026-03-05T10:20:00",
        },
      ],
    };

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

    await page.route("**/api/community/proposals/*/deliberation", async (route) => {
      const method = route.request().method();
      if (method === "GET") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(deliberation),
        });
        return;
      }

      deliberationPayload = route.request().postDataJSON() as Record<string, unknown>;
      const newEntry: CommunityProposalDeliberation["entries"][number] = {
        id: "88888888-8888-8888-8888-888888888888",
        proposalId: proposals[0].id,
        authorId: "44444444-4444-4444-4444-444444444444",
        authorUsername: "admin",
        entryType: String(deliberationPayload.type) as CommunityProposalDeliberation["entries"][number]["entryType"],
        content: String(deliberationPayload.content),
        supportingLink: deliberationPayload.supportingLink ? String(deliberationPayload.supportingLink) : null,
        hidden: false,
        moderationReason: null,
        hiddenByUsername: null,
        hiddenAt: null,
        createdAt: "2026-03-05T11:10:00",
        updatedAt: "2026-03-05T11:10:00",
      };
      deliberation = {
        proposalId: proposals[0].id,
        counts: {
          pros: deliberation.counts.pros,
          cons: deliberation.counts.cons,
          questions: deliberation.counts.questions,
          evidence: deliberation.counts.evidence + (newEntry.entryType === "EVIDENCE" ? 1 : 0),
          visibleEntries: deliberation.counts.visibleEntries + 1,
          hiddenEntries: deliberation.counts.hiddenEntries,
        },
        entries: [...deliberation.entries, newEntry],
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(deliberation),
      });
    });

    await page.route("**/api/community/proposals/*/vote", async (route) => {
      const method = route.request().method();
      if (method === "GET") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(voting),
        });
        return;
      }

      const payload = route.request().postDataJSON() as Record<string, unknown>;
      const choice = payload.choice ? String(payload.choice) : null;
      const scoreValue = typeof payload.scoreValue === "number" ? payload.scoreValue : null;
      voting = {
        ...voting,
        canCurrentUserVote: false,
        blockedReason: "You already voted on this proposal.",
        currentUserVote: {
          voterId: "44444444-4444-4444-4444-444444444444",
          voterUsername: "admin",
          membershipRole: "COORDINATOR",
          verifiedMember: true,
          choice: choice === "FOR" || choice === "AGAINST" ? choice : null,
          scoreValue,
          castAt: "2026-03-05T11:30:00",
        },
        tally: {
          ...voting.tally,
          totalBallots: voting.tally.totalBallots + 1,
          distinctVoters: voting.tally.distinctVoters + 1,
          turnoutPercentage: 33.3,
          forVotes: choice === "FOR" ? voting.tally.forVotes + 1 : voting.tally.forVotes,
          againstVotes: choice === "AGAINST" ? voting.tally.againstVotes + 1 : voting.tally.againstVotes,
          averageScore: scoreValue,
          scoreDistribution:
            scoreValue == null
              ? voting.tally.scoreDistribution
              : [1, 2, 3, 4, 5].map((score) => ({
                  score,
                  count: score === scoreValue ? 1 : 0,
                })),
        },
        auditSummary: {
          ...voting.auditSummary,
          acceptedVotes: voting.auditSummary.acceptedVotes + 1,
        },
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(voting),
      });
    });

    await page.route("**/api/community/proposals/*/deliberation/*/moderate", async (route) => {
      moderatedPayload = route.request().postDataJSON() as Record<string, unknown>;
      deliberation = {
        ...deliberation,
        counts: {
          ...deliberation.counts,
          visibleEntries: deliberation.counts.visibleEntries - 1,
          hiddenEntries: deliberation.counts.hiddenEntries + 1,
          evidence: deliberation.counts.evidence - 1,
        },
        entries: deliberation.entries.map((entry): CommunityProposalDeliberation["entries"][number] =>
          entry.id === "88888888-8888-8888-8888-888888888888"
            ? {
                ...entry,
                hidden: true,
                moderationReason: String(moderatedPayload?.reason ?? ""),
                hiddenByUsername: "admin",
                hiddenAt: "2026-03-05T11:20:00",
              }
            : entry
        ),
      };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(deliberation),
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
        voteMode: String(savedPayload.voteMode) as "YES_NO" | "SCORE_1_5",
        resultVisibility: String(savedPayload.resultVisibility) as "COMMUNITY" | "AFTER_VOTE",
        eligibilityRule: String(savedPayload.eligibilityRule) as "ALL_MEMBERS" | "VERIFIED_MEMBERS",
        votingOpensAt: null,
        votingClosesAt: savedPayload.votingClosesAt ? String(savedPayload.votingClosesAt) : null,
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

    const deliberationCard = page.getByTestId("community-proposal-deliberation-card");
    await expect(deliberationCard).toContainText("Structured deliberation");
    await expect(deliberationCard).toContainText("Arguments and evidence");
    await expect(deliberationCard).toContainText("Families already documented repeated close calls");

    await page.getByTestId("proposal-deliberation-type-select").click();
    await page.getByText("Evidence", { exact: true }).click();
    await page.getByTestId("proposal-deliberation-link-input").fill("https://example.com/evidence/crossing-data");
    await page
      .getByTestId("proposal-deliberation-content-input")
      .fill("Survey log with traffic counts and parent observations for the last two weeks.");
    await page.getByTestId("proposal-deliberation-submit-button").click();

    await expect.poll(() => deliberationPayload).not.toBeNull();
    expect(deliberationPayload).toMatchObject({
      type: "EVIDENCE",
      supportingLink: "https://example.com/evidence/crossing-data",
    });
    await expect(deliberationCard).toContainText("Survey log with traffic counts and parent observations for the last two weeks.");
    await expect(deliberationCard).toContainText("3 visible entries");

    page.once("dialog", async (dialog) => {
      expect(dialog.message()).toContain("Why should this entry be hidden");
      await dialog.accept("Duplicate evidence, merged into the updated traffic count source.");
    });
    await page.getByTestId("proposal-deliberation-moderate-88888888-8888-8888-8888-888888888888").click();

    await expect.poll(() => moderatedPayload).not.toBeNull();
    expect(moderatedPayload).toMatchObject({
      hidden: true,
      reason: "Duplicate evidence, merged into the updated traffic count source.",
    });
    await expect(deliberationCard).toContainText("1 hidden by moderation");
    await expect(deliberationCard).toContainText("Hidden");
  });
});
