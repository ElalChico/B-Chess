/*
 * B-Chess Desktop — Service Worker (PWA)
 * Estrategia: HTML/JSON "network-first" (se actualiza al instante en cada visita),
 * assets estáticos "cache-first".
 * Auto-update de verdad: al instalar un SW nuevo, toma el control al instante.
 * Al publicar una nueva versión en GitHub Pages, basta con recargar para recibirla.
 */
const VERSION = 'b-chess-desktop-v2.0.0-2026-11-15';
const CORE = [
  './',
  './index.html',
  './manifest.json',
  './css/app.css',
  './js/app.js',
  './js/pieces.js',
  './js/sounds.js',
  './js/engine.js',
  './js/net.js',
  './js/profile.js',
  './js/stats.js',
  './js/firebase-config.js',
  './js/vendor/chess.min.js',
  './js/vendor/stockfish-19-lite-single.js',
  './js/vendor/stockfish-19-lite-single.wasm',
  './assets/animations/kitten-success.png',
  './icono.ico',
  './assets/img/icon-192.png',
  './assets/img/icon-512.png'
];

self.addEventListener('install', function (event) {
  event.waitUntil(
    caches.open(VERSION).then(function (cache) { return cache.addAll(CORE); })
      .then(function () { return self.skipWaiting(); })
  );
});

self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.filter(function (k) { return k !== VERSION; }).map(function (k) { return caches.delete(k); }));
    }).then(function () { return self.clients.claim(); })
  );
});

self.addEventListener('message', function (event) {
  if (event.data === 'skipWaiting' && self.skipWaiting) self.skipWaiting();
});

self.addEventListener('fetch', function (event) {
  var req = event.request;
  var url = new URL(req.url);

  if (req.method !== 'GET' || url.origin !== self.location.origin) return;

  var accept = req.headers.get('accept') || '';
  var isHtml = accept.includes('text/html') || url.pathname.endsWith('.html') || url.pathname.endsWith('/');
  var isJson = url.pathname.endsWith('.json');

  if (isHtml || isJson) {
    event.respondWith(
      fetch(req).then(function (res) {
        var clone = res.clone();
        caches.open(VERSION).then(function (cache) { cache.put(req, clone); });
        return res;
      }).catch(function () {
        return caches.match(req).then(function (cached) { return cached || caches.match('./index.html'); });
      })
    );
    return;
  }

  event.respondWith(
    caches.match(req).then(function (cached) {
      if (cached) return cached;
      return fetch(req).then(function (res) {
        if (res && res.ok) {
          var clone = res.clone();
          caches.open(VERSION).then(function (cache) { cache.put(req, clone); });
        }
        return res;
      });
    })
  );
});
