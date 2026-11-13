/*
 * B-Chess · © Elal Chico
 * Vista previa de temas de tablero: mini-tablero 4x4 con casillas alternadas
 * sobre fondo neutro para que el usuario vea contraste y tonalidad antes de elegir.
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bchess.android.data.BoardTheme

private val Card = Color(0xFF151C25)
private val CardSel = Color(0x26E2B94E)
private val Line = Color(0xFF232C38)
private val LineSel = Color(0xFFC9B15A)
private val Text1 = Color(0xFFEEF1F5)
private val Gold = Color(0xFFC9B15A)
private val TrackBg = Color(0xFF0F141B)

/**
 * Grid de temas de tablero con vista previa 4x4. 3 columnas.
 * Las casillas claras/oscuras tienen always contraste alto respecto al fondo del panel.
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
            .background(if (selected) CardSel else Card, RoundedCornerShape(14.dp))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) LineSel else Line, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            if (isEn) theme.labelEn else theme.labelEs,
            color = if (selected) Gold else Text1,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            minLines = 1
        )
        // Mini-tablero 4x4 sobre fondo oscuro del panel
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TrackBg)
                .border(1.dp, Color(0xFF2E3A45), RoundedCornerShape(8.dp))
        ) {
            for (row in 0 until 4) {
                Row {
                    for (col in 0 until 4) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(if ((row + col) % 2 == 0) theme.lightSquare else theme.darkSquare)
                        )
                    }
                }
            }
        }
    }
}
