@echo off
REM merge_obj.bat — drag-and-drop wrapper pro merge_obj.py
REM Uso: arraste uma pasta cheia de .obj em cima deste .bat
REM      OU rode: merge_obj.bat caminho\da\pasta

setlocal

if "%~1"=="" (
    echo Uso: merge_obj.bat ^<pasta_com_obj^>
    echo  ou: arraste uma pasta em cima deste arquivo .bat
    pause
    exit /b 1
)

REM Detecta python
where python >nul 2>nul
if errorlevel 1 (
    echo ERRO: Python nao instalado ou nao esta no PATH.
    echo Instale Python 3 em https://python.org
    pause
    exit /b 1
)

python "%~dp0merge_obj.py" "%~1"

echo.
echo Pressione qualquer tecla pra fechar...
pause >nul
