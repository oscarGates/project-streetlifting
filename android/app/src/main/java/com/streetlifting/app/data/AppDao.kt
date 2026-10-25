package com.streetlifting.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ---- Config ----
    @Query("SELECT * FROM config WHERE id = 0")
    fun configFlow(): Flow<ConfigEntity?>

    @Query("SELECT * FROM config WHERE id = 0")
    suspend fun configOnce(): ConfigEntity?

    @Upsert
    suspend fun upsertConfig(config: ConfigEntity)

    // ---- Lifts ----
    @Query("SELECT * FROM lifts")
    fun liftsFlow(): Flow<List<LiftEntity>>

    @Query("SELECT * FROM lifts")
    suspend fun liftsOnce(): List<LiftEntity>

    @Query("SELECT * FROM lifts WHERE clave = :clave")
    suspend fun lift(clave: String): LiftEntity?

    @Upsert
    suspend fun upsertLift(lift: LiftEntity)

    @Upsert
    suspend fun upsertLifts(lifts: List<LiftEntity>)

    // ---- Accesorios ----
    @Query("SELECT * FROM accesorios")
    fun accesoriosFlow(): Flow<List<AccesorioEntity>>

    @Query("SELECT * FROM accesorios")
    suspend fun accesoriosOnce(): List<AccesorioEntity>

    @Upsert
    suspend fun upsertAccesorios(accesorios: List<AccesorioEntity>)

    // ---- Avance de ciclo ----
    @Query("UPDATE lifts SET ciclo = ciclo + 1")
    suspend fun avanzarCicloLifts()

    @Query("UPDATE accesorios SET ciclo = ciclo + 1")
    suspend fun avanzarCicloAccesorios()

    @Query("UPDATE config SET ciclo = ciclo + 1 WHERE id = 0")
    suspend fun avanzarMes()

    /**
     * Pasa al siguiente ciclo: cada principal suma su incremento al TM, cada
     * accesorio avanza su progresión doble y empieza un mes nuevo. Las sesiones
     * registradas no se tocan (quedan en el mes anterior).
     */
    @Transaction
    suspend fun avanzarCiclo() {
        avanzarCicloLifts()
        avanzarCicloAccesorios()
        avanzarMes()
    }

    // ---- Sessions ----
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertSetLogs(logs: List<SetLogEntity>)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query(
        "SELECT * FROM sessions WHERE ciclo = :ciclo AND semana = :semana " +
            "AND diaIndex = :diaIndex LIMIT 1"
    )
    suspend fun sessionFor(ciclo: Int, semana: Int, diaIndex: Int): SessionEntity?

    /**
     * Reemplaza la sesión de (ciclo, semana, día) con sus registros, de forma
     * atómica. Conserva la fecha en que empezó la sesión (se guarda en cada cambio).
     */
    @Transaction
    suspend fun replaceSession(
        ciclo: Int,
        semana: Int,
        diaIndex: Int,
        notas: String,
        logs: List<SetLogEntity>,
        ahora: Long,
    ) {
        val previa = sessionFor(ciclo, semana, diaIndex)
        previa?.let { deleteSession(it) }
        val id = insertSession(
            SessionEntity(
                fechaEpoch = previa?.fechaEpoch ?: ahora,
                ciclo = ciclo,
                semana = semana,
                diaIndex = diaIndex,
                notas = notas,
            )
        )
        insertSetLogs(logs.map { it.copy(id = 0, sessionId = id) })
    }

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY bloque ASC, idx ASC")
    suspend fun setLogsFor(sessionId: Long): List<SetLogEntity>

    /** Sesiones del mismo día del plan, salvo la de (ciclo, semana); la más reciente primero. */
    @Transaction
    @Query(
        "SELECT * FROM sessions WHERE diaIndex = :diaIndex " +
            "AND NOT (ciclo = :ciclo AND semana = :semana) ORDER BY fechaEpoch DESC"
    )
    suspend fun otrasSesionesDelDia(diaIndex: Int, ciclo: Int, semana: Int): List<SessionWithLogs>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY fechaEpoch DESC")
    fun sessionsWithLogsFlow(): Flow<List<SessionWithLogs>>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY fechaEpoch ASC")
    suspend fun allSessionsWithLogs(): List<SessionWithLogs>
}
