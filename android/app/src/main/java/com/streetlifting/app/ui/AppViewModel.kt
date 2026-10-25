package com.streetlifting.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streetlifting.app.core.BloqueCalc
import com.streetlifting.app.core.PrincipalCalc
import com.streetlifting.app.core.Program
import com.streetlifting.app.data.AccesorioEntity
import com.streetlifting.app.data.ConfigEntity
import com.streetlifting.app.data.LiftEntity
import com.streetlifting.app.data.Repository
import com.streetlifting.app.data.SessionWithLogs
import com.streetlifting.app.data.SetLogEntity
import kotlinx.coroutines.flow.SharingStarted
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
