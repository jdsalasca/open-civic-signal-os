import assert from 'node:assert/strict';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');

test('web lockfile records every dependency declared by the web manifest', () => {
  const manifest = JSON.parse(readFileSync(path.join(repositoryRoot, 'apps/web-react/package.json'), 'utf8'));
  const lockfile = JSON.parse(readFileSync(path.join(repositoryRoot, 'apps/web-react/package-lock.json'), 'utf8'));
  const lockedManifest = lockfile.packages[''];

  assert.deepEqual(lockedManifest.dependencies, manifest.dependencies);
  assert.deepEqual(lockedManifest.devDependencies, manifest.devDependencies);
});

const testValues = {
  POSTGRES_DB: 'signalos',
  POSTGRES_USER: 'signalos',
  POSTGRES_PASSWORD: 'throwaway-postgres-password',
  JWT_SECRET: 'throwaway-test-jwt-key-which-is-long-enough-for-local-validation-only',
  INTEGRATION_CREDENTIAL_KEY: 'throwaway-integration-key-for-compose-validation-only',
  CORS_ALLOWED_ORIGINS: 'https://open-comunity.eridu.top',
  VITE_API_BASE_URL: '/api',
  MAIL_ENABLED: 'false',
};

function runComposeConfig(values = testValues) {
  const temporaryDirectory = mkdtempSync(path.join(tmpdir(), 'open-community-compose-'));
  const envFile = path.join(temporaryDirectory, '.env.prod');
  const composeFile = path.join(repositoryRoot, 'infra/docker-compose.prod.yml');
  const env = { ...process.env };
  for (const key of Object.keys(testValues)) delete env[key];
  writeFileSync(envFile, Object.entries(values).map(([key, value]) => `${key}=${value}`).join('\n'));

  const result = spawnSync('docker', [
    'compose', '--project-name', 'open-community-test', '--file', composeFile,
    '--env-file', envFile, 'config', '--format', 'json',
  ], { cwd: repositoryRoot, encoding: 'utf8', env });

  rmSync(temporaryDirectory, { recursive: true, force: true });
  return result;
}

function memoryBytes(value) {
  if (typeof value === 'number') return value;
  const match = String(value).match(/^(\d+(?:\.\d+)?)([kmg])?b?$/i);
  assert.ok(match, `unrecognized memory limit: ${value}`);
  const scale = { undefined: 1, k: 1024, m: 1024 ** 2, g: 1024 ** 3 }[match[2]?.toLowerCase()];
  return Number(match[1]) * scale;
}

function configuredProductionCompose() {
  const result = runComposeConfig();
  assert.equal(result.status, 0, result.stderr || result.stdout);
  return JSON.parse(result.stdout);
}

function runDeploymentFixture({ unhealthyService = '' } = {}) {
  const temporaryDirectory = mkdtempSync(path.join(tmpdir(), 'open-community-deploy-'));
  const backupDirectory = path.join(temporaryDirectory, 'backups');
  const infraDirectory = path.join(temporaryDirectory, 'infra');
  const envFile = path.join(infraDirectory, '.env.prod');
  const composeFile = path.join(infraDirectory, 'docker-compose.prod.yml');
  const eventLog = path.join(temporaryDirectory, 'events.log');
  const bashEnvironment = path.join(temporaryDirectory, 'bash-env.sh');
  const scriptsDirectory = path.join(repositoryRoot, 'scripts');
  const shell = process.env.BASH_BIN || 'bash';

  mkdirSync(infraDirectory, { recursive: true });
  mkdirSync(backupDirectory, { recursive: true });
  writeFileSync(envFile, 'POSTGRES_PASSWORD=throwaway\nJWT_SECRET=throwaway\n');
  writeFileSync(composeFile, 'services: {}\n');
  writeFileSync(bashEnvironment, `
docker() {
  printf 'docker %s\\n' "$*" >> "$EVENT_LOG"
  case "$*" in
    *"ps --status running --quiet db"*) printf 'db-container\\n' ;;
    *"ps --all --quiet db"*) printf 'db-container\\n' ;;
    *"ps --all --quiet api"*) printf 'api-container\\n' ;;
    *"ps --all --quiet web"*) printf 'web-container\\n' ;;
    *"exec -T db"*) printf 'throwaway-custom-dump' ;;
    *"inspect --format {{.State.Status}}"*) printf 'running\\n' ;;
    *"inspect --format {{.State.Health.Status}} api-container"*)
      if [ "$TEST_UNHEALTHY_SERVICE" = "api" ]; then printf 'unhealthy\\n'; else printf 'healthy\\n'; fi ;;
    *"inspect --format {{.State.Health.Status}}"*) printf 'healthy\\n' ;;
  esac
}
git() {
  printf 'git %s\\n' "$*" >> "$EVENT_LOG"
}
export -f docker git
`);

  const shellPath = (nativePath) => {
    if (process.platform !== 'win32') return nativePath;
    const cygpath = process.env.CYGPATH_BIN || path.resolve(path.dirname(shell), '../usr/bin/cygpath.exe');
    const conversion = spawnSync(cygpath, ['-u', nativePath], { encoding: 'utf8' });
    assert.equal(conversion.status, 0, conversion.stderr || `could not convert path: ${nativePath}`);
    return conversion.stdout.trim();
  };
  const env = {
    ...process.env,
    BASH_ENV: shellPath(bashEnvironment),
    COMPOSE_PROJECT_NAME: 'open-community-test',
    EVENT_LOG: shellPath(eventLog),
    PROD_BACKUP_DIR: shellPath(backupDirectory),
    PROD_COMPOSE_FILE: shellPath(composeFile),
    PROD_ENV_FILE: shellPath(envFile),
    PROD_HEALTH_POLL_SECONDS: '1',
    PROD_HEALTH_TIMEOUT_SECONDS: unhealthyService ? '2' : '10',
    TEST_UNHEALTHY_SERVICE: unhealthyService,
  };
  const result = spawnSync(shell, ['deploy-prod.sh'], {
    cwd: scriptsDirectory,
    encoding: 'utf8',
    env,
  });
  const events = existsSync(eventLog) ? readFileSync(eventLog, 'utf8').trim().split(/\r?\n/) : [];
  const backups = existsSync(backupDirectory)
    ? readdirSync(backupDirectory).filter((filename) => filename.endsWith('.dump'))
    : [];
  const backupContent = backups.length ? readFileSync(path.join(backupDirectory, backups[0]), 'utf8') : '';

  rmSync(temporaryDirectory, { recursive: true, force: true });
  return { result, events, backups, backupContent };
}

test('production compose keeps database and API private and publishes only the local web port', () => {
  const config = configuredProductionCompose();
  const { db, api, web, backup } = config.services;

  assert.deepEqual(Object.keys(config.services).sort(), ['api', 'backup', 'db', 'web']);
  assert.equal(db.image, 'postgres:15-alpine');
  assert.equal(db.ports, undefined);
  assert.equal(db.healthcheck.test.join(' ').includes('pg_isready'), true);
  assert.equal(db.restart, 'unless-stopped');
  assert.ok(db.volumes.some((volume) => volume.source === 'postgres-data' && volume.target === '/var/lib/postgresql/data'));

  assert.equal(api.ports, undefined);
  assert.equal(api.environment.SPRING_PROFILES_ACTIVE, 'prod');
  assert.equal(api.environment.CORS_ALLOWED_ORIGINS, 'https://open-comunity.eridu.top');
  assert.match(api.environment.JAVA_TOOL_OPTIONS, /-Xmx384m/);
  assert.ok(memoryBytes(api.mem_limit) <= 700 * 1024 ** 2);
  assert.equal(api.depends_on.db.condition, 'service_healthy');
  assert.match(api.healthcheck.test.join(' '), /\/actuator\/health/);

  assert.equal(web.ports.length, 1);
  assert.equal(web.ports[0].host_ip, '127.0.0.1');
  assert.equal(web.ports[0].published, '5175');
  assert.equal(web.ports[0].target, 80);
  assert.equal(web.build.args.VITE_API_BASE_URL, '/api');
  assert.equal(web.restart, 'unless-stopped');

  assert.equal(backup.image, 'postgres:15-alpine');
  assert.equal(backup.restart, 'unless-stopped');
  assert.ok(backup.volumes.some((volume) => volume.type === 'volume' && volume.target === '/backups'));
  assert.ok(config.volumes['postgres-data']);
  assert.ok(config.volumes['postgres-backups']);
  assert.equal(api.environment.APPLICATION_MAIL_ENABLED, 'false');
  assert.equal(config.services.mailpit, undefined);
});

test('production compose refuses to render without each required secret or public origin', () => {
  for (const required of ['POSTGRES_PASSWORD', 'JWT_SECRET', 'INTEGRATION_CREDENTIAL_KEY', 'CORS_ALLOWED_ORIGINS']) {
    const values = { ...testValues };
    delete values[required];
    const result = runComposeConfig(values);
    assert.notEqual(result.status, 0, `${required} unexpectedly had a default`);
    assert.match(`${result.stderr}\n${result.stdout}`, new RegExp(required));
  }
});

test('production defaults to same-origin API calls and keeps optional mail disabled', () => {
  const values = { ...testValues };
  delete values.VITE_API_BASE_URL;
  delete values.MAIL_ENABLED;
  const { services } = configuredProductionCompose(values);

  assert.equal(services.web.build.args.VITE_API_BASE_URL, '/api');
  assert.equal(services.api.environment.APPLICATION_MAIL_ENABLED, 'false');
});

test('production images use pinned build inputs and exclude generated files from contexts', () => {
  const apiDockerfile = readFileSync(path.join(repositoryRoot, 'apps/api-java/Dockerfile'), 'utf8');
  const webDockerfile = readFileSync(path.join(repositoryRoot, 'apps/web-react/Dockerfile'), 'utf8');
  const apiIgnore = readFileSync(path.join(repositoryRoot, 'apps/api-java/.dockerignore'), 'utf8');
  const webIgnore = readFileSync(path.join(repositoryRoot, 'apps/web-react/.dockerignore'), 'utf8');

  assert.match(apiDockerfile, /^FROM maven:\d+\.\d+\.\d+-eclipse-temurin-21-[\w.]+ AS build/m);
  assert.match(apiDockerfile, /^FROM eclipse-temurin:21\.[\w.-]+ AS runtime/m);
  assert.match(apiDockerfile, /USER 10001:10001/);
  assert.match(webDockerfile, /^FROM node:\d+\.\d+\.\d+-alpine[\d.]+ AS build/m);
  assert.match(webDockerfile, /^FROM nginx:\d+\.\d+\.\d+-alpine[\d.]+/m);
  assert.match(webDockerfile, /RUN npm ci/);

  for (const [filename, contents] of [['api', apiIgnore], ['web', webIgnore]]) {
    for (const generated of ['node_modules', 'dist', 'target', 'logs', 'coverage', 'output']) {
      assert.match(contents, new RegExp(`(?:^|\\*\\*/)${generated}(?:/|$)`, 'm'), `${filename} .dockerignore excludes ${generated}`);
    }
  }
});

test('database restore refuses to run until explicitly confirmed', () => {
  const env = { ...process.env };
  delete env.RESTORE_CONFIRM;
  const result = spawnSync(process.env.BASH_BIN || 'bash', ['restore-prod.sh'], {
    cwd: path.join(repositoryRoot, 'scripts'),
    encoding: 'utf8',
    env,
  });

  assert.equal(result.status, 2, result.stderr || result.stdout);
  assert.match(result.stderr, /RESTORE_CONFIRM=yes/);
});

test('deployment backs up before fast-forwarding and waits for healthy services', () => {
  const { result, events, backups, backupContent } = runDeploymentFixture();

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.equal(backups.length, 1);
  assert.equal(backupContent, 'throwaway-custom-dump');
  const backupIndex = events.findIndex((event) => event.includes('exec -T db'));
  const pullIndex = events.findIndex((event) => event.startsWith('git ') && event.includes('pull --ff-only'));
  const deployIndex = events.findIndex((event) => event.includes('up --detach --build'));
  assert.ok(backupIndex >= 0 && backupIndex < pullIndex, events.join('\n'));
  assert.ok(pullIndex < deployIndex, events.join('\n'));
  assert.match(result.stdout, /db, api, and web are healthy/i);
});

test('deployment exits non-zero when the API never reports healthy', () => {
  const { result } = runDeploymentFixture({ unhealthyService: 'api' });

  assert.notEqual(result.status, 0);
  assert.match(`${result.stderr}\n${result.stdout}`, /api.*healthy|healthy.*api/i);
});
