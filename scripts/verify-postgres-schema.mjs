// Runs the real migration files against PostgreSQL, then checks the suite's H2 schema matches it.
//
// Round 56 made the integration tests build their schema with Flyway on H2. That only stands in for
// production if both engines come out of the same migrations with the same tables and columns. They do
// today - 62 tables, 582 columns, identical - and nothing was enforcing it.
//
// Usage: node scripts/verify-postgres-schema.mjs
//
// Starts a throwaway PostgreSQL on 127.0.0.1:55432, migrates it with the real migration files using the
// official Flyway image, runs PostgresSchemaParityIT against it, and removes the container either way.
// Deliberately does not touch the developer's civic-db: that one keeps its data and its credentials, and
// this check should not depend on either.
import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';

const CONTAINER = 'signalos-pg-verify';
const NETWORK = 'signalos-pg-verify-net';
const PORT = '55432';
const DB = 'signalos_verify';
const USER = 'verify';
// Bound to loopback, thrown away with the container. Not a secret and not used anywhere else.
const PASSWORD = 'verify_only_not_a_secret';
const FLYWAY_IMAGE = 'flyway/flyway:11';

const repoRoot = resolve(import.meta.dirname, '..');
const migrations = resolve(repoRoot, 'apps/api-java/src/main/resources/db/migration');

function run(command, args, options = {}) {
  return execFileSync(command, args, { encoding: 'utf8', stdio: 'pipe', ...options });
}

function docker(...args) {
  return run('docker', args);
}

/** Maven's own location when Node cannot see it on PATH; the bare name otherwise. */
function mavenCommand() {
  if (process.platform !== 'win32') return { command: 'mvn', prefix: [] };
  for (const candidate of [
    'C:/ProgramData/chocolatey/lib/maven/apache-maven-3.9.11/bin/mvn.cmd',
    'C:/ProgramData/chocolatey/bin/mvn.cmd',
  ]) {
    if (existsSync(candidate)) return { command: 'cmd.exe', prefix: ['/c', candidate] };
  }
  return { command: 'mvn', prefix: [] };
}

function stopContainer() {
  try {
    docker('rm', '-f', CONTAINER);
  } catch {
    // Already gone. Nothing to clean up.
  }
  try {
    docker('network', 'rm', NETWORK);
  } catch {
    // Already gone.
  }
}

async function waitForPostgres() {
  for (let attempt = 0; attempt < 30; attempt++) {
    try {
      const out = docker('exec', CONTAINER, 'pg_isready', '-U', USER, '-d', DB);
      if (out.includes('accepting connections')) return;
    } catch {
      // Not up yet.
    }
    await new Promise((resolveWait) => setTimeout(resolveWait, 1000));
  }
  throw new Error(`PostgreSQL did not become ready on port ${PORT}`);
}

let failed = false;
try {
  stopContainer();

  // A dedicated network rather than host.docker.internal. That name is a Docker Desktop convenience
  // and does not resolve on a Linux runner, so a script that used it would pass on a developer's
  // Windows machine and fail in CI - the worst place to find out. Container name over a user-defined
  // network works identically on both.
  docker('network', 'create', NETWORK);

  console.log(`Starting a throwaway PostgreSQL on 127.0.0.1:${PORT}...`);
  docker('run', '-d', '--rm', '--name', CONTAINER,
    '--network', NETWORK,
    '-p', `127.0.0.1:${PORT}:5432`,
    '-e', `POSTGRES_DB=${DB}`,
    '-e', `POSTGRES_USER=${USER}`,
    '-e', `POSTGRES_PASSWORD=${PASSWORD}`,
    'postgres:15-alpine');

  await waitForPostgres();

  console.log('Applying every migration with the real migration files...');
  const migrated = docker('run', '--rm', '--network', NETWORK,
    '-v', `${migrations}:/flyway/sql`,
    FLYWAY_IMAGE, 'migrate',
    `-url=jdbc:postgresql://${CONTAINER}:5432/${DB}`,
    `-user=${USER}`,
    `-password=${PASSWORD}`,
    '-locations=filesystem:/flyway/sql');
  const applied = migrated.match(/Successfully applied (\d+) migrations/);
  console.log(applied ? `  ${applied[1]} migrations applied.` : '  Flyway finished.');

  // Validate after migrate: catches a migration whose checksum no longer matches what ran, which is the
  // failure that only shows up on a database that already has history.
  console.log('Validating...');
  docker('run', '--rm', '--network', NETWORK,
    '-v', `${migrations}:/flyway/sql`,
    FLYWAY_IMAGE, 'validate',
    `-url=jdbc:postgresql://${CONTAINER}:5432/${DB}`,
    `-user=${USER}`,
    `-password=${PASSWORD}`,
    '-locations=filesystem:/flyway/sql');
  console.log('  Schema history is consistent.');

  console.log('Comparing the PostgreSQL schema with the one the suite builds on H2...');
  // Captured rather than inherited, so a failure can print what Maven actually said instead of just
  // "the verification failed".
  //
  // Maven is resolved explicitly because it is not always on a Node process's PATH on Windows - a
  // PowerShell session can find mvn.cmd where spawnSync cannot, and the symptom is a bare ENOENT that
  // reads like "the schema does not match" when nothing was ever compared.
  // Maven is resolved explicitly for two reasons. It is not always on a Node process's PATH on Windows -
  // a PowerShell session finds mvn.cmd where spawnSync cannot - and spawnSync cannot execute a .cmd
  // directly either, so it goes through cmd.exe. The symptom of getting this wrong is a bare EINVAL or
  // ENOENT that reads like "the schema does not match" when nothing was ever compared.
  const maven = mavenCommand();
  // Offline is opt-in, not the default. It worked on a developer machine because ~/.m2 was already
  // warm, and failed on the first GitHub runner with "Non-resolvable parent POM" - a fresh runner has
  // an empty Maven repository, so offline mode cannot resolve even the Spring Boot parent. The failure
  // read as a schema problem for about as long as it took to notice it was a build flag.
  const offline = process.env.MAVEN_OFFLINE === '1' ? ['-o'] : [];
  run(maven.command, [...maven.prefix, ...offline,
    'clean', 'test', '-Dtest=PostgresSchemaParityIT', '-DfailIfNoSpecifiedTests=false'], {
    cwd: resolve(repoRoot, 'apps/api-java'),
    stdio: ['ignore', 'pipe', 'pipe'],
    // 127.0.0.1 rather than localhost: the published port is bound to the IPv4 loopback only, and on a
    // Linux runner "localhost" can resolve to ::1 first, which would fail to connect.
    env: { ...process.env, POSTGRES_VERIFY_URL: `jdbc:postgresql://127.0.0.1:${PORT}/${DB}` },
  });
  console.log('Schemas agree.');
} catch (error) {
  failed = true;
  // Maven's own output is the useful part; error.message alone says "Command failed".
  if (error.stdout) console.error(error.stdout);
  if (error.stderr) console.error(error.stderr);
  console.error(error.message);
} finally {
  stopContainer();
}

if (failed) {
  console.error('PostgreSQL schema verification FAILED.');
  process.exit(1);
}