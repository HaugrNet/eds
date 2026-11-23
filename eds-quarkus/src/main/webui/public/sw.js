// Service worker of Rune, the EDS PWA (RUNE.md). It keeps the app shell
// for offline starts, and nothing else: API calls, the chat and the content
// of Data Objects are never cached, so no decrypted data ends up in a cache.
// The cache has a new name since the API moved below Rune's own address: the
// old one is removed on activation, with whatever it held.
const CACHE = 'rune-shell-v2';
const SCOPE = new URL(self.registration.scope).pathname;
// Below the scope, but not Rune: the API with the chat and the content, and
// the stone's own pages (health, metrics).
const NOT_RUNE = ['api/', 'q/'];
const SHELL = ['./', 'manifest.webmanifest', 'favicon.svg', 'favicon.ico', 'rune-mark-light.svg', 'rune-mark-dark.svg'];
// The page itself, whatever the navigation's query was.
const INDEX = new URL('./', self.registration.scope).href;
// The scripts, styles and fonts of a release have a hash in their name, so
// each release adds new ones here, and never asks for the old ones again.
const ASSETS = new URL('assets/', self.registration.scope).href;

// Removes the assets which the page doesn't use: those of earlier releases,
// which would stay for ever. An asset is used if the page names it, or an
// asset in use does (a style its fonts, a script its parts).
async function prune(cache, page) {
  const assets = (await cache.keys()).filter((request) => request.url.startsWith(ASSETS));
  const used = new Set();
  let texts = [page];
  while (texts.length > 0) {
    const found = assets.filter((request) => !used.has(request.url)
      && texts.some((text) => text.includes(request.url.slice(ASSETS.length))));
    texts = [];
    for (const request of found) {
      used.add(request.url);
      if (/\.(js|css)$/.test(request.url)) {
        const response = await cache.match(request);
        texts.push(response ? await response.text() : '');
      }
    }
  }
  await Promise.all(assets.filter((request) => !used.has(request.url)).map((request) => cache.delete(request)));
}

// Keeps the page of a navigation. A page which differs from the kept one is a
// new release: the assets of the old one go.
async function keepPage(response) {
  const cache = await caches.open(CACHE);
  const kept = await cache.match(INDEX);
  const page = await response.clone().text();
  if (kept && (await kept.text()) !== page) {
    await prune(cache, page);
  }
  await cache.put(INDEX, response.clone());
}

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  // A new service worker fetched the page again at its installation, so a
  // new release can't be told by the page any more: prune here too. What the
  // page uses stays, so Rune still starts offline afterwards.
  event.waitUntil(caches.keys()
    .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
    .then(() => caches.open(CACHE))
    .then((cache) => cache.match(INDEX).then((kept) => (kept ? kept.text().then((page) => prune(cache, page)) : undefined)))
    .catch(() => undefined)
    .then(() => self.clients.claim()));
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  // Only the app itself: same origin, inside the scope, GET.
  if (event.request.method !== 'GET' || url.origin !== self.location.origin || !url.pathname.startsWith(SCOPE)) {
    return;
  }
  // Never the API: these requests go to the network untouched, and nothing
  // of them is kept.
  const path = url.pathname.slice(SCOPE.length);
  if (NOT_RUNE.some((prefix) => path.startsWith(prefix))) {
    return;
  }
  // Network first, so a new version is used at once; the cache when offline.
  // Only a navigation falls back to the page: a script, style or picture
  // which isn't cached fails as the network did, rather than getting HTML.
  const navigation = event.request.mode === 'navigate';
  event.respondWith(fetch(event.request)
    .then(async (response) => {
      if (response.ok) {
        if (navigation && (response.headers.get('Content-Type') ?? '').includes('text/html')) {
          await keepPage(response).catch(() => undefined);
        } else {
          const copy = response.clone();
          caches.open(CACHE).then((cache) => cache.put(event.request, copy)).catch(() => undefined);
        }
      }
      return response;
    })
    .catch(() => caches.match(event.request)
      .then((cached) => cached || (navigation ? caches.match(INDEX) : undefined))
      .then((cached) => cached || Response.error())));
});

// A click on a notification of a new message: to Rune, in its room.
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const circleId = event.notification.data?.circleId;
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windows) => {
    const open = windows.find((client) => new URL(client.url).pathname.startsWith(SCOPE));
    if (open) {
      open.postMessage({ type: 'open-room', circleId });
      return open.focus();
    }
    return self.clients.openWindow(SCOPE);
  }));
});
