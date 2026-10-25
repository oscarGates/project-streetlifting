package com.streetlifting.app

import com.streetlifting.app.core.AccesorioEstado
import com.streetlifting.app.core.FiveThreeOne
import com.streetlifting.app.core.MaxInput
import com.streetlifting.app.core.PlanCalculator
import com.streetlifting.app.core.Program
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que el motor reproduce "Plan 5-3-1 mensual por movimiento.xlsx"
 * (TM = 85% del 1RM total, ROUND de Excel, lastre/asistencia, progresión doble).
 */
class FiveThreeOneTest {

    private val env = PlanCalculator.Entorno(pesoCorporal = 94.0, pcUnit = "kg", micro = false)

    private val sentadilla = MaxInput(rm = 130.0, unit = "kg", esBw = false, ciclo = 1, incremento = 5.0)
    private val fondo = MaxInput(rm = 177.5, unit = "kg", esBw = true, ciclo = 1, incremento = 2.5)
    private val dominada = MaxInput(rm = 142.5, unit = "kg", esBw = true, ciclo = 2, incremento = 2.5)

    private fun cargas(max: MaxInput): List<List<Double>> =
        PlanCalculator.calcularPrincipal(max, env, incluirDescarga = true)
            .semanas.map { w -> w.series.map { it.objetivo - if (max.esBw) 94.0 else 0.0 } }

    @Test
    fun redondeoComoExcel() {
        // ROUND de Excel: la mitad se aleja de cero.
        assertEquals(3.0, FiveThreeOne.roundExcel(2.5), 0.0)
        assertEquals(-3.0, FiveThreeOne.roundExcel(-2.5), 0.0)
        assertEquals(72.5, FiveThreeOne.redondear(71.825, "kg"), 0.0001)
    }

    @Test
    fun conversionRoundTrip() {
        val kg = 100.0
        val lbs = FiveThreeOne.convertir(kg, "kg", "lbs")
        assertEquals(220.462, lbs, 0.001)
        assertEquals(kg, FiveThreeOne.convertir(lbs, "lbs", "kg"), 0.0001)
    }

    @Test
    fun sentadillaHoja() {
        assertEquals(110.5, PlanCalculator.tm(sentadilla), 0.0001)
        assertEquals(
            listOf(
                listOf(72.5, 82.5, 95.0),
                listOf(77.5, 87.5, 100.0),
                listOf(82.5, 95.0, 105.0),
                listOf(45.0, 55.0, 67.5),
            ),
            cargas(sentadilla),
        )
    }

    @Test
    fun fondoHoja() {
        assertEquals(150.875, PlanCalculator.tm(fondo), 0.0001)
        assertEquals(
            listOf(
                listOf(5.0, 20.0, 35.0),
                listOf(12.5, 27.5, 42.5),
                listOf(20.0, 35.0, 50.0),
                listOf(-32.5, -17.5, -2.5),
            ),
            cargas(fondo),
        )
        val s1 = PlanCalculator.calcularPrincipal(fondo, env, true).semanas[0].series
        assertEquals("+5 kg", s1[0].pesoTexto)
        assertTrue(s1[2].amrap)
        val deload = PlanCalculator.calcularPrincipal(fondo, env, true).semanas[3].series
        assertEquals("asistencia 32.5 kg", deload[0].pesoTexto)
    }

    @Test
    fun dominadaCiclo2Hoja() {
        assertEquals(123.625, PlanCalculator.tm(dominada), 0.0001)
        assertEquals(
            listOf(
                listOf(-12.5, -2.5, 10.0),
                listOf(-7.5, 5.0, 17.5),
                listOf(-2.5, 10.0, 22.5),
                listOf(-45.0, -32.5, -20.0),
            ),
            cargas(dominada),
        )
    }

    @Test
    fun deloadExcluidoSiNoSeIncluye() {
        val calc = PlanCalculator.calcularPrincipal(sentadilla, env, incluirDescarga = false)
        assertEquals(3, calc.semanas.size)
        assertTrue(calc.semanas.none { it.deload })
    }

    @Test
    fun cicloSubeTm() {
        val c2 = PlanCalculator.tm(sentadilla.copy(ciclo = 2))
        assertEquals(5.0, c2 - PlanCalculator.tm(sentadilla), 0.0001)
    }

    @Test
    fun juevesSecundarios() {
        val lifts = mapOf("sentadilla" to sentadilla, "fondo" to fondo, "dominada" to dominada)
        val jueves = Program.DIAS[3]
        val sem1 = PlanCalculator.calcularDia(jueves, 1, lifts, emptyMap(), env, true)
        // Fondo medium 75% → +20; dominada medium 80% → +5 (total = BW + lastre).
        assertEquals(114.0, sem1[1].pesoSugerido!!, 0.0001)
        assertEquals(99.0, sem1[2].pesoSugerido!!, 0.0001)
        val deload = PlanCalculator.calcularDia(jueves, 4, lifts, emptyMap(), env, true)
        assertEquals(76.5, deload[1].pesoSugerido!!, 0.0001) // fondo 50% → −17.5
    }

    @Test
    fun accesoriosCiclo1Hoja() {
        fun acc(def: com.streetlifting.app.core.AccesorioDef, semana: Int, ciclo: Int = 1) =
            PlanCalculator.calcularAccesorio(def, AccesorioEstado(def.pesoInicial, def.salto, ciclo), semana)

        assertEquals(listOf(6, 7, 8), (1..3).map { acc(Program.OHP, it).reps })
        assertEquals(45.0, acc(Program.OHP, 4).peso!!, 0.0)
        assertEquals(5, acc(Program.OHP, 4).reps)
        assertEquals(35.0, acc(Program.HACK, 4).peso!!, 0.0)
        assertEquals(70.0, acc(Program.CURL_FEMORAL, 4).peso!!, 0.0)
        assertEquals(60.0, acc(Program.REMO, 4).peso!!, 0.0)
        assertEquals(12.0, acc(Program.CURL_BICEPS, 4).peso!!, 0.0)
        assertEquals(listOf(12, 13, 14), (1..3).map { acc(Program.PUSH_UPS, it).reps })
        assertEquals(null, acc(Program.PUSH_UPS, 1).peso)
    }

    @Test
    fun accesoriosProgresionDoble() {
        val ohp = { ciclo: Int ->
            PlanCalculator.calcularAccesorio(Program.OHP, AccesorioEstado(45.0, 5.0, ciclo), 1)
        }
        assertEquals(7, ohp(2).reps)       // vista previa del ciclo 2 en la hoja
        assertEquals(10, ohp(5).reps)
        assertEquals(50.0, ohp(6).peso!!, 0.0) // tras 3×10: +5 kg y reinicia a 6
        assertEquals(6, ohp(6).reps)
    }

    @Test
    fun epley() {
        assertEquals(120.0, PlanCalculator.epley1rm(100.0, 6), 0.0001)
    }
}
