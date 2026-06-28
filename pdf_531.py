#!/usr/bin/env python3
"""
Generador de PDF del plan 5/3/1 (1 ciclo: 3 semanas + descarga opcional).

- Reutiliza la logica 5/3/1 del motor `ciclo_531.py`: la onda de porcentajes
  (`core.CYCLE`), TM = 90% del 1RM (`core.TM_FACTOR`), la conversion de unidades
  (`core.convertir`) y el formateo (`core.fmt`).
- Redondeo propio (decision de diseno): 2.5 kg por defecto, 1.25 kg con
  microplacas (`micro=True`); en lbs equivale a 5 / 2.5 lbs.
- Dominada y fondo son LASTRADOS: el % se aplica sobre el TM del sistema
  (peso_corporal + lastre) y se reporta el LASTRE = objetivo - peso_corporal.
  Si el lastre sale negativo se reporta como "asistencia (liga)".
- Parametrizado y re-ejecutable: para el siguiente ciclo solo cambias los 1RM
  (o el perfil) y regeneras; no hay que editar el codigo.

Uso como funcion:
    from pdf_531 import generar_pdf
    generar_pdf(
        maxes={
            "dominada":    {"rm": 50,  "unit": "kg"},
            "sentadilla":  {"rm": 180, "unit": "kg"},
            "fondo":       {"rm": 60,  "unit": "kg"},
            "peso_muerto": {"rm": 220, "unit": "kg"},
        },
        peso_corporal=95, pc_unit="kg",
        salida="plan.pdf", micro=False, incluir_descarga=True)

Uso por terminal (toma los 4 principales del perfil guardado):
    python3 pdf_531.py
"""

import os
from datetime import date

import ciclo_531 as core

try:
    from reportlab.lib import colors
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.lib.units import cm
    from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer, Table,
                                    TableStyle, PageBreak)
except ImportError:
    raise SystemExit("Falta reportlab. Instala con: pip3 install reportlab")


# --------------------------------------------------------------------------- #
# Programa fijo: 4 dias. Cada dia tiene un principal (5/3/1) y auxiliares.
# Principal: (etiqueta, clave_en_maxes, es_lastrado, tren)
# Auxiliar:  (ejercicio, esquema, RIR)
# --------------------------------------------------------------------------- #
DISTRIBUCION = ("Lun D1  ·  Mar D2  ·  Mie descanso  ·  "
                "Jue D3  ·  Vie D4  ·  finde descanso")

DIAS = [
    {
        "titulo": "Dia 1 - Traccion + brazos",
        "principal": ("Dominada", "dominada", True, "upper"),
        "auxiliares": [
            ("Fondo (volumen)", "4x8-12", "1-2"),
            ("Press militar", "3x6-8", "2"),
            ("Remo chest-supported", "3x10-12", "1-2"),
            ("Elevacion lateral", "3-4x12-20", "1, ultima 0"),
            ("Curl supinado", "3x8-12", "0-1"),
        ],
    },
    {
        "titulo": "Dia 2 - Sentadilla",
        "principal": ("Sentadilla", "sentadilla", False, "lower"),
        "auxiliares": [
            ("Prensa", "3x10-15", "1-2"),
            ("Single-leg RDL", "3x8-10 / pierna", "2"),
            ("Curl femoral", "3x10-15", "1, ultima 0"),
            ("Gemelo", "3-4x10-15", "1, ultima 0"),
        ],
    },
    {
        "titulo": "Dia 3 - Empuje + brazos",
        "principal": ("Fondo", "fondo", True, "upper"),
        "auxiliares": [
            ("Dominada (volumen)", "6x6 a peso corporal", "3-4"),
            ("Press banca cerrado", "3x6-10", "1-2"),
            ("Elevacion lateral", "3-4x12-20", "1, ultima 0"),
            ("Face pull", "3x15-20", "0-1"),
            ("Curl martillo", "3x8-12", "0-1"),
        ],
    },
    {
        "titulo": "Dia 4 - Muerto",
        "principal": ("Peso muerto", "peso_muerto", False, "lower"),
        "auxiliares": [
            ("Pistol / box squat", "3x6-8 / pierna", "2"),
            ("Abductores", "3x15-20", "0-1"),
            ("Curl femoral o hip thrust", "3x10-12", "1, ultima 0"),
            ("Gemelo", "3-4x10-15", "1, ultima 0"),
        ],
    },
]

