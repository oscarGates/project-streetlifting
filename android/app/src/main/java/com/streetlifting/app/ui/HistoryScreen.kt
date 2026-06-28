package com.streetlifting.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streetlifting.app.core.PlanCalculator
import com.streetlifting.app.core.Program
import com.streetlifting.app.data.SessionWithLogs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: AppViewModel, onBack: () -> Unit) {
    val sessions by vm.sessions.collectAsStateSafe()
    val df = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial y progreso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Una tarjeta de progreso (1RM estimado) por principal.
            item { Text("1RM estimado (Epley)", style = MaterialTheme.typography.titleLarge) }

            Program.DIAS.forEachIndexed { idx, dia ->
                val serie = estimated1rmSeries(sessions, idx)
                if (serie.isNotEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(dia.principal.etiqueta, fontWeight = FontWeight.Bold)
                                    Text("máx: ${"%.1f".format(serie.max())}")
                                }
                                LineChart(points = serie, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                Text("Sesiones", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp))
            }

            if (sessions.isEmpty()) {
                item { Text("Aún no has registrado sesiones.") }
            }

            items(sessions) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "${Program.DIAS.getOrNull(s.session.diaIndex)?.titulo ?: "Día"} · " +
                                "Ciclo ${s.session.ciclo} · Sem ${s.session.semana}",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(df.format(Date(s.session.fechaEpoch)),
                            style = MaterialTheme.typography.bodySmall)
                        s.logs.filter { it.tipo == "MAIN" }.forEach { log ->
                            val reps = log.reps?.toString() ?: "-"
                            val peso = log.pesoUsado?.let { fmtNum1(it) } ?: "-"
                            Text("${log.label}: $peso × $reps",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (s.session.notas.isNotBlank()) {
                            Text("📝 ${s.session.notas}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

/** 1RM estimado por sesión (cronológico) para un día/principal. */
private fun estimated1rmSeries(sessions: List<SessionWithLogs>, diaIndex: Int): List<Float> =
    sessions
        .filter { it.session.diaIndex == diaIndex }
        .sortedBy { it.session.fechaEpoch }
        .mapNotNull { s ->
            s.logs.filter { it.tipo == "MAIN" && it.pesoUsado != null && it.reps != null }
                .maxOfOrNull { PlanCalculator.epley1rm(it.pesoUsado!!, it.reps!!) }
                ?.toFloat()
        }

private fun fmtNum1(x: Double): String =
    if (x == kotlin.math.floor(x)) x.toLong().toString() else "%.1f".format(x)
