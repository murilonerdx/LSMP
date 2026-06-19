# Multi-stage build do frontend (React + Vite → nginx)
# Stage 1: build com node
FROM node:20-alpine AS build
WORKDIR /app

# Copia package files primeiro (cache de npm install)
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --no-audit --no-fund

# Copia source + build
COPY frontend/ ./
RUN npm run build

# Stage 2: nginx serve
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf

# IMPORTANTE: nginx escuta em 8999 (não 80). VPS tem outros serviços na 80.
# Antes EXPOSE=80 + healthcheck em localhost:80 = healthcheck SEMPRE falhava,
# Docker Swarm marcava container como unhealthy e fazia restart loop.
EXPOSE 8999

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8999/healthz || exit 1
