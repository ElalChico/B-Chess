/*
 * B-Chess V2 · P2P por Firebase Realtime Database + WebRTC DataChannel.
 * Mismo esquema que el APK: matches/<código> { createdAt, offer, answer, iceCandidates/{host,guest} }
 * - Host: crea oferta, escucha respuesta e ICE del invitado.
 * - Guest: escucha oferta, responde, envía ICE.
 * - Una vez conectados, Firebase queda fuera de la partida.
 * - El tráfico de juego va por el DataChannel (DTLS).
 */
window.BCHESS_NET = (function () {
  const STUN = { iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    { urls: 'stun:stun2.l.google.com:19302' },
    { urls: 'stun:stun3.l.google.com:19302' },
    { urls: 'stun:stun.cloudflare.com:3478' },
    // TURN gratuito (relay) para cuando el PC/móvil están en redes distintas o el navegador bloquea la red local
    { urls: 'turn:openrelay.metered.ca:80', username: 'openrelayproject', credential: 'openrelayproject' },
    { urls: 'turn:openrelay.metered.ca:443', username: 'openrelayproject', credential: 'openrelayproject' }
  ] };
  const WATCHDOG_MS = 90000;
  const STALE_MS = 12 * 60 * 60 * 1000; // 12 horas: los códigos vencen y pueden reciclarse al día siguiente

  const state = {
    pc: null,
    dc: null,
    ref: null,
    code: null,
    isHost: false,
    connected: false,
    watchdog: null,
    name: 'Jugador',
    remoteName: 'Rival'
  };

  const listeners = [];

  function onState(fn) { listeners.push(fn); }
  function emitState(s) {
    try { if (window.BCHESS_CONN_DEBUG) console.log('[net] state', s); } catch (e) {}
    listeners.forEach(function (f) { try { f(s); } catch (e) {} });
  }

  let db = null;

  function initFirebase() {
    if (db) return true;
    if (!window.BCHESS_FIREBASE_CONFIG) return false;
    try {
      if (!window.firebase || !firebase.database) return false;
      if (!firebase.apps || firebase.apps.length === 0) {
        firebase.initializeApp(window.BCHESS_FIREBASE_CONFIG);
      }
      db = firebase.database();
      return true;
    } catch (e) {
      console.error('[net] Firebase init failed', e);
      return false;
    }
  }

  function normalizeJoinCode(raw) {
    const code = (raw || '').trim();
    if (!code) return '';
    if (code.indexOf('-') !== 0 && /^[A-Za-z0-9]{4,12}$/.test(code)) return code.toUpperCase();
    return code; // claves push de Firebase empiezan por '-'
  }

  function startWatchdog(onError) {
    clearWatchdog();
    state.watchdog = setTimeout(function () {
      if (!state.connected) {
        cleanup(false);
        emitState({ state: 'failed', reason: 'No se pudo conectar en 90 s' });
        onError && onError('No se pudo conectar en 90 s. Revisa la red o el código.');
      }
    }, WATCHDOG_MS);
  }

  function clearWatchdog() {
    if (state.watchdog) { clearTimeout(state.watchdog); state.watchdog = null; }
  }

  function setupDataChannel(dc, handlers) {
    state.dc = dc;
    dc.onopen = function () {
      state.connected = true;
      clearWatchdog();
      emitState({ state: 'connected', code: state.code });
      handlers.onConnected && handlers.onConnected();
    };
    dc.onmessage = function (e) {
      const line = String(e.data || '').trim();
      if (line) handlers.onMessage && handlers.onMessage(line);
    };
    dc.onclose = function () {
      if (state.connected) {
        state.connected = false;
        emitState({ state: 'disconnected', reason: 'Conexión cerrada' });
        handlers.onDisconnected && handlers.onDisconnected('Conexión cerrada');
      }
    };
    dc.onerror = function (err) {
      emitState({ state: 'failed', reason: 'Error en el canal de datos' });
      handlers.onError && handlers.onError('Error en el canal de datos');
    };
  }

  function createPeerConnection(ref, role, handlers) {
    const pc = new RTCPeerConnection(STUN);
    state.pc = pc;

    pc.onicecandidate = function (e) {
      if (!e.candidate) return;
      const c = {
        sdpMid: e.candidate.sdpMid,
        sdpMLineIndex: e.candidate.sdpMLineIndex,
        candidate: e.candidate.candidate
      };
      ref.child('iceCandidates/' + role).push(JSON.stringify(c));
    };

    pc.onconnectionstatechange = function () {
      if (pc.connectionState === 'failed' || pc.connectionState === 'closed') {
        if (state.connected) {
          state.connected = false;
          emitState({ state: 'disconnected', reason: 'Conexión perdida' });
          handlers.onDisconnected && handlers.onDisconnected('Conexión perdida');
        } else if (pc.connectionState === 'failed') {
          emitState({ state: 'failed', reason: 'Conexión perdida' });
        }
      }
    };

    return pc;
  }

  function listenIce(ref, role, pc) {
    ref.child('iceCandidates/' + role).on('child_added', function (snap) {
      try {
        const c = JSON.parse(snap.val());
        pc.addIceCandidate(new RTCIceCandidate(c));
      } catch (e) { /* candidato inválido */ }
    });
  }

  function createRoom(customCode, handlers) {
    handlers = handlers || {};
    if (!initFirebase()) { handlers.onError && handlers.onError('Firebase no está disponible.'); return; }

    const custom = (customCode || '').trim();

    function openRoom(code) {
      state.isHost = true;
      state.code = code;
      state.connected = false;
      emitState({ state: 'connecting', code: code, host: true });

      const ref = db.ref('matches/' + code);
      state.ref = ref;
      ref.child('createdAt').set(firebase.database.ServerValue.TIMESTAMP);

      const pc = createPeerConnection(ref, 'host', handlers);
      const dc = pc.createDataChannel('chess-data', { ordered: true });
      setupDataChannel(dc, handlers);

      pc.createOffer()
        .then(function (offer) { return pc.setLocalDescription(offer); })
        .then(function () { ref.child('offer').set(pc.localDescription.sdp); })
        .catch(function () { handlers.onError && handlers.onError('No se pudo crear la oferta.'); });

      ref.child('answer').on('value', function (snap) {
        const sdp = snap.val();
        if (sdp && !pc.remoteDescription) {
          pc.setRemoteDescription({ type: 'answer', sdp: sdp }).catch(function () {});
        }
      });

      listenIce(ref, 'guest', pc);
      startWatchdog(handlers.onError);
      handlers.onCreated && handlers.onCreated(code);
    }

    if (!custom) {
      const key = db.ref('matches').push().key;
      openRoom(key);
      return;
    }

    const norm = normalizeJoinCode(custom);
    if (!/^[A-Z0-9]{4,12}$/.test(norm)) {
      handlers.onError && handlers.onError('Código no válido: usa de 4 a 12 letras o números.');
      return;
    }

    const ref = db.ref('matches/' + norm);
    ref.once('value').then(function (snap) {
      if (!snap.exists()) { openRoom(norm); return; }
      const createdAt = snap.child('createdAt').val();
      const stale = !createdAt || (Date.now() - createdAt > STALE_MS);
      if (stale) { ref.remove().then(function () { openRoom(norm); }); return; }
      handlers.onError && handlers.onError('El código ' + norm + ' ya está en uso. Elige otro.');
    }).catch(function () {
      // Si falla la verificación, abrir directamente
      openRoom(norm);
    });
  }

  function joinRoom(rawCode, handlers) {
    handlers = handlers || {};
    if (!initFirebase()) { handlers.onError && handlers.onError('Firebase no está disponible.'); return; }

    const code = normalizeJoinCode(rawCode);
    if (!code) { handlers.onError && handlers.onError('Código de partida inválido.'); return; }

    state.isHost = false;
    state.code = code;
    state.connected = false;
    emitState({ state: 'connecting', code: code, host: false });

    const ref = db.ref('matches/' + code);
    state.ref = ref;

    // No unirse a salas expiradas (12h): se eliminan y se avisa
    ref.once('value').then(function (snap) {
      if (!snap.exists()) { handlers.onError && handlers.onError('El código no existe o ya expiró.'); return; }
      const createdAt = snap.child('createdAt').val();
      const stale = !createdAt || (Date.now() - createdAt > STALE_MS);
      if (stale) {
        ref.remove();
        handlers.onError && handlers.onError('El código expiró (más de 12 h) y fue liberado. Pide uno nuevo.');
        return;
      }
    }).catch(function () {});

    const pc = createPeerConnection(ref, 'guest', handlers);
    pc.ondatachannel = function (e) { setupDataChannel(e.channel, handlers); };

    ref.child('offer').on('value', function (snap) {
      const sdp = snap.val();
      if (!sdp || pc.remoteDescription) return;
      pc.setRemoteDescription({ type: 'offer', sdp: sdp })
        .then(function () { return pc.createAnswer(); })
        .then(function (answer) { return pc.setLocalDescription(answer); })
        .then(function () { ref.child('answer').set(pc.localDescription.sdp); })
        .catch(function () { handlers.onError && handlers.onError('No se pudo aplicar la oferta.'); });
    });

    listenIce(ref, 'host', pc);
    startWatchdog(handlers.onError);
  }

  function sendLine(line) {
    if (state.dc && state.dc.readyState === 'open') {
      try { state.dc.send(line); } catch (e) { console.error('[net] send failed', e); }
    }
  }

  function cleanup(removeRoom) {
    clearWatchdog();
    try { state.dc && state.dc.close(); } catch (e) {}
    try { state.pc && state.pc.close(); } catch (e) {}
    state.dc = null;
    state.pc = null;
    if (removeRoom !== false && state.isHost && state.ref) {
      try { state.ref.remove(); } catch (e) {}
    }
    const wasConnected = state.connected;
    state.connected = false;
    state.ref = null;
    emitState({ state: wasConnected ? 'disconnected' : 'idle', reason: 'cancel' });
  }

  function setName(name) { state.name = name || 'Jugador'; }
  function setRemoteName(name) { state.remoteName = name || 'Rival'; }
  function getCode() { return state.code; }
  function isHost() { return state.isHost; }
  function isConnected() { return state.connected; }

  return {
    createRoom: createRoom,
    joinRoom: joinRoom,
    sendLine: sendLine,
    cleanup: cleanup,
    setName: setName,
    setRemoteName: setRemoteName,
    getCode: getCode,
    isHost: isHost,
    isConnected: isConnected,
    onState: onState
  };
})();
