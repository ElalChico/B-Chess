/*
 * B-Chess V2 · Sonidos
 * Sin sonido de colocación de pieza. El reloj usa siempre los sonidos de lichess.
 */
window.BCHESS_SOUNDS = (function () {
  const base = 'assets/sounds/';

  // Mapa nombre lógico → archivo. Sin 'move' ni 'select'.
  const files = {
    capture: 'gm_capture.mp3',
    check: 'gm_check.mp3',
    mate: 'gm_mate.mp3',
    error: 'gm_error.mp3',
    draw: 'gm_draw.mp3',
    start: 'gm_start.mp3',
    enroque: 'gm_enroque.mp3',
    torre: 'gm_torre.mp3',
    victory: 'victory.mp3',
    defeat: 'defeat.mp3',
    connect: 'gm_connect.mp3',
    connectError: 'gm_connect_error.mp3',
    lowtime: 'lowtime.mp3',
    countdown3: 'countdown3.mp3',
    countdown2: 'countdown2.mp3',
    countdown1: 'countdown1.mp3'
  };

  let enabled = true;
  let volume = 0.8;
  const audio = {};

  function load(name) {
    if (!audio[name] && files[name]) {
      audio[name] = new Audio(base + files[name]);
      audio[name].preload = 'auto';
    }
    return audio[name];
  }

  function play(name) {
    if (!enabled) return;
    const a = load(name);
    if (!a) return;
    try {
      a.pause();
      a.currentTime = 0;
      a.volume = volume;
      a.play().catch(function () {});
    } catch (e) { /* silencio */ }
  }

  function setEnabled(v) { enabled = !!v; }
  function isEnabled() { return enabled; }
  function setVolume(v) { volume = Math.max(0, Math.min(1, v / 100)); }
  function getVolume() { return Math.round(volume * 100); }

  // Precarga
  Object.keys(files).forEach(load);

  return { play, setEnabled, isEnabled, setVolume, getVolume };
})();
