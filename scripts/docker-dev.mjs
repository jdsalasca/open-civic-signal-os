#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import net from "node:net";
import process from "node:process";

const args = process.argv.slice(2);
const command = args[0] ?? "help";
const composeArgs = ["compose", "-f", "infra/docker-compose.dev.yml"];
const healthTargets = [
  { name: "API", url: "http://localhost:8081/actuator/health" },
  { name: "Web", url: "http://localhost:5173" }
];
const requiredHostPorts = [5173, 8081, 5432, 8025];

function run(commandName, commandArgs, options = {}) {
  const result = spawnSync(commandName, commandArgs, {
    stdio: "inherit",
    shell: false,
    ...options
  });
  return result.status ?? 1;
}

function capture(commandName, commandArgs) {
  return spawnSync(commandName, commandArgs, {
    stdio: "pipe",
    shell: false,
    encoding: "utf8"
  });
}

function printUsage() {
  console.log("Usage: node scripts/docker-dev.mjs <up|down|ps|logs|doctor>");
  console.log("");
  console.log("Commands:");
  console.log("  up      Start the hot-reload Docker dev stack and wait for web/api");
  console.log("  down    Stop the Docker dev stack");
  console.log("  ps      Show current Docker dev stack status");
  console.log("  logs    Show recent logs for the Docker dev stack");
  console.log("  doctor  Check Docker engine and compose prerequisites");
}

function ensureDockerAvailable() {
  const dockerVersion = capture("docker", ["version", "--format", "{{.Server.Version}}"]);
  if (dockerVersion.status !== 0) {
    console.error("Docker Engine is not available.");
    console.error("Start Docker Desktop or the Docker daemon, then rerun `npm run docker:dev:up`.");
    console.error("Expected local dev entrypoint for agents: `infra/docker-compose.dev.yml` only.");
    process.exit(1);
  }

  const composeVersion = capture("docker", ["compose", "version"]);
  if (composeVersion.status !== 0) {
    console.error("Docker Compose v2 is required but not available.");
    process.exit(1);
  }
}

function stackHasRunningServices() {
  const result = capture("docker", [...composeArgs, "ps", "-q"]);
  return result.status === 0 && result.stdout.trim().length > 0;
}

function assertPortIsAvailable(port) {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once("error", () => reject(new Error(`Port ${port} is already in use on the host.`)));
    server.once("listening", () => {
      server.close(() => resolve());
    });
    server.listen(port, "0.0.0.0");
  });
}

async function ensureRequiredPortsAvailable() {
  for (const port of requiredHostPorts) {
    try {
      await assertPortIsAvailable(port);
    } catch (error) {
      console.error(error.message);
      console.error("Stop the conflicting local process, then rerun `npm run docker:dev:up`.");
      console.error("Agents must not keep separate local frontend/backend servers on these ports.");
      process.exit(1);
    }
  }
}

async function waitForHealthyTargets(timeoutMs = 120000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const checks = await Promise.all(
      healthTargets.map(async (target) => {
        try {
          const response = await fetch(target.url);
          return { ...target, ok: response.ok };
        } catch {
          return { ...target, ok: false };
        }
      })
    );

    if (checks.every((check) => check.ok)) {
      console.log("Docker dev stack is healthy.");
      for (const check of checks) {
        console.log(`- ${check.name}: ${check.url}`);
      }
      return;
    }

    await new Promise((resolve) => setTimeout(resolve, 3000));
  }

  console.error("Docker dev stack did not become healthy in time.");
  console.error("Inspect with `npm run docker:dev:logs`.");
  process.exit(1);
}

switch (command) {
  case "up":
    ensureDockerAvailable();
    if (!stackHasRunningServices()) {
      await ensureRequiredPortsAvailable();
    }
    if (run("docker", [...composeArgs, "up", "--build", "-d"]) !== 0) {
      process.exit(1);
    }
    await waitForHealthyTargets();
    break;
  case "down":
    ensureDockerAvailable();
    process.exit(run("docker", [...composeArgs, "down"]));
    break;
  case "ps":
    ensureDockerAvailable();
    process.exit(run("docker", [...composeArgs, "ps"]));
    break;
  case "logs":
    ensureDockerAvailable();
    process.exit(run("docker", [...composeArgs, "logs", "--tail", "200"]));
    break;
  case "doctor":
    ensureDockerAvailable();
    console.log("Docker Engine and Docker Compose are available.");
    console.log("Canonical hot-reload stack: infra/docker-compose.dev.yml");
    console.log("Run `npm run docker:dev:up` before frontend/backend work.");
    break;
  default:
    printUsage();
    break;
}
