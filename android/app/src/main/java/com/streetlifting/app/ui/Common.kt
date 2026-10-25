package com.streetlifting.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streetlifting.app.core.BloqueCalc
import kotlinx.coroutines.flow.StateFlow

/** Atajo uniforme para recolectar un StateFlow en Compose. */
@Composable
fun <T> StateFlow<T>.collectAsStateSafe(): State<T> = collectAsState()

/** Resumen de un día: sus bloques con la prescripción de la semana. */
@Composable
fun DayCard(
    titulo: String,
    bloques: List<BloqueCalc>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val m = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Card(modifier = m) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (titulo.isNotEmpty()) Text(titulo, style = MaterialTheme.typography.titleMedium)
            bloques.forEach { b ->
                Text(b.titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (b.series531.isNotEmpty()) {
                    b.series531.forEach { s ->
                        val reps = "${s.reps}${if (s.amrap) "+" else ""}"
                        Text(
                            "${s.pct}% x$reps · ${s.pesoTexto}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    Text(b.prescripcion, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
