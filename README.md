# B-Chess · Ajedrez por Bluetooth y Peer-to-Peer

**Ajedrez entre móviles. Super liviano. Con Stockfish.**

Juega entre dos móviles por **Bluetooth (SPP)** o por **Peer-to-Peer (WebRTC)**:
conexión directa entre los dos dispositivos, sin cuentas ni servidores en la
partida. También practica contra Stockfish con fuerza ajustable.

[![Descargar B-Chess APK](https://img.shields.io/github/v/release/ElalChico/B-Chess?label=Descargar%20APK&color=0078D4&logo=github)](https://github.com/ElalChico/B-Chess/releases/latest)
[![B-Chess Desktop](https://img.shields.io/github/v/release/ElalChico/B-Chess?label=%E2%9C%8B%20Desktop&color=2ea043&logo=github)](https://github.com/ElalChico/B-Chess/blob/main/B-Chess-Desktop/README.md)

> 💻 **B-Chess Desktop** — también hay versión para PC e instálable en el navegador (PWA). Abre [B-Chess-Desktop](B-Chess-Desktop/README.md) o visita **https://elalchico.github.io/B-Chess/B-Chess-Desktop/** en Chrome/Edge y pulsa **«Instalar B-Chess Desktop»**. Se instala como una app del sistema, con el propio motor Stockfish y sincronización P2P con el APK, y **se actualiza sola** cada vez que publicas un release aquí (mismo flujo de GitHub Releases que el APK).

## Qué es

App de ajedrez simple y directa para partidas entre dos dispositivos. Un móvil
crea la partida (anfitrión) y el otro se une (invitado). Se vinculan al instante
y juegan con reloj, sonidos y movimientos legales comprobados por un motor propio.

- **Bluetooth (SPP)** para partidas presenciales.
- **Peer-to-Peer por Internet** para partidas a distancia: los dos móviles se
  conectan directamente por WebRTC; Firebase solo comparte el código/SDP inicial
  (apretón de manos), y una vez abierta la conexión la partida viaja de
  dispositivo a dispositivo, sin intermediarios ni servidores.
- **Sin registro.** Solo escribes tu nombre para jugar.
- **Stockfish local** con fuerza ajustable; a partir del 50 % usa la potencia
  máxima real del motor (Elo 3190, Skill 20).
- **Actualizaciones in-app** desde GitHub Releases (misma firma, datos conservados).

## Funciones principales

- **Partidas Bluetooth y Peer-to-Peer** con el mismo protocolo de mensajes.
- **Torneo** por Bluetooth: varias partidas con puntuación acumulada.
- **Replay estilo chess.com**: repasa cualquier partida del historial con
  navegación paso a paso, reproducción automática (1x/2x/3x), reloj y coordenadas.
- **17 conjuntos de piezas** con vista previa (Alpha, Anarcandy, Gatos Punk,
  Cburnett, Leipzig, Merida, Pixel, Staunty, Tatiana…).
- **7 temas de tablero** con vista previa (Marrón Lichess, Oscuro Gris, Verde
  Profundo, Acero Azul, Pergamino, Burdeos, Marfil); las casillas
  claras/oscuras siempre contrastan con el fondo.
- **Historial** con pestañas Historial/Torneos, estadísticas (V/D/E, racha,
  balance) y minigráfico de las últimas partidas. Límite de 2 MB: se
  sobreescriben automáticamente las más antiguas.
- **Exportar PGN/OGN** manual desde el diálogo de fin de partida y desde el
  perfil (formato "User vs Stockfish").
- **Perfil del rival**: foto, estado de datos compartidos (foto/historial/PGN)
  y revisiones a petición.

## Proyecto relacionado

**Chess-X** es el proyecto hermano: el mismo juego de ajedrez por Bluetooth,
pero **sin Stockfish** y más ligero.

[![Ver Chess-X en GitHub](https://img.shields.io/badge/Chess--X-Proyecto%20similar%20sin%20Stockfish-0078D4?style=for-the-badge&logo=github)](https://github.com/ElalChico/chess-x)

[→ Ir a https://github.com/ElalChico/chess-x](https://github.com/ElalChico/chess-x)

## El desarrollador

Creado por **[Elal Chico](https://github.com/ElalChico)**.

> Desarrollador autodidacta | Hacking ético & software libre | Comprometido con
> la justicia, el antiespecismo y la tecnología al servicio de la crítica social.

[![Perfil de GitHub](https://img.shields.io/badge/GitHub-ElalChico-181717?style=for-the-badge&logo=github)](https://github.com/ElalChico)

## Instalar

Descarga el APK de la última versión desde **Releases** e instálalo en Android
8 o superior.

**Peer-to-Peer por Internet** requiere una conexión de datos disponible; el
Bluetooth funciona siempre sin red. Si falta la configuración de Firebase en un
build concreto, el modo P2P mostrará un error y podrás jugar por Bluetooth.

## Actualizaciones

Al iniciar, la app consulta GitHub Releases. Si hay una versión nueva, aparece
un aviso distintivo en **Inicio** y en **Ajustes** con un botón para descargar
e instalar. Android lo instala sobre la versión actual sin desinstalar,
conservando tus partidas, perfil y ajustes (mismo paquete y misma firma de
release).

## Compilar tú mismo

```bash
./gradlew assembleDebug      # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest  # tests del motor de reglas
```

Para un release firmado:

```bash
./gradlew assembleRelease -PbchessVersion=1.0.16
```

Requiere JDK 17 y Android SDK 34. La firma debe ser siempre la misma (mismo
keystore) para que las actualizaciones futuras no requieran desinstalar.
El modo Peer-to-Peer requiere `google-services.json` (Firebase Realtime
Database) para la señalización inicial.

---

## 💻 B-Chess Desktop (PC en el navegador)

Además del APK para Android, B-Chess tiene **versión de escritorio** en [`B-Chess-Desktop/`](B-Chess-Desktop/): mismas piezas, mismo motor y mismo protocolo que el APK — versión para **PC**: PWA instalable desde Chrome/Edge para ajedrez contra la IA y online P2P compatible con el APK móvil.

Instalación y actualización automática **sin reinstalar nunca**:

1. Abre **https://elalchico.github.io/B-Chess/B-Chess-Desktop/** en Chrome o Edge.
2. Pulsa **«Instalar B-Chess Desktop»** (icono de instalación en la barra de direcciones).
3. Instalas una vez y, en cada **release nuevo** de [Releases](https://github.com/ElalChico/B-Chess/releases/latest), GitHub Pages publica la versión actualizada: al abrir la app la recibes instantáneamente (service worker). Tu historial y tu configuración se conservan siempre.

Incluye además: torneos con colores alternados, replay del historial, premove como en Lichess, relojes con autoridad del anfitrión y estadísticas gráficas (resumen, flujo, aperturas, progreso y resultados por color).

---

Hecho por **Elal Chico** · B-Chess

## Licencia

Código bajo **MIT** (`LICENSE`): puedes modificarlo, contribuir y redistribuirlo,
incluso comercialmente, siempre que mantengas el aviso de copyright
`© 2026 Elal Chico`.
