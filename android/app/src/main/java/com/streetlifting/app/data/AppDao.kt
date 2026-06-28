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

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY tipo DESC, idx ASC")
    suspend fun setLogsFor(sessionId: Long): List<SetLogEntity>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY fechaEpoch DESC")
    fun sessionsWithLogsFlow(): Flow<List<SessionWithLogs>>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY fechaEpoch ASC")
    suspend fun allSessionsWithLogs(): List<SessionWithLogs>
}
