package com.streetlifting.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streetlifting.app.core.FiveThreeOne
import com.streetlifting.app.core.PrincipalCalc
import com.streetlifting.app.core.Program
import com.streetlifting.app.data.ConfigEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    vm: AppViewModel,
    onOpenDay: (semana: Int, diaIndex: Int) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val config by vm.config.collectAsStateSafe()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan 5/3/1") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Historial")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                    }
                },
            )
        },
    ) { padding ->
        val cfg = config
        if (cfg == null) {
            Text("Cargando…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        val calcs by produceState<Map<String, PrincipalCalc>>(emptyMap(), cfg) {
            value = vm.calcularTodos(cfg)
        }

        val maxSemanas = if (cfg.incluirDescarga) 4 else 3
        var semana by rememberSaveable { mutableIntStateOf(1) }
        if (semana > maxSemanas) semana = maxSemanas

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CycleHeaderCard(cfg, calcs) }

            item {
                Text("Semana", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..maxSemanas).forEach { w ->
                        val label = if (w == 4) "Descarga" else "Sem $w"
                        FilterChip(
                            selected = semana == w,
                            onClick = { semana = w },
                            label = { Text(label) },
                        )
                    }
                }
            }

            items(Program.DIAS.withIndex().toList()) { (idx, dia) ->
                val calc = calcs[dia.principal.clave]
                DayCard(
                    titulo = dia.titulo,
                    principal = dia.principal.etiqueta,
                    calc = calc,
                    semana = semana,
                    onClick = { onOpenDay(semana, idx) },
                )
            }
        }
    }
}

@Composable
private fun CycleHeaderCard(cfg: ConfigEntity, calcs: Map<String, PrincipalCalc>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ciclo ${cfg.ciclo}", style = MaterialTheme.typography.titleLarge)
            Text(Program.DISTRIBUCION, style = MaterialTheme.typography.bodySmall)
            val redondeo = if (cfg.micro) "microplacas (1.25 kg / 2.5 lbs)"
            else "estándar (2.5 kg / 5 lbs)"
            Text(
                "Redondeo: $redondeo" + if (cfg.incluirDescarga) "" else " · sin descarga",
                style = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Text("Training Max", style = MaterialTheme.typography.titleMedium)
            Program.DIAS.forEach { dia ->
                val c = calcs[dia.principal.clave] ?: return@forEach
                val tm = FiveThreeOne.fmt(FiveThreeOne.redondear(c.tm, c.unit, cfg.micro))
                val tmNext = FiveThreeOne.fmt(FiveThreeOne.redondear(c.tmNext, c.unit, cfg.micro))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dia.principal.etiqueta, fontWeight = FontWeight.Medium)
                    Text("$tm ${c.unit}  →  $tmNext ${c.unit}")
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    titulo: String,
    principal: String,
    calc: PrincipalCalc?,
    semana: Int,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text("Principal: $principal · 5/3/1", style = MaterialTheme.typography.bodyMedium)
            val week = calc?.semanas?.getOrNull(semana - 1)
            if (week != null) {
                week.series.forEach { s ->
                    val reps = "${s.reps}${if (s.amrap) "+" else ""}"
                    Text(
                        "${s.pct}% x$reps · ${s.pesoTexto}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
