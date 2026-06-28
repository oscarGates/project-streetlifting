package com.streetlifting.app.core

import kotlin.math.floor

/**
 * Motor 5/3/1 portado de `ciclo_531.py` / `pdf_531.py`.
 *
 * - La onda de porcentajes ([CYCLE]) y el Training Max = 90% del 1RM ([TM_FACTOR])
 *   son idénticos al motor Python.
 * - Redondeo con microplacas: 2.5 kg por defecto, 1.25 kg con micro=true
 *   (en lbs equivale a 5 / 2.5 lbs).
 */
object FiveThreeOne {

    const val KG_TO_LBS = 2.20462
    const val TM_FACTOR = 0.90

    // Incrementos de TM para el siguiente ciclo (en kg; se convierten por unidad).
    const val INCREMENT_UPPER_KG = 2.5
    const val INCREMENT_LOWER_KG = 5.0

    data class SetScheme(val pct: Double, val reps: Int, val amrap: Boolean)
    data class WeekScheme(val nombre: String, val deload: Boolean, val sets: List<SetScheme>)

    /** Un ciclo de 4 semanas: 3 de trabajo + descarga. */
    val CYCLE: List<WeekScheme> = listOf(
        WeekScheme(
            "Semana 1 - 5/5/5+", false,
            listOf(SetScheme(0.65, 5, false), SetScheme(0.75, 5, false), SetScheme(0.85, 5, true))
        ),
        WeekScheme(
            "Semana 2 - 3/3/3+", false,
            listOf(SetScheme(0.70, 3, false), SetScheme(0.80, 3, false), SetScheme(0.90, 3, true))
        ),
        WeekScheme(
            "Semana 3 - 5/3/1+", false,
            listOf(SetScheme(0.75, 5, false), SetScheme(0.85, 3, false), SetScheme(0.95, 1, true))
        ),
        WeekScheme(
            "Semana 4 - Deload", true,
            listOf(SetScheme(0.40, 5, false), SetScheme(0.50, 5, false), SetScheme(0.60, 5, false))
        ),
    )

    fun convertir(valor: Double, desde: String, hacia: String): Double {
        if (desde == hacia) return valor
        return if (desde == "kg") valor * KG_TO_LBS else valor / KG_TO_LBS
    }

    /** Salto de disco según unidad y si se usan microplacas. */
    fun incremento(unit: String, micro: Boolean): Double =
        if (unit == "kg") (if (micro) 1.25 else 2.5) else (if (micro) 2.5 else 5.0)

    fun redondear(x: Double, unit: String, micro: Boolean = false): Double {
        val inc = incremento(unit, micro)
        // Math.rint = redondeo a par (half-to-even), igual que round() de Python.
        return Math.rint(x / inc) * inc
    }

    /** Formatea quitando el `.0` innecesario, como `core.fmt` en Python. */
    fun fmt(x: Double): String =
        if (x == floor(x) && !x.isInfinite()) x.toLong().toString() else x.toString()
}
