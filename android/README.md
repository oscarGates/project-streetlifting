# Streetlifting 531 — app Android

App nativa (Kotlin + Jetpack Compose + Room, **offline-first**) para seguir el
plan 5/3/1 y registrar cada sesión. La lógica del plan es un **port directo** de
`pdf_531.py` / `ciclo_531.py` (mismo TM = 90 % del 1RM, misma onda de
porcentajes, mismo redondeo a par y mismo cálculo de lastre / por-lado).

## Qué hace

- **Plan** (`PlanScreen`): resumen del ciclo, Training Max de cada principal
  (este ciclo → siguiente), selector de semana (1-3 + descarga) y los 4 días.
- **Día / sesión** (`DayScreen`): muestra las 3 series prescritas del principal
  (con AMRAP resaltado) y los auxiliares; registras **peso y reps reales** de
  cada uno y se guarda en la base de datos local.
- **Historial** (`HistoryScreen`): gráfico de 1RM estimado (Epley) por principal
  y lista de sesiones registradas.
- **Ajustes** (`SettingsScreen`): edita los maxes, peso corporal, microplacas,
  semana de descarga, peso de barra y número de ciclo.

Programa fijo (de `pdf_531.py`):

| Día | Principal | Tren |
|-----|-----------|------|
| 1 · Tracción + brazos | Dominada (lastrado) | superior |
| 2 · Sentadilla | Sentadilla | inferior |
| 3 · Empuje + brazos | Fondo (lastrado) | superior |
| 4 · Muerto | Peso muerto | inferior |

## Estructura

```
app/src/main/java/com/streetlifting/app/
  core/   FiveThreeOne.kt, Program.kt, PlanCalculator.kt   (motor 5/3/1)
  data/   Room: entidades, DAO, base de datos, Repository
  ui/     Compose: Plan / Day / History / Settings + tema y gráfico
app/src/test/java/...  FiveThreeOneTest.kt  (valida el port contra el motor Python)
```

## Cómo abrir y ejecutar

1. Abrir la carpeta `android/` en **Android Studio** (sincroniza Gradle solo).
2. Ejecutar en un emulador o dispositivo (▶).

### Por línea de comandos

```bash
cd android
./gradlew test            # tests del motor 5/3/1
./gradlew assembleDebug   # genera app/build/outputs/apk/debug/app-debug.apk
```

Requiere JDK 17+ y el SDK de Android (compileSdk 35). `local.properties` con
`sdk.dir` se genera localmente y no se versiona.

## Pendiente (fase posterior)

- Sincronización a Google Drive/Sheets (requiere OAuth).
- Exportar sesiones a CSV.
