package com.streetlifting.app.core

/**
 * Estructura fija del programa, portada de `DIAS` en `pdf_531.py`:
 * 4 días, cada uno con un principal (5/3/1) y sus auxiliares.
 */

enum class Tren { UPPER, LOWER }

/** clave: identificador estable usado como PK del max en la BD. */
data class Principal(
    val etiqueta: String,
    val clave: String,
    val esLastrado: Boolean,
    val tren: Tren,
)

data class Auxiliar(
    val nombre: String,
    val esquema: String,
    val rir: String,
)

data class Dia(
    val titulo: String,
    val principal: Principal,
    val auxiliares: List<Auxiliar>,
)

object Program {

    const val DISTRIBUCION =
        "Lun D1 · Mar D2 · Mié descanso · Jue D3 · Vie D4 · finde descanso"

    const val PESO_BARRA_KG = 20.0
    const val PESO_BARRA_LBS = 45.0

    fun pesoBarra(unit: String): Double = if (unit == "kg") PESO_BARRA_KG else PESO_BARRA_LBS

    val DIAS: List<Dia> = listOf(
        Dia(
            titulo = "Día 1 · Tracción + brazos",
            principal = Principal("Dominada", "dominada", esLastrado = true, tren = Tren.UPPER),
            auxiliares = listOf(
                Auxiliar("Fondo (volumen)", "4x8-12", "1-2"),
                Auxiliar("Press militar", "3x6-8", "2"),
                Auxiliar("Remo chest-supported", "3x10-12", "1-2"),
                Auxiliar("Elevación lateral", "3-4x12-20", "1, última 0"),
                Auxiliar("Curl supinado", "3x8-12", "0-1"),
            ),
        ),
        Dia(
            titulo = "Día 2 · Sentadilla",
            principal = Principal("Sentadilla", "sentadilla", esLastrado = false, tren = Tren.LOWER),
            auxiliares = listOf(
                Auxiliar("Prensa", "3x10-15", "1-2"),
                Auxiliar("Single-leg RDL", "3x8-10 / pierna", "2"),
                Auxiliar("Curl femoral", "3x10-15", "1, última 0"),
                Auxiliar("Gemelo", "3-4x10-15", "1, última 0"),
            ),
        ),
        Dia(
            titulo = "Día 3 · Empuje + brazos",
            principal = Principal("Fondo", "fondo", esLastrado = true, tren = Tren.UPPER),
            auxiliares = listOf(
                Auxiliar("Dominada (volumen)", "6x6 a peso corporal", "3-4"),
                Auxiliar("Press banca cerrado", "3x6-10", "1-2"),
                Auxiliar("Elevación lateral", "3-4x12-20", "1, última 0"),
                Auxiliar("Face pull", "3x15-20", "0-1"),
                Auxiliar("Curl martillo", "3x8-12", "0-1"),
            ),
        ),
        Dia(
            titulo = "Día 4 · Muerto",
            principal = Principal("Peso muerto", "peso_muerto", esLastrado = false, tren = Tren.LOWER),
            auxiliares = listOf(
                Auxiliar("Pistol / box squat", "3x6-8 / pierna", "2"),
                Auxiliar("Abductores", "3x15-20", "0-1"),
                Auxiliar("Curl femoral o hip thrust", "3x10-12", "1, última 0"),
                Auxiliar("Gemelo", "3-4x10-15", "1, última 0"),
            ),
        ),
    )
}
