// Firebase web config (proyecto b-chess-app → app "B-Chess Web").
// La API key es pública por diseño; la seguridad la dan las reglas de Realtime Database.
// Usa la misma Realtime Database que el APK — las partidas PC↔APK comparten `matches/<código>`.
window.BCHESS_FIREBASE_CONFIG = {
  apiKey: 'AIzaSyACsaw7MN5Byukf1iBpmvdll_rkvLz6Ypk',
  authDomain: 'b-chess-app.firebaseapp.com',
  databaseURL: 'https://b-chess-app-default-rtdb.firebaseio.com',
  projectId: 'b-chess-app',
  storageBucket: 'b-chess-app.firebasestorage.app',
  messagingSenderId: '964669262407',
  appId: '1:964669262407:web:8d41e539c8430c8f5cff62'
};
