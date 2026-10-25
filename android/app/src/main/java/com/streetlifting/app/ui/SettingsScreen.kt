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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.streetlifting.app.core.Program
import kotlinx.coroutines.launch
import com.streetlifting.app.data.AccesorioEntity
import com.streetlifting.app.data.ConfigEntity
import com.streetlifting.app.data.LiftEntity

private class LiftState(
    val clave: String,
    val etiqueta: String,
    rm: String,
    unit: String,
    esBw: Boolean,
    val tren: String,
    ciclo: String,
    incremento: String,
) {
    var rm by mutableStateOf(rm)
    var unit by mutableStateOf(unit)
    var esBw by mutableStateOf(esBw)
    var ciclo by mutableStateOf(ciclo)
    var incremento by mutableStateOf(incremento)
}

private class AccState(
    val clave: String,
    val nombre: String,
    val unidad: String?,
    pesoInicial: String,
    salto: String,
    ciclo: String,
) {
    var pesoInicial by mutableStateOf(pesoInicial)
    var salto by mutableStateOf(salto)
    var ciclo by mutableStateOf(ciclo)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val config by vm.config.collectAsStateSafe()
    val lifts by vm.lifts.collectAsStateSafe()
    val accesorios by vm.accesorios.collectAsStateSafe()

    var pesoCorporal by remember { mutableStateOf("") }
    var pcUnit by remember { mutableStateOf("kg") }
    var micro by remember { mutableStateOf(false) }
    var descarga by remember { mutableStateOf(true) }
    var ciclo by remember { mutableStateOf("1") }
    var pesoBarra by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val liftStates = remember { mutableStateListOf<LiftState>() }
    val accStates = remember { mutableStateListOf<AccState>() }

    LaunchedEffect(config, lifts, accesorios) {
        val c = config
        if (!loaded && c != null && lifts.isNotEmpty() && accesorios.isNotEmpty()) {
            pesoCorporal = fmtCfg(c.pesoCorporal)
            pcUnit = c.pcUnit
            micro = c.micro
            descarga = c.incluirDescarga
            ciclo = c.ciclo.toString()
            pesoBarra = c.pesoBarraKg?.let { fmtCfg(it) } ?: ""
            liftStates.clear()
            Program.PRINCIPALES.mapNotNull { p -> lifts.firstOrNull { it.clave == p.clave } }.forEach {
                liftStates.add(
                    LiftState(
                        it.clave, it.etiqueta, fmtCfg(it.rm), it.unit, it.esBw, it.tren,
                        it.ciclo.toString(), fmtCfg(it.incremento),
                    )
                )
            }
            accStates.clear()
            Program.ACCESORIOS.forEach { def ->
                val e = accesorios.firstOrNull { it.clave == def.clave } ?: return@forEach
                accStates.add(
                    AccState(
                        def.clave, def.nombre, def.unidad,
                        e.pesoInicial?.let { fmtCfg(it) } ?: "", fmtCfg(e.salto), e.ciclo.toString(),
                    )
                )
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes y maxes") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                                label = { Text("Mes actual") },
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
                                label = { Text(if (ls.esBw) "1RM total (cuerpo + lastre)" else "1RM") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            UnitChips(ls.unit) { ls.unit = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumField("Ciclo", ls.ciclo, KeyboardType.Number, Modifier.weight(1f)) { ls.ciclo = it }
                            NumField("Incremento TM/ciclo", ls.incremento, KeyboardType.Decimal, Modifier.weight(1f)) {
                                ls.incremento = it
                            }
                        }
                        SwitchRow("Lastrado (peso corporal)", ls.esBw) { ls.esBw = it }
                    }
                }
            }

            item { Text("Accesorios", style = MaterialTheme.typography.titleLarge) }

            items(accStates) { a ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(a.nombre, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (a.unidad != null) {
                                NumField("Peso inicial (${a.unidad})", a.pesoInicial, KeyboardType.Decimal,
                                    Modifier.weight(1f)) { a.pesoInicial = it }
                                NumField("Salto", a.salto, KeyboardType.Decimal, Modifier.weight(1f)) { a.salto = it }
                            }
                            NumField("Ciclo", a.ciclo, KeyboardType.Number, Modifier.weight(1f)) { a.ciclo = it }
                        }
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
                            val prev = lifts.firstOrNull { it.clave == ls.clave }
                            vm.updateLift(
                                LiftEntity(
                                    clave = ls.clave,
                                    etiqueta = ls.etiqueta,
                                    rm = ls.rm.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                    unit = ls.unit,
                                    esBw = ls.esBw,
                                    tren = ls.tren,
                                    ciclo = ls.ciclo.toIntOrNull()?.coerceAtLeast(1) ?: prev?.ciclo ?: 1,
                                    incremento = ls.incremento.replace(",", ".").toDoubleOrNull()
                                        ?: prev?.incremento ?: 0.0,
                                )
                            )
                        }
                        vm.updateAccesorios(
                            accStates.map { a ->
                                val prev = accesorios.firstOrNull { it.clave == a.clave }
                                AccesorioEntity(
                                    clave = a.clave,
                                    pesoInicial = a.pesoInicial.replace(",", ".").toDoubleOrNull()
                                        ?: prev?.pesoInicial,
                                    salto = a.salto.replace(",", ".").toDoubleOrNull() ?: prev?.salto ?: 0.0,
                                    ciclo = a.ciclo.toIntOrNull()?.coerceAtLeast(1) ?: prev?.ciclo ?: 1,
                                )
                            }
                        )
                        scope.launch { snackbar.showSnackbar("Ajustes guardados ✓") }
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
private fun NumField(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
    )
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
