# B-Chess · Ajedrez por Bluetooth

**Ajedrez por bluetooth. Super liviano. Con Stockfish.**

Juega entre dos móviles por **Bluetooth (SPP)**, sin cuentas ni servidores. También
practica localmente contra Stockfish.

[![Descargar B-Chess APK](https://img.shields.io/github/v/release/ElalChico/B-Chess?label=Descargar%20APK&color=0078D4&logo=github)](https://github.com/ElalChico/B-Chess/releases/latest)

## Qué es

App de ajedrez simple y directa para partidas presenciales. Un móvil crea la
partida (anfitrión) y el otro se une (invitado). Se vinculan por Bluetooth al
instante y juegan con reloj, sonidos y movimientos legales comprobados por un
motor propio.

- **Solo Bluetooth.** No usa WiFi ni servidores para jugar.
- **Sin registro.** Solo escribes tu nombre para jugar.
- **Practica contra Stockfish** (motor local incluido).
- **Actualizaciones in-app** desde GitHub Releases (misma firma, datos conservados).

## Instalar

Descarga el APK de la última versión desde **Releases** e instálalo en Android
8 o superior.

## Actualizaciones

Al iniciar, la app consulta GitHub Releases. Si hay una versión nueva, aparece
un aviso en **Inicio** y en **Ajustes** con un botón para descargar e instalar.
Android lo instala sobre la versión actual sin desinstalar, conservando tus
partidas, perfil y ajustes (mismo paquete y misma firma de release).

## Compilar tú mismo

```bash
./gradlew assembleDebug      # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest  # tests del motor de reglas
```

Para un release firmado:

```bash
./gradlew assembleRelease -PbchessVersion=1.0.1
```

Requiere JDK 17 y Android SDK 34. La firma debe ser siempre la misma (mismo
keystore) para que las actualizaciones futuras no requieran desinstalar.

---

Hecho por **Elal Chico** · B-Chess

## Licencia

Código bajo **MIT** (`LICENSE`): puedes modificarlo, contribuir y redistribuirlo,
incluso comercialmente, siempre que mantengas el aviso de copyright
`© 2026 Elal Chico`.
