# Multi-stage build do backend (Spring Boot 3 + Java 17 + whisper.cpp)
#
# ─── IMPORTANTE ────────────────────────────────────────────────────────────
# Esse Dockerfile compila whisper.cpp + baixa modelo large-v3-turbo (~1.5GB).
# Sem whisper-cli e modelo, o WhisperTranscriptionService boota e
# IMEDIATAMENTE seta enabled=false (vide WhisperTranscriptionService.java:198-205),
# resultando na tela do painel mostrando "✗ disabled" com clipes pendentes
# acumulando pra sempre.
#
# Equivalente ao backend/Dockerfile original — só ajusta paths pq esse roda
# com context=`..` do deploy/ (vê `backend/`, `frontend/`, etc).
# ────────────────────────────────────────────────────────────────────────────

# Stage 1: build do JAR com Gradle
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copia gradle wrapper + configs primeiro (cache layer)
COPY backend/gradle/ gradle/
COPY backend/gradlew backend/gradlew.bat backend/build.gradle backend/settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copia source e builda
COPY backend/src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: runtime com whisper.cpp + ffmpeg + yt-dlp
FROM eclipse-temurin:17-jre
WORKDIR /app

# Cria user não-root
RUN groupadd --system liberthia && useradd --system --gid liberthia liberthia

# yt-dlp + ffmpeg + build tools pra whisper.cpp.
# Pacote python3-full + venv pra contornar o PEP 668 do Debian 12 que bloqueia
# pip install global. Alpine não tem essa restrição, mas usamos debian-based
# (temurin:17-jre é debian) pra compatibilidade ARM/AMD64 e gcc/cmake nativos.
#
# IMPORTANTE: a lista de pacotes aqui DEVE ficar idêntica à v89 — qualquer
# alteração invalida o cache dessa layer e força whisper.cpp ser recompilado
# do zero (15+ min). Pacotes novos (ex: gosu pra drop-priv no entrypoint)
# vão num RUN separado mais abaixo, DEPOIS do whisper build.
RUN apt-get update && apt-get install -y --no-install-recommends \
        ffmpeg ca-certificates curl wget bash \
        python3 python3-pip python3-venv \
        git build-essential cmake \
    && rm -rf /var/lib/apt/lists/*

# yt-dlp via venv (não conflita com python do sistema)
RUN python3 -m venv /opt/ytdlp-venv \
    && /opt/ytdlp-venv/bin/pip install --no-cache-dir --upgrade yt-dlp \
    && ln -sf /opt/ytdlp-venv/bin/yt-dlp /usr/local/bin/yt-dlp \
    && yt-dlp --version

# whisper.cpp build + modelo large-v3-turbo (~1.5GB).
# BUILD_SHARED_LIBS=OFF pra ter binário estático (sem dep de .so).
# GGML_NATIVE=OFF e GGML_OPENMP=OFF pra portabilidade entre CPUs.
ARG WHISPER_MODEL=large-v3-turbo
RUN cd /opt \
    && git clone --depth 1 https://github.com/ggerganov/whisper.cpp.git whisper \
    && cd whisper \
    && cmake -B build \
         -DCMAKE_BUILD_TYPE=Release \
         -DBUILD_SHARED_LIBS=OFF \
         -DWHISPER_BUILD_EXAMPLES=ON \
         -DWHISPER_BUILD_TESTS=OFF \
         -DGGML_NATIVE=OFF \
         -DGGML_OPENMP=OFF \
    && cmake --build build --target whisper-cli -j2 \
    && bash ./models/download-ggml-model.sh ${WHISPER_MODEL} \
    && cp build/bin/whisper-cli /usr/local/bin/whisper-cli \
    && cp models/ggml-${WHISPER_MODEL}.bin /usr/local/share/ggml-${WHISPER_MODEL}.bin \
    && chmod +x /usr/local/bin/whisper-cli \
    && cd / \
    && rm -rf /opt/whisper \
    && ls -lh /usr/local/share/ggml-*.bin \
    && /usr/local/bin/whisper-cli --help 2>&1 | head -3 || true

# Pré-cria diretórios de storage com owner correto (no-op se volume vai
# sobrescrever — o entrypoint chama chown -R em runtime pra cobrir esse caso).
RUN mkdir -p /app/data/voice-clips /app/data/photos /app/data/wardrobe \
             /app/data/etched-audio /app/data/videos /app/data/paintings \
    && chown -R liberthia:liberthia /app

# Instala `gosu` + `cpulimit` AQUI (não junto do apt-get inicial) pra
# preservar o cache do whisper.cpp build acima.
#
# - gosu: dropa de root pra liberthia no entrypoint sem perder SIGTERM.
# - cpulimit: cap HARD de % CPU do whisper-cli quando ativado via env
#   WHISPER_CPU_PERCENT (ex: =150 = max 1.5 cores). Default desabilitado
#   (usa só `nice -n 15` que é nativo do kernel).
RUN apt-get update && apt-get install -y --no-install-recommends gosu cpulimit \
    && rm -rf /var/lib/apt/lists/*

# Entrypoint que conserta permissões do volume DEPOIS do mount e dropa
# pra `liberthia` antes de exec da JVM. Sem isso, qualquer volume montado em
# /app/data fica root:root e o user liberthia recebe AccessDeniedException
# tentando criar /app/data/voice-clips/YYYY-MM/ durante upload de clipes.
COPY deploy/backend-entrypoint.sh /usr/local/bin/backend-entrypoint.sh
RUN chmod +x /usr/local/bin/backend-entrypoint.sh

# NÃO faz USER liberthia aqui — o entrypoint precisa de root pra chown.
# Ele dropa pra liberthia via `gosu` no final, antes do exec java.

COPY --from=build --chown=liberthia:liberthia /app/build/libs/liberthia-admin-backend-*.jar app.jar

EXPOSE 8090

ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["/usr/local/bin/backend-entrypoint.sh"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8090/api/health || exit 1
