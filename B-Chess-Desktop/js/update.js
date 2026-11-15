/*
 * B-Chess Desktop · Actualizador inspirado en el APK
 * - Auto-update real: el Service Worker toma control al instante tras instalar
 *   una nueva versión y recarga cuando el usuario lo confirme.
 * - Además, consulta los Releases de GitHub y avisa cuando hay una versión nueva.
 */
window.BCHESS_UPDATE = (function () {
  'use strict';

  var CURRENT_VERSION = '2.0.0';
  var REPO = 'ElalChico/B-Chess';
  var CHECK_URL = 'https://api.github.com/repos/' + REPO + '/releases/latest';

  // ---------- Registro del Service Worker (auto-update al instante) ----------
  function registerSW() {
    var isAllowed = location.protocol === 'https:' ||
      location.hostname === 'localhost' ||
      location.hostname === '127.0.0.1';
    if (!isAllowed) return;
    if (!('serviceWorker' in navigator)) return;
    navigator.serviceWorker.register('service-worker.js').then(function () {}).catch(function () {});
  }

  function checkForUpdate() {
    return fetch(CHECK_URL, { headers: { 'Accept': 'application/vnd.github+json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        if (!data || !data.tag_name) return null;
        var latest = String(data.tag_name).replace(/^v/, '');
        var available = compareSemver(latest, CURRENT_VERSION) > 0;
        return { latest: latest, available: available, url: data.html_url, notes: data.body || '' };
      })
      .catch(function () { return null; });
  }

  function compareSemver(a, b) {
    var pa = String(a).split('.').map(function (n) { return parseInt(n, 10) || 0; });
    var pb = String(b).split('.').map(function (n) { return parseInt(n, 10) || 0; });
    for (var i = 0; i < 3; i++) {
      if ((pa[i] || 0) !== (pb[i] || 0)) return (pa[i] || 0) > (pb[i] || 0) ? 1 : -1;
    }
    return 0;
  }

  function applyUpdateNow() {
    if (!('serviceWorker' in navigator)) return;
    navigator.serviceWorker.getRegistration().then(function (reg) {
      if (reg && reg.waiting) { reg.waiting.postMessage('skipWaiting'); }
      location.reload();
    });
  }

  // ---------- UI: insignia/actualización ----------
  function wireBadge() {
    var badge = document.getElementById('version-badge');
    if (!badge) return;
    badge.classList.add('is-clickable');
    badge.title = 'Comprobar actualizaciones';
    badge.addEventListener('click', function () {
      checkForUpdate().then(function (info) {
        badge.textContent = (info && info.available) ? 'Actualización v' + info.latest + ' disponible' : 'B-Chess Desktop v' + CURRENT_VERSION;
        badge.classList.toggle('has-update', !!(info && info.available));
        if (info && info.available) {
          applyUpdateNow();
        } else if (info) {
          var t = document.getElementById('toast');
          if (t) { t.textContent = 'Estás en la última versión (v' + CURRENT_VERSION + ')'; t.classList.add('show'); setTimeout(function () { t.classList.remove('show'); }, 2500); }
        }
      });
    });
  }

  function updateFooterBadge() {
    var badge = document.getElementById('version-badge');
    if (badge) badge.textContent = 'B-Chess Desktop v' + CURRENT_VERSION;
  }

  registerSW();
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', updateFooterBadge);
  else updateFooterBadge();
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', wireBadge);
  else wireBadge();

  return {
    CURRENT_VERSION: CURRENT_VERSION,
    checkForUpdate: checkForUpdate,
    applyUpdateNow: applyUpdateNow
  };
})();
