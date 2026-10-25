package com.streetlifting.app.core

/**
 * Estructura del programa según "Plan 5-3-1 mensual por movimiento.xlsx":
 * 3 principales 5/3/1 (sentadilla, fondo, dominada), cada uno con días
 * secundarios a % fijo del TM, accesorios con progresión doble y cardio.
 * La distribución semanal es la de la hoja `schedule`.
 */

enum class Tren { UPPER, LOWER }

/** clave: identificador estable usado como PK del max en la BD. */
data class Principal(
    val etiqueta: String,
    val clave: String,
    val esLastrado: Boolean,
    val tren: Tren,
)

/** Día secundario de un principal: % fijo del TM, sin AMRAP. */
data class Variante(
    val nombre: String,
    val pct: Double,
    val series: Int,
    val reps: Int,
    val pctDeload: Double,
    val seriesDeload: Int,
    val repsDeload: Int,
    val indicacion: String,
)

/**
 * Accesorio con progresión doble: primero reps, después peso.
 * Cada ciclo suma 1 rep por serie; al llegar a [repsTope] sube [salto] y
 * reinicia en [repsInicio]. Dentro del ciclo, cada semana suma 1 rep más.
 * [repsTope] null = sin tope (sólo reps); [unidad] null = peso corporal.
 */
data class AccesorioDef(
    val clave: String,
    val nombre: String,
    val unidad: String?,
    val pesoInicial: Double?,
    val salto: Double,
    val series: Int,
    val repsInicio: Int,
    val repsTope: Int?,
    val seriesDeload: Int,
    val repsDeload: Int,
    /** Cuántos saltos se resta a la carga en la semana de descarga. */
    val saltosDeload: Int,
    val regla: String,
    val indicacion: String,
)

sealed interface Bloque {
    /** Principal con la onda 5/3/1 (series registradas como MAIN). */
    data class Principal531(val principal: Principal) : Bloque
    data class Secundario(val principal: Principal, val variante: Variante) : Bloque
    data class Accesorio(val def: AccesorioDef) : Bloque
    data class Cardio(val descripcion: String) : Bloque
}

data class Dia(
    val titulo: String,
    val bloques: List<Bloque>,
) {
    /** El principal 5/3/1 del día, si lo tiene. */
    val principal531: Principal?
        get() = bloques.filterIsInstance<Bloque.Principal531>().firstOrNull()?.principal
}

object Program {

    const val DISTRIBUCION =
        "Lun dominada · Mar fondo · Mié cardio · Jue sentadilla · Vie ligero + cardio"

    const val PESO_BARRA_KG = 20.0
    const val PESO_BARRA_LBS = 45.0

    fun pesoBarra(unit: String): Double = if (unit == "kg") PESO_BARRA_KG else PESO_BARRA_LBS

    // ---- Principales ----
    val SENTADILLA = Principal("Sentadilla", "sentadilla", esLastrado = false, tren = Tren.LOWER)
    val FONDO = Principal("Fondo", "fondo", esLastrado = true, tren = Tren.UPPER)
    val DOMINADA = Principal("Dominada", "dominada", esLastrado = true, tren = Tren.UPPER)

    val PRINCIPALES: List<Principal> = listOf(SENTADILLA, FONDO, DOMINADA)

    // ---- Días secundarios (bloques "DÍA MEDIUM" / "DÍA 3" de cada hoja) ----
    val SENTADILLA_MEDIUM = Variante("medium", 0.70, 3, 5, 0.50, 2, 5, "Sin AMRAP")
    val FONDO_MEDIUM = Variante("medium", 0.75, 3, 5, 0.50, 2, 5, "Sin AMRAP")
    val FONDO_LIGERO = Variante("ligero", 0.65, 3, 8, 0.50, 2, 8, "RIR 3–4; ajusta asistencia")
    val DOMINADA_MEDIUM = Variante("medium", 0.80, 3, 5, 0.50, 2, 5, "Sin AMRAP")
    val DOMINADA_LIGERO = Variante("ligero", 0.76, 3, 5, 0.76, 2, 6, "RIR 3–4; ajusta asistencia")

