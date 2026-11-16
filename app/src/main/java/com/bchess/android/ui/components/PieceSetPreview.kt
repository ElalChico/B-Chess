/*
 * B-Chess · © Elal Chico
 * Selector de conjunto de piezas como un DIAL < Clásico >: ahorra espacio y
 * evita listar los 24 conjuntos. La vista previa del tablero en Ajustes
 * muestra cómo se ven las piezas.
 */

package com.bchess.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.bchess.android.data.PieceSet

private val Card = Color(0xFF151C25)
private val Line = Color(0xFF232C38)
private val Text1 = Color(0xFFEEF1F5)
private val Gold = Color(0xFFC9B15A)
private val Muted = Color(0xFF8B96A5)

/** Dial compacto: < Anterior | Nombre del conjunto (i/N) | Siguiente >. */
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { step(-1) }, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = if (isEn) "Previous set" else "Conjunto anterior",
                tint = Gold,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                if (isEn) selected.labelEn else selected.labelEs,
                color = Text1,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${index + 1} / $count",
                color = Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(onClick = { step(1) }, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = if (isEn) "Next set" else "Conjunto siguiente",
                tint = Gold,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
