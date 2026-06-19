#!/bin/sh
# Liberthia Backend — entrypoint que arruma permissões antes de dropar root.
#
# Por que isso existe:
#   Quando o Docker Swarm/Compose monta um volume vazio em /app/data/...,
#   ele monta como root:root por padrão. O usuário `liberthia` (criado no
#   Dockerfile) não consegue escrever. Resultado: AccessDeniedException em
#   /app/data/voice-clips/2026-05/XXX.wav, e 503 em cascata pro frontend.
#
#   Esse script roda como root NO BOOT, faz chown nos volumes (idempotente —
#   chown de algo que já é do user não custa nada), e depois usa `su-exec`
#   pra trocar pro user liberthia antes de exec o java.
#
# IMPORTANTE: NUNCA usa `chmod 777`. Mantém o owner correto e usa as
# permissões padrão (755 dirs, 644 files).

set -eu

DATA_DIR="${LIBERTHIA_DATA_DIR:-/app/data}"

# 1. Garante que o diretório raiz existe (Swarm pode montar volume vazio)
if [ ! -d "$DATA_DIR" ]; then
    mkdir -p "$DATA_DIR"
fi

# 2. chown recursivo SÓ se algum arquivo dentro NÃO for owned by liberthia.
#    O check evita 2-3s de chown em cada boot quando tudo já está OK.
NEED_CHOWN=0
if [ -d "$DATA_DIR" ]; then
    # `find ... ! -user liberthia -print -quit` retorna 1ª match e para —
    # rápido mesmo em milhares de arquivos. Se achou algo, precisa chown.
    if find "$DATA_DIR" ! -user liberthia -print -quit 2>/dev/null | grep -q .; then
        NEED_CHOWN=1
    fi
fi

if [ "$NEED_CHOWN" = "1" ]; then
    echo "[entrypoint] Corrigindo ownership em $DATA_DIR -> liberthia:liberthia..."
    chown -R liberthia:liberthia "$DATA_DIR" 2>/dev/null || {
        echo "[entrypoint] AVISO: chown falhou (não estamos root?). Continuando assim mesmo."
    }
else
    echo "[entrypoint] Ownership OK em $DATA_DIR (skip chown)"
fi

# 3. Subdiretorios que o app cria em runtime (ex: voice-clips/2026-05/) podem
#    ser criados via Files.createDirectories no Java rodando como liberthia.
#    Mas se algum mês veio do volume com owner errado, conserta também.
for sub in voice-clips photos wardrobe etched-audio videos tester-packages tester-models tester-audios; do
    if [ -d "$DATA_DIR/$sub" ]; then
        # Conserta só permissão da pasta pai — subpastas mensais herdam
        chown liberthia:liberthia "$DATA_DIR/$sub" 2>/dev/null || true
        chmod 755 "$DATA_DIR/$sub" 2>/dev/null || true
    fi
done

# 4. Drop privilégios e executa java como user liberthia
echo "[entrypoint] Iniciando JVM como user liberthia..."

# Se já não somos root (rodando local sem privilégios), apenas exec sem su-exec
if [ "$(id -u)" = "0" ]; then
    exec su-exec liberthia:liberthia sh -c "java \$JAVA_OPTS \
        -Dadmin.web.password=\$LIBERTHIA_PASSWORD \
        -Dmod.url=\$MOD_URL \
        -Dmod.token=\$MOD_TOKEN \
        -jar /app/app.jar"
else
    exec java $JAVA_OPTS \
        -Dadmin.web.password="$LIBERTHIA_PASSWORD" \
        -Dmod.url="$MOD_URL" \
        -Dmod.token="$MOD_TOKEN" \
        -jar /app/app.jar
fi
