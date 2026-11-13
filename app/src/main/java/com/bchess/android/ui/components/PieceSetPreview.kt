/*
 * B-Chess · © Elal Chico
 * Selector de conjunto de piezas con vista previa de 12 piezas por set
 * (6 blancas arriba, 6 negras abajo) montadas sobre casillas claras/oscuras,
 * para que el usuario vea exactamente cómo se ve cada conjunto antes de elegirlo.
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.R
import com.bchess.android.core.Piece
import com.bchess.android.core.PieceType
import com.bchess.android.core.Color as ChessColor
import com.bchess.android.data.PieceSet

private val Card = Color(0xFF151C25)
private val CardSel = Color(0x26E2B94E)
private val Line = Color(0xFF232C38)
private val LineSel = Color(0xFFE2B94E)
private val Text1 = Color(0xFFEEF1F5)
private val Gold = Color(0xFFE2B94E)
private val LightSquare = Color(0xFFB79C6D)
private val DarkSquare = Color(0xFF5F4F35)
private val TrackBg = Color(0xFF0F141B)
private val VBorder = Color(0xFF2E3A45)

/**
 * Grid de 17 conjuntos con vista previa de 12 piezas cada uno.
 * 3 columnas; cada celda muestra miniaturas de Torre, Caballo, Alfil, Dama, Rey, Peón
 * (fila superior blancas, inferior negras) con casillas claras/oscuras alternadas.
 */
@Composable
fun PieceSetGridSelector(
    selected: PieceSet,
    isEn: Boolean,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val pieces = listOf(
        PieceType.ROOK,
        PieceType.KNIGHT,
        PieceType.BISHOP,
        PieceType.QUEEN,
        PieceType.KING,
        PieceType.PAWN
    )
    Column(
        modifier = modifier
            .background(if (selected) CardSel else Card, RoundedCornerShape(14.dp))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) LineSel else Line, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            if (isEn) set.labelEn else set.labelEs,
            color = if (selected) Gold else Text1,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            minLines = 1
        )
        // Fila superior: piezas blancas
        MiniRow(
            pieces = pieces,
            colorType = ChessColor.WHITE,
            set = set,
            context = ctx,
            rowIndex = 0
        )
        // Fila inferior: piezas negras
        MiniRow(
            pieces = pieces,
            colorType = ChessColor.BLACK,
            set = set,
            context = ctx,
            rowIndex = 1
        )
    }
}

@Composable
private fun MiniRow(
    pieces: List<PieceType>,
    colorType: ChessColor,
    set: PieceSet,
    context: android.content.Context,
    rowIndex: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(TrackBg)
            .border(1.dp, VBorder, RoundedCornerShape(8.dp))
    ) {
        pieces.forEachIndexed { index, type ->
            val light = (index + rowIndex) % 2 == 0
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        if (light) LightSquare else DarkSquare,
                        RoundedCornerShape(3.dp)
                    )
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                if (set.resKey != null || set == PieceSet.LICHESS) {
                    Image(
                        painter = painterResource(id = pieceResource(context, Piece(type, colorType), set)),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PieceIcon(
                        piece = Piece(type, colorType),
                        modifier = Modifier.fillMaxSize(),
                        pieceSet = set
                    )
                }
            }
        }
    }
}
