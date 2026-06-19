#!/bin/bash
# ============================================================================
# Backend entrypoint — corrige permissões do volume Docker antes de subir JVM.
# ============================================================================
#
# PROBLEMA:
# Quando o docker-stack monta `backend_data:/app/data`, o Docker cria o volume
# vazio com owner root:root. O usuário `liberthia` (dropping de root no
# Dockerfile) não consegue escrever em `/app/data/voice-clips/2026-05/` →
# AccessDeniedException no Files.write() do VoiceClipService, então cada
# clipe de voz que chega via /api/mod/voice/clips/{id}/audio falha com 500.
#
# SOLUÇÃO:
# Container inicia como root (sem USER liberthia no Dockerfile), entrypoint
# faz chown -R no /app/data, e depois dropa pra liberthia via `gosu` antes
# de exec java. Assim qualquer subdir criado pelo Files.createDirectories
# dentro de /app/data herda owner liberthia.
#
# Idempotente: roda em todo start, mas chown só corrige se necessário.
# ============================================================================

set -e

# Pasta de dados que o backend escreve em runtime
DATA_DIR="${LIBERTHIA_DATA_DIR:-/app/data}"

# Cria subdirs (no-op se já existem)
mkdir -p "$DATA_DIR/voice-clips" \
         "$DATA_DIR/photos" \
         "$DATA_DIR/wardrobe" \
         "$DATA_DIR/etched-audio" \
         "$DATA_DIR/videos" \
         "$DATA_DIR/paintings"

# Conserta owner do volume montado.
# `chown -R` é rápido em FS local; se o volume tiver muitos arquivos (10k+),
# considera usar `find ... -not -user liberthia -exec chown ... +` pra
# acelerar (só toca arquivos com owner errado).
chown -R liberthia:liberthia "$DATA_DIR" 2>/dev/null || {
    echo "[entrypoint] WARN: chown falhou em $DATA_DIR — talvez o volume seja read-only ou já esteja correto" >&2
}

# Dropa pra liberthia e exec a JVM. `exec` substitui o processo do shell
# pelo java, então sinais (SIGTERM do docker stop) chegam direto.
exec gosu liberthia java $JAVA_OPTS -jar /app/app.jar