# Incremento de TM para el siguiente ciclo (mismos valores que el motor).
INC_NEXT_KG = {"upper": core.INCREMENT_UPPER_KG, "lower": core.INCREMENT_LOWER_KG}

# Peso de la barra por unidad (para calcular cuanto cargar por lado).
PESO_BARRA = {"kg": 20.0, "lbs": 45.0}

# Mapeo de nombres de perfil -> clave de principal (para la GUI / terminal).
KEYWORDS = {
    "dominada": ("domin", "pull"),
    "sentadilla": ("sentad", "squat"),
    "fondo": ("fondo", "dip"),
    "peso_muerto": ("muerto", "dead"),
}


# --------------------------------------------------------------------------- #
# Redondeo propio (microplacas)
# --------------------------------------------------------------------------- #
def incremento(unit, micro):
    if unit == "kg":
        return 1.25 if micro else 2.5
    return 2.5 if micro else 5.0


def redondear(x, unit, micro=False):
    inc = incremento(unit, micro)
    return round(x / inc) * inc


# --------------------------------------------------------------------------- #
# Calculo de un principal (reutiliza core.CYCLE / TM_FACTOR / convertir)
# --------------------------------------------------------------------------- #
def calcular_principal(rm, unit, es_bw, tren, peso_corporal, pc_unit,
                       micro, incluir_descarga, peso_barra=None):
    """Devuelve dict con TM, TM siguiente ciclo y las semanas con sus series."""
    barra = peso_barra if peso_barra is not None else PESO_BARRA[unit]
    if es_bw:
        bw_disp = core.convertir(peso_corporal, pc_unit, unit)
        tm = (bw_disp + rm) * core.TM_FACTOR
    else:
        bw_disp = None
        tm = rm * core.TM_FACTOR

    tm_next = tm + core.convertir(INC_NEXT_KG[tren], "kg", unit)

    semanas = []
    for wk in core.CYCLE:
        if wk["deload"] and not incluir_descarga:
            continue
        series = []
        for pct, reps, amrap in wk["sets"]:
            objetivo = redondear(tm * pct, unit, micro)
            if es_bw:
                lastre = redondear(objetivo - bw_disp, unit, micro)
                if lastre >= 0:
                    peso_txt = "+%s %s" % (core.fmt(lastre), unit)
                else:
                    peso_txt = "asistencia (liga)"
            else:
                por_lado = (objetivo - barra) / 2.0
                if por_lado > 0:
                    peso_txt = "%s %s  (%s/lado)" % (
                        core.fmt(objetivo), unit, core.fmt(por_lado))
                else:
                    peso_txt = "%s %s  (solo barra)" % (core.fmt(objetivo), unit)
            series.append({
                "pct": int(round(pct * 100)),
                "reps": reps,
                "amrap": amrap,
                "peso": peso_txt,
            })
        semanas.append({"nombre": wk["nombre"], "deload": wk["deload"],
                        "series": series})

    return {"unit": unit, "es_bw": es_bw, "bw_disp": bw_disp,
            "tm": tm, "tm_next": tm_next, "semanas": semanas}


# --------------------------------------------------------------------------- #
# Construccion del PDF
# --------------------------------------------------------------------------- #
def _estilos():
    ss = getSampleStyleSheet()
    ss.add(ParagraphStyle("DiaTit", parent=ss["Heading2"], spaceBefore=2,
                          spaceAfter=4, textColor=colors.HexColor("#305496")))
    ss.add(ParagraphStyle("Princ", parent=ss["Heading3"], spaceBefore=2,
                          spaceAfter=2))
    ss.add(ParagraphStyle("Nota", parent=ss["Normal"], fontSize=8,
                          textColor=colors.grey))
    ss.add(ParagraphStyle("Aux", parent=ss["Heading4"], spaceBefore=8,
                          spaceAfter=2))
    return ss


