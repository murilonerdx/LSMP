#!/usr/bin/env bash
# ============================================================================
#  Liberthia - wrapper OPCIONAL de auto-restart (Linux/macOS)
#
#  O comando /liberthia server shutdown faz: salva -> backup -> avisa ->
#  kicka todos -> espera 10s -> PARA o servidor (o processo java SAI).
#
#  Por padrao voce sobe o servidor de novo na mao. Se quiser que ele volte
#  SOZINHO depois de parar, lance o servidor por ESTE script em vez do java
#  direto: ele re-executa o servidor toda vez que o processo sai.
#
#  1. Copie este arquivo pra pasta do servidor MC (onde esta o forge .jar).
#  2. Ajuste JAR e MEM abaixo.  3. chmod +x start_server.sh && ./start_server.sh
#  Ctrl+C durante os 10s de espera cancela o loop.
# ============================================================================
set -u

JAR="forge-1.20.1-47.4.0-server.jar"
MEM="6G"

while true; do
  echo
  echo "[wrapper] iniciando o servidor..."
  java -Xms"${MEM}" -Xmx"${MEM}" -jar "${JAR}" nogui
  code=$?
  echo
  echo "[wrapper] o servidor parou (exit code ${code})."
  echo "[wrapper] reiniciando em 10s -- Ctrl+C pra NAO reiniciar."
  sleep 10 || break
done
