import { expect, test } from "@playwright/test";
import type { Signal } from "../types";

const signalId = "12121212-1212-1212-1212-121212121212";

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

    await page.goto(`/signal/${signalId}`);

    const provenance = page.getByTestId("signal-detail-provenance");
    await expect(provenance).toBeVisible();
    await expect(page.getByTestId("signal-detail-provenance-channel")).toContainText("Institutional update");
    await expect(page.getByTestId("signal-detail-provenance-ref")).toContainText("municipal-ticket/88213");
    await expect(page.getByTestId("signal-detail-provenance-version")).toContainText("v1");
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

    await page.goto(`/signal/${signalId}`);

    await expect(page.getByTestId("signal-detail-provenance-channel")).toContainText("Community web form");
    await expect(page.getByTestId("signal-detail-provenance-ref")).toHaveCount(0);
  });
});