/*
 * Open Civic Signal OS service worker.
 *
 * Scope is deliberately small: the app shell and its static assets, so a neighbour with a bad
 * connection can reopen the app and keep a report draft instead of staring at the browser error.
 * API responses are never cached: a stale backlog or somebody else's notification must not be
 * served from disk. Anything under /api/ is left to the network untouched.
 */
const VERSION = 'v1';
const SHELL_CACHE = `signalos-shell-${VERSION}`;
const ASSET_CACHE = `signalos-assets-${VERSION}`;
const SHELL_URL = '/index.html';
const OFFLINE_URL = '/offline.html';
const PRECACHE = [OFFLINE_URL, '/site.webmanifest', '/favicon.svg'];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(SHELL_CACHE)
      .then((cache) => cache.addAll(PRECACHE))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((key) => key !== SHELL_CACHE && key !== ASSET_CACHE)
            .map((key) => caches.delete(key))
        )
      )
      .then(() => self.clients.claim())
  );
});

const isStaticAsset = (url) =>
  url.pathname.startsWith('/assets/') ||
  url.pathname.startsWith('/fonts/') ||
  /\.(?:css|js|woff2?|png|jpe?g|svg|webp|ico)$/.test(url.pathname);

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.startsWith('/api/')) return;

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response.ok) {
            const copy = response.clone();
            caches.open(SHELL_CACHE).then((cache) => cache.put(SHELL_URL, copy));
          }
          return response;
        })
        .catch(async () => (await caches.match(SHELL_URL)) || (await caches.match(OFFLINE_URL)))
    );
    return;
  }

  if (!isStaticAsset(url)) return;

  event.respondWith(
    caches.open(ASSET_CACHE).then(async (cache) => {
      const cached = await cache.match(request);
      const network = fetch(request)
        .then((response) => {
          if (response.ok) cache.put(request, response.clone());
          return response;
        })
        .catch(() => cached || Response.error());
      return cached || network;
    })
  );
});
