package com.streetlifting.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.streetlifting.app.core.Bloque
import com.streetlifting.app.core.BloqueCalc
import com.streetlifting.app.core.FiveThreeOne
import com.streetlifting.app.core.Program
import com.streetlifting.app.core.SetPlan
import com.streetlifting.app.data.SetLogEntity
import kotlinx.coroutines.delay

/**
 * Estado editable de una serie. [peso] es lo que se muestra: el lastre en
 * lastrados (negativo = asistencia) y la carga en el resto.
 */
private class SetRow(peso: String, reps: String, hecho: Boolean) {
    var peso by mutableStateOf(peso)
    var reps by mutableStateOf(reps)
    var hecho by mutableStateOf(hecho)
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

    val cfg = config
    if (cfg == null) {
        Text("Cargando…", Modifier.padding(16.dp)); return
    }

    var bloques by remember { mutableStateOf<List<BloqueCalc>>(emptyList()) }
    var rows by remember { mutableStateOf<List<List<SetRow>>>(emptyList()) }
    var notas by remember { mutableStateOf("") }
    // Cada cambio sube la versión; el guardado espera a que paren los cambios.
    var version by remember { mutableIntStateOf(0) }
    var guardada by remember { mutableIntStateOf(0) }
    val onChange: () -> Unit = { version++ }

    LaunchedEffect(cfg.ciclo, semana, diaIndex) {
        val bs = vm.calcularDia(diaIndex, semana, cfg)
        val existing = vm.sessionFor(cfg.ciclo, semana, diaIndex)
        val logs = existing?.let { vm.setLogsFor(it.id) }.orEmpty()
        notas = existing?.notas ?: ""
        rows = bs.mapIndexed { bi, b ->
            b.sets.mapIndexed { si, plan ->
                val log = logs.firstOrNull { it.bloque == bi && it.idx == si }
                val peso = log?.pesoUsado ?: plan.peso
                SetRow(
                    peso = peso?.let { fmtNum(it - (b.pesoCorporal ?: 0.0)) } ?: "",
                    reps = (log?.reps ?: plan.reps).toString(),
                    hecho = log?.hecho ?: false,
                )
            }
        }
        bloques = bs
    }

    // Autoguardado: 600 ms después del último cambio.
    LaunchedEffect(version) {
        if (version == 0) return@LaunchedEffect
        delay(600)
        vm.saveSession(cfg.ciclo, semana, diaIndex, notas, buildLogs(bloques, rows))
        guardada = version
    }
    // Si se sale antes de que se dispare el autoguardado, guarda al salir.
    val pendiente by rememberUpdatedState(version != guardada)
    val guardarAlSalir by rememberUpdatedState {
        vm.saveSession(cfg.ciclo, semana, diaIndex, notas, buildLogs(bloques, rows))
    }
    DisposableEffect(Unit) {
        onDispose { if (pendiente) guardarAlSalir() }
    }

    val total = rows.sumOf { it.size }
    val hechas = rows.sumOf { r -> r.count { it.hecho } }

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
    ) { padding ->
        val semanaNombre = FiveThreeOne.CYCLE.getOrNull(semana - 1)?.nombre ?: "Semana $semana"
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProgressHeader(
                    titulo = "$semanaNombre · Mes ${cfg.ciclo}",
                    hechas = hechas,
                    total = total,
                    estado = when {
                        version == 0 -> ""
                        version == guardada -> "Guardado ✓"
                        else -> "Guardando…"
                    },
                    lastrado = dia.bloques.any { it.lastrado },
                )
            }

            bloques.forEachIndexed { bi, b ->
                item {
                    BloqueCard(b, rows.getOrNull(bi).orEmpty(), onChange)
                }
            }

            item {
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it; onChange() },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProgressHeader(titulo: String, hechas: Int, total: Int, estado: String, lastrado: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                estado,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else hechas.toFloat() / total },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text("$hechas/$total", style = MaterialTheme.typography.titleSmall)
        }
        if (lastrado) {
            Text(
                "Lastrados: se registra el lastre (negativo = asistencia).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BloqueCard(b: BloqueCalc, rows: List<SetRow>, onChange: () -> Unit) {
    val completo = rows.isNotEmpty() && rows.all { it.hecho }
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (completo) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(b.titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                b.prescripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            b.sets.forEachIndexed { i, plan ->
                rows.getOrNull(i)?.let { SetRowView(i + 1, plan, b, it, onChange) }
            }
        }
    }
}

@Composable
private fun SetRowView(numero: Int, plan: SetPlan, b: BloqueCalc, row: SetRow, onChange: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val fondo = if (row.hecho) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        if (plan.detalle.isNotEmpty() || plan.amrap) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (plan.amrap) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "AMRAP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    plan.detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "$numero",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(22.dp),
            )
            if (b.unidad != null) {
                Stepper(
                    value = row.peso,
                    onValueChange = { row.peso = it; onChange() },
                    paso = b.paso,
                    // Lastrados admiten negativo (asistencia).
                    minimo = if (b.pesoCorporal != null) null else 0.0,
                    caption = if (b.pesoCorporal != null) "lastre ${b.unidad}" else b.unidad,
                    decimal = true,
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Stepper(
                value = row.reps,
                onValueChange = { row.reps = it; onChange() },
                paso = if (b.cardio) 5.0 else 1.0,
                minimo = 0.0,
                caption = if (b.cardio) "min" else if (plan.amrap) "reps (máx)" else "reps",
                decimal = false,
            )
            FilledIconToggleButton(
                checked = row.hecho,
                onCheckedChange = {
                    row.hecho = it
                    if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onChange()
                },
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Serie hecha")
            }
        }
    }
}

/** − valor + : los botones suman [paso]; el número también se puede escribir. */
@Composable
private fun Stepper(
    value: String,
    onValueChange: (String) -> Unit,
    paso: Double,
    minimo: Double?,
    caption: String,
    decimal: Boolean,
) {
    fun bump(delta: Double) {
        var v = (parseD(value) ?: 0.0) + delta
        if (minimo != null) v = v.coerceAtLeast(minimo)
        onValueChange(if (decimal) fmtNum(v) else v.toInt().toString())
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { bump(-paso) }, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "Menos")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(62.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
        FilledTonalIconButton(onClick = { bump(paso) }, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Más")
        }
    }
}

private val Bloque.lastrado: Boolean
    get() = when (this) {
        is Bloque.Principal531 -> principal.esLastrado
        is Bloque.Secundario -> principal.esLastrado
        else -> false
    }

private fun buildLogs(bloques: List<BloqueCalc>, rows: List<List<SetRow>>): List<SetLogEntity> =
    bloques.flatMapIndexed { bi, b ->
        b.sets.mapIndexedNotNull { si, plan ->
            val row = rows.getOrNull(bi)?.getOrNull(si) ?: return@mapIndexedNotNull null
            val principal = b.series531.isNotEmpty()
            SetLogEntity(
                sessionId = 0,
                tipo = if (principal) "MAIN" else "AUX",
                bloque = bi,
                idx = si,
                label = if (principal) plan.etiqueta else "${b.titulo} · ${plan.etiqueta}",
                prescripcion = plan.detalle.ifEmpty { b.prescripcion },
                // Se guarda la carga total (cuerpo + lastre en lastrados).
                pesoUsado = parseD(row.peso)?.let { it + (b.pesoCorporal ?: 0.0) },
                reps = parseI(row.reps),
                hecho = row.hecho,
            )
        }
    }

private fun fmtNum(x: Double): String =
    if (x == kotlin.math.floor(x)) x.toLong().toString() else x.toString()
