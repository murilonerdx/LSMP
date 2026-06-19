@echo off
REM Wrapper pra dar duplo-clique no deploy.ps1 sem precisar abrir PowerShell.
REM Passa todos argumentos pro script principal.
REM
REM Uso:
REM   deploy.bat                 -> incrementa patch + deploy
REM   deploy.bat -NoIncrement    -> rebuild sem virar versao
REM   deploy.bat -VersionPart minor  -> vira 0.X.0

setlocal
cd /d "%~dp0"

REM ExecutionPolicy Bypass evita o "scripts são desabilitados nesse sistema"
REM sem mudar a policy global do Windows.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy.ps1" %*

if errorlevel 1 (
    echo.
    echo *** DEPLOY FALHOU - veja erros acima ***
    pause
    exit /b 1
)

echo.
pause
