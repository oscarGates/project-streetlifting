package com.streetlifting.app.data

import android.content.Context
import com.streetlifting.app.core.MaxInput
import com.streetlifting.app.core.PlanCalculator
import com.streetlifting.app.core.PrincipalCalc
import com.streetlifting.app.core.Program
import com.streetlifting.app.core.Tren
import kotlinx.coroutines.flow.Flow

/** Punto único de acceso a datos + cálculo del plan. */
class Repository(private val dao: AppDao) {

    val configFlow: Flow<ConfigEntity?> = dao.configFlow()
    val liftsFlow: Flow<List<LiftEntity>> = dao.liftsFlow()
    val sessionsFlow: Flow<List<SessionWithLogs>> = dao.sessionsWithLogsFlow()

    /** Crea config y lifts por defecto la primera vez (basados en el perfil). */
    suspend fun seedIfEmpty() {
        if (dao.configOnce() == null) {
            dao.upsertConfig(
                ConfigEntity(
                    id = 0,
                    pesoCorporal = 95.0,
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
                    LiftEntity("dominada", "Dominada", 45.0, "kg", esBw = true, tren = "upper"),
                    LiftEntity("sentadilla", "Sentadilla", 280.0, "lbs", esBw = false, tren = "lower"),
                    LiftEntity("fondo", "Fondo", 80.0, "kg", esBw = true, tren = "upper"),
                    LiftEntity("peso_muerto", "Peso muerto", 75.0, "kg", esBw = false, tren = "lower"),
                )
            )
        }
    }

    suspend fun config(): ConfigEntity = dao.configOnce()!!
    suspend fun lifts(): List<LiftEntity> = dao.liftsOnce()

    suspend fun upsertConfig(config: ConfigEntity) = dao.upsertConfig(config)
    suspend fun upsertLift(lift: LiftEntity) = dao.upsertLift(lift)

    /** Calcula un principal por su clave, usando config + lift guardados. */
    suspend fun calcularPrincipal(clave: String, config: ConfigEntity): PrincipalCalc? {
        val lift = dao.lift(clave) ?: return null
        return PlanCalculator.calcularPrincipal(
            max = MaxInput(
                rm = lift.rm,
                unit = lift.unit,
                esBw = lift.esBw,
                tren = if (lift.tren == "upper") Tren.UPPER else Tren.LOWER,
            ),
            pesoCorporal = config.pesoCorporal,
            pcUnit = config.pcUnit,
            micro = config.micro,
            incluirDescarga = config.incluirDescarga,
            pesoBarra = config.pesoBarraKg,
            ciclo = config.ciclo,
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
            Program.DIAS.getOrNull(diaIndex)?.titulo ?: "Día ${diaIndex + 1}"
    }
}
