package com.streetlifting.app.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Qué día y qué semana del plan tocan, según el calendario y lo ya registrado. */
object Calendario {

    /** Índice en [Program.DIAS] para un día de la semana (lunes = 0), o null el finde. */
    fun diaDelPlan(dow: DayOfWeek): Int? =
        (dow.value - 1).takeIf { it in Program.DIAS.indices }

    /** Próximo día de entrenamiento desde [dow] (el mismo día si toca entrenar). */
    fun proximoDia(dow: DayOfWeek): Int = diaDelPlan(dow) ?: 0

    /**
     * Semana del plan (1-based) a partir de las sesiones del mes en curso: la
     * última semana registrada, o la siguiente si su última sesión fue en una
     * semana de calendario anterior a [hoy]. Puede devolver `maxSemanas + 1`,
     * que indica que el mes terminó y toca avanzar el ciclo.
     *
     * @param sesiones pares (semana del plan, fecha en epoch ms) del mes en curso.
     */
    fun semanaActual(
        sesiones: List<Pair<Int, Long>>,
        hoy: LocalDate,
        zona: ZoneId,
        maxSemanas: Int,
    ): Int {
        if (sesiones.isEmpty()) return 1
        val ultima = sesiones.maxOf { it.first }
        val fecha = sesiones.filter { it.first == ultima }.maxOf { it.second }
        val lunesUltima = Instant.ofEpochMilli(fecha).atZone(zona).toLocalDate().lunes()
        return if (lunesUltima.isBefore(hoy.lunes())) minOf(ultima + 1, maxSemanas + 1) else ultima
    }

    private fun LocalDate.lunes(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
