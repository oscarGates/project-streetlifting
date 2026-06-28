package com.streetlifting.app

import com.streetlifting.app.core.FiveThreeOne
import com.streetlifting.app.core.MaxInput
import com.streetlifting.app.core.PlanCalculator
import com.streetlifting.app.core.Tren
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que el port Kotlin reproduce el motor 5/3/1 de `ciclo_531.py` /
 * `pdf_531.py` (TM = 90% del 1RM, onda de %, redondeo a par, lastre/por-lado).
 */
class FiveThreeOneTest {

    @Test
    fun redondeoMatchesPythonBankers() {
        // round(32.5) == 32 en Python (half-to-even) -> 160 lbs.
        assertEquals(160.0, FiveThreeOne.redondear(162.5, "lbs"), 0.0001)
        assertEquals(157.5, FiveThreeOne.redondear(157.5, "kg"), 0.0001)
        assertEquals(102.5, FiveThreeOne.redondear(102.375, "kg"), 0.0001)
    }

    @Test
    fun conversionRoundTrip() {
        val kg = 100.0
        val lbs = FiveThreeOne.convertir(kg, "kg", "lbs")
        assertEquals(220.462, lbs, 0.001)
        assertEquals(kg, FiveThreeOne.convertir(lbs, "lbs", "kg"), 0.0001)
    }

    @Test
    fun lastradoFondo() {
        // Fondo: 1RM lastre 80 kg, BW 95 kg -> TM = (95+80)*0.9 = 157.5
        val calc = PlanCalculator.calcularPrincipal(
            max = MaxInput(rm = 80.0, unit = "kg", esBw = true, tren = Tren.UPPER),
            pesoCorporal = 95.0, pcUnit = "kg",
            micro = false, incluirDescarga = true,
        )
        assertEquals(157.5, calc.tm, 0.0001)
        val s1 = calc.semanas[0].series // semana 1: 65/75/85
        assertEquals("+7.5 kg", s1[0].pesoTexto)
        assertEquals("+22.5 kg", s1[1].pesoTexto)
        assertEquals("+40 kg", s1[2].pesoTexto)
        assertTrue(s1[2].amrap) // la 3a serie de la semana 1 es AMRAP
    }

    @Test
    fun barraSentadilla() {
        // Sentadilla: 1RM 280 lbs -> TM = 252 (display 250), barra 45 lbs.
        val calc = PlanCalculator.calcularPrincipal(
            max = MaxInput(rm = 280.0, unit = "lbs", esBw = false, tren = Tren.LOWER),
            pesoCorporal = 95.0, pcUnit = "kg",
            micro = false, incluirDescarga = true,
        )
        assertEquals(250.0, FiveThreeOne.redondear(calc.tm, "lbs"), 0.0001)
        // El objetivo usa el TM real (252), no el redondeado: 252*0.65 = 163.8 -> 165.
        val s1 = calc.semanas[0].series[0]
        assertEquals(165.0, s1.objetivo, 0.0001)
        assertTrue(s1.pesoTexto.contains("60/lado"))
    }

    @Test
    fun deloadExcluidoSiNoSeIncluye() {
        val calc = PlanCalculator.calcularPrincipal(
            max = MaxInput(rm = 280.0, unit = "lbs", esBw = false, tren = Tren.LOWER),
            pesoCorporal = 95.0, pcUnit = "kg",
            micro = false, incluirDescarga = false,
        )
        assertEquals(3, calc.semanas.size)
        assertTrue(calc.semanas.none { it.deload })
    }

    @Test
    fun cicloSubeTm() {
        val base = PlanCalculator.calcularPrincipal(
            MaxInput(280.0, "lbs", false, Tren.LOWER), 95.0, "kg", false, true, ciclo = 1
        )
        val c2 = PlanCalculator.calcularPrincipal(
            MaxInput(280.0, "lbs", false, Tren.LOWER), 95.0, "kg", false, true, ciclo = 2
        )
        // tren inferior sube 5 kg -> 11.0231 lbs
        assertEquals(FiveThreeOne.convertir(5.0, "kg", "lbs"), c2.tm - base.tm, 0.001)
    }

    @Test
    fun epley() {
        assertEquals(120.0, PlanCalculator.epley1rm(100.0, 6), 0.0001)
    }
}
