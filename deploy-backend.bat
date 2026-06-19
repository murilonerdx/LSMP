@echo off
REM ==========================================================================
REM Script de deploy do backend — roda local no Windows.
REM Builda + pusha a imagem pro Docker Hub.
REM Depois você precisa atualizar o docker-stack.yml na VPS e redeploy.
REM ==========================================================================

set TAG=v7
echo.
echo [1/3] Fazendo build da imagem murilonerdx/lsmp-backend:%TAG%...
cd /d "%~dp0backend"
docker build -t murilonerdx/lsmp-backend:%TAG% .
if errorlevel 1 (
    echo ✗ BUILD FALHOU. Confira se Docker Desktop está rodando.
    pause
    exit /b 1
)

echo.
echo [2/3] Fazendo push pro Docker Hub...
docker push murilonerdx/lsmp-backend:%TAG%
if errorlevel 1 (
    echo ✗ PUSH FALHOU. Faça 'docker login' antes.
    pause
    exit /b 1
)

echo.
echo [3/3] OK! Agora na VPS, faça:
echo.
echo    ssh root@srv614263
echo    nano /root/docker-stack.yml
echo    # troca a linha 'image: murilonerdx/lsmp-backend:vX' pra ':%TAG%'
echo    docker stack deploy -c /root/docker-stack.yml lsmp
echo.
echo Aguarde 30s e teste:
echo    curl https://backend.astaroneremita.com/api/quotes
echo.
echo Se retornar JSON em vez de 404, FUNCIONOU.
echo.
pause
