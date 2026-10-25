package com.streetlifting.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streetlifting.app.core.BloqueCalc
import com.streetlifting.app.core.Calendario
import com.streetlifting.app.core.PlanCalculator
import com.streetlifting.app.core.PrincipalCalc
import com.streetlifting.app.core.Program
import com.streetlifting.app.data.AccesorioEntity
import com.streetlifting.app.data.ConfigEntity
import com.streetlifting.app.data.LiftEntity
import com.streetlifting.app.data.Repository
import com.streetlifting.app.data.SessionWithLogs
import com.streetlifting.app.data.SetLogEntity
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.from(app)

    init {
        viewModelScope.launch { repo.seedIfEmpty() }
    }

    val config: StateFlow<ConfigEntity?> =
        repo.configFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lifts: StateFlow<List<LiftEntity>> =
        repo.liftsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accesorios: StateFlow<List<AccesorioEntity>> =
        repo.accesoriosFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<SessionWithLogs>> =
        repo.sessionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Semana del plan que toca hoy (puede ser `maxSemanas + 1`: mes terminado).
     * Se recalcula con cada sesión guardada o cambio de mes.
     */
    val semanaActual: StateFlow<Int> =
        combine(repo.configFlow, repo.sessionsFlow) { cfg, sesiones ->
            if (cfg == null) return@combine 1
            Calendario.semanaActual(
                sesiones.filter { it.session.ciclo == cfg.ciclo }
                    .map { it.session.semana to it.session.fechaEpoch },
                LocalDate.now(),
                ZoneId.systemDefault(),
                if (cfg.incluirDescarga) 4 else 3,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    /** Última sesión del mismo día del plan, distinta de la de (ciclo, semana). */
    suspend fun sesionAnterior(diaIndex: Int, ciclo: Int, semana: Int): SessionWithLogs? =
        repo.otrasSesionesDelDia(diaIndex, ciclo, semana).firstOrNull()

    /** Mejor 1RM estimado (Epley) en series 5/3/1 hechas de otras sesiones del día. */
    suspend fun mejor1rm(diaIndex: Int, ciclo: Int, semana: Int): Double? =
        repo.otrasSesionesDelDia(diaIndex, ciclo, semana)
            .flatMap { it.logs }
            .filter { it.tipo == "MAIN" && it.hecho && it.pesoUsado != null && (it.reps ?: 0) > 0 }
            .maxOfOrNull { PlanCalculator.epley1rm(it.pesoUsado!!, it.reps!!) }

    /** Calcula los principales para la config actual. */
    suspend fun calcularTodos(config: ConfigEntity): Map<String, PrincipalCalc> {
        val out = LinkedHashMap<String, PrincipalCalc>()
        for (p in Program.PRINCIPALES) {
            repo.calcularPrincipal(p.clave, config)?.let { out[p.clave] = it }
        }
        return out
    }

    suspend fun calcularDia(diaIndex: Int, semana: Int, config: ConfigEntity): List<BloqueCalc> =
        repo.calcularDia(diaIndex, semana, config)

    suspend fun sessionFor(ciclo: Int, semana: Int, diaIndex: Int) =
        repo.sessionFor(ciclo, semana, diaIndex)

    suspend fun setLogsFor(sessionId: Long): List<SetLogEntity> = repo.setLogsFor(sessionId)

    fun saveSession(
        ciclo: Int,
        semana: Int,
        diaIndex: Int,
        notas: String,
        logs: List<SetLogEntity>,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            repo.saveSession(ciclo, semana, diaIndex, notas, logs)
            onDone()
        }
    }

    fun updateConfig(config: ConfigEntity) {
        viewModelScope.launch { repo.upsertConfig(config) }
    }

    fun updateLift(lift: LiftEntity) {
        viewModelScope.launch { repo.upsertLift(lift) }
    }

    fun avanzarCiclo() {
        viewModelScope.launch { repo.avanzarCiclo() }
    }

    fun updateAccesorios(accesorios: List<AccesorioEntity>) {
        viewModelScope.launch { repo.upsertAccesorios(accesorios) }
    }
}
