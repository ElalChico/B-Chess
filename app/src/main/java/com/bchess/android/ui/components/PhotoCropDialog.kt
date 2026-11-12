/*
 * B-CHESS · © Elal Chico
 * Diálogo de recorte de foto de perfil.
 *
 * La foto se dibuja con una matriz EXPLÍCITA (escala + desplazamiento) sobre
 * un Canvas, de forma que lo que se ve en el recuadro circular es exactamente
 * la región que se guarda. Antes se usaba ContentScale.Crop, que recortaba la
 * imagen antes de aplicar el zoom: la previsualización y el resultado no
 * coincidían y la foto se salía del recuadro.
 */

package com.bchess.android.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Zoom máximo permitido con el pellizco. */
private const val MAX_ZOOM = 5f

/**
 * Diálogo para recortar la foto de perfil.
 * - Pellizca para acercar y arrastra para posicionar.
 * - Botones para zoom preciso y para restablecer.
 * - El recuadro circular es SIEMPRE el centro y es lo que se guarda.
 * - [onConfirm] recibe un Bitmap cuadrado ya recortado.
 */
@Composable
fun PhotoCropDialog(
    bitmap: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val maxW = maxWidth
            val maxH = maxHeight

            // Recuadro CIRCULAR SIEMPRE centrado, con margen respecto a la pantalla
            val frameSide: Dp = (if (maxW < maxH) maxW else maxH) * 0.78f
            val frameSizePx = with(density) { frameSide.toPx() }

            val srcW = bitmap.width.toFloat().coerceAtLeast(1f)
            val srcH = bitmap.height.toFloat().coerceAtLeast(1f)

            // Escala base: la foto SIEMPRE cubre el recuadro (nunca quedan huecos)
            val coverScale = remember(frameSizePx, srcW, srcH) {
                max(frameSizePx / srcW, frameSizePx / srcH)
            }

            // Zoom del usuario y desplazamiento del contenido
            var zoom by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            // Tamaño real del contenido dibujado (para limitar el arrastre)
            val drawnW = srcW * coverScale * zoom
            val drawnH = srcH * coverScale * zoom
            val maxOffsetX = max(0f, (drawnW - frameSizePx) / 2f)
            val maxOffsetY = max(0f, (drawnH - frameSizePx) / 2f)

            fun clampOffset(value: Offset): Offset = Offset(
                value.x.coerceIn(-maxOffsetX, maxOffsetX),
                value.y.coerceIn(-maxOffsetY, maxOffsetY)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Ajusta tu foto",
                    color = Color(0xFFEEF1F5),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Pellizca para acercar · arrastra para centrar",
                    color = Color(0xFF8B96A5),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Zona interactiva: mide EXACTAMENTE igual que el recuadro circular
                Box(
                    modifier = Modifier
                        .size(frameSide)
                        .clip(CircleShape)
                        .background(Color(0xFF0B0D10)),
                    contentAlignment = Alignment.Center
                ) {
                    // Foto dibujada con matriz explícita: lo que se ve es lo que se guarda
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                                    val oldZoom = zoom
                                    val newZoom = (oldZoom * gestureZoom).coerceIn(1f, MAX_ZOOM)

                                    // Mantiene fijo el punto que está bajo el dedo
                                    val anchor = centroid - Offset(size.width / 2f, size.height / 2f) - offset
                                    val zoomShift = anchor * (1f - newZoom / oldZoom)

                                    val target = offset + zoomShift + pan
                                    zoom = newZoom

                                    // Los límites dependen del nuevo tamaño del contenido
                                    val newDrawnW = srcW * coverScale * newZoom
                                    val newDrawnH = srcH * coverScale * newZoom
                                    val limitX = max(0f, (newDrawnW - frameSizePx) / 2f)
                                    val limitY = max(0f, (newDrawnH - frameSizePx) / 2f)
                                    offset = Offset(
                                        target.x.coerceIn(-limitX, limitX),
                                        target.y.coerceIn(-limitY, limitY)
                                    )
                                }
                            }
                    ) {
                        val dstW = srcW * coverScale * zoom
                        val dstH = srcH * coverScale * zoom
                        val left = size.width / 2f - dstW / 2f + offset.x
                        val top = size.height / 2f - dstH / 2f + offset.y
                        drawImage(
                            image = bitmap.asImageBitmap(),
                            srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                            srcSize = androidx.compose.ui.unit.IntSize(bitmap.width, bitmap.height),
                            dstOffset = androidx.compose.ui.unit.IntOffset(left.roundToInt(), top.roundToInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(dstW.roundToInt(), dstH.roundToInt())
                        )
                    }

                    // Borde VERDE del recuadro CIRCULAR (siempre centrado)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFF3DDC97),
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Zoom preciso y restablecer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZoomButton("-") {
                        val newZoom = (zoom / 1.4f).coerceIn(1f, MAX_ZOOM)
                        zoom = newZoom
                        offset = clampOffset(offset)
                    }
                    Text(
                        "${(zoom * 100).roundToInt()}%",
                        color = Color(0xFF8B96A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    ZoomButton("+") {
                        val newZoom = (zoom * 1.4f).coerceIn(1f, MAX_ZOOM)
                        zoom = newZoom
                        offset = clampOffset(offset)
                    }
                    Spacer(Modifier.size(6.dp))
                    Button(
                        onClick = {
                            zoom = 1f
                            offset = Offset.Zero
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF151C25),
                            contentColor = Color(0xFFEEF1F5)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        )
                    ) { Text("Centrar", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }

                Spacer(Modifier.height(10.dp))

                // Botones: recorte real centrado sobre la foto transformada
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151C25), contentColor = Color(0xFFEEF1F5))
                    ) { Text("Cancelar", fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = {
                            val cropped = buildCroppedBitmap(
                                source = bitmap,
                                viewSizePx = frameSizePx,
                                coverScale = coverScale,
                                zoom = zoom,
                                offsetPx = offset
                            )
                            onConfirm(cropped)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC97), contentColor = Color(0xFF0B0D10))
                    ) { Text("Guardar", fontWeight = FontWeight.ExtraBold) }
                }
                Text(
                    "El recuadro circular es lo que se guarda",
                    color = Color(0xFF6B6244),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF151C25),
            contentColor = Color(0xFFEEF1F5)
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

