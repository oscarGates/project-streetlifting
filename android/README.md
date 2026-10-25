# Streetlifting 531 — app Android

App nativa (Kotlin + Jetpack Compose + Room, **offline-first**) para seguir el
plan 5/3/1 y registrar cada sesión. El plan sigue la hoja
**"Plan 5-3-1 mensual por movimiento.xlsx"**: TM = 85 % del 1RM total (en
lastrados incluye el peso corporal), redondeo como `ROUND()` de Excel, lastre
negativo = asistencia, ciclo e incremento de TM **por movimiento** y accesorios
con progresión doble (primero reps, después peso).

## Qué hace

- **Plan** (`PlanScreen`): resumen del ciclo, Training Max de cada principal
  (este ciclo → siguiente), selector de semana (1-3 + descarga) y los 4 días.
- **Día / sesión** (`DayScreen`): muestra las 3 series prescritas del principal
  (con AMRAP resaltado) y los auxiliares; registras **peso y reps reales** de
  cada uno y se guarda en la base de datos local.
- **Historial** (`HistoryScreen`): gráfico de 1RM estimado (Epley) por principal
  y lista de sesiones registradas.
- **Ajustes** (`SettingsScreen`): edita 1RM, ciclo e incremento de cada
  principal, carga inicial / salto / ciclo de cada accesorio, peso corporal,
  microplacas, semana de descarga, peso de barra y mes actual.

Programa (hoja `schedule`):

| Día | Bloques |
|-----|---------|
| Lunes | **Dominada 5/3/1** · sentadilla medium (70 %) · remo máquina · curl bíceps · curl femoral |
| Martes | **Fondo 5/3/1** · press militar · push-ups · elevaciones laterales |
| Miércoles | Cardio zona 2 · 1 h |
| Jueves | **Sentadilla 5/3/1** · fondo medium (75 %) · dominada medium (80 %) · hack squat · curl femoral |
| Viernes | Dominada ligera (76 %) · fondo ligero (65 % 3×8) · cardio zona 2 |

## Estructura

```
app/src/main/java/com/streetlifting/app/
  core/   FiveThreeOne.kt, Program.kt, PlanCalculator.kt   (motor 5/3/1)
  data/   Room: entidades, DAO, base de datos, Repository
  ui/     Compose: Plan / Day / History / Settings + tema y gráfico
app/src/test/java/...  FiveThreeOneTest.kt  (valida el motor contra los valores de la hoja)
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