def _tabla_principal(princ, etiqueta, ss):
    """Tabla semanas x 3 series con % / reps / peso ya calculado."""
    head = ["Semana", "Serie 1", "Serie 2", "Serie 3"]
    filas = [head]
    estilo_amrap = []  # coordenadas de celdas AMRAP para resaltar

    for r, sem in enumerate(princ["semanas"], 1):
        celdas = [sem["nombre"].split(" - ")[0]]  # "Semana 1"
        for c, s in enumerate(sem["series"], 1):
            reps = "%d%s" % (s["reps"], "+" if s["amrap"] else "")
            txt = "%d%% x%s\n%s" % (s["pct"], reps, s["peso"])
            if s["amrap"]:
                txt += "\nAMRAP  reps: ____"
                estilo_amrap.append((c, r))
            celdas.append(txt)
        filas.append(celdas)

    t = Table(filas, colWidths=[2.2 * cm, 3.6 * cm, 3.6 * cm, 3.6 * cm])
    estilo = [
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#305496")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.grey),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (0, 0), (-1, -1), "CENTER"),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1),
         [colors.white, colors.HexColor("#EEF3FB")]),
    ]
    for (c, r) in estilo_amrap:
        estilo.append(("BACKGROUND", (c, r), (c, r), colors.HexColor("#FCE4D6")))
    # Resalta fila de descarga
    for r, sem in enumerate(princ["semanas"], 1):
        if sem["deload"]:
            estilo.append(("BACKGROUND", (0, r), (0, r),
                           colors.HexColor("#FFF2CC")))
    t.setStyle(TableStyle(estilo))
    return t


def _tabla_auxiliares(auxiliares, ss):
    head = ["Auxiliar", "Sets x reps", "RIR", "Peso usado", "Reps reales"]
    filas = [head]
    for nombre, esquema, rir in auxiliares:
        filas.append([nombre, esquema, rir, "", ""])
    t = Table(filas, colWidths=[5.2 * cm, 3.4 * cm, 2.6 * cm, 2.8 * cm, 2.6 * cm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#7F7F7F")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.grey),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1),
         [colors.white, colors.HexColor("#F2F2F2")]),
        ("TOPPADDING", (0, 1), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 1), (-1, -1), 7),
    ]))
    return t


