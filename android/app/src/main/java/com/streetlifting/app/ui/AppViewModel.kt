package com.streetlifting.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streetlifting.app.core.PrincipalCalc
import com.streetlifting.app.core.Program
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

    val sessions: StateFlow<List<SessionWithLogs>> =
        repo.sessionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Calcula los 4 principales para la config actual. */
    suspend fun calcularTodos(config: ConfigEntity): Map<String, PrincipalCalc> {
        val out = LinkedHashMap<String, PrincipalCalc>()
        for (dia in Program.DIAS) {
            repo.calcularPrincipal(dia.principal.clave, config)?.let {
                out[dia.principal.clave] = it
            }
        }
        return out
    }

    suspend fun calcular(clave: String, config: ConfigEntity): PrincipalCalc? =
        repo.calcularPrincipal(clave, config)

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
}
