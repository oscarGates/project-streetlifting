package com.streetlifting.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.streetlifting.app.data.ConfigEntity
import com.streetlifting.app.data.LiftEntity

private class LiftState(
    val clave: String,
    val etiqueta: String,
    rm: String,
    unit: String,
    esBw: Boolean,
    tren: String,
) {
    var rm by mutableStateOf(rm)
    var unit by mutableStateOf(unit)
    var esBw by mutableStateOf(esBw)
    var tren by mutableStateOf(tren)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val config by vm.config.collectAsStateSafe()
    val lifts by vm.lifts.collectAsStateSafe()

    var pesoCorporal by remember { mutableStateOf("") }
    var pcUnit by remember { mutableStateOf("kg") }
    var micro by remember { mutableStateOf(false) }
    var descarga by remember { mutableStateOf(true) }
    var ciclo by remember { mutableStateOf("1") }
    var pesoBarra by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    val liftStates = remember { mutableStateListOf<LiftState>() }

    LaunchedEffect(config, lifts) {
        val c = config
        if (!loaded && c != null && lifts.isNotEmpty()) {
            pesoCorporal = fmtCfg(c.pesoCorporal)
            pcUnit = c.pcUnit
            micro = c.micro
            descarga = c.incluirDescarga
            ciclo = c.ciclo.toString()
            pesoBarra = c.pesoBarraKg?.let { fmtCfg(it) } ?: ""
            liftStates.clear()
            lifts.forEach {
                liftStates.add(LiftState(it.clave, it.etiqueta, fmtCfg(it.rm), it.unit, it.esBw, it.tren))
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes y maxes") },
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
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Configuración del ciclo", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pesoCorporal,
                                onValueChange = { pesoCorporal = it },
                                label = { Text("Peso corporal") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            UnitChips(pcUnit) { pcUnit = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ciclo,
                                onValueChange = { ciclo = it },
                                label = { Text("Ciclo actual") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = pesoBarra,
                                onValueChange = { pesoBarra = it },
                                label = { Text("Barra (opc.)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        SwitchRow("Microplacas (1.25 kg)", micro) { micro = it }
                        SwitchRow("Incluir semana de descarga", descarga) { descarga = it }
                    }
                }
            }

            item { Text("Principales (maxes)", style = MaterialTheme.typography.titleLarge) }

            items(liftStates) { ls ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(ls.etiqueta, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ls.rm,
                                onValueChange = { ls.rm = it },
                                label = { Text(if (ls.esBw) "1RM (lastre)" else "1RM") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            UnitChips(ls.unit) { ls.unit = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = ls.tren == "upper",
                                onClick = { ls.tren = "upper" },
                                label = { Text("Superior") },
                            )
                            FilterChip(
                                selected = ls.tren == "lower",
                                onClick = { ls.tren = "lower" },
                                label = { Text("Inferior") },
                            )
                        }
                        SwitchRow("Lastrado (peso corporal)", ls.esBw) { ls.esBw = it }
                    }
                }
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        config?.let { c ->
                            vm.updateConfig(
                                ConfigEntity(
                                    id = 0,
                                    pesoCorporal = pesoCorporal.replace(",", ".").toDoubleOrNull() ?: c.pesoCorporal,
                                    pcUnit = pcUnit,
                                    micro = micro,
                                    incluirDescarga = descarga,
                                    pesoBarraKg = pesoBarra.replace(",", ".").toDoubleOrNull(),
                                    ciclo = ciclo.toIntOrNull()?.coerceAtLeast(1) ?: c.ciclo,
                                )
                            )
                        }
                        liftStates.forEach { ls ->
                            vm.updateLift(
                                LiftEntity(
                                    clave = ls.clave,
                                    etiqueta = ls.etiqueta,
                                    rm = ls.rm.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                    unit = ls.unit,
                                    esBw = ls.esBw,
                                    tren = ls.tren,
                                )
                            )
                        }
                        onBack()
                    },
                ) { Text("Guardar") }
            }
        }
    }
}

@Composable
private fun UnitChips(unit: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(selected = unit == "kg", onClick = { onChange("kg") }, label = { Text("kg") })
        FilterChip(selected = unit == "lbs", onClick = { onChange("lbs") }, label = { Text("lbs") })
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

private fun fmtCfg(x: Double): String =
    if (x == kotlin.math.floor(x)) x.toLong().toString() else x.toString()