def generar_pdf(maxes, peso_corporal, pc_unit="kg", salida=None,
                micro=False, incluir_descarga=True, peso_barra=None):
    """Genera el PDF de un ciclo completo. Devuelve la ruta del archivo.
    peso_barra: None usa la barra estandar (20 kg / 45 lbs) segun la unidad."""
    if salida is None:
        salida = "plan_531_%s.pdf" % date.today().isoformat()

    ss = _estilos()
    doc = SimpleDocTemplate(salida, pagesize=A4,
                            leftMargin=1.4 * cm, rightMargin=1.4 * cm,
                            topMargin=1.2 * cm, bottomMargin=1.2 * cm)
    flow = []

    # Calcula los 4 principales primero (para el encabezado de TM).
    calc = {}
    for dia in DIAS:
        etiqueta, clave, es_bw, tren = dia["principal"]
        m = maxes[clave]
        calc[clave] = calcular_principal(
            m["rm"], m["unit"], es_bw, tren, peso_corporal, pc_unit,
            micro, incluir_descarga, peso_barra)

    # ---- Encabezado general ----
    flow.append(Paragraph("Plan 5/3/1 - Ciclo (%s)"
                          % date.today().isoformat(), ss["Title"]))
    flow.append(Paragraph("Distribucion semanal: <b>%s</b>" % DISTRIBUCION,
                          ss["Normal"]))
    redondeo_txt = "microplacas (1.25 kg / 2.5 lbs)" if micro \
        else "estandar (2.5 kg / 5 lbs)"
    flow.append(Paragraph("Redondeo: %s%s" % (
        redondeo_txt,
        "" if incluir_descarga else "  -  sin semana de descarga"),
        ss["Nota"]))
    flow.append(Spacer(1, 6))

    # Tabla de TM usados + TM del siguiente ciclo.
    tm_filas = [["Principal", "TM este ciclo", "TM siguiente ciclo"]]
    for dia in DIAS:
        etiqueta, clave, es_bw, tren = dia["principal"]
        c = calc[clave]
        u = c["unit"]
        nota = " (sistema: BW+lastre)" if es_bw else ""
        tm_filas.append([etiqueta + nota,
                         "%s %s" % (core.fmt(redondear(c["tm"], u, micro)), u),
                         "%s %s" % (core.fmt(redondear(c["tm_next"], u, micro)), u)])
    tm_tabla = Table(tm_filas, colWidths=[7 * cm, 4.5 * cm, 4.5 * cm])
    tm_tabla.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#305496")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.grey),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1),
         [colors.white, colors.HexColor("#EEF3FB")]),
    ]))
    flow.append(tm_tabla)

    # ---- Una seccion por dia (una pagina cada uno) ----
    for dia in DIAS:
        etiqueta, clave, es_bw, tren = dia["principal"]
        c = calc[clave]
        flow.append(PageBreak())
        flow.append(Paragraph(dia["titulo"], ss["DiaTit"]))
        flow.append(Paragraph("Principal: <b>%s</b>  -  5/3/1" % etiqueta,
                              ss["Princ"]))
        if es_bw:
            bw_txt = "%s %s" % (core.fmt(redondear(c["bw_disp"], c["unit"], micro)),
                               c["unit"])
            flow.append(Paragraph(
                "Lastrado: los pesos son <b>lastre anadido</b> sobre tu peso "
                "corporal (%s). 'asistencia (liga)' = el objetivo cae bajo el BW."
                % bw_txt, ss["Nota"]))
        flow.append(Spacer(1, 3))
        flow.append(_tabla_principal(c, etiqueta, ss))
        flow.append(Paragraph(
            "La serie con <b>+</b> es AMRAP: haz el maximo de reps y anotalas.",
            ss["Nota"]))

        flow.append(Paragraph("Auxiliares", ss["Aux"]))
        flow.append(_tabla_auxiliares(dia["auxiliares"], ss))

    doc.build(flow)
    return salida


# --------------------------------------------------------------------------- #
# Mapeo desde el perfil de la GUI (lista de ejercicios) -> maxes
# --------------------------------------------------------------------------- #
def maxes_desde_perfil(ejercicios):
    """Devuelve (maxes, peso_corporal, pc_unit, faltantes) a partir del perfil.
    Empareja por palabras clave en el nombre del ejercicio."""
    maxes = {}
    peso_corporal = None
    pc_unit = "kg"
    for clave, palabras in KEYWORDS.items():
        for ej in ejercicios:
            nombre = ej["nombre"].lower()
            if any(p in nombre for p in palabras):
                maxes[clave] = {"rm": ej["rm"], "unit": ej["unit"]}
                if ej.get("es_bw") and ej.get("bw") is not None \
                        and peso_corporal is None:
                    peso_corporal = ej["bw"]
                    pc_unit = ej["bw_unit"]
                break
    faltantes = [k for k in KEYWORDS if k not in maxes]
    return maxes, peso_corporal, pc_unit, faltantes


# --------------------------------------------------------------------------- #
# Terminal: genera el PDF desde el perfil guardado (re-ejecutable sin editar)
# --------------------------------------------------------------------------- #
def _main():
    import json
    perfil_path = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                               "perfil_531.json")
    if not os.path.exists(perfil_path):
        raise SystemExit("No hay perfil_531.json. Usa la GUI o llama a "
                         "generar_pdf() con tus maxes.")
    with open(perfil_path, "r", encoding="utf-8") as f:
        ejercicios = json.load(f)

    maxes, pc, pc_unit, faltan = maxes_desde_perfil(ejercicios)
    if faltan:
        raise SystemExit("Faltan principales en el perfil: %s\n"
                         "(se emparejan por nombre: dominada/pull, "
                         "sentadilla/squat, fondo/dip, muerto/dead)"
                         % ", ".join(faltan))
    if pc is None:
        raise SystemExit("No encontre peso corporal en dominada/fondo del perfil.")

    out = generar_pdf(maxes, pc, pc_unit)
    print("PDF generado:", out)


if __name__ == "__main__":
    _main()
