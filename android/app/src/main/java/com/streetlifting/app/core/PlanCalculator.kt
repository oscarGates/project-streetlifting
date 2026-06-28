package com.streetlifting.app.core

import com.streetlifting.app.core.FiveThreeOne.convertir
import com.streetlifting.app.core.FiveThreeOne.fmt
import com.streetlifting.app.core.FiveThreeOne.redondear

/**
 * Cálculo de un principal, portado de `calcular_principal` en `pdf_531.py`.
 * Devuelve las semanas con sus 3 series y el peso ya formateado como en el PDF.
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
    val rm: Double,
    val unit: String,
    val esBw: Boolean,
    val tren: Tren,
)

object PlanCalculator {

    fun calcularPrincipal(
        max: MaxInput,
        pesoCorporal: Double,
        pcUnit: String,
        micro: Boolean,
        incluirDescarga: Boolean,
        pesoBarra: Double? = null,
        ciclo: Int = 1,
    ): PrincipalCalc {
        val unit = max.unit
        val barra = pesoBarra ?: Program.pesoBarra(unit)

        val incKg = if (max.tren == Tren.UPPER)
            FiveThreeOne.INCREMENT_UPPER_KG else FiveThreeOne.INCREMENT_LOWER_KG
        val incUnit = convertir(incKg, "kg", unit)

        val bwDisp: Double?
        val tmBase: Double
        if (max.esBw) {
            bwDisp = convertir(pesoCorporal, pcUnit, unit)
            tmBase = (bwDisp + max.rm) * FiveThreeOne.TM_FACTOR
        } else {
            bwDisp = null
            tmBase = max.rm * FiveThreeOne.TM_FACTOR
        }

        // Cada ciclo avanzado suma un incremento al TM.
        val tm = tmBase + (ciclo - 1).coerceAtLeast(0) * incUnit
        val tmNext = tm + incUnit

        val semanas = FiveThreeOne.CYCLE
            .filter { !it.deload || incluirDescarga }
            .map { wk ->
                val series = wk.sets.map { s ->
                    val objetivo = redondear(tm * s.pct, unit, micro)
                    val pesoTexto: String = if (max.esBw) {
                        val lastre = redondear(objetivo - bwDisp!!, unit, micro)
                        if (lastre >= 0) "+${fmt(lastre)} $unit" else "asistencia (liga)"
                    } else {
                        val porLado = (objetivo - barra) / 2.0
                        if (porLado > 0)
                            "${fmt(objetivo)} $unit  (${fmt(porLado)}/lado)"
                        else
                            "${fmt(objetivo)} $unit  (solo barra)"
                    }
                    SerieCalc(
                        pct = Math.round(s.pct * 100).toInt(),
                        reps = s.reps,
                        amrap = s.amrap,
                        pesoTexto = pesoTexto,
                        objetivo = objetivo,
                    )
                }
                WeekCalc(wk.nombre, wk.deload, series)
            }

        return PrincipalCalc(unit, max.esBw, bwDisp, tm, tmNext, semanas)
    }

    /** 1RM estimado (Epley) a partir de un set: peso * (1 + reps/30). */
    fun epley1rm(peso: Double, reps: Int): Double =
        if (reps <= 0) peso else peso * (1.0 + reps / 30.0)
}