    // ---- Accesorios (hojas Push / Pierna / Pull · Accesorios) ----
    val OHP = AccesorioDef(
        "ohp", "Press militar (OHP)", "kg", 45.0, 5.0, 3, 6, 10, 2, 5, 0,
        "Al completar 3×10: +5 kg, reiniciar a 3×6", "No subir de peso hasta completar 3×10",
    )
    val LATERALES = AccesorioDef(
        "laterales", "Elevaciones laterales", "kg/mano", 5.0, 2.0, 3, 12, 20, 2, 12, 0,
        "Al completar 3×20: +2 kg por mano, reiniciar a 3×12", "Técnica limpia, sin impulso",
    )
    val PUSH_UPS = AccesorioDef(
        "push_ups", "Push-ups", null, null, 0.0, 3, 12, null, 2, 10, 0,
        "Sumar repeticiones totales; reevaluar al final del ciclo",
        "Objetivo de reps; ajustar si cae la técnica",
    )
    val HACK = AccesorioDef(
        "hack", "Hack squat", "kg", 45.0, 5.0, 3, 8, 12, 2, 8, 2,
        "3×12 con 2–3 RIR → +5 kg, reiniciar 3×8", "Mantener 2–3 RIR, técnica estable",
    )
    val CURL_FEMORAL = AccesorioDef(
        "curl_femoral", "Curl femoral", "lb", 80.0, 10.0, 3, 10, 15, 2, 10, 1,
        "3×15 limpio → +10 lb; reiniciar 3×10", "Controlar fase excéntrica; 1–3 RIR",
    )
    val REMO = AccesorioDef(
        "remo", "Remo máquina", "kg", 65.0, 5.0, 3, 8, 12, 2, 8, 1,
        "3×12 completo → +5 kg y reiniciar a 3×8", "Objetivo 1–3 RIR; no forzar fallo",
    )
    val CURL_BICEPS = AccesorioDef(
        "curl_biceps", "Curl bíceps", "kg/mano", 14.0, 2.0, 3, 8, 12, 2, 8, 1,
        "3×12 completo → +2 kg/mano y reiniciar a 3×8", "Objetivo 1–3 RIR; no forzar fallo",
    )

    val ACCESORIOS: List<AccesorioDef> =
        listOf(OHP, LATERALES, PUSH_UPS, HACK, CURL_FEMORAL, REMO, CURL_BICEPS)

    val DIAS: List<Dia> = listOf(
        Dia(
            titulo = "Lunes · Dominada",
            bloques = listOf(
                Bloque.Principal531(DOMINADA),
                Bloque.Secundario(SENTADILLA, SENTADILLA_MEDIUM),
                Bloque.Accesorio(REMO),
                Bloque.Accesorio(CURL_BICEPS),
                Bloque.Accesorio(CURL_FEMORAL),
            ),
        ),
        Dia(
            titulo = "Martes · Fondo",
            bloques = listOf(
                Bloque.Principal531(FONDO),
                Bloque.Accesorio(OHP),
                Bloque.Accesorio(PUSH_UPS),
                Bloque.Accesorio(LATERALES),
            ),
        ),
        Dia(
            titulo = "Miércoles · Cardio",
            bloques = listOf(Bloque.Cardio("Zona 2 · 1 h")),
        ),
        Dia(
            titulo = "Jueves · Sentadilla",
            bloques = listOf(
                Bloque.Principal531(SENTADILLA),
                Bloque.Secundario(FONDO, FONDO_MEDIUM),
                Bloque.Secundario(DOMINADA, DOMINADA_MEDIUM),
                Bloque.Accesorio(HACK),
                Bloque.Accesorio(CURL_FEMORAL),
            ),
        ),
        Dia(
            titulo = "Viernes · Ligero + cardio",
            bloques = listOf(
                Bloque.Secundario(DOMINADA, DOMINADA_LIGERO),
                Bloque.Secundario(FONDO, FONDO_LIGERO),
                Bloque.Cardio("Zona 2"),
            ),
        ),
    )

    /** Índice del día donde se hace el 5/3/1 de [clave], o -1. */
    fun diaDel531(clave: String): Int = DIAS.indexOfFirst { it.principal531?.clave == clave }
}
