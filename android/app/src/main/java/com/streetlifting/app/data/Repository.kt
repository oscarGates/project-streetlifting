package com.streetlifting.app.data

import android.content.Context
import com.streetlifting.app.core.AccesorioEstado
import com.streetlifting.app.core.BloqueCalc
import com.streetlifting.app.core.MaxInput
import com.streetlifting.app.core.PlanCalculator
import com.streetlifting.app.core.PrincipalCalc
import com.streetlifting.app.core.Program
import kotlinx.coroutines.flow.Flow

/** Punto único de acceso a datos + cálculo del plan. */
class Repository(private val dao: AppDao) {

    val configFlow: Flow<ConfigEntity?> = dao.configFlow()
    val liftsFlow: Flow<List<LiftEntity>> = dao.liftsFlow()
    val accesoriosFlow: Flow<List<AccesorioEntity>> = dao.accesoriosFlow()
    val sessionsFlow: Flow<List<SessionWithLogs>> = dao.sessionsWithLogsFlow()

    /** Crea config, lifts y accesorios por defecto (valores de la hoja del plan). */
    suspend fun seedIfEmpty() {
        if (dao.configOnce() == null) {
            dao.upsertConfig(
                ConfigEntity(
                    id = 0,
                    pesoCorporal = 94.0,
                    pcUnit = "kg",
                    micro = false,
                    incluirDescarga = true,
                    pesoBarraKg = null,
                    ciclo = 1,
                )
            )
        }
        if (dao.liftsOnce().isEmpty()) {
            dao.upsertLifts(
                listOf(
                    LiftEntity("sentadilla", "Sentadilla", 130.0, "kg", esBw = false,
                        tren = "lower", ciclo = 1, incremento = 5.0),
                    LiftEntity("fondo", "Fondo", 177.5, "kg", esBw = true,
                        tren = "upper", ciclo = 1, incremento = 2.5),
                    LiftEntity("dominada", "Dominada", 142.5, "kg", esBw = true,
                        tren = "upper", ciclo = 2, incremento = 2.5),
                )
            )
        }
        val existentes = dao.accesoriosOnce().map { it.clave }.toSet()
        val faltan = Program.ACCESORIOS.filter { it.clave !in existentes }
        if (faltan.isNotEmpty()) {
            dao.upsertAccesorios(faltan.map { AccesorioEntity(it.clave, it.pesoInicial, it.salto, 1) })
        }
    }

    suspend fun config(): ConfigEntity = dao.configOnce()!!
    suspend fun lifts(): List<LiftEntity> = dao.liftsOnce()

    suspend fun upsertConfig(config: ConfigEntity) = dao.upsertConfig(config)
    suspend fun upsertLift(lift: LiftEntity) = dao.upsertLift(lift)
    suspend fun upsertAccesorios(accesorios: List<AccesorioEntity>) = dao.upsertAccesorios(accesorios)
    suspend fun avanzarCiclo() = dao.avanzarCiclo()

    /** Calcula un principal por su clave, usando config + lift guardados. */
    suspend fun calcularPrincipal(clave: String, config: ConfigEntity): PrincipalCalc? {
        val lift = dao.lift(clave) ?: return null
        return PlanCalculator.calcularPrincipal(lift.toMaxInput(), config.entorno(), config.incluirDescarga)
    }

    /** Calcula los bloques de un día para una semana. */
    suspend fun calcularDia(diaIndex: Int, semana: Int, config: ConfigEntity): List<BloqueCalc> {
        val dia = Program.DIAS.getOrNull(diaIndex) ?: return emptyList()
        val lifts = dao.liftsOnce().associate { it.clave to it.toMaxInput() }
        val accesorios = dao.accesoriosOnce().associate {
            it.clave to AccesorioEstado(it.pesoInicial, it.salto, it.ciclo)
        }
        return PlanCalculator.calcularDia(
            dia, semana, lifts, accesorios, config.entorno(), config.incluirDescarga,
        )
    }

    suspend fun sessionFor(ciclo: Int, semana: Int, diaIndex: Int): SessionEntity? =
        dao.sessionFor(ciclo, semana, diaIndex)

    suspend fun setLogsFor(sessionId: Long): List<SetLogEntity> = dao.setLogsFor(sessionId)

    /** Guarda (reemplazando si ya existía) una sesión con sus registros. */
    suspend fun saveSession(
        ciclo: Int,
        semana: Int,
        diaIndex: Int,
        notas: String,
        logs: List<SetLogEntity>,
    ) {
        dao.sessionFor(ciclo, semana, diaIndex)?.let { dao.deleteSession(it) }
        val id = dao.insertSession(
            SessionEntity(
                fechaEpoch = System.currentTimeMillis(),
                ciclo = ciclo,
                semana = semana,
                diaIndex = diaIndex,
                notas = notas,
            )
        )
        dao.insertSetLogs(logs.map { it.copy(id = 0, sessionId = id) })
    }

    suspend fun deleteSession(session: SessionEntity) = dao.deleteSession(session)

    companion object {
        fun from(context: Context): Repository =
            Repository(AppDatabase.get(context).dao())

        /** Etiqueta legible del día por índice. */
        fun tituloDia(diaIndex: Int): String =
            Program.DIAS.getOrNull(diaIndex)?.titulo ?: "Plan anterior"
    }
}

private fun LiftEntity.toMaxInput() =
    MaxInput(rm = rm, unit = unit, esBw = esBw, ciclo = ciclo, incremento = incremento)

private fun ConfigEntity.entorno() =
    PlanCalculator.Entorno(pesoCorporal, pcUnit, micro, pesoBarraKg)
