package com.streetlifting.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streetlifting.app.core.AccesorioEstado
import com.streetlifting.app.core.BloqueCalc
import com.streetlifting.app.core.FiveThreeOne
import com.streetlifting.app.core.PlanCalculator
import com.streetlifting.app.core.PrincipalCalc
import com.streetlifting.app.core.Program
import com.streetlifting.app.data.AccesorioEntity
import com.streetlifting.app.data.ConfigEntity
import com.streetlifting.app.data.LiftEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    vm: AppViewModel,
    onOpenDay: (semana: Int, diaIndex: Int) -> Unit,
) {
    val config by vm.config.collectAsStateSafe()
    val lifts by vm.lifts.collectAsStateSafe()
    val accesorios by vm.accesorios.collectAsStateSafe()
    val semanaActual by vm.semanaActual.collectAsStateSafe()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Plan 5/3/1") }) },
        // La barra de navegación inferior ya reserva su espacio.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        val cfg = config
        if (cfg == null) {
            Text("Cargando…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        val calcs by produceState<Map<String, PrincipalCalc>>(emptyMap(), cfg, lifts) {
            value = vm.calcularTodos(cfg)
        }

        val maxSemanas = if (cfg.incluirDescarga) 4 else 3
        // 0 = sin elegir: se muestra la semana que toca según el calendario.
        var elegida by rememberSaveable { mutableIntStateOf(0) }
        val hoySemana = semanaActual.coerceIn(1, maxSemanas)
        val semana = if (elegida == 0) hoySemana else elegida.coerceAtMost(maxSemanas)

        val dias by produceState<List<List<BloqueCalc>>>(emptyList(), cfg, lifts, accesorios, semana) {
            value = Program.DIAS.indices.map { vm.calcularDia(it, semana, cfg) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                var confirmar by rememberSaveable { mutableStateOf(false) }
                CycleHeaderCard(cfg, calcs, lifts, onAvanzar = { confirmar = true })
                if (confirmar) {
                    AvanzarCicloDialog(
                        cfg = cfg,
                        calcs = calcs,
                        accesorios = accesorios,
                        onConfirm = {
                            vm.avanzarCiclo()
                            elegida = 0
                            confirmar = false
                        },
                        onDismiss = { confirmar = false },
                    )
                }
            }

            item {
                Text("Semana", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..maxSemanas).forEach { w ->
                        val label = (if (w == 4) "Descarga" else "Sem $w") + if (w == hoySemana) " •" else ""
                        FilterChip(
                            selected = semana == w,
                            onClick = { elegida = w },
                            label = { Text(label) },
                        )
                    }
                }
            }

            items(Program.DIAS.withIndex().toList()) { (idx, dia) ->
                DayCard(
                    titulo = dia.titulo,
                    bloques = dias.getOrNull(idx).orEmpty(),
                    onClick = { onOpenDay(semana, idx) },
                )
            }
        }
    }
}

@Composable
private fun CycleHeaderCard(
    cfg: ConfigEntity,
    calcs: Map<String, PrincipalCalc>,
    lifts: List<LiftEntity>,
    onAvanzar: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Mes ${cfg.ciclo}", style = MaterialTheme.typography.titleLarge)
            Text(Program.DISTRIBUCION, style = MaterialTheme.typography.bodySmall)
            val redondeo = if (cfg.micro) "microplacas (1.25 kg / 2.5 lbs)"
            else "estándar (2.5 kg / 5 lbs)"
            Text(
                "Redondeo: $redondeo" + if (cfg.incluirDescarga) "" else " · sin descarga",
                style = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Text("Training Max (85%)", style = MaterialTheme.typography.titleMedium)
            Program.PRINCIPALES.forEach { p ->
                val c = calcs[p.clave] ?: return@forEach
                val ciclo = lifts.firstOrNull { it.clave == p.clave }?.ciclo ?: 1
                val tm = FiveThreeOne.fmt(FiveThreeOne.redondear(c.tm, c.unit, cfg.micro))
                val tmNext = FiveThreeOne.fmt(FiveThreeOne.redondear(c.tmNext, c.unit, cfg.micro))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("${p.etiqueta} · ciclo $ciclo", fontWeight = FontWeight.Medium)
                    Text(
                        "$tm → $tmNext ${c.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            OutlinedButton(
                onClick = onAvanzar,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Avanzar al siguiente ciclo") }
        }
    }
}

/** Confirma el avance de ciclo mostrando cómo quedan principales y accesorios. */
@Composable
private fun AvanzarCicloDialog(
    cfg: ConfigEntity,
    calcs: Map<String, PrincipalCalc>,
    accesorios: List<AccesorioEntity>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Pasar al mes ${cfg.ciclo + 1}?") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item { Text("Training Max", fontWeight = FontWeight.Bold) }
                items(Program.PRINCIPALES) { p ->
                    val c = calcs[p.clave] ?: return@items
                    val tm = FiveThreeOne.fmt(FiveThreeOne.redondear(c.tm, c.unit, cfg.micro))
                    val tmNext = FiveThreeOne.fmt(FiveThreeOne.redondear(c.tmNext, c.unit, cfg.micro))
                    Text("${p.etiqueta}: $tm → $tmNext ${c.unit}", style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Text("Accesorios · semana 1", fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp))
                }
                items(Program.ACCESORIOS) { def ->
                    val e = accesorios.firstOrNull { it.clave == def.clave } ?: return@items
                    val ahora = PlanCalculator.calcularAccesorio(
                        def, AccesorioEstado(e.pesoInicial, e.salto, e.ciclo), 1)
                    val luego = PlanCalculator.calcularAccesorio(
                        def, AccesorioEstado(e.pesoInicial, e.salto, e.ciclo + 1), 1)
                    fun txt(a: com.streetlifting.app.core.AccesorioCalc) =
                        "${a.series}×${a.reps}" + (a.peso?.let { " · ${FiveThreeOne.fmt(it)} ${def.unidad}" } ?: "")
                    Text("${def.nombre}: ${txt(ahora)} → ${txt(luego)}",
                        style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Text(
                        "Las sesiones registradas se conservan en el historial del mes ${cfg.ciclo}.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Avanzar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