/**
 * Recorta el cuadrado que ocupa el recuadro en la previsualización.
 *
 * Usa EXACTAMENTE la misma matriz que el Canvas:
 *   escala = coverScale * zoom,  centro del contenido = centro + offset
 */
private fun buildCroppedBitmap(
    source: Bitmap,
    viewSizePx: Float,
    coverScale: Float,
    zoom: Float,
    offsetPx: Offset
): Bitmap {
    val bmpW = source.width.toFloat()
    val bmpH = source.height.toFloat()
    val imageScale = coverScale * zoom.coerceIn(1f, MAX_ZOOM)

    // Lado del cuadrado del recuadro expresado en píxeles de la imagen original
    val side = (viewSizePx / imageScale).coerceIn(1f, min(bmpW, bmpH))

    // Centro del recuadro en coordenadas de la imagen original
    val centerX = bmpW / 2f - offsetPx.x / imageScale
    val centerY = bmpH / 2f - offsetPx.y / imageScale

    val pixelSide = side.roundToInt().coerceIn(1, min(source.width, source.height))
    val x = (centerX - pixelSide / 2f).roundToInt().coerceIn(0, max(0, source.width - pixelSide))
    val y = (centerY - pixelSide / 2f).roundToInt().coerceIn(0, max(0, source.height - pixelSide))
    return Bitmap.createBitmap(source, x, y, pixelSide, pixelSide)
}

/** Utilidad: reduce un bitmap grande antes de mostrar el diálogo. */
fun downscale(src: Bitmap, maxSide: Int): Bitmap {
    val w = src.width
    val h = src.height
    val m = max(w, h)
    if (m <= maxSide) return src
    val f = maxSide.toFloat() / m
    val matrix = Matrix().apply { postScale(f, f) }
    return Bitmap.createBitmap(src, 0, 0, w, h, matrix, true)
}
