/*
 * B-Chess · © Elal Chico
 * Selector de tema de tablero con vista previa estilo chess-bt-android:
 * mini-tablero 3x2 (2 filas, 3 columnas) con las casillas del tema.
 */

package com.bchess.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.data.BoardTheme

private val Card = Color(0xFF151C25)
private val CardSel = Color(0x26C9B15A)
private val Line = Color(0xFF232C38)
private val LineSel = Color(0xFFC9B15A)
private val Text1 = Color(0xFFEEF1F5)
private val Gold = Color(0xFFC9B15A)

/**
 * Grid de temas de tablero con vista previa 3x2 (estilo chess-bt-android).
 * 3 columnas por fila.
 */
@Composable
fun BoardThemeGridSelector(
    selected: BoardTheme,
    isEn: Boolean,
    onSelect: (BoardTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoardTheme.entries.chunked(3).forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowThemes.forEach { theme ->
                    BoardThemeCard(
                        theme = theme,
                        selected = theme == selected,
                        isEn = isEn,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(theme) }
                    )
                }
                repeat(3 - rowThemes.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BoardThemeCard(
    theme: BoardTheme,
    selected: Boolean,
    isEn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        // Mini-tablero 3x2 con las casillas del tema
        Column(
            Modifier
                .size(width = 40.dp, height = 27.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            repeat(2) { row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(3) { col ->
                        val light = (row + col) % 2 == 0
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (light) theme.lightSquare else theme.darkSquare)
                        )
                    }
                }
            }
        }
        Text(
            if (isEn) theme.labelEn else theme.labelEs,
            color = if (selected) Gold else Text1,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
