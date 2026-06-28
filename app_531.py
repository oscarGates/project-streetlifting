#!/usr/bin/env python3
"""
Interfaz grafica (tkinter) para la calculadora de ciclo 5/3/1.

- Agrega/edita/elimina ejercicios desde una ventana.
- Mezcla barra y peso corporal (dips/pull-ups), cada uno en su unidad (kg o lbs).
- Guarda y precarga los inputs en un perfil local JSON (perfil_531.json).
- Genera el Excel de 8 semanas reutilizando la logica de ciclo_531.py.
"""

import json
import os
import sys
import shutil
import subprocess
import threading
from datetime import date
import tkinter as tk
from tkinter import ttk, messagebox, filedialog, simpledialog

import ciclo_531 as core

_DIR = os.path.dirname(os.path.abspath(__file__))


def _data_dir():
    """Carpeta escribible para perfiles/config.
    - App empaquetada (.app): ~/Documents/Calculadora531 (el bundle es de solo lectura).
    - Script normal: junto al .py (comportamiento de siempre)."""
    if getattr(sys, "frozen", False):
        d = os.path.join(os.path.expanduser("~"), "Documents", "Calculadora531")
    else:
        d = _DIR
    os.makedirs(d, exist_ok=True)
    return d


_DATA_DIR = _data_dir()
PERFIL_PATH = os.path.join(_DATA_DIR, "perfil_531.json")
DRIVE_CONFIG_PATH = os.path.join(_DATA_DIR, "drive_531.json")


# --------------------------------------------------------------------------- #
# Persistencia del perfil
# --------------------------------------------------------------------------- #
CAMPOS = ("nombre", "es_bw", "unit", "rm", "tren", "bw", "bw_unit")


def validar_perfil(data):
    """Valida que el JSON sea una lista de ejercicios con la forma esperada.
    Devuelve la lista normalizada o lanza ValueError con un mensaje claro."""
    if not isinstance(data, list):
        raise ValueError("El JSON debe ser una lista de ejercicios.")
    norm = []
    for i, e in enumerate(data, 1):
        if not isinstance(e, dict):
            raise ValueError("El elemento %d no es un objeto." % i)
        faltan = [c for c in ("nombre", "es_bw", "unit", "rm") if c not in e]
        if faltan:
            raise ValueError("Ejercicio %d: faltan campos %s." % (i, faltan))
        if e["unit"] not in ("kg", "lbs"):
            raise ValueError("Ejercicio '%s': unidad invalida." % e["nombre"])
        item = {c: e.get(c) for c in CAMPOS}
        item["es_bw"] = bool(item["es_bw"])
        item["rm"] = float(item["rm"])
        item["tren"] = item["tren"] if item["tren"] in ("upper", "lower") else "upper"
        if item["es_bw"]:
            if item["bw"] is None or item["bw_unit"] not in ("kg", "lbs"):
                raise ValueError(
                    "Ejercicio '%s': peso corporal/unidad invalidos." % e["nombre"])
            item["bw"] = float(item["bw"])
        else:
            item["bw"], item["bw_unit"] = None, None
        norm.append(item)
    return norm


def cargar_perfil(path=PERFIL_PATH):
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        return validar_perfil(json.load(f))


def guardar_perfil(ejercicios, path=PERFIL_PATH):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(ejercicios, f, indent=2, ensure_ascii=False)


def incremento_progresion(tren, unit):
    """Salto al progresar de bloque: superior +2.5kg/+5lbs, inferior +5kg/+10lbs."""
    if unit == "kg":
        return 2.5 if tren == "upper" else 5.0
    return 5.0 if tren == "upper" else 10.0


def progresar_perfil(ejercicios):
    """Devuelve una copia del perfil con el RM subido un bloque."""
    nuevos = []
    for e in ejercicios:
        e2 = dict(e)
        e2["rm"] = e["rm"] + incremento_progresion(e["tren"], e["unit"])
        nuevos.append(e2)
    return nuevos


