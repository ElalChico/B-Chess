/*
 * B-Chess V2 · Motor (Stockfish / Atlas)
 * Un solo worker. Atlas = Stockfish con UCI_LimitStrength + Skill Level escalado.
 */
window.BCHESS_ENGINE = (function () {
  let worker = null;
  let initPromise = null;
  let initResolve = null;
  let pending = null;

  // Debug solo con ?debug=1 en la URL
  const DEBUG = (function () {
    try { return /(?:\?|&)debug=1(?:&|$)/.test(window.location.search) || window.BCHESS_DEBUG === true; }
    catch (e) { return false; }
  })();
  function log(...args) { if (DEBUG) try { console.log('[engine]', ...args); } catch (e) {} }

  function init() {
    if (initPromise) return initPromise;
    initPromise = new Promise(function (resolve) {
      initResolve = resolve;
      try {
        log('creating worker');
        worker = new Worker('js/vendor/stockfish-19-lite-single.js');
        worker.onmessage = function (e) { handleLine(e); };
        worker.onerror = function (e) { console.error('[engine] worker error', e); };
        worker.postMessage('uci');
      } catch (e) {
        console.error('[engine] no se pudo iniciar Stockfish', e);
        initResolve();
      }
    });
    return initPromise;
  }

  function handleLine(e) {
    const line = String((e && e.data) || '');
    if (line === 'uciok') { log('uciok'); worker.postMessage('isready'); return; }
    if (line === 'readyok') { log('readyok'); initResolve(); return; }
    if (line.indexOf('bestmove') === 0) {
      const move = line.split(/\s+/)[1] || '';
      if (pending) { const cb = pending; pending = null; cb(move); }
    }
  }

  async function findBestMove(fen, opts) {
    opts = opts || {};
    await init();
    if (!worker) return null;

    const engine = opts.engine || 'stockfish';
    const difficulty = Math.max(1, Math.min(10, parseInt(opts.difficulty, 10) || 5));
    worker.postMessage('setoption name Threads value 1');
    worker.postMessage('setoption name Hash value 128');
    worker.postMessage('setoption name MultiPV value 1');

    if (engine === 'atlas') {
      const atlasSkill = Math.round(13 * (difficulty - 1) / 9) + 1; // 1 → 1 … 10 → 14
      worker.postMessage('setoption name UCI_LimitStrength value true');
      worker.postMessage('setoption name Skill Level value ' + atlasSkill);
      const atlasDepth = difficulty <= 3 ? 8 : (difficulty <= 7 ? 10 : 12);
      worker.postMessage('position fen ' + fen);
      worker.postMessage('go depth ' + atlasDepth + ' movetime ' + (300 + difficulty * 100));
    } else {
      const skill = Math.round((difficulty - 1) * (20 / 9)); // 1→0 … 10→20
      worker.postMessage('setoption name UCI_LimitStrength value true');
      worker.postMessage('setoption name Skill Level value ' + skill);
      worker.postMessage('position fen ' + fen);
      worker.postMessage('go movetime ' + (350 + difficulty * 70));
    }

    return new Promise(function (resolve) {
      pending = resolve;
      setTimeout(function () {
        if (!pending) return;
        console.warn('[engine] timeout esperando bestmove');
        pending = null;
        try { worker && worker.postMessage('stop'); } catch (e) {}
        resolve(null);
      }, 12000);
    });
  }

  function stop() {
    try { if (worker) worker.postMessage('stop'); } catch (e) {}
  }

  function quit() {
    try { if (worker) { worker.postMessage('quit'); worker.terminate && worker.terminate(); } } catch (e) {}
    worker = null; initPromise = null; initResolve = null; pending = null;
  }

  return { init: init, findBestMove: findBestMove, stop: stop, quit: quit };
})();
