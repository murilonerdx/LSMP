@echo off
REM Liberthia Spell Designer — launcher
REM Requires Python 3.8+ with Pillow installed (pip install pillow)

cd /d "%~dp0\.."
python tools\spell_designer.py
if errorlevel 1 (
    echo.
    echo === ERRO ===
    echo Se viu "ModuleNotFoundError: pillow", rode:
    echo   pip install pillow
    echo.
    pause
)
