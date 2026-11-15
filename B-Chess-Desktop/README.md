# B-Chess Desktop

**B-Chess en el escritorio**: el mismo motor y las mismas piezas que el APK ([B-Chess](../app/src/main)); ajedrez completo contra la IA o en línea P2P, **sin cuentas ni registros**.

> Este directorio es la versión PC de B-Chess. La versión móvil vive en `app/`.

## Qué es

Versión web instalable de escritorio (PWA) del ajedrez B-Chess:

- **VS IA** — motor **Atlas** (Stockfish con fuerza ajustada, dificultades 1–10) o **Stockfish** completo de escritorio.
- **En línea P2P** — crea una sala o únete con código de 6 caracteres; partida directa dispositivo↔dispositivo, compatible con el APK móvil.
- **Torneos** — varias partidas con rondas, colores alternados y marcador.
- **Historial** — todas tus partidas con PGN y miniaturas; estadísticas con gráficas (resumen, flujo, aperturas, progreso, resultados por color).
- **Premove** — encadena jugadas por adelantado como en Lichess.
- **Relojes** con autoridad del anfitrión, sincronizados por red (igual que en el APK).

## Instalar en el PC

1. Abre **[B-Chess Desktop](https://elalchico.github.io/B-Chess/B-Chess-Desktop/)** (GitHub Pages) en **Chrome** o **Edge**.
2. Pulsa el icono **Instalar B-Chess Desktop** (o Menú → «Instalar B-Chess»).
3. Se crea un acceso directo tipo app, se abre en su propia ventana y listo.

También funciona sirviéndolo localmente:

```bash
python -m http.server 8090
```

## Actualizaciones automáticas (como en el APK)

El APK se actualiza solo cuando hay un release nuevo en [B-Chess Releases](https://github.com/ElalChico/B-Chess/releases). **La versión PC hace lo mismo**:

1. Cada **release nuevo** de [ElalChico/B-Chess](../releases/latest) actualiza GitHub Pages con la última versión de `B-Chess-Desktop/`.
2. Al abrir la app, el **service worker** descarga los cambios al instante (sin instalar ni reinstalar).
3. En la barra inferior hay la insignia de versión; al pulsarla la app **comprueba los releases** de GitHub y, si hay uno nuevo, aplica la actualización al instante.

Por tanto: **instalas una vez y te olvidas**. Tu historial, tu configuración y tu perfil se conservan siempre (están en tu navegador).

## Tecnología

JavaScript puro + [chess.js](https://github.com/jhlywa/chess.js), Stockfish (lite-single) como motor, P2P por WebRTC + Firebase Realtime Database para el protocolo (mismo formato que el APK), Apache ECharts para las estadísticas. Sin backend, sin registro.
