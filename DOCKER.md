# Liberthia Admin Panel — Docker Guide

Stack completo via Docker: **backend Spring Boot** + **frontend nginx** + **PostgreSQL**.

O mod (servidor Minecraft) roda na sua máquina — Docker apenas hospeda a interface web e a API que conversa com o mod.

---

## 📋 Pré-requisitos

- Docker + Docker Compose v2 (`docker compose` — não `docker-compose`)
- Conta no [Docker Hub](https://hub.docker.com/) (pra `docker push`)
- Servidor MC com mod Liberthia rodando, com a porta `25580` acessível da máquina/VPS onde os containers vão rodar

---

## 🚀 Setup rápido

### 1. Clone + configure

```bash
git clone <repo-url> liberthia_mod
cd liberthia_mod
cp .env.example .env
```

Edite `.env` com os valores corretos:

```env
# Senha do painel (TROQUE)
LIBERTHIA_PASSWORD=minhasenhasecreta

# Porta do frontend nginx (diferente de 80/8080)
LIBERTHIA_FRONTEND_PORT=8888

# Conexão com o mod (servidor MC)
# Pra MC rodando na mesma máquina:
MOD_HOST=host.docker.internal
MOD_PORT=25580
# Pra MC em outra máquina/VPS:
# MOD_HOST=ip-publico-do-mc.com  ou  IP fixo

# Token do mod (pega via /liberthia admin link no chat in-game,
# ou lê de world/serverconfig/liberthia_admin_url.txt)
MOD_TOKEN=cole-o-token-do-mod-aqui

# Postgres (deixe os defaults pra dev)
POSTGRES_USER=liberthia
POSTGRES_PASSWORD=liberthia
POSTGRES_DB=liberthia
```

### 2. Build + sobe

```bash
docker compose up -d --build
```

Aguarde alguns segundos (Spring Boot leva uns 20–30s pra iniciar). Verifica:

```bash
docker compose ps
docker compose logs -f backend  # acompanha logs do backend
```

### 3. Acessa

- **Frontend (painel admin)**: http://localhost:8888
- **Backend API**: http://localhost:8090 (use só pra debug — frontend já proxia)
- **Postgres**: localhost:5432 (cliente externo)

Senha: a que você definiu em `LIBERTHIA_PASSWORD`.

---

## 🐳 Build + Push das imagens (Docker Hub)

Substitua `seu-usuario-dockerhub` pelo seu username:

```bash
# Login
docker login

# Build com tag
docker build -t seu-usuario-dockerhub/liberthia-backend:latest ./backend
docker build -t seu-usuario-dockerhub/liberthia-frontend:latest ./frontend

# Push pro Docker Hub
docker push seu-usuario-dockerhub/liberthia-backend:latest
docker push seu-usuario-dockerhub/liberthia-frontend:latest
```

### Multi-arch (opcional — pra ARM/Raspberry)

```bash
docker buildx create --use --name liberthia-builder
docker buildx build --platform linux/amd64,linux/arm64 \
  -t seu-usuario-dockerhub/liberthia-backend:latest \
  --push ./backend
docker buildx build --platform linux/amd64,linux/arm64 \
  -t seu-usuario-dockerhub/liberthia-frontend:latest \
  --push ./frontend
```

### Versionamento

Use tags semânticas:

```bash
# Major release
docker build -t seu-usuario-dockerhub/liberthia-backend:1.0.0 ./backend
docker build -t seu-usuario-dockerhub/liberthia-backend:1.0 ./backend
docker build -t seu-usuario-dockerhub/liberthia-backend:latest ./backend
docker push seu-usuario-dockerhub/liberthia-backend --all-tags
```

---

## 🌐 Deploy em VPS (backend remoto)

Pra usar o painel hospedado em VPS com o MC rodando em casa:

### Setup VPS

```bash
ssh user@vps
git clone <repo> liberthia
cd liberthia
cp .env.example .env
# Edita .env com:
#   MOD_HOST=ip-publico-da-sua-casa  (ou DDNS)
#   MOD_TOKEN=token-do-seu-mod
#   LIBERTHIA_PASSWORD=senha-forte
nano .env
docker compose up -d --build
```

### Roteador de casa

Forwarda porta `25580` TCP do MC pra dentro da rede, restrito ao IP da VPS:

- Roteador admin → Port Forwarding
- `Externa: 25580 TCP → Interna: 192.168.x.y:25580 (PC do MC)`
- IP de origem permitido: **só o IP da VPS**

### Firewall do MC server

```bash
# Linux iptables (exemplo)
sudo iptables -A INPUT -p tcp -s VPS_IP --dport 25580 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 25580 -j DROP
```

---

## 🔐 HTTPS / TLS (produção)

O nginx do frontend serve HTTP simples. Pra HTTPS, **coloca um reverse proxy na frente**:

### Opção 1: Caddy (mais simples, auto-renova certs)

`Caddyfile` na VPS:

```caddy
painel.meudominio.com {
    reverse_proxy localhost:8888
}
```

Sobe o Caddy + edita o compose pra `LIBERTHIA_FRONTEND_PORT=8888` (não exposto pro mundo).

### Opção 2: Cloudflare Tunnel

```bash
cloudflared tunnel create liberthia
cloudflared tunnel route dns liberthia painel.meudominio.com
cat > ~/.cloudflared/config.yml <<EOF
tunnel: <id>
credentials-file: ~/.cloudflared/<id>.json
ingress:
  - hostname: painel.meudominio.com
    service: http://localhost:8888
  - service: http_status:404
EOF
cloudflared tunnel run liberthia
```

### Opção 3: Nginx host + Let's Encrypt

```nginx
server {
  listen 443 ssl;
  server_name painel.meudominio.com;
  ssl_certificate /etc/letsencrypt/.../fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/.../privkey.pem;

  location / {
    proxy_pass http://localhost:8888;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
  }
}
```

---

## 🧱 Arquitetura

```
                  Internet
                     │
              [Frontend nginx :8888]
                     │
        ┌────────────┴────────────┐
        │   /api  →               │
        │   /ws   →               │
        ▼                         │
[Backend Spring :8090] ◄──────────┘
        │
        │ HTTP + X-Liberthia-Token
        ▼
[Servidor MC :25580]
  (Mod Liberthia)
```

- **Frontend** serve assets estáticos + proxia `/api` e `/ws` pro backend
- **Backend** valida o token web, faz CORS + WS broadcast, e proxia comandos pro mod via HTTP
- **Mod** tem servidor HTTP embarcado que executa comandos no servidor MC
- **Postgres** ainda não é usado pelo código mas tá pronto pra futuras integrações (histórico, analytics)

---

## 🔧 Comandos úteis

```bash
# Sobe / desce
docker compose up -d --build       # build + sobe
docker compose down                # para tudo
docker compose down -v             # para tudo + apaga volumes (postgres data!)
docker compose restart backend     # reinicia só backend

# Logs
docker compose logs -f             # todos serviços
docker compose logs -f backend     # só backend
docker compose logs --tail=100 backend

# Status
docker compose ps
docker compose top

# Atualizar (após git pull)
docker compose down
docker compose up -d --build

# Limpar imagens antigas
docker image prune -a

# Acessar shell do container
docker compose exec backend sh
docker compose exec postgres psql -U liberthia

# Backup postgres
docker compose exec postgres pg_dump -U liberthia liberthia > backup.sql

# Volume sizes
docker system df -v
```

---

## 🐛 Troubleshooting

### "Mod offline" no painel

- Backend não consegue conectar no mod. Verifica:
  - `MOD_URL` no `.env` aponta pro IP/host correto
  - Porta `25580` acessível da máquina dos containers
  - Mod rodando e mensagem `[AdminAPI] HTTP server listening` no log do MC
  - Firewall não tá bloqueando

```bash
# De dentro do container backend, testa:
docker compose exec backend wget -O- http://host.docker.internal:25580/health
```

### Frontend não carrega (502/504)

- Backend pode estar caído. `docker compose logs backend`
- Se o backend tá saudável mas frontend não chega nele, verifica nginx.conf

### Upload falha com "Failed to parse multipart"

- Já configurado pra 100MB. Se ainda falhar, aumenta em `backend/src/main/resources/application.yml`:
  ```yaml
  spring:
    servlet:
      multipart:
        max-file-size: 500MB
        max-request-size: 500MB
  ```

### Postgres não inicia

- Verifica permissões: `chown -R 999:999 ./postgres_data` (Linux)
- Ou apaga o volume: `docker compose down -v && docker compose up -d`

### CORS errors no browser

- Backend já tem CORS `*` permitido. Se ainda dá erro, verifica que a request passa pelo nginx do frontend (não direto pro backend).

### WebSocket "failed"

- Browser bloqueia WS em context inseguro acessado por IP remoto. Use HTTPS.
- Verifica que o nginx tem o bloco `proxy_set_header Upgrade $http_upgrade` (já tá no nginx.conf default).

---

## 📦 Arquivos relevantes

| Arquivo | Função |
|---|---|
| `docker-compose.yml` | Define os 3 serviços |
| `.env.example` | Template de configuração |
| `backend/Dockerfile` | Multi-stage Gradle build → JRE Alpine |
| `frontend/Dockerfile` | Node build → nginx Alpine |
| `frontend/nginx.conf` | Proxy `/api` + `/ws` + serve SPA + gzip |
| `backend/src/main/resources/application.yml` | Spring config (port, multipart, CORS) |
