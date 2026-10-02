import { expect, test } from "@playwright/test";
import type { PrioritizationFormula, Signal } from "../types";

const signalId = "12121212-1212-1212-1212-121212121212";

const formula: PrioritizationFormula = {
  version: "v1",
  formula: "(Urgency * 30) + (Impact * 25) + min(People/10, 30) + min(Votes/5, 15)",
  effectiveFrom: "2026-02-19",
  weights: [
    { factor: "urgency", input: "urgency (1-5)", expression: "urgency * 30", cap: 150 },
    { factor: "impact", input: "impact (1-5)", expression: "impact * 25", cap: 125 },
    { factor: "affectedPeople", input: "affectedPeople (citizens)", expression: "min(affectedPeople / 10, 30)", cap: 30 },
    { factor: "communityVotes", input: "communityVotes", expression: "min(communityVotes / 5, 15)", cap: 15 },
  ],
  cappedFactors: ["affectedPeople", "communityVotes"],
  changeNote: "Initial published weighting: urgency 30, impact 25, affected people capped at 30, community votes capped at 15.",
};

async function mockSignalAndFormula(page: import("@playwright/test").Page, signal: Signal) {
  await page.route("**/api/signals/formula", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(formula),
    });
  });
  await page.route(`**/api/signals/${signalId}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(signal),
    });
  });
  await page.route(`**/api/signals/${signalId}/history`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
}

test.describe("Signal provenance", () => {
  test("shows the ingest channel and scoring rule version for an institutional update", async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem(
        "auth-storage",
        JSON.stringify({
          state: {
            accessToken: "test-token",
            userName: "liaison",
            activeRole: "PUBLIC_SERVANT",
            rawRoles: ["PUBLIC_SERVANT", "CITIZEN"],
            isLoggedIn: true,
            isHydrated: true,
          },
          version: 0,
        })
      );
    });

    const signal: Signal = {
      id: signalId,
      title: "Streetlight outage on the main corridor",
      description: "Main corridor lights are out for three consecutive nights.",
      category: "infrastructure",
      status: "IN_PROGRESS",
      priorityScore: 177,
      scoreBreakdown: { urgency: 90, impact: 75, affectedPeople: 12, communityVotes: 0 },
      communityVotes: 0,
      reactions: {},
      explainabilitySummary: { version: "v1", topFactors: [], summary: "Deterministic factors." },
      sourceChannel: "INSTITUTIONAL_UPDATE",
      sourceRef: "municipal-ticket/88213",
      transformationVersion: "v1",
    };

    await mockSignalAndFormula(page, signal);

    await page.goto(`/signal/${signalId}`);

    const provenance = page.getByTestId("signal-detail-provenance");
    await expect(provenance).toBeVisible();
    await expect(page.getByTestId("signal-detail-provenance-channel")).toContainText("Institutional update");
    await expect(page.getByTestId("signal-detail-provenance-ref")).toContainText("municipal-ticket/88213");
    await expect(page.getByTestId("signal-detail-provenance-version")).toContainText("v1");

    // The "why ranked here" panel must reflect the backend formula, not hardcoded copy.
    const weights = page.getByTestId("signal-detail-formula-weights");
    await expect(weights).toContainText("urgency * 30");
    await expect(weights).toContainText("min(affectedPeople / 10, 30)");
    await expect(page.getByTestId("signal-detail-formula-meta")).toContainText("2026-02-19");
  });

  test("labels a citizen report differently from an institutional update", async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem(
        "auth-storage",
        JSON.stringify({
          state: {
            accessToken: "test-token",
            userName: "citizen",
            activeRole: "CITIZEN",
            rawRoles: ["CITIZEN"],
            isLoggedIn: true,
            isHydrated: true,
          },
          version: 0,
        })
      );
    });

    const signal: Signal = {
      id: signalId,
      title: "Pothole on the school route",
      description: "Deep pothole in front of the primary school gate.",
      category: "infrastructure",
      status: "NEW",
      priorityScore: 120,
      scoreBreakdown: { urgency: 60, impact: 50, affectedPeople: 10, communityVotes: 0 },
      communityVotes: 0,
      reactions: {},
      explainabilitySummary: { version: "v1", topFactors: [], summary: "Deterministic factors." },
      sourceChannel: "WEB_FORM",
      sourceRef: null,
      transformationVersion: "v1",
    };

    await mockSignalAndFormula(page, signal);

    await page.goto(`/signal/${signalId}`);

    await expect(page.getByTestId("signal-detail-provenance-channel")).toContainText("Community web form");
    await expect(page.getByTestId("signal-detail-provenance-ref")).toHaveCount(0);
  });
});