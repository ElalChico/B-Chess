/*
 * B-Chess V2 · Piezas y temas del tablero
 * 13 sets SVG de Lichess + 3 sets PNG propios.
 * Los SVG se generaron desde los VectorDrawable de Android con tools/convert_vector_drawables.py
 */
window.BCHESS_PIECES = {
  sets: [
    { id: 'cburnett', es: 'Clásico', en: 'Classic', kind: 'svg' },
    { id: 'alpha', es: 'Alpha', en: 'Alpha', kind: 'svg' },
    { id: 'anarcandy', es: 'Anarcandy', en: 'Anarcandy', kind: 'svg' },
    { id: 'anarcandywhite', es: 'Anarcandy Blanco', en: 'Anarcandy White', kind: 'svg' },
    { id: 'companion', es: 'Cómplice', en: 'Companion', kind: 'svg' },
    { id: 'horsey', es: 'Horsey', en: 'Horsey', kind: 'svg' },
    { id: 'leipzig', es: 'Leipzig', en: 'Leipzig', kind: 'svg' },
    { id: 'merida', es: 'Merida', en: 'Merida', kind: 'svg' },
    { id: 'pixel', es: 'Pixel', en: 'Pixel', kind: 'svg' },
    { id: 'riohacha', es: 'Riohacha', en: 'Riohacha', kind: 'svg' },
    { id: 'staunty', es: 'Staunty', en: 'Staunty', kind: 'svg' },
    { id: 'tatiana', es: 'Tatiana', en: 'Tatiana', kind: 'svg' },
    { id: 'icpieces', es: 'Icpieces', en: 'Icpieces', kind: 'svg' },
    { id: 'set1', es: 'Gatos 1', en: 'Cats 1', kind: 'png' },
    { id: 'set2', es: 'Gatos 2', en: 'Cats 2', kind: 'png' },
    { id: 'set3', es: 'Gatos 3', en: 'Cats 3', kind: 'png' }
  ],

  themes: [
    { id: 'lichess', es: 'Lichess', light: '#f0d9b5', dark: '#b58863' },
    { id: 'wood', es: 'Madera', light: '#ead7b7', dark: '#8b5a2b' },
    { id: 'blue', es: 'Azul', light: '#dee3e6', dark: '#8ca2ad' },
    { id: 'green', es: 'Verde', light: '#ffffdd', dark: '#86a666' },
    { id: 'red', es: 'Rojo', light: '#f2d7d5', dark: '#b03a2e' },
    { id: 'dark', es: 'Oscuro', light: '#9a9a9a', dark: '#4a4a4a' }
  ],

  pieceUrl(setId, color, type) {
    const set = this.sets.find(s => s.id === setId) || this.sets[0];
    const code = color + type; // w/b + kqrbnp
    if (set.kind === 'svg') return 'assets/pieces/lichess/' + set.id + '/' + code + '.svg';
    return 'assets/pieces/nuestras/' + set.id + '/' + code + '.png';
  },

  setLabel(id, isEn) {
    const s = this.sets.find(x => x.id === id);
    return s ? (isEn ? s.en : s.es) : id;
  },

  themeLabel(id, isEn) {
    const t = this.themes.find(x => x.id === id);
    return t ? t.es : id;
  }
};
