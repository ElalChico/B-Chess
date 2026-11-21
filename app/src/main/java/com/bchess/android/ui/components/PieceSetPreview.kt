/*
 * B-Chess · © Elal Chico
 * Selector de conjunto de piezas tipo dial: navegación fácil con botones grandes
 * y vista de todas las piezas del conjunto seleccionado.
 */

package com.bchess.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.core.Piece
import com.bchess.android.core.PieceType
import com.bchess.android.data.PieceSet
import com.bchess.android.core.Color as ChessColor

private val Card = Color(0xFF151C25)
private val Card2 = Color(0xFF10151C)
private val Line = Color(0xFF232C38)
private val Line2 = Color(0xFF2F3947)
private val Text1 = Color(0xFFEEF1F5)
private val Gold = Color(0xFFC9B15A)
private val Muted = Color(0xFF8B96A5)

private val PIECE_ORDER = listOf(
    PieceType.KING,
    PieceType.QUEEN,
    PieceType.ROOK,
    PieceType.BISHOP,
    PieceType.KNIGHT,
    PieceType.PAWN
)

/** Dial de conjuntos de piezas: botones grandes y vista de todas las piezas. */
@Composable
fun PieceSetDial(
    selected: PieceSet,
    isEn: Boolean,
    onSelect: (PieceSet) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = PieceSet.entries
    val index = entries.indexOf(selected).coerceAtLeast(0)
    val count = entries.size

    fun step(delta: Int) {
        val next = ((index + delta) % count + count) % count
        onSelect(entries[next])
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                if (isEn) selected.labelEn else selected.labelEs,
                color = Text1,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                "${index + 1} / $count",
                color = Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        PieceSetPiecesPreview(pieceSet = selected)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DialNavButton(
                onClick = { step(-1) },
                contentDescription = if (isEn) "Previous set" else "Conjunto anterior",
                isPrevious = true
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (isEn) "Tap arrows to browse sets" else "Toca las flechas para cambiar",
                color = Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(3f)
            )
            Spacer(Modifier.weight(1f))
            DialNavButton(
                onClick = { step(1) },
                contentDescription = if (isEn) "Next set" else "Conjunto siguiente",
                isPrevious = false
            )
        }
    }
}

@Composable
private fun PieceSetPiecesPreview(pieceSet: PieceSet) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Card2, RoundedCornerShape(12.dp))
            .border(1.dp, Line2, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PieceRow(color = ChessColor.WHITE, pieceSet = pieceSet)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Line2)
        )
        PieceRow(color = ChessColor.BLACK, pieceSet = pieceSet)
    }
}

@Composable
private fun PieceRow(color: ChessColor, pieceSet: PieceSet) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PIECE_ORDER.forEach { type ->
            val piece = Piece(type, color)
            val res = pieceResource(context, piece, pieceSet)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (color == ChessColor.WHITE) Color(0xFF1C222C) else Color(0xFF12161D))
                    .border(1.dp, Line2, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (res != 0) {
                    Image(
                        painter = painterResource(id = res),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(3.dp)
                            .size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialNavButton(
    onClick: () -> Unit,
    contentDescription: String,
    isPrevious: Boolean
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E2530), CircleShape)
            .border(2.dp, Gold, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPrevious) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
            contentDescription = contentDescription,
            tint = Gold,
            modifier = Modifier.size(34.dp)
        )
    }
}
