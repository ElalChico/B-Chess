#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Genera el PDF de resumen del proyecto B-Chess."""

from fpdf import FPDF


class ResumenPDF(FPDF):
    def header(self):
        self.set_font("Arial", "B", 16)
        self.cell(0, 10, "B-Chess - Resumen del proyecto y pasos realizados", ln=True, align="C")
        self.ln(5)

    def chapter_title(self, title):
        self.set_font("Arial", "B", 13)
        self.set_text_color(0x1A, 0x1A, 0x1A)
        self.cell(0, 10, title, ln=True)
        self.ln(2)

    def chapter_body(self, body):
        self.set_font("Arial", "", 11)
        self.set_text_color(0x33, 0x33, 0x33)
        self.multi_cell(0, 7, body)
        self.ln(4)

    def bullet(self, text):
        self.set_font("Arial", "", 11)
        self.cell(5, 7, "-", ln=False)
        self.multi_cell(0, 7, text)


pdf = ResumenPDF()
pdf.add_page()

pdf.chapter_title("1. Estado del proyecto")
pdf.chapter_body(
    "Repositorio: https://github.com/ElalChico/B-Chess (rama main)\n"
    "Codigo fuente subido completo respetando el .gitignore (no se suben build/, .gradle/, keystore/, keystore.properties ni APKs).\n"
    "El APK no se sube al historial de git; se adjunta al Release de GitHub."
)

pdf.chapter_title("2. Paquete y firma propios (sin conflictos con Chess-X)")
pdf.chapter_body(
    "applicationId = com.bchess.android y namespace = com.bchess.android (antes com.chessx).\n"
    "Se actualizaron todas las referencias de com.chessx.R a com.bchess.android.R en HomeScreen.kt, RoomScreen.kt, ChessBoard.kt, GameDialogs.kt y SoundManager.kt.\n"
    "Todo el codigo se renombro del paquete com.chessx a com.bchess.android (35 archivos).\n"
    "Keystore exclusivo de B-Chess: keystore/bchess-release.jks, alias bchess/androiddebugkey.\n"
    "La firma es idéntica en todas las versiones (SHA-256: 51942381b8c8631402d517597bf63bd5f10f23cc37a001891d5702828439aee7), permitiendo actualizar sin desinstalar y sin perder datos.\n"
    "keystore.properties esta LOCAL y NO se sube al repo (ya esta en .gitignore)."
)

pdf.chapter_title("3. APK subido a Release de GitHub")
pdf.chapter_body(
    "Release v1.0.5 publicado: https://github.com/ElalChico/B-Chess/releases/tag/v1.0.5\n"
    "APK adjunto: B-Chess-1.0.5.apk\n"
    "URL directa de descarga: https://github.com/ElalChico/B-Chess/releases/download/v1.0.5/B-Chess-1.0.5.apk\n"
    "El CI de GitHub Actions compila y firma el APK automaticamente al publicar un tag v*.\n"
    "Secrets de GitHub configurados: BCHESS_ANDROID_KEYSTORE_BASE64 y BCHESS_ANDROID_KEYSTORE_PROPERTIES_BASE64."
)

pdf.chapter_title("4. README simple")
pdf.chapter_body(
    "Titulo: B-Chess · Ajedrez por Bluetooth\n"
    "Descripcion: 'Ajedrez por bluetooth. Super liviano. Con Stockfish.'\n"
    "Boton de descarga directa al Release mas reciente (releases/latest).\n"
    "Seccion 'Proyecto relacionado': Chess-X (https://github.com/ElalChico/chess-x) con boton.\n"
    "Seccion 'El desarrollador': creado por Elal Chico con bio y enlace a https://github.com/ElalChico."
)

pdf.chapter_title("5. Actualizaciones limpias (sin desinstalar)")
pdf.chapter_body(
    "UpdateManager apunta a ElalChico/B-Chess y descarga e instala de verdad el APK desde GitHub Releases.\n"
    "Banner de actualizacion en Home (UpdateFooter) y seccion en Configuracion (SettingsScreen) con boton 'Descargar e instalar', barra de progreso y estados.\n"
    "El APK se descarga al cache privado de la app (no al almacenamiento externo), evitando que el FileProvider falle y que queden copias en el movil.\n"
    "cleanupStaleDownloads() borra APKs de actualizaciones anteriores al iniciar el check.\n"
    "file_provider_paths.xml registra <cache-path path=\"updates/\" />.\n"
    "Como el paquete y la firma son idénticos, Android instala sobre la version actual sin desinstalar, conservando partidas, perfil y ajustes."
)

pdf.chapter_title("6. Perfil del desarrollador en Home")
pdf.chapter_body(
    "Tarjeta 'Creado por Elal Chico' con avatar sincronizado desde GitHub (se mantiene siempre).\n"
    "Boton 'Mi perfil de GitHub' -> https://github.com/ElalChico\n"
    "Boton 'Acerca de este desarrollador' con dialogo que muestra:\n"
    "  'Desarrollador autodidacta | Hacking ético & software libre | Comprometido con la justicia, el antiespecismo y la tecnología al servicio de la crítica social.'\n"
    "Boton 'Más detalles en GitHub' -> https://github.com/ElalChico.\n"
    "La foto de perfil que configure el usuario tiene prioridad sobre el avatar de GitHub."
)

pdf.chapter_title("7. Foto de perfil del usuario")
pdf.chapter_body(
    "Se corrigio el recorte: ahora se dibuja con matriz explicita (escala + desplazamiento) sobre Canvas, de modo que lo que se ve en el recuadro circular es exactamente lo que se guarda.\n"
    "Zoom, arrastre y centrado precisos; botones de zoom y centrar.\n"
    "setProfilePhotoBase64 rechaza base64 truncado o invalido en lugar de guardar una foto cortada."
)

pdf.chapter_title("8. GitHub Actions (CI)")
pdf.chapter_body(
    "Se arreglo el workflow bchess-release.yml: se aceptan las licencias del Android SDK de forma no interactiva (evitando el prompt 'Accept? (y/N)' que hacía fallar el job por timeout).\n"
    "Se elimino la accion android-actions/setup-android (intentaba instalar el paquete 'tools' que ya no existe) y se instala el SDK a mano con sdkmanager.\n"
    "Se agrego trigger workflow_dispatch para compilar y adjuntar el APK a un release sin crear tags a mano."
)

pdf.chapter_title("9. Archivos importantes NO subidos al repositorio")
pdf.chapter_body(
    "keystore/bchess-release.jks\n"
    "keystore.properties\n"
    "build/, .gradle/, *.apk, *.aab, .kotlin/\n"
    "Todos estos ya estaban en .gitignore; el PDF de este resumen se agrega ahora al .gitignore tambien."
)

pdf.output("RESUMEN-B-Chess.pdf")
print("PDF generado: RESUMEN-B-Chess.pdf")
