package com.streetlifting.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.streetlifting.app.core.Bloque
import com.streetlifting.app.core.BloqueCalc
import com.streetlifting.app.core.FiveThreeOne
import com.streetlifting.app.core.Program
import com.streetlifting.app.data.SetLogEntity
import kotlinx.coroutines.launch

private class FieldRow(peso: String, reps: String) {
    var peso by mutableStateOf(peso)
    var reps by mutableStateOf(reps)
}

private fun parseD(s: String): Double? = s.trim().replace(",", ".").toDoubleOrNull()
private fun parseI(s: String): Int? = s.trim().toIntOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    vm: AppViewModel,
    semana: Int,
    diaIndex: Int,
    onBack: () -> Unit,
) {
    val config by vm.config.collectAsStateSafe()
    val dia = Program.DIAS.getOrNull(diaIndex) ?: return
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cfg = config
    if (cfg == null) {
        Text("Cargando…", Modifier.padding(16.dp)); return
    }

    var bloques by remember { mutableStateOf<List<BloqueCalc>>(emptyList()) }
    val mainRows = remember { mutableStateListOf<FieldRow>() }
    val auxRows = remember { mutableStateListOf<FieldRow>() }
    var notas by remember { mutableStateOf("") }

    LaunchedEffect(cfg, semana, diaIndex) {
        val bs = vm.calcularDia(diaIndex, semana, cfg)
        val existing = vm.sessionFor(cfg.ciclo, semana, diaIndex)
        val logs = existing?.let { vm.setLogsFor(it.id) }.orEmpty()
        notas = existing?.notas ?: ""

        mainRows.clear()
        bs.flatMap { it.series531 }.forEachIndexed { i, s ->
            val log = logs.firstOrNull { it.tipo == "MAIN" && it.idx == i }
            mainRows.add(
                FieldRow(
                    peso = log?.pesoUsado?.let { fmtNum(it) } ?: fmtNum(s.objetivo),
                    reps = log?.reps?.toString() ?: s.reps.toString(),
                )
            )
        }
        auxRows.clear()
        bs.filter { it.series531.isEmpty() }.forEachIndexed { i, b ->
            val log = logs.firstOrNull { it.tipo == "AUX" && it.idx == i }
            auxRows.add(
                FieldRow(
                    peso = log?.pesoUsado?.let { fmtNum(it) } ?: b.pesoSugerido?.let { fmtNum(it) } ?: "",
                    reps = log?.nota ?: b.repsSugeridas,
                )
            )
        }
        bloques = bs
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dia.titulo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val semanaNombre = FiveThreeOne.CYCLE.getOrNull(semana - 1)?.nombre ?: "Semana $semana"
        val auxBloques = bloques.filter { it.series531.isEmpty() }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "$semanaNombre · Mes ${cfg.ciclo}",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (dia.bloques.any { it.lastrado }) {
                    Text(
                        "Lastrado: el peso es el total del sistema (cuerpo + lastre).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // ---- Principal 5/3/1: una fila por serie ----
            bloques.firstOrNull { it.series531.isNotEmpty() }?.let { b ->
                item { Text(b.titulo, style = MaterialTheme.typography.titleMedium) }
                b.series531.forEachIndexed { i, s ->
                    item {
                        val reps = "${s.reps}${if (s.amrap) "+" else ""}"
                        SetCard(
                            highlight = s.amrap,
                            titulo = "Serie ${i + 1}",
                            prescripcion = "${s.pct}% x$reps · ${s.pesoTexto}",
                            repsLabel = if (s.amrap) "Reps (AMRAP)" else "Reps",
                            repsNumeric = true,
                            row = mainRows.getOrNull(i),
                        )
                    }
                }
            }

            // ---- Resto de bloques: secundarios, accesorios y cardio ----
            if (auxBloques.isNotEmpty()) {
                item { Text("Secundarios y accesorios", style = MaterialTheme.typography.titleMedium) }
            }
            auxBloques.forEachIndexed { i, b ->
                item {
                    val esCardio = b.pesoSugerido == null && b.repsSugeridas.isEmpty()
                    SetCard(
                        highlight = false,
                        titulo = b.titulo,
                        prescripcion = b.prescripcion,
                        repsLabel = if (esCardio) "Duración / notas" else "Reps reales",
                        repsNumeric = false,
                        row = auxRows.getOrNull(i),
                        mostrarPeso = !esCardio,
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val logs = buildLogs(bloques, mainRows, auxRows)
                        vm.saveSession(cfg.ciclo, semana, diaIndex, notas, logs) {
                            scope.launch { snackbar.showMessage("Sesión guardada") }
                        }
                    },
                ) { Text("Guardar sesión") }
            }
        }
    }
}

private suspend fun SnackbarHostState.showMessage(msg: String) {
    showSnackbar(msg)
}

private val Bloque.lastrado: Boolean
    get() = when (this) {
        is Bloque.Principal531 -> principal.esLastrado
        is Bloque.Secundario -> principal.esLastrado
        else -> false
    }

private fun buildLogs(
    bloques: List<BloqueCalc>,
    mainRows: List<FieldRow>,
    auxRows: List<FieldRow>,
): List<SetLogEntity> {
    val out = ArrayList<SetLogEntity>()
    bloques.flatMap { it.series531 }.forEachIndexed { i, s ->
        val row = mainRows.getOrNull(i) ?: return@forEachIndexed
        val reps = "${s.reps}${if (s.amrap) "+" else ""}"
        out.add(
            SetLogEntity(
                sessionId = 0,
                tipo = "MAIN",
                idx = i,
                label = "Serie ${i + 1}",
                prescripcion = "${s.pct}% x$reps · ${s.pesoTexto}",
                pesoUsado = parseD(row.peso),
                reps = parseI(row.reps),
            )
        )
    }
    bloques.filter { it.series531.isEmpty() }.forEachIndexed { i, b ->
        val row = auxRows.getOrNull(i) ?: return@forEachIndexed
        out.add(
            SetLogEntity(
                sessionId = 0,
                tipo = "AUX",
                idx = i,
                label = b.titulo,
                prescripcion = b.prescripcion,
                pesoUsado = parseD(row.peso),
                reps = null,
                nota = row.reps.trim(),
            )
        )
    }
    return out
}

@Composable
private fun SetCard(
    highlight: Boolean,
    titulo: String,
    prescripcion: String,
    repsLabel: String,
    repsNumeric: Boolean,
    row: FieldRow?,
    mostrarPeso: Boolean = true,
) {
    if (row == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (highlight) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (highlight) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "AMRAP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Text("  ")
                }
                Text(titulo, fontWeight = FontWeight.Bold)
            }
            Text(prescripcion, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mostrarPeso) {
                    OutlinedTextField(
                        value = row.peso,
                        onValueChange = { row.peso = it },
                        label = { Text("Peso") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(140.dp),
                    )
                }
                OutlinedTextField(
                    value = row.reps,
                    onValueChange = { row.reps = it },
                    label = { Text(repsLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (repsNumeric) KeyboardType.Number else KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun fmtNum(x: Double): String =
    if (x == kotlin.math.floor(x)) x.toLong().toString() else x.toString()
