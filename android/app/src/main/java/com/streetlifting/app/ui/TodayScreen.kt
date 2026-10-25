package com.streetlifting.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.streetlifting.app.core.BloqueCalc
import com.streetlifting.app.core.Calendario
import com.streetlifting.app.core.FiveThreeOne
import com.streetlifting.app.core.Program
import com.streetlifting.app.data.SessionWithLogs
import java.time.LocalDate

private val NOMBRES = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val INICIALES = listOf("L", "M", "X", "J", "V")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(vm: AppViewModel, onOpenDay: (semana: Int, diaIndex: Int) -> Unit) {
    val config by vm.config.collectAsStateSafe()
    val lifts by vm.lifts.collectAsStateSafe()
    val accesorios by vm.accesorios.collectAsStateSafe()
    val sessions by vm.sessions.collectAsStateSafe()
    val semanaActual by vm.semanaActual.collectAsStateSafe()

    val hoy = remember { LocalDate.now() }
    val diaHoy = Calendario.diaDelPlan(hoy.dayOfWeek)
    val diaIdx = Calendario.proximoDia(hoy.dayOfWeek)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hoy") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        val cfg = config
        if (cfg == null) {
            Text("Cargando…", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val maxSemanas = if (cfg.incluirDescarga) 4 else 3
        val mesTerminado = semanaActual > maxSemanas
        val semana = semanaActual.coerceIn(1, maxSemanas)

        val bloques by produceState<List<BloqueCalc>>(emptyList(), cfg, lifts, accesorios, semana, diaIdx) {
            value = vm.calcularDia(diaIdx, semana, cfg)
        }
        val delMes = sessions.filter { it.session.ciclo == cfg.ciclo && it.session.semana == semana }
        fun sesion(d: Int): SessionWithLogs? = delMes.firstOrNull { it.session.diaIndex == d }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column {
                    Text(
                        NOMBRES[hoy.dayOfWeek.value - 1],
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val nombreSemana = FiveThreeOne.CYCLE.getOrNull(semana - 1)?.nombre ?: "Semana $semana"
                    Text(
                        "$nombreSemana · Mes ${cfg.ciclo}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (mesTerminado) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("¡Mes terminado! 🎉", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Completaste las $maxSemanas semanas. Ve a Plan → " +
                                    "\"Avanzar al siguiente ciclo\" para subir los pesos.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            item {
                WeekStrip(
                    diaHoy = diaHoy,
                    estado = { d -> progreso(sesion(d)) },
                    onClick = { d -> onOpenDay(semana, d) },
                )
            }

            item {
                val p = progreso(sesion(diaIdx))
                val dia = Program.DIAS[diaIdx]
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (diaHoy == null) "Descanso 😴 · próximo: ${dia.titulo}" else dia.titulo,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Button(
                        onClick = { onOpenDay(semana, diaIdx) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text(
                            when {
                                p == null -> if (diaHoy == null) "  Ver entrenamiento" else "  Empezar"
                                p.first == p.second -> "  Completado ✓"
                                else -> "  Continuar (${p.first}/${p.second})"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            item { DayCard(titulo = "", bloques = bloques, onClick = null) }
        }
    }
}

/** (series hechas, series totales) de una sesión, o null si no hay sesión. */
private fun progreso(s: SessionWithLogs?): Pair<Int, Int>? =
    s?.let { it.logs.count { l -> l.hecho } to it.logs.size }

/** L M X J V: lleno = día completo, borde = empezado, anillo = hoy. */
@Composable
private fun WeekStrip(diaHoy: Int?, estado: (Int) -> Pair<Int, Int>?, onClick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        INICIALES.forEachIndexed { d, letra ->
            val p = estado(d)
            val completo = p != null && p.second > 0 && p.first == p.second
            val empezado = p != null && p.first > 0
            val primary = MaterialTheme.colorScheme.primary
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                completo -> primary
                                empezado -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                        .then(
                            if (d == diaHoy) Modifier.border(BorderStroke(2.dp, primary), CircleShape)
                            else Modifier
                        )
                        .clickable { onClick(d) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (completo) "✓" else letra,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (completo) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
