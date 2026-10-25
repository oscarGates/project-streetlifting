package com.streetlifting.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.streetlifting.app.core.Discos
import com.streetlifting.app.core.FiveThreeOne
import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

// ---- Temporizador de descanso ----

/** Barra inferior con la cuenta atrás del descanso. */
@Composable
fun RestBar(restanteSeg: Int, totalSeg: Int, onMas30: () -> Unit, onSaltar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            LinearProgressIndicator(
                progress = { if (totalSeg == 0) 0f else restanteSeg.toFloat() / totalSeg },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Descanso",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "%d:%02d".format(restanteSeg / 60, restanteSeg % 60),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onMas30) { Text("+30 s") }
                TextButton(onClick = onSaltar) { Text("Saltar") }
            }
        }
    }
}

/** Vibración de fin de descanso (dos pulsos). */
fun vibrar(ctx: Context) {
    val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        ctx.getSystemService(Vibrator::class.java)
    }
    v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
}

// ---- Discos por lado ----

private fun colorDisco(d: Double, unit: String): Color {
    val orden = if (unit == "kg") Discos.KG else Discos.LBS
    return when (orden.indexOf(d)) {
        0 -> Color(0xFFD32F2F) // 25 kg / 45 lb
        1 -> Color(0xFF1E88E5) // 20 / 35
        2 -> Color(0xFFFDD835) // 15 / 25
        3 -> Color(0xFF43A047) // 10
        4 -> Color(0xFFF5F5F5) // 5
        5 -> Color(0xFFE57373) // 2.5
        else -> Color(0xFFBDBDBD) // 1.25
    }
}

private fun altoDisco(d: Double, unit: String): Int {
    val orden = if (unit == "kg") Discos.KG else Discos.LBS
    return listOf(34, 30, 26, 22, 17, 13, 10).getOrElse(orden.indexOf(d)) { 10 }
}

/** Dibujo de los discos de un lado de la barra, con el detalle en texto. */
@Composable
fun PlateStrip(total: Double, barra: Double, unit: String) {
    val discos = Discos.porLado(total, barra, unit)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Box(
            Modifier.width(16.dp).height(6.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline)
        )
        discos.forEach { d ->
            Spacer(Modifier.width(2.dp))
            Box(
                Modifier.width(if (d >= 10) 7.dp else 5.dp).height(altoDisco(d, unit).dp)
                    .clip(RoundedCornerShape(2.dp)).background(colorDisco(d, unit))
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (discos.isEmpty()) "solo barra"
            else "por lado: " + discos.joinToString(" + ") { FiveThreeOne.fmt(it) },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- Récord en la serie AMRAP ----

/** Resultado de una serie AMRAP: 1RM estimado y mejor marca previa. */
data class AmrapResultado(val e1rm: Double, val mejorPrevio: Double?, val unidad: String) {
    val record: Boolean get() = mejorPrevio != null && e1rm > mejorPrevio + 1e-6
}

/** Aviso flotante con el 1RM estimado; confeti si es récord. */
@Composable
fun AmrapOverlay(r: AmrapResultado) {
    Box(Modifier.fillMaxSize()) {
        if (r.record) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        speed = 0f,
                        maxSpeed = 32f,
                        damping = 0.9f,
                        spread = 360,
                        colors = listOf(0xFF7A1A, 0xFFC24B, 0xFFFFFF, 0xE6BEA5),
                        emitter = Emitter(duration = 150, TimeUnit.MILLISECONDS).max(150),
                        position = Position.Relative(0.5, 0.25),
                    )
                ),
            )
        }
        Card(
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (r.record) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (r.record) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    when {
                        r.record -> "¡Nuevo récord! 🎉"
                        r.mejorPrevio == null -> "Primera marca registrada"
                        else -> "1RM estimado"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("%.1f %s".format(r.e1rm, r.unidad), style = MaterialTheme.typography.displaySmall)
                r.mejorPrevio?.let {
                    val dif = r.e1rm - it
                    Text(
                        "Mejor anterior: %.1f %s (%s%.1f)".format(it, r.unidad, if (dif >= 0) "+" else "", dif),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
