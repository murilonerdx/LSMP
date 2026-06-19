@echo off
REM ==========================================================================
REM Deploy completo: backend + frontend.
REM Roda no Windows. Builda local, pusha pro Docker Hub.
REM ==========================================================================

set TAG=v104

echo.
echo ===========================================
echo BUILD ^& PUSH — tag: %TAG%
echo ===========================================
echo.

REM ===== BACKEND =====
echo [1/4] Build backend...
cd /d "%~dp0backend"
docker build -t murilonerdx/lsmp-backend:%TAG% .
if errorlevel 1 (echo BACKEND BUILD FALHOU & pause & exit /b 1)

echo.
echo [2/4] Push backend...
docker push murilonerdx/lsmp-backend:%TAG%
if errorlevel 1 (echo BACKEND PUSH FALHOU. Rode 'docker login' & pause & exit /b 1)

REM ===== FRONTEND =====
echo.
echo [3/4] Build frontend...
cd /d "%~dp0frontend"
docker build -t murilonerdx/lsmp-frontend:%TAG% .
if errorlevel 1 (echo FRONTEND BUILD FALHOU & pause & exit /b 1)

echo.
echo [4/4] Push frontend...
docker push murilonerdx/lsmp-frontend:%TAG%
if errorlevel 1 (echo FRONTEND PUSH FALHOU & pause & exit /b 1)

echo.
echo ===========================================
echo ✅ TUDO PUSHADO PRO DOCKER HUB
echo ===========================================
echo.
echo AGORA NA VPS faça:
echo.
echo    ssh root@srv614263
echo.
echo    # Edita o docker-stack.yml
echo    nano /root/docker-stack.yml
echo.
echo    # Troca as DUAS linhas:
echo    #   image: murilonerdx/lsmp-backend:vX   ^→ image: murilonerdx/lsmp-backend:%TAG%
echo    #   image: murilonerdx/lsmp-frontend:vX  ^→ image: murilonerdx/lsmp-frontend:%TAG%
echo.
echo    # Salva (Ctrl+O, Enter, Ctrl+X)
echo.
echo    # Redeploy
echo    docker stack deploy -c /root/docker-stack.yml lsmp
echo.
echo    # Aguarda 30s
echo    sleep 30
echo.
echo    # Testa
echo    curl https://backend.astaroneremita.com/api/quotes -H "Authorization: Bearer SEU_TOKEN"
echo.
echo Se retornar {"content":[],"pageable":...} = funcionou.
echo Se 404 ainda = a tag no docker-stack.yml não foi atualizada.
echo.
pause
