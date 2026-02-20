import { chromium } from "playwright";
import fs from "node:fs";
import path from "node:path";

const baseUrl = "http://localhost:3002";
const stamp = new Date().toISOString().replace(/[:.]/g, "-");
const outDir = path.resolve("output/playwright", `audit-fullstack-${stamp}`);
fs.mkdirSync(outDir, { recursive: true });

const viewports = [
  { name: "desktop", viewport: { width: 1365, height: 768 } },
  { name: "mobile", viewport: { width: 390, height: 844 } }
];

const summary = { baseUrl, outDir, runs: [] };

for (const vp of viewports) {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: vp.viewport });
  const page = await context.newPage();

  const run = {
    viewport: vp.name,
    screenshots: [],
    consoleErrors: [],
    pageErrors: [],
    requestFailures: [],
    badResponses: []
  };

  page.on("console", (msg) => {
    if (msg.type() === "error") {
      run.consoleErrors.push({ text: msg.text(), location: msg.location() });
    }
  });
  page.on("pageerror", (err) => run.pageErrors.push({ message: err.message, stack: err.stack }));
  page.on("requestfailed", (req) =>
    run.requestFailures.push({
      url: req.url(),
      method: req.method(),
      failure: req.failure()?.errorText || "unknown"
    })
  );
  page.on("response", (res) => {
    if (res.status() >= 400) {
      run.badResponses.push({ url: res.url(), method: res.request().method(), status: res.status() });
    }
  });

  const shot = async (name) => {
    const file = path.join(outDir, `${vp.name}-${name}.png`);
    await page.screenshot({ path: file, fullPage: true });
    run.screenshots.push(file);
  };

  await page.goto(`${baseUrl}/login`, { waitUntil: "networkidle" });
  await shot("login");

  await page.getByTestId("login-username-input").fill("admin");
  await page.getByTestId("login-password-input").fill("admin12345");
  await page.getByTestId("login-submit-button").click();
  await page.waitForLoadState("networkidle");
  await shot("dashboard");

  await page.goto(`${baseUrl}/report`, { waitUntil: "networkidle" });
  await shot("report");

  await page.goto(`${baseUrl}/forbidden`, { waitUntil: "networkidle" });
  await shot("forbidden");

  await page.goto(`${baseUrl}/route-not-found-audit`, { waitUntil: "networkidle" });
  await shot("notfound");

  summary.runs.push(run);
  await context.close();
  await browser.close();
}

const summaryPath = path.join(outDir, "summary.json");
fs.writeFileSync(summaryPath, JSON.stringify(summary, null, 2));
console.log(summaryPath);
