/*
 * B-Chess V2 · Inspector de desarrollo (F2)
 * - F2 activa/desactiva el modo inspector.
 * - En modo inspector, al pasar el ratón se resalta el elemento.
 * - Al hacer clic se copia su data-dev (o un selector generado) al portapapeles.
 */
(function () {
  let enabled = false;
  let panel = null;

  function makePanel() {
    if (panel) return panel;
    panel = document.createElement('div');
    panel.className = 'dev-panel';
    panel.textContent = 'MODO DEV · F2 para salir · clic copia selector';
    document.body.appendChild(panel);
    return panel;
  }

  function ensureSelector(el) {
    const id = el.getAttribute('data-dev');
    if (id) return '[data-dev="' + id + '"]';
    if (el.id) return '#' + el.id;
    const parts = [];
    let cur = el;
    while (cur && cur !== document.body && parts.length < 4) {
      let sel = cur.tagName.toLowerCase();
      if (cur.className) {
        const cls = String(cur.className).trim().split(/\s+/).filter(Boolean).slice(0, 2).join('.');
        if (cls) sel += '.' + cls;
      }
      parts.unshift(sel);
      cur = cur.parentElement;
    }
    return parts.join(' > ');
  }

  function toggle() {
    enabled = !enabled;
    document.body.classList.toggle('dev-mode', enabled);
    if (enabled) makePanel();
    else if (panel) { panel.remove(); panel = null; }
  }

  document.addEventListener('keydown', function (e) {
    if (e.key === 'F2') { e.preventDefault(); toggle(); }
  });

  document.addEventListener('mouseover', function (e) {
    if (!enabled) return;
    const t = e.target;
    if (t && t.classList) {
      t.classList.add('dev-highlight');
      panel.textContent = ensureSelector(t);
    }
  }, true);

  document.addEventListener('mouseout', function (e) {
    if (!enabled) return;
    if (e.target && e.target.classList) e.target.classList.remove('dev-highlight');
  }, true);

  document.addEventListener('click', function (e) {
    if (!enabled) return;
    e.preventDefault();
    e.stopPropagation();
    const sel = ensureSelector(e.target);
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(sel).catch(function () {});
    }
    if (window.showToast) window.showToast('Copiado: ' + sel);
  }, true);
})();
