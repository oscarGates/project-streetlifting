package com.streetlifting.app.core

import com.streetlifting.app.core.FiveThreeOne.convertir
import com.streetlifting.app.core.FiveThreeOne.fmt
import com.streetlifting.app.core.FiveThreeOne.redondear

/**
 * Cálculo del plan con las fórmulas de "Plan 5-3-1 mensual por movimiento.xlsx":
 * - TM = 1RM total × 0.85 + (ciclo − 1) × incremento.
 * - Barra: ROUND(TM × % / redondeo) × redondeo.
 * - Lastrado: lastre = ROUND((TM × % − peso corporal) / redondeo) × redondeo;
 *   negativo = asistencia.
 */

data class SerieCalc(
    val pct: Int,
    val reps: Int,
    val amrap: Boolean,
    val pesoTexto: String,
    /** Carga objetivo total en la unidad del ejercicio (para registrar/analizar). */
    val objetivo: Double,
)

data class WeekCalc(
    val nombre: String,
    val deload: Boolean,
    val series: List<SerieCalc>,
)

data class PrincipalCalc(
    val unit: String,
    val esBw: Boolean,
    val bwDisp: Double?,
    val tm: Double,
    val tmNext: Double,
    val semanas: List<WeekCalc>,
)

/** Parámetros de un principal tal y como los guarda el usuario. */
data class MaxInput(
    /** 1RM total: en lastrados incluye peso corporal + lastre. */
    val rm: Double,
    val unit: String,
    val esBw: Boolean,
    /** Ciclo actual del movimiento (1-based). */
    val ciclo: Int = 1,
    /** Incremento de TM por ciclo, en la unidad del ejercicio. */
    val incremento: Double,
)

/** Estado editable de un accesorio (carga inicial, salto y ciclo). */
data class AccesorioEstado(
    val pesoInicial: Double?,
    val salto: Double,
    val ciclo: Int,
)

/** Prescripción de un accesorio para una semana concreta. */
data class AccesorioCalc(
    val peso: Double?,
    val series: Int,
    val reps: Int,
    val deload: Boolean,
)

/** Una serie concreta a registrar. [peso] es la carga total (null = sin carga). */
data class SetPlan(
    val etiqueta: String,
    val peso: Double?,
    val reps: Int,
    val amrap: Boolean = false,
    val detalle: String = "",
)

/** Un bloque del día ya calculado para una semana, listo para mostrar/registrar. */
data class BloqueCalc(
    val titulo: String,
    /** Series del 5/3/1 (vacío si el bloque no es el principal 5/3/1). */
    val series531: List<SerieCalc>,
    /** Prescripción en una línea. */
    val prescripcion: String,
    /** Series a registrar, una fila por serie. */
    val sets: List<SetPlan>,
    /** Unidad de la carga ("kg", "lb", "kg/mano"), null = sin carga. */
    val unidad: String?,
    /** Salto de los botones −/+ de peso. */
    val paso: Double,
    /** Peso corporal (en [unidad]) si es lastrado: se edita el lastre. */
    val pesoCorporal: Double? = null,
    /** Peso de la barra si es un levantamiento con barra (para los discos). */
    val barra: Double? = null,
    /** Descanso sugerido tras cada serie, en segundos (0 = sin temporizador). */
    val descanso: Int = 0,
    val cardio: Boolean = false,
)

object PlanCalculator {

    const val DESCANSO_PRINCIPAL = 180
    const val DESCANSO_SECUNDARIO = 120
    const val DESCANSO_ACCESORIO = 90

    /** Parámetros globales que afectan a la carga. */
    data class Entorno(
        val pesoCorporal: Double,
        val pcUnit: String,
        val micro: Boolean,
        val pesoBarra: Double? = null,
    )

    /** Carga para un % del TM: (objetivo total, texto para mostrar). */
    private fun carga(max: MaxInput, tm: Double, pct: Double, env: Entorno): Pair<Double, String> {
        val unit = max.unit
        return if (max.esBw) {
            val bw = convertir(env.pesoCorporal, env.pcUnit, unit)
            val lastre = redondear(tm * pct - bw, unit, env.micro)
            val texto = when {
                lastre > 0 -> "+${fmt(lastre)} $unit"
                lastre == 0.0 -> "peso corporal"
                else -> "asistencia ${fmt(-lastre)} $unit"
            }
            (bw + lastre) to texto
        } else {
            val objetivo = redondear(tm * pct, unit, env.micro)
            val barra = env.pesoBarra ?: Program.pesoBarra(unit)
            val porLado = (objetivo - barra) / 2.0
            val texto = if (porLado > 0)
                "${fmt(objetivo)} $unit  (${fmt(porLado)}/lado)"
            else
                "${fmt(objetivo)} $unit  (solo barra)"
            objetivo to texto
        }
    }

    fun tm(max: MaxInput): Double =
        max.rm * FiveThreeOne.TM_FACTOR + (max.ciclo - 1).coerceAtLeast(0) * max.incremento

    fun calcularPrincipal(
        max: MaxInput,
        env: Entorno,
        incluirDescarga: Boolean,
    ): PrincipalCalc {
        val tm = tm(max)
        val semanas = FiveThreeOne.CYCLE
            .filter { !it.deload || incluirDescarga }
            .map { wk ->
                val series = wk.sets.map { s ->
                    val (objetivo, texto) = carga(max, tm, s.pct, env)
                    SerieCalc(
                        pct = Math.round(s.pct * 100).toInt(),
                        reps = s.reps,
                        amrap = s.amrap,
                        pesoTexto = texto,
                        objetivo = objetivo,
                    )
                }
                WeekCalc(wk.nombre, wk.deload, series)
            }
        val bwDisp = if (max.esBw) convertir(env.pesoCorporal, env.pcUnit, max.unit) else null
        return PrincipalCalc(max.unit, max.esBw, bwDisp, tm, tm + max.incremento, semanas)
    }

