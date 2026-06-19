# syntax=docker/dockerfile:1.6
# ===== Stage 1: build =====
FROM node:20-alpine AS build
WORKDIR /app

# Cache deps
COPY package.json package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

# Source — tailwind.config é .js (não .ts), postcss.config.js, vite.config.ts
COPY tsconfig.json vite.config.ts tailwind.config.js postcss.config.js index.html ./
COPY src ./src
COPY public ./public

RUN npm run build

# ===== Stage 2: nginx =====
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8999
HEALTHCHECK --interval=30s --timeout=5s \
    CMD wget -q -O- http://localhost:8999/healthz > /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
