# -*- mode: python ; coding: utf-8 -*-
# Build de la GUI 5/3/1 como app de macOS:
#     pyinstaller app_531.spec
# Resultado: dist/Calculadora 5-3-1.app  (doble clic en Finder)

block_cipher = None

a = Analysis(
    ['app_531.py'],
    pathex=[],
    binaries=[],
    datas=[],
    # Modulos locales y libs que se importan de forma perezosa o dinamica.
    hiddenimports=['ciclo_531', 'pdf_531', 'reportlab', 'openpyxl'],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    # numpy arrastra MKL (libiomp5) y crashea el .app; no se usa de verdad
    # (openpyxl/Pillow solo lo importan de forma perezosa). Excluir eso ademas
    # adelgaza el bundle de ~900 MB a unas decenas de MB.
    excludes=['numpy', 'scipy', 'pandas', 'matplotlib', 'IPython', 'mkl',
              'sympy', 'numba', 'PyQt5', 'PySide2', 'notebook', 'pytest',
              'sphinx', 'cython', 'tornado'],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)
pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='Calculadora 5-3-1',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=False,
    console=False,            # sin terminal (modo ventana)
    disable_windowed_traceback=False,
    argv_emulation=True,      # asociar archivos / drag&drop en macOS
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
coll = COLLECT(
    exe,
    a.binaries,
    a.datas,
    strip=False,
    upx=False,
    upx_exclude=[],
    name='Calculadora 5-3-1',
)
app = BUNDLE(
    coll,
    name='Calculadora 5-3-1.app',
    icon=None,
    bundle_identifier='com.oscar.calculadora531',
    info_plist={
        'CFBundleName': 'Calculadora 5-3-1',
        'CFBundleDisplayName': 'Calculadora 5/3/1',
        'NSHighResolutionCapable': True,
    },
)