def siguiente_version(path):
    """Sugiere el nombre de la version siguiente: foo.json -> foo_v2.json,
    foo_v2.json -> foo_v3.json."""
    import re
    carpeta, base = os.path.split(path)
    nombre, ext = os.path.splitext(base)
    m = re.search(r"_v(\d+)$", nombre)
    if m:
        nombre = nombre[:m.start()] + "_v%d" % (int(m.group(1)) + 1)
    else:
        nombre += "_v2"
    return os.path.join(carpeta, nombre + ext)


# --------------------------------------------------------------------------- #
# Subida a Google Drive (via rclone, convirtiendo a Google Sheets)
# --------------------------------------------------------------------------- #
def cargar_config_drive():
    cfg = {"remote": "drive", "folder": ""}
    if os.path.exists(DRIVE_CONFIG_PATH):
        try:
            with open(DRIVE_CONFIG_PATH, "r", encoding="utf-8") as f:
                cfg.update(json.load(f))
        except (ValueError, OSError):
            pass
    return cfg


def guardar_config_drive(cfg):
    with open(DRIVE_CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)


def rclone_remotes():
    """Lista de remotes de rclone (sin los ':' finales). [] si no hay rclone."""
    if not shutil.which("rclone"):
        return None  # rclone no instalado
    try:
        out = subprocess.run(["rclone", "listremotes"],
                             capture_output=True, text=True, timeout=15)
    except (OSError, subprocess.SubprocessError):
        return []
    return [r.strip().rstrip(":") for r in out.stdout.splitlines() if r.strip()]


def subir_a_drive(local_xlsx, remote, folder):
    """Sube el .xlsx a Drive convirtiendolo a Google Sheets.
    Devuelve (ok, mensaje)."""
    destino = "%s:%s" % (remote, folder.strip("/"))
    cmd = ["rclone", "copy", local_xlsx, destino,
           "--drive-import-formats", "xlsx"]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    except subprocess.TimeoutExpired:
        return False, "La subida tardo demasiado (timeout 120s)."
    except (OSError, subprocess.SubprocessError) as e:
        return False, "No se pudo ejecutar rclone: %s" % e
    if res.returncode == 0:
        nombre = os.path.splitext(os.path.basename(local_xlsx))[0]
        return True, "Subido como Google Sheet '%s' en %s" % (nombre, destino)
    return False, (res.stderr.strip() or "rclone fallo (codigo %d)"
                   % res.returncode)


# --------------------------------------------------------------------------- #
# Conversion perfil -> dict que entiende el motor (ciclo_531)
# --------------------------------------------------------------------------- #
def a_ejercicio_core(item):
    """Convierte un registro del perfil al dict que usa construir_semanas()."""
    unit = item["unit"]
    if item["es_bw"]:
        bw_disp = core.convertir(item["bw"], item["bw_unit"], unit)
        tm = (bw_disp + item["rm"]) * core.TM_FACTOR
    else:
        bw_disp = None
        tm = item["rm"] * core.TM_FACTOR
    return {
        "nombre": item["nombre"],
        "es_bw": item["es_bw"],
        "tren": item["tren"],
        "unit": unit,
        "bw_disp": bw_disp,
        "tm_cycle1": tm,
    }


