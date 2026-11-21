/*
 * B-Chess · © Elal Chico
 * Vista previa grande del conjunto de piezas con navegación.
 * Reemplaza al PieceSetDial compacto.
 */

package com.bchess.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.core.ChessEngine
import com.bchess.android.core.Color as ChessColor
import com.bchess.android.data.AppPreferences
import com.bchess.android.data.PieceSet
import com.bchess.android.ui.components.ChessBoard
import com.bchess.android.ui.components.BoardSelection

private val Card = Color(0xFF151C25)
private val Card2 = Color(0xFF1A2330)
private val Line = Color(0xFF232C38)
private val Line2 = Color(0xFF2F3947)
private val Text1 = Color(0xFFEEF1F5)
private val Gold = Color(0xFFC9B15A)
private val Green = Color(0xFF3DDC97)
private val Muted = Color(0xFF8B96A5)

@Composable
fun PieceSetPreviewLarge(
    selected: PieceSet,
    isEn: Boolean,
    onSelect: (PieceSet) -> Unit,
    prefs: AppPreferences,
    modifier: Modifier = Modifier
) {
    val entries = PieceSet.entries
    val index = entries.indexOf(selected).coerceAtLeast(0)
    val count = entries.size

    fun step(delta: Int) {
        val next = ((index + delta) % count + count) % count
        onSelect(entries[next])
    }

    val previewSnapshot = ChessEngine().startingPosition()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Card2.copy(alpha = 0.9f),
                            Card.copy(alpha = 0.95f),
                            Card2.copy(alpha = 0.9f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .border(1.5.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (isEn) selected.labelEn else selected.labelEs,
                        color = Gold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${index + 1} / $count",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                ChessBoard(
                    snapshot = previewSnapshot,
                    perspective = ChessColor.WHITE,
                    selection = BoardSelection(null),
                    onSquareTap = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .border(2.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    theme = prefs.boardTheme,
                    pieceSet = selected,
                    showMoveIndicators = false,
                    showCoordinates = true,
                    interactionEnabled = false
                )

                // Panel de piezas (simplificado con símbolos Unicode)
                PieceSampleStrip(pieceSet = selected)

                // Botones de navegación grandes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF2A2412), Color(0xFF1E1A0F))),
                                CircleShape
                            )
                            .border(2.dp, Gold, CircleShape)
                            .clickable { step(-1) }
                            .padding(0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = if (isEn) "Previous set" else "Conjunto anterior",
                            tint = Gold,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        entries.forEachIndexed { i, _ ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == index) 12.dp else 8.dp, 8.dp)
                                    .background(
                                        if (i == index) Gold else Line2,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }

                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF2A2412), Color(0xFF1E1A0F))),
                                CircleShape
                            )
                            .border(2.dp, Gold, CircleShape)
                            .clickable { step(1) }
                            .padding(0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = if (isEn) "Next set" else "Conjunto siguiente",
                            tint = Gold,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    if (isEn) "Swipe or tap arrows to browse all piece sets" else "Desliza o toca las flechas para ver todos los conjuntos",
                    color = Muted.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PieceSampleStrip(pieceSet: PieceSet) {
    val whitePieces = listOf("♔", "♕", "♖", "♗", "♘", "♙")
    val blackPieces = listOf("♚", "♛", "♜", "♝", "♞", "♟")
    val pieceLabels = listOf("K", "Q", "R", "B", "N", "P")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Piezas del conjunto", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            whitePieces.forEachIndexed { i, symbol ->
                PieceSymbolBox(
                    symbol = symbol,
                    label = pieceLabels[i],
                    size = 52.dp,
                    isWhite = true,
                    isKing = (i == 0)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(0.dp, 1.dp)
                .background(Gold.copy(alpha = 0.3f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            blackPieces.forEachIndexed { i, symbol ->
                PieceSymbolBox(
                    symbol = symbol,
                    label = pieceLabels[i].lowercase(),
                    size = 52.dp,
                    isWhite = false,
                    isKing = (i == 0)
                )
            }
        }
    }
}

@Composable
private fun PieceSymbolBox(
    symbol: String,
    label: String,
    size: androidx.compose.ui.unit.Dp,
    isWhite: Boolean,
    isKing: Boolean
) {
    val bgColor = if (isWhite) Color(0xFF1E1E1E) else Color(0xFF0F0F0F)
    val borderColor = if (isWhite) Gold.copy(alpha = 0.5f) else Line2
    val symbolColor = if (isWhite) Color(0xFFF5F5F5) else Color(0xFFCCCCCC)

    Box(
        modifier = Modifier
            .size(size)
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                symbol,
                color = symbolColor,
                fontSize = 28.sp,
                fontWeight = if (isKing) FontWeight.ExtraBold else FontWeight.Bold,
                letterSpacing = -2.sp
            )
            Text(
                label.uppercase(),
                color = Muted,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}