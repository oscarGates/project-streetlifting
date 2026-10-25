package com.streetlifting.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConfigEntity::class, LiftEntity::class, AccesorioEntity::class,
        SessionEntity::class, SetLogEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "streetlifting531.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }

        /**
         * v2: plan "5-3-1 mensual por movimiento". El 1RM pasa a ser total y el
         * TM al 85%, así que los lifts se vuelven a sembrar con los valores de la
         * hoja. Las sesiones se conservan, reubicando el día de cada principal.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lifts ADD COLUMN ciclo INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE lifts ADD COLUMN incremento REAL NOT NULL DEFAULT 2.5")
                db.execSQL("DELETE FROM lifts")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `accesorios` (`clave` TEXT NOT NULL, " +
                        "`pesoInicial` REAL, `salto` REAL NOT NULL, `ciclo` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`clave`))"
                )
                db.execSQL("UPDATE config SET pesoCorporal = 94.0, pcUnit = 'kg'")
                // v1: 0 dominada, 1 sentadilla, 2 fondo, 3 peso muerto (ya no existe).
                db.execSQL(
                    "UPDATE sessions SET diaIndex = CASE diaIndex " +
                        "WHEN 1 THEN 3 WHEN 2 THEN 1 WHEN 3 THEN -1 ELSE diaIndex END"
                )
            }
        }

        /** v3: registro por serie con ✓ (`hecho`) y bloque del día (`bloque`). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE set_logs ADD COLUMN bloque INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN hecho INTEGER NOT NULL DEFAULT 0")
                // El 5/3/1 siempre es el primer bloque; los auxiliares antiguos
                // eran una fila de texto por bloque y no se pueden reubicar.
                db.execSQL("UPDATE set_logs SET bloque = -1 WHERE tipo = 'AUX'")
                db.execSQL("UPDATE set_logs SET hecho = 1 WHERE tipo = 'MAIN' AND reps IS NOT NULL")
            }
        }
    }
}