# --------------------------------------------------------------------------- #
# Aplicacion
# --------------------------------------------------------------------------- #
class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Calculadora 5/3/1  -  Streetlifting")
        self.geometry("760x560")
        self.minsize(720, 520)

        try:
            self.ejercicios = cargar_perfil()
        except (ValueError, OSError):
            self.ejercicios = []  # perfil por defecto corrupto -> empezar vacio
        self.perfil_path = PERFIL_PATH  # ruta del perfil actual (para versionar)
        self.editando = None  # indice en edicion, o None
        self.ultimo_excel = None  # ruta del ultimo .xlsx generado (para subir)
        self.drive_cfg = cargar_config_drive()

        self._build_form()
        self._build_tabla()
        self._build_acciones()
        self._refrescar_tabla()
        self._toggle_bw()

    # ----------------------------- Formulario ----------------------------- #
    def _build_form(self):
        frm = ttk.LabelFrame(self, text="Ejercicio")
        frm.pack(fill="x", padx=10, pady=(10, 6))

        # Fila 0: nombre + checkbox peso corporal
        ttk.Label(frm, text="Nombre:").grid(row=0, column=0, sticky="e",
                                            padx=4, pady=4)
        self.var_nombre = tk.StringVar()
        ttk.Entry(frm, textvariable=self.var_nombre, width=22).grid(
            row=0, column=1, sticky="w", padx=4, pady=4)

        self.var_bw = tk.BooleanVar(value=False)
        ttk.Checkbutton(frm, text="Peso corporal (dips / pull-ups)",
                        variable=self.var_bw,
                        command=self._toggle_bw).grid(
            row=0, column=2, columnspan=2, sticky="w", padx=4, pady=4)

        # Fila 1: unidad + 1RM
        ttk.Label(frm, text="Unidad output:").grid(row=1, column=0, sticky="e",
                                                   padx=4, pady=4)
        self.var_unit = tk.StringVar(value="kg")
        ttk.OptionMenu(frm, self.var_unit, "kg", "kg", "lbs").grid(
            row=1, column=1, sticky="w", padx=4, pady=4)

        self.lbl_rm = ttk.Label(frm, text="1RM:")
        self.lbl_rm.grid(row=1, column=2, sticky="e", padx=4, pady=4)
        self.var_rm = tk.StringVar()
        ttk.Entry(frm, textvariable=self.var_rm, width=10).grid(
            row=1, column=3, sticky="w", padx=4, pady=4)

        # Fila 2: tren (barra) | peso corporal (bw)
        self.lbl_tren = ttk.Label(frm, text="Tren:")
        self.lbl_tren.grid(row=2, column=0, sticky="e", padx=4, pady=4)
        self.var_tren = tk.StringVar(value="superior")
        self.opt_tren = ttk.OptionMenu(frm, self.var_tren, "superior",
                                       "superior", "inferior")
        self.opt_tren.grid(row=2, column=1, sticky="w", padx=4, pady=4)

        self.lbl_bw = ttk.Label(frm, text="Peso corporal:")
        self.lbl_bw.grid(row=2, column=2, sticky="e", padx=4, pady=4)
        self.bw_box = ttk.Frame(frm)
        self.bw_box.grid(row=2, column=3, sticky="w", padx=4, pady=4)
        self.var_bwval = tk.StringVar()
        ttk.Entry(self.bw_box, textvariable=self.var_bwval, width=8).pack(
            side="left")
        self.var_bwunit = tk.StringVar(value="kg")
        ttk.OptionMenu(self.bw_box, self.var_bwunit, "kg", "kg", "lbs").pack(
            side="left", padx=(4, 0))

        # Fila 3: boton agregar
        self.btn_add = ttk.Button(frm, text="+ Agregar", command=self._agregar)
        self.btn_add.grid(row=3, column=1, sticky="w", padx=4, pady=(2, 8))
        ttk.Button(frm, text="Limpiar", command=self._limpiar_form).grid(
            row=3, column=3, sticky="w", padx=4, pady=(2, 8))

    def _toggle_bw(self):
        """Muestra/oculta campos segun sea peso corporal o barra."""
        es_bw = self.var_bw.get()
        self.lbl_rm.config(text="1RM (lastre):" if es_bw else "1RM:")
        if es_bw:
            # Peso corporal -> oculta 'Tren', muestra 'Peso corporal'.
            self.lbl_tren.grid_remove()
            self.opt_tren.grid_remove()
            self.lbl_bw.grid()
            self.bw_box.grid()
        else:
            # Barra -> muestra 'Tren', oculta 'Peso corporal'.
            self.lbl_bw.grid_remove()
            self.bw_box.grid_remove()
            self.lbl_tren.grid()
            self.opt_tren.grid()

    # ----------------------------- Tabla ---------------------------------- #
    def _build_tabla(self):
        frm = ttk.LabelFrame(self, text="Ejercicios del ciclo")
        frm.pack(fill="both", expand=True, padx=10, pady=6)

        cols = ("nombre", "tipo", "unit", "rm", "extra")
        self.tree = ttk.Treeview(frm, columns=cols, show="headings", height=8)
        for c, txt, w in [("nombre", "Ejercicio", 160), ("tipo", "Tipo", 90),
                          ("unit", "Unidad", 70), ("rm", "1RM", 90),
                          ("extra", "BW / Tren", 150)]:
            self.tree.heading(c, text=txt)
            self.tree.column(c, width=w, anchor="w")
        self.tree.pack(side="left", fill="both", expand=True, padx=(4, 0),
                       pady=4)
        sb = ttk.Scrollbar(frm, orient="vertical", command=self.tree.yview)
        sb.pack(side="right", fill="y")
        self.tree.configure(yscrollcommand=sb.set)
        self.tree.bind("<Double-1>", lambda e: self._editar())

        barra = ttk.Frame(self)
        barra.pack(fill="x", padx=10)
        ttk.Button(barra, text="Editar seleccionado",
                   command=self._editar).pack(side="left")
        ttk.Button(barra, text="Eliminar seleccionado",
                   command=self._eliminar).pack(side="left", padx=6)

    def _refrescar_tabla(self):
        self.tree.delete(*self.tree.get_children())
        for i, ej in enumerate(self.ejercicios):
            tipo = "Peso corp." if ej["es_bw"] else "Barra"
            rm = "+%s" % core.fmt(ej["rm"]) if ej["es_bw"] else core.fmt(ej["rm"])
            if ej["es_bw"]:
                extra = "BW %s %s" % (core.fmt(ej["bw"]), ej["bw_unit"])
            else:
                extra = ej["tren"]
            self.tree.insert("", "end", iid=str(i),
                             values=(ej["nombre"], tipo, ej["unit"], rm, extra))

    # ----------------------------- Acciones ------------------------------- #
    def _build_acciones(self):
        # Fila 1: generar / Drive + status
        fila1 = ttk.Frame(self)
        fila1.pack(fill="x", padx=10, pady=(10, 2))
        ttk.Button(fila1, text="Generar Excel (8 semanas)",
                   command=self._generar).pack(side="left")
        ttk.Button(fila1, text="Generar PDF (plan 4 dias)",
                   command=self._generar_pdf).pack(side="left", padx=6)
        ttk.Button(fila1, text="Subir a Drive",
                   command=self._subir_drive).pack(side="left")
        ttk.Button(fila1, text="Config Drive...",
                   command=self._config_drive).pack(side="left", padx=6)
        self.var_status = tk.StringVar(value="%d ejercicio(s) cargado(s)."
                                       % len(self.ejercicios))
        ttk.Label(fila1, textvariable=self.var_status,
                  foreground="#305496").pack(side="right")

        # Fila 2: gestion de perfil
        fila2 = ttk.Frame(self)
        fila2.pack(fill="x", padx=10, pady=(2, 10))
        ttk.Button(fila2, text="Progresar perfil (v2)",
                   command=self._progresar).pack(side="left")
        ttk.Button(fila2, text="Cargar perfil...",
                   command=self._cargar_como).pack(side="left", padx=6)
        ttk.Button(fila2, text="Guardar perfil",
                   command=self._guardar).pack(side="left")
        ttk.Button(fila2, text="Guardar como...",
                   command=self._guardar_como).pack(side="left", padx=6)

    def _leer_form(self):
        nombre = self.var_nombre.get().strip()
        if not nombre:
            messagebox.showwarning("Falta dato", "Escribe el nombre del ejercicio.")
            return None
        try:
            rm = float(self.var_rm.get().strip().replace(",", "."))
            if rm <= 0:
                raise ValueError
        except ValueError:
            messagebox.showwarning("Dato invalido", "El 1RM debe ser un numero > 0.")
            return None

        es_bw = self.var_bw.get()
        item = {"nombre": nombre, "es_bw": es_bw, "unit": self.var_unit.get(),
                "rm": rm, "tren": "upper", "bw": None, "bw_unit": None}
        if es_bw:
            try:
                bw = float(self.var_bwval.get().strip().replace(",", "."))
                if bw <= 0:
                    raise ValueError
            except ValueError:
                messagebox.showwarning(
                    "Dato invalido", "El peso corporal debe ser un numero > 0.")
                return None
            item["bw"] = bw
            item["bw_unit"] = self.var_bwunit.get()
            item["tren"] = "upper"  # dips/pull-ups son tren superior
        else:
            item["tren"] = ("upper" if self.var_tren.get() == "superior"
                            else "lower")
        return item

    def _agregar(self):
        item = self._leer_form()
        if item is None:
            return
        if self.editando is not None:
            self.ejercicios[self.editando] = item
            self.editando = None
            self.btn_add.config(text="+ Agregar")
        else:
            self.ejercicios.append(item)
        self._refrescar_tabla()
        self._limpiar_form()
        self._set_status("%d ejercicio(s)." % len(self.ejercicios))

    def _seleccion(self):
        sel = self.tree.selection()
        return int(sel[0]) if sel else None

    def _editar(self):
        i = self._seleccion()
        if i is None:
            messagebox.showinfo("Editar", "Selecciona un ejercicio en la tabla.")
            return
        ej = self.ejercicios[i]
        self.var_nombre.set(ej["nombre"])
        self.var_bw.set(ej["es_bw"])
        self.var_unit.set(ej["unit"])
        self.var_rm.set(core.fmt(ej["rm"]))
        if ej["es_bw"]:
            self.var_bwval.set(core.fmt(ej["bw"]))
            self.var_bwunit.set(ej["bw_unit"])
        else:
            self.var_tren.set("superior" if ej["tren"] == "upper" else "inferior")
        self._toggle_bw()
        self.editando = i
        self.btn_add.config(text="Guardar cambios")
        self._set_status("Editando '%s'..." % ej["nombre"])

    def _eliminar(self):
        i = self._seleccion()
        if i is None:
            messagebox.showinfo("Eliminar", "Selecciona un ejercicio en la tabla.")
            return
        nombre = self.ejercicios[i]["nombre"]
        del self.ejercicios[i]
        if self.editando == i:
            self.editando = None
            self.btn_add.config(text="+ Agregar")
            self._limpiar_form()
        self._refrescar_tabla()
        self._set_status("Eliminado '%s'." % nombre)

    def _limpiar_form(self):
        self.var_nombre.set("")
        self.var_rm.set("")
        self.var_bwval.set("")
        self.var_bw.set(False)
        self.var_unit.set("kg")
        self.var_bwunit.set("kg")
        self.var_tren.set("superior")
        self.editando = None
        self.btn_add.config(text="+ Agregar")
        self._toggle_bw()

    def _cargar_como(self):
        path = filedialog.askopenfilename(
            title="Cargar perfil JSON",
            initialdir=os.path.dirname(PERFIL_PATH),
            filetypes=[("JSON", "*.json"), ("Todos", "*.*")])
        if not path:
            return
        try:
            nuevos = cargar_perfil(path)
        except ValueError as e:
            messagebox.showerror("JSON invalido", "No se pudo cargar:\n%s" % e)
            return
        except OSError as e:
            messagebox.showerror("Error", "No se pudo leer el archivo:\n%s" % e)
            return
        self.ejercicios = nuevos
        self.perfil_path = path
        self._limpiar_form()
        self._refrescar_tabla()
        self._set_status("Perfil cargado (%d ejercicio(s)) desde %s"
                         % (len(nuevos), os.path.basename(path)))

    def _guardar(self):
        try:
            guardar_perfil(self.ejercicios)
            self._set_status("Perfil guardado en perfil_531.json")
        except OSError as e:
            messagebox.showerror("Error", "No se pudo guardar: %s" % e)

    def _progresar(self):
        if not self.ejercicios:
            messagebox.showwarning("Sin ejercicios",
                                   "Carga o agrega un perfil primero.")
            return
        # Crea la v2 y la carga en la tabla (el original en disco no se toca).
        self.ejercicios = progresar_perfil(self.ejercicios)
        self._limpiar_form()
        self._refrescar_tabla()
        self._set_status("Perfil progresado (RM +1 bloque). Guardalo si quieres.")

        sugerido = siguiente_version(self.perfil_path)
        path = filedialog.asksaveasfilename(
            title="Guardar perfil progresado (v2)",
            initialdir=os.path.dirname(sugerido),
            initialfile=os.path.basename(sugerido),
            defaultextension=".json",
            filetypes=[("JSON", "*.json")])
        if not path:
            return  # se queda en la tabla aunque no lo guarde
        try:
            guardar_perfil(self.ejercicios, path)
            self.perfil_path = path
            self._set_status("Perfil progresado guardado en %s"
                             % os.path.basename(path))
        except OSError as e:
            messagebox.showerror("Error", "No se pudo guardar: %s" % e)

    def _guardar_como(self):
        if not self.ejercicios:
            messagebox.showwarning("Sin ejercicios", "No hay nada que guardar.")
            return
        path = filedialog.asksaveasfilename(
            title="Guardar perfil como",
            initialdir=os.path.dirname(PERFIL_PATH),
            initialfile="perfil_531.json",
            defaultextension=".json",
            filetypes=[("JSON", "*.json")])
        if not path:
            return
        try:
            guardar_perfil(self.ejercicios, path)
            self.perfil_path = path
            self._set_status("Perfil guardado en %s" % os.path.basename(path))
        except OSError as e:
            messagebox.showerror("Error", "No se pudo guardar: %s" % e)

    def _generar(self):
        if not self.ejercicios:
            messagebox.showwarning("Sin ejercicios",
                                   "Agrega al menos un ejercicio.")
            return
        destino = filedialog.asksaveasfilename(
            title="Guardar Excel del ciclo",
            initialdir=os.path.dirname(PERFIL_PATH),
            initialfile="ciclo_531_%s.xlsx" % date.today().isoformat(),
            defaultextension=".xlsx",
            filetypes=[("Excel", "*.xlsx")])
        if not destino:
            return  # el usuario cancelo

        nucleo = [a_ejercicio_core(e) for e in self.ejercicios]
        semanas, avisos = core.construir_semanas(nucleo)
        try:
            fname = core.exportar_excel(semanas, destino)
        except PermissionError:
            messagebox.showerror(
                "Excel abierto",
                "Cierra el archivo Excel si lo tienes abierto y reintenta.")
            return
        try:
            guardar_perfil(self.ejercicios)  # autosave del perfil por defecto
        except OSError:
            pass

        self.ultimo_excel = fname
        msg = "Excel generado:\n%s\n\n(8 hojas, una por semana)" % fname
        if avisos:
            msg += "\n\nAVISO - sets de trabajo bajo el peso corporal:"
            for nombre, (lastre_min, u) in avisos.items():
                msg += ("\n  '%s': lastre minimo +%s %s en el 1RM"
                        % (nombre, core.fmt(core.redondear(lastre_min, u)), u))
        messagebox.showinfo("Listo", msg)
        self._set_status("Excel generado: %s" % os.path.basename(fname))

        if messagebox.askyesno("Subir a Drive",
                               "¿Subir este Excel a Google Drive ahora\n"
                               "(convertido a Google Sheets)?"):
            self._subir_drive()

    # ----------------------------- PDF ------------------------------------ #
    def _generar_pdf(self):
        if not self.ejercicios:
            messagebox.showwarning("Sin ejercicios", "Agrega o carga un perfil.")
            return
        try:
            import pdf_531
        except Exception as e:  # reportlab ausente u otro fallo de import
            messagebox.showerror(
                "Falta reportlab",
                "No se pudo cargar el generador de PDF:\n%s\n\n"
                "Instala con: pip3 install reportlab" % e)
            return

        maxes, pc, pc_unit, faltan = pdf_531.maxes_desde_perfil(self.ejercicios)
        if faltan:
            messagebox.showwarning(
                "Faltan principales",
                "El PDF necesita los 4 principales. Faltan: %s\n\n"
                "Se emparejan por nombre: dominada/pull, sentadilla/squat, "
                "fondo/dip, peso muerto/dead." % ", ".join(faltan))
            return
        if pc is None:
            messagebox.showwarning(
                "Sin peso corporal",
                "No encontre el peso corporal en dominada o fondo del perfil.")
            return

        descarga = messagebox.askyesno(
            "Descarga", "¿Incluir la semana 4 de descarga?")
        micro = messagebox.askyesno(
            "Microplacas", "¿Usar microplacas (redondeo 1.25 kg / 2.5 lbs)?\n"
            "(No = redondeo estandar 2.5 kg / 5 lbs)")

        destino = filedialog.asksaveasfilename(
            title="Guardar PDF del plan",
            initialdir=os.path.dirname(PERFIL_PATH),
            initialfile="plan_531_%s.pdf" % date.today().isoformat(),
            defaultextension=".pdf",
            filetypes=[("PDF", "*.pdf")])
        if not destino:
            return
        try:
            out = pdf_531.generar_pdf(maxes, pc, pc_unit, salida=destino,
                                      micro=micro, incluir_descarga=descarga)
        except PermissionError:
            messagebox.showerror(
                "PDF abierto", "Cierra el PDF si lo tienes abierto y reintenta.")
            return
        except Exception as e:
            messagebox.showerror("Error", "No se pudo generar el PDF:\n%s" % e)
            return
        messagebox.showinfo("Listo", "PDF generado:\n%s" % out)
        self._set_status("PDF generado: %s" % os.path.basename(out))

    # ----------------------------- Drive ---------------------------------- #
    def _config_drive(self):
        remotes = rclone_remotes()
        if remotes is None:
            messagebox.showerror(
                "rclone no instalado",
                "Instala rclone y configura un remote:\n\n"
                "  brew install rclone\n  rclone config\n\n"
                "Luego vuelve a 'Config Drive...'.")
            return
        ayuda = ("Remotes disponibles: %s" % ", ".join(remotes)) if remotes \
            else "No hay remotes. Corre 'rclone config' para crear uno."
        remote = simpledialog.askstring(
            "Config Drive", "Nombre del remote de rclone:\n(%s)" % ayuda,
            initialvalue=self.drive_cfg.get("remote", "drive"), parent=self)
        if remote is None:
            return
        folder = simpledialog.askstring(
            "Config Drive",
            "Carpeta destino en Drive (vacio = raiz).\nEj: 531/ciclos",
            initialvalue=self.drive_cfg.get("folder", ""), parent=self)
        if folder is None:
            return
        self.drive_cfg = {"remote": remote.strip(), "folder": folder.strip()}
        try:
            guardar_config_drive(self.drive_cfg)
            self._set_status("Config Drive guardada (remote '%s')."
                             % self.drive_cfg["remote"])
        except OSError as e:
            messagebox.showerror("Error", "No se pudo guardar la config: %s" % e)

    def _subir_drive(self):
        if not self.ultimo_excel or not os.path.exists(self.ultimo_excel):
            messagebox.showwarning("Sin Excel",
                                   "Genera el Excel primero (boton 'Generar Excel').")
            return
        remotes = rclone_remotes()
        if remotes is None:
            messagebox.showerror(
                "rclone no instalado",
                "Instala rclone y configura un remote:\n\n"
                "  brew install rclone\n  rclone config")
            return
        remote = self.drive_cfg.get("remote", "drive")
        if remote not in remotes:
            messagebox.showerror(
                "Remote no encontrado",
                "El remote '%s' no existe en rclone.\n"
                "Remotes: %s\n\nUsa 'Config Drive...' o corre 'rclone config'."
                % (remote, ", ".join(remotes) or "(ninguno)"))
            return

        self._set_status("Subiendo a Drive...")
        local = self.ultimo_excel
        folder = self.drive_cfg.get("folder", "")

        def tarea():
            ok, info = subir_a_drive(local, remote, folder)
            self.after(0, lambda: self._fin_subida(ok, info))

        threading.Thread(target=tarea, daemon=True).start()

    def _fin_subida(self, ok, info):
        if ok:
            self._set_status(info)
            messagebox.showinfo("Drive", info)
        else:
            self._set_status("Fallo la subida a Drive.")
            messagebox.showerror("Drive", "No se pudo subir:\n%s" % info)

    def _set_status(self, txt):
        self.var_status.set(txt)


if __name__ == "__main__":
    App().mainloop()
