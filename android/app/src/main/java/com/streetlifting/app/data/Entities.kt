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
    /** Mes / bloque de 4 semanas en curso (agrupa las sesiones registradas). */
    val ciclo: Int,
)

/** Un principal del programa (sentadilla, fondo, dominada). */
@Entity(tableName = "lifts")
data class LiftEntity(
    @PrimaryKey val clave: String,
    val etiqueta: String,
    /** 1RM total: en lastrados incluye peso corporal + lastre. */
    val rm: Double,
    val unit: String,
    val esBw: Boolean,
    val tren: String, // "upper" | "lower"
    /** Ciclo del movimiento (1-based); cada ciclo suma [incremento] al TM. */
    val ciclo: Int = 1,
    /** Incremento de TM por ciclo, en la unidad del ejercicio. */
    val incremento: Double,
)

/** Estado editable de un accesorio; el resto de su definición vive en `Program`. */
@Entity(tableName = "accesorios")
data class AccesorioEntity(
    @PrimaryKey val clave: String,
    val pesoInicial: Double?,
    val salto: Double,
    val ciclo: Int,
)

/** Una sesión registrada (un día concreto de una semana/ciclo). */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fechaEpoch: Long,
    val ciclo: Int,
    val semana: Int,   // 1..4
    val diaIndex: Int, // índice en Program.DIAS; -1 = día del plan anterior
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
    val tipo: String,        // "MAIN" (series 5/3/1) | "AUX" (resto de bloques)
    /** Índice del bloque en el día (-1 = registro antiguo sin bloque). */
    val bloque: Int = 0,
    /** Índice de la serie dentro del bloque. */
    val idx: Int,
    val label: String,
    val prescripcion: String,
    val pesoUsado: Double?,
    val reps: Int?,
    val nota: String = "",
    /** Serie marcada como hecha (✓). */
    val hecho: Boolean = false,
)

data class SessionWithLogs(
    @Embedded val session: SessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val logs: List<SetLogEntity>,
)
