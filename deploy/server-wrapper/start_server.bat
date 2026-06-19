@echo off
REM ============================================================================
REM  Liberthia - wrapper OPCIONAL de auto-restart (Windows)
REM
REM  O comando /liberthia server shutdown faz: salva -> backup -> avisa ->
REM  kicka todos -> espera 10s -> PARA o servidor (o processo java SAI).
REM
REM  Por padrao voce sobe o servidor de novo na mao. Se quiser que ele volte
REM  SOZINHO depois de parar, lance o servidor por ESTE .bat em vez do java
REM  direto: ele re-executa o servidor toda vez que o processo sai.
REM
REM  1. Copie este arquivo pra pasta do servidor MC (onde esta o forge .jar).
REM  2. Ajuste JAR e MEM abaixo.
REM  3. De um duplo-clique (ou rode no cmd). Ctrl+C duas vezes encerra o loop.
REM ============================================================================

setlocal
set JAR=forge-1.20.1-47.4.0-server.jar
set MEM=6G

:loop
echo.
echo [wrapper] iniciando o servidor...
java -Xms%MEM% -Xmx%MEM% -jar "%JAR%" nogui
echo.
echo [wrapper] o servidor parou (exit code %ERRORLEVEL%).
echo [wrapper] reiniciando em 10s -- feche esta janela ou Ctrl+C pra NAO reiniciar.
timeout /t 10
goto loop
