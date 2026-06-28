package com.streetlifting.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Configuración global del ciclo (fila única, id=0). */
@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey val id: Int = 0,
    val pesoCorporal: Double,
    val pcUnit: String,
    val micro: Boolean,
    val incluirDescarga: Boolean,
    /** null = barra estándar (20 kg / 45 lbs). */
    val pesoBarraKg: Double?,
    /** Ciclo actual (1-based); cada ciclo sube el TM un incremento. */
    val ciclo: Int,
)

/** Un principal del programa (dominada, sentadilla, fondo, peso_muerto). */
@Entity(tableName = "lifts")
data class LiftEntity(
    @PrimaryKey val clave: String,
    val etiqueta: String,
    val rm: Double,
    val unit: String,
    val esBw: Boolean,
    val tren: String, // "upper" | "lower"
)

/** Una sesión registrada (un día concreto de una semana/ciclo). */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fechaEpoch: Long,
    val ciclo: Int,
    val semana: Int,   // 1..4
    val diaIndex: Int, // 0..3
    val notas: String = "",
)

/** Una fila registrada: serie del principal o auxiliar. */
@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId")],
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val tipo: String,        // "MAIN" | "AUX"
    val idx: Int,
    val label: String,
    val prescripcion: String,
    val pesoUsado: Double?,
    val reps: Int?,
    val nota: String = "",
)

data class SessionWithLogs(
    @Embedded val session: SessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val logs: List<SetLogEntity>,
)