    /** Progresión doble de un accesorio; [semana] 1..4 (4 = descarga). */
    fun calcularAccesorio(def: AccesorioDef, est: AccesorioEstado, semana: Int): AccesorioCalc {
        val c = (est.ciclo - 1).coerceAtLeast(0)
        val tope = def.repsTope
        val pasos = tope?.let { it - def.repsInicio + 1 }
        val pesoCiclo = est.pesoInicial?.let { p ->
            if (pasos == null) p else p + est.salto * (c / pasos)
        }
        if (semana >= 4) {
            val peso = pesoCiclo?.let { (it - def.saltosDeload * est.salto).coerceAtLeast(0.0) }
            return AccesorioCalc(peso, def.seriesDeload, def.repsDeload, deload = true)
        }
        val extra = semana - 1
        val reps = if (tope == null || pasos == null) def.repsInicio + c + extra
        else minOf(tope, def.repsInicio + c % pasos + extra)
        return AccesorioCalc(pesoCiclo, def.series, reps, deload = false)
    }

    /** Calcula todos los bloques de [dia] para la [semana] (1..4). */
    fun calcularDia(
        dia: Dia,
        semana: Int,
        lifts: Map<String, MaxInput>,
        accesorios: Map<String, AccesorioEstado>,
        env: Entorno,
        incluirDescarga: Boolean,
    ): List<BloqueCalc> = dia.bloques.mapNotNull { b ->
        when (b) {
            is Bloque.Principal531 -> {
                val max = lifts[b.principal.clave] ?: return@mapNotNull null
                val week = calcularPrincipal(max, env, incluirDescarga).semanas.getOrNull(semana - 1)
                val series = week?.series.orEmpty()
                BloqueCalc(
                    titulo = "${b.principal.etiqueta} · 5/3/1",
                    series531 = series,
                    prescripcion = week?.nombre ?: "",
                    sets = series.mapIndexed { i, s ->
                        SetPlan("Serie ${i + 1}", s.objetivo, s.reps, s.amrap, "${s.pct}% · ${s.pesoTexto}")
                    },
                    unidad = max.unit,
                    paso = FiveThreeOne.incremento(max.unit, env.micro),
                    pesoCorporal = bwDe(max, env),
                    barra = barraDe(max, env),
                    descanso = DESCANSO_PRINCIPAL,
                )
            }
            is Bloque.Secundario -> {
                val max = lifts[b.principal.clave] ?: return@mapNotNull null
                val v = b.variante
                val deload = semana >= 4
                val pct = if (deload) v.pctDeload else v.pct
                val series = if (deload) v.seriesDeload else v.series
                val reps = if (deload) v.repsDeload else v.reps
                val (objetivo, texto) = carga(max, tm(max), pct, env)
                val nota = if (deload) "Deload" else v.indicacion
                BloqueCalc(
                    titulo = "${b.principal.etiqueta} ${v.nombre}",
                    series531 = emptyList(),
                    prescripcion = "${series}×$reps @ ${Math.round(pct * 100)}% · $texto · $nota",
                    sets = List(series) { SetPlan("Serie ${it + 1}", objetivo, reps) },
                    unidad = max.unit,
                    paso = FiveThreeOne.incremento(max.unit, env.micro),
                    pesoCorporal = bwDe(max, env),
                    barra = barraDe(max, env),
                    descanso = DESCANSO_SECUNDARIO,
                )
            }
            is Bloque.Accesorio -> {
                val d = b.def
                val est = accesorios[d.clave]
                    ?: AccesorioEstado(d.pesoInicial, d.salto, 1)
                val a = calcularAccesorio(d, est, semana)
                val carga = a.peso?.let { "${fmt(it)} ${d.unidad}" } ?: "peso corporal"
                val nota = if (a.deload) "Deload" else d.indicacion
                BloqueCalc(
                    titulo = d.nombre,
                    series531 = emptyList(),
                    prescripcion = "${a.series}×${a.reps} · $carga · $nota",
                    sets = List(a.series) { SetPlan("Serie ${it + 1}", a.peso, a.reps) },
                    unidad = d.unidad.takeIf { a.peso != null },
                    paso = pasoAccesorio(d.unidad),
                    descanso = DESCANSO_ACCESORIO,
                )
            }
            is Bloque.Cardio -> BloqueCalc(
                titulo = "Cardio",
                series531 = emptyList(),
                prescripcion = b.descripcion,
                sets = listOf(SetPlan("Minutos", null, b.minutos)),
                unidad = null,
                paso = 0.0,
                cardio = true,
            )
        }
    }

    private fun barraDe(max: MaxInput, env: Entorno): Double? =
        if (max.esBw) null else env.pesoBarra ?: Program.pesoBarra(max.unit)

    private fun bwDe(max: MaxInput, env: Entorno): Double? =
        if (max.esBw) convertir(env.pesoCorporal, env.pcUnit, max.unit) else null

    /** Salto de los botones −/+ según la unidad del accesorio. */
    private fun pasoAccesorio(unidad: String?): Double = when (unidad) {
        "lb" -> 5.0
        "kg/mano" -> 1.0
        else -> 2.5
    }

    /** 1RM estimado (Epley) a partir de un set: peso * (1 + reps/30). */
    fun epley1rm(peso: Double, reps: Int): Double =
        if (reps <= 0) peso else peso * (1.0 + reps / 30.0)
}
