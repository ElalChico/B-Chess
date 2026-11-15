/*
 * B-Chess · © Elal Chico
 * Selector de conjunto de piezas (compacto, solo nombre): la vista previa
 * del tablero en Ajustes muestra cómo se ven las piezas.
 */

package com.bchess.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.data.BoardTheme
import com.bchess.android.data.PieceSet

private val Card = Color(0xFF151C25)
private val CardSel = Color(0x26C9B15A)
private val Line = Color(0xFF232C38)
private val LineSel = Color(0xFFC9B15A)
private val Text1 = Color(0xFFEEF1F5)
private val Gold = Color(0xFFC9B15A)

/**
 * Selector de 17 conjuntos: chips solo con el nombre (la previsualización
 * del tablero en Ajustes muestra las piezas).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieceSetGridSelector(
    selected: PieceSet,
    isEn: Boolean,
    boardTheme: BoardTheme,
    onSelect: (PieceSet) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PieceSet.entries.forEach { set ->
            PieceSetCard(
                set = set,
                selected = set == selected,
                isEn = isEn,
                modifier = Modifier.width(112.dp),
                onClick = { onSelect(set) }
            )
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
    Column(
        modifier = modifier
            .background(if (selected) CardSel else Card, RoundedCornerShape(10.dp))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) LineSel else Line, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            if (isEn) set.labelEn else set.labelEs,
            color = if (selected) Gold else Text1,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
