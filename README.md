# Streetlifting 5/3/1

Herramientas para programar y seguir un plan **5/3/1** de streetlifting
(sentadilla, fondo lastrado y dominada lastrada) con accesorios y cardio.

El repo tiene dos partes:

| Carpeta / archivo | Qué es | Plan que usa |
|---|---|---|
| [`android/`](android/README.md) | App Android (Kotlin + Compose + Room) para ver el plan y registrar sesiones | **Plan 5-3-1 mensual por movimiento** (actual) |
| `app_531.py`, `ciclo_531.py`, `pdf_531.py` | Calculadora de escritorio en Python (GUI, CLI, Excel y PDF) | Plan anterior (TM 90 %, 4 días) |

## App Android

Es la herramienta principal. Funciona sin conexión y guarda todo en el teléfono.

- **Plan:** Training Max de cada principal, selector de semana (1–3 y descarga)
  y los 5 días con sus cargas ya calculadas.
- **Día:** prescripción de cada bloque y registro de peso y reps reales.
- **Historial:** sesiones por mes y gráfico de 1RM estimado (Epley).
- **Ajustes:** 1RM, ciclo e incremento por principal; carga inicial, salto y
  ciclo por accesorio; peso corporal, microplacas, barra y descarga.
- **Avanzar al siguiente ciclo:** sube el TM de los principales y la
  progresión de los accesorios y empieza un mes nuevo sin borrar el historial.

### El plan

| Día | Bloques |
|---|---|
| Lunes | **Dominada 5/3/1** · sentadilla medium · remo máquina · curl bíceps · curl femoral |
| Martes | **Fondo 5/3/1** · press militar · push-ups · elevaciones laterales |
| Miércoles | Cardio zona 2 · 1 h |
| Jueves | **Sentadilla 5/3/1** · fondo medium · dominada medium · hack squat · curl femoral |
| Viernes | Dominada ligera · fondo ligero · cardio zona 2 |

Reglas de cálculo:

- **Training Max** = 1RM × 85 % + (ciclo − 1) × incremento. En fondo y dominada
  el 1RM es **total** (peso corporal + lastre).
- **Onda 5/3/1:** semana 1 65/75/85 % ×5 (5+), semana 2 70/80/90 % ×3 (3+),
  semana 3 75/85/95 % ×5/3/1+, semana 4 de descarga 40/50/60 % ×5.
- **Redondeo** a 2.5 kg (1.25 kg con microplacas), igual que `ROUND()` en Excel.
  En lastrados se redondea el lastre; si sale negativo es **asistencia**.
- **Incremento por ciclo:** +5 kg sentadilla, +2.5 kg fondo y dominada.
- **Accesorios con progresión doble:** cada ciclo suma 1 rep por serie; al
  llegar al tope (p. ej. 3×10 en press militar) sube el peso y vuelve a las
  reps iniciales.

### Compilar e instalar

Requiere JDK 17 y el SDK de Android (compileSdk 35).

```bash
cd android
./gradlew test            # tests del motor contra los valores de la hoja
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

En teléfonos Xiaomi hay que activar **Instalar vía USB** en Opciones de
desarrollador y aceptar el aviso que aparece en pantalla al instalar.

Más detalle de la estructura del código en [`android/README.md`](android/README.md).

## Calculadora de escritorio (Python)

Todavía usa el plan anterior (TM al 90 % del 1RM y lastre como 1RM en
dominadas y fondos). No está sincronizada con la app.

- `ciclo_531.py`: CLI interactiva que genera un Excel de 8 semanas (2 ciclos).
- `pdf_531.py`: genera el PDF del plan de 4 días.
- `app_531.py`: GUI en tkinter con perfil guardado (`perfil_531.json`),
  export a Excel/PDF y subida a Google Drive con `rclone` (`drive_531.json`).
- `app_531.spec`: empaqueta la GUI como `Calculadora 5-3-1.app` con PyInstaller.

```bash
pip3 install openpyxl reportlab
python3 app_531.py        # GUI
python3 ciclo_531.py      # CLI → ciclo_531_<fecha>.xlsx
python3 pdf_531.py        # PDF desde el perfil guardado
pyinstaller app_531.spec  # app de macOS en dist/
```
