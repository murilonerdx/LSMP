# syntax=docker/dockerfile:1.6
# ===== Stage 1: build =====
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copia gradle wrapper primeiro pra cachear o download
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Agora o source
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# ===== Stage 2: runtime =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S liberthia && adduser -S liberthia -G liberthia

# yt-dlp + ffmpeg + whisper.cpp: extração de mp3 + transcrição automática
# de voice clips. Whisper.cpp é a alternativa nativa C++ ao OpenAI Whisper —
# ~50MB binary + modelo, sem dependência do PyTorch (que adicionaria ~2GB).
#
# Modelo padrão: ggml-base (~150MB) — equilíbrio aceitável pt-BR.
# Override via env WHISPER_MODEL=tiny|base|small|medium.
RUN apk add --no-cache python3 py3-pip ffmpeg ca-certificates curl bash \
       git build-base cmake \
    && pip install --no-cache-dir --break-system-packages \
       --upgrade yt-dlp[default] \
    && ln -sf /usr/bin/python3 /usr/local/bin/python \
    && yt-dlp --version

# whisper.cpp — clona, builda e baixa modelo large-v3-turbo (~1.5GB).
#
# Por que large-v3-turbo (e não large-v3 ou medium)?
#  - large-v3 (3GB)        → melhor qualidade, MAS 2× mais devagar
#  - large-v3-turbo (1.5GB) → quase mesma qualidade do large-v3, 8× mais rápido
#  - medium (1.5GB)        → boa qualidade, mas pt-BR é fraco vs turbo
#  - base (150MB)          → o que usávamos. Pt-BR ruim, perdia palavras.
# Pra pt-BR informal de servidor de Minecraft, turbo é o sweet spot.
#
# Build estático (BUILD_SHARED_LIBS=OFF) — sem isso, whisper-cli depende de
# libwhisper.so.1 + libggml.so.0 que não copiamos pro runtime (deu erro
# "Error loading shared library libwhisper.so.1" em v37).
#
# ARGs configuráveis pra trocar modelo via build-arg sem editar Dockerfile.
ARG WHISPER_MODEL=large-v3-turbo
ENV WHISPER_MODEL_NAME=${WHISPER_MODEL}
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
    && ldd /usr/local/bin/whisper-cli 2>&1 | head -10 || true \
    && /usr/local/bin/whisper-cli --help 2>&1 | head -3 || true

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

# Pré-cria diretórios de storage com owner correto. Sem isso, quando o volume
# Docker monta vazio em /app/data/*, ele fica root:root e o user liberthia
# não consegue escrever — daí o PUT /api/mod/voice/clips/{id}/audio retorna 500.
RUN mkdir -p /app/data/voice-clips /app/data/photos /app/data/wardrobe /app/data/etched-audio /app/data/videos /app/data/tester-packages /app/data/tester-models /app/data/tester-audios \
    && chown -R liberthia:liberthia /app

# Pacote `su-exec` é o equivalente Alpine do `gosu` — permite o entrypoint
# rodar como root primeiro (pra fazer chown em volumes montados), e DEPOIS
# trocar pra user liberthia antes de exec o java. Sem isso, voice-clips
# falha com AccessDenied quando o volume monta como root:root.
RUN apk add --no-cache su-exec

# Entrypoint script — copia em camada própria pra não invalidar cache acima.
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# NOTA: USER directive REMOVIDA. O entrypoint roda como root, faz chown
# nos diretórios de dados que vieram do volume, e DEPOIS `su-exec liberthia
# java ...`. Mais seguro do que rodar tudo como root, e resolve o problema
# de volume mountado com owner errado.

EXPOSE 8090
# JVM heap aumentado de 512m → 1G — o backend agora processa whisper em
# scheduler, e o JFM precisa de mais memória pra buffers de áudio + connection
# pool. Usuário tem 32GB RAM disponível, então 1G é confortável.
# O whisper-cli roda como processo SEPARADO (não dentro da JVM), e usa ~2-3GB
# em pico com large-v3-turbo. Total backend ≈ 4GB RAM.
ENV JAVA_OPTS="-Xms512m -Xmx1g"
ENV LIBERTHIA_PASSWORD=changeme
ENV MOD_URL=http://host.docker.internal:25580
ENV MOD_TOKEN=

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s \
    CMD wget -q -O- http://localhost:8090/api/health || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
