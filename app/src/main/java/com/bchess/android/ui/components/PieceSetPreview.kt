/*
 * B-Chess · © Elal Chico
 * Selector de conjunto de piezas con vista previa de tablero (estilo chess-bt-android):
 * mini-tablero 2x1 (casilla clara + casilla oscura del tema actual) con los
 * caballos blanco y negro del set seleccionado.
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.core.Piece
import com.bchess.android.core.PieceType
import com.bchess.android.core.Color as ChessColor
import com.bchess.android.data.BoardTheme
import com.bchess.android.data.PieceSet

private val Card = Color(0xFF151C25)
private val CardSel = Color(0x26C9B15A)
private val Line = Color(0xFF232C38)
private val LineSel = Color(0xFFC9B15A)
private val Text1 = Color(0xFFEEF1F5)
private val Gold = Color(0xFFC9B15A)

/**
 * Grid de 17 conjuntos con vista previa de tablero.
 * Cada chip muestra un mini-tablero 2x1 (casilla clara + oscura del tema)
 * con el caballo blanco y el caballo negro del set.
 */
@Composable
fun PieceSetGridSelector(
    selected: PieceSet,
    isEn: Boolean,
    boardTheme: BoardTheme,
    onSelect: (PieceSet) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PieceSet.entries.chunked(3).forEach { rowSets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSets.forEach { set ->
                    PieceSetCard(
                        set = set,
                        selected = set == selected,
                        isEn = isEn,
                        theme = boardTheme,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(set) }
                    )
                }
                repeat(3 - rowSets.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PieceSetCard(
    set: PieceSet,
    selected: Boolean,
    isEn: Boolean,
    theme: BoardTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    Column(
        modifier = modifier
            .background(if (selected) CardSel else Card, RoundedCornerShape(12.dp))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) LineSel else Line, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Dos filas: negras arriba y blancas abajo — las 6 piezas principales del set
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            val types = listOf(
                PieceType.KING, PieceType.QUEEN, PieceType.ROOK,
                PieceType.BISHOP, PieceType.KNIGHT, PieceType.PAWN
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(theme.darkSquare)
                    .padding(vertical = 3.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                types.forEach { t ->
                    PreviewPiece(Piece(t, ChessColor.BLACK), set, Modifier.size(22.dp), ctx)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(theme.lightSquare)
                    .padding(vertical = 3.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                types.forEach { t ->
                    PreviewPiece(Piece(t, ChessColor.WHITE), set, Modifier.size(22.dp), ctx)
                }
            }
        }
        Text(
            if (isEn) set.labelEn else set.labelEs,
            color = if (selected) Gold else Text1,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PreviewPiece(piece: Piece, set: PieceSet, modifier: Modifier, context: android.content.Context) {
    if (set.resKey != null || set == PieceSet.LICHESS) {
        Image(
            painter = painterResource(id = pieceResource(context, piece, set)),
            contentDescription = null,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        PieceIcon(
            piece = piece,
            modifier = modifier,
            pieceSet = set
        )
    }
}
