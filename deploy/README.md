# Liberthia Admin — Deploy

Stack completa pra rodar o painel de admin do Liberthia em Docker Swarm (ou Compose).

```
┌──────────────────┐
│  Mod (Forge)     │  porta 25580 (HTTP API + SSE)
│  rodando no host │
└────────┬─────────┘
         │ HTTP X-Liberthia-Token
         ▼
┌──────────────────────────────────────────────────────────────────┐
│  Docker Swarm Stack                                              │
│                                                                  │
│  ┌──────────────┐  ┌──────────────────┐  ┌────────────────────┐  │
│  │  PostgreSQL  │  │  Backend (Java)  │  │  Frontend (nginx)  │  │
│  │  port 5432   │←─│  port 8090       │←─│  port 5173 → 80    │  │
│  │              │  │  • engines       │  │  • SPA React       │  │
│  │  • configs   │  │  • REST API      │  │  • proxy /api /ws  │  │
│  │  • snapshots │  │  • WebSocket /ws │  │                    │  │
│  │  • chat log  │  │  • SSE consumer  │  │                    │  │
│  │  • map cache │  │                  │  │                    │  │
│  └──────────────┘  └──────────────────┘  └────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

## Pré-requisitos

- Docker 24+ e Docker Compose v2 (ou Swarm)
- Servidor Minecraft com o mod Liberthia rodando em algum host alcançável

## Início rápido — Compose local (sem swarm)

```bash
cd deploy
# (opcional) ajusta variáveis em .env
cat > .env <<EOF
LIBERTHIA_PASSWORD=minhasenhasegura
LIBERTHIA_SECRET=$(openssl rand -hex 32)
MOD_URL=http://host.docker.internal:25580
MOD_TOKEN=cole-aqui-o-token-do-mod
POSTGRES_PASSWORD=postgres-strong-password
EOF

docker compose -f docker-stack.yml up --build
```

Acesse o painel em http://localhost:5173

## Deploy em Swarm

```bash
# Inicializa o swarm (1 vez só)
docker swarm init

# Builda as imagens locais
docker build -t liberthia/backend:latest -f deploy/backend.Dockerfile .
docker build -t liberthia/frontend:latest -f deploy/frontend.Dockerfile .

# Deploy do stack
docker stack deploy -c deploy/docker-stack.yml liberthia

# Status
docker stack services liberthia
docker stack ps liberthia

# Logs
docker service logs -f liberthia_backend
docker service logs -f liberthia_frontend

# Update (após rebuild da imagem)
docker service update --image liberthia/backend:latest liberthia_backend

# Remover stack
docker stack rm liberthia
```

## Variáveis de ambiente

| Variável | Default | Descrição |
|---|---|---|
| `LIBERTHIA_PASSWORD` | `liberthia2026` | Senha do painel web |
| `LIBERTHIA_SECRET` | random | Secret JWT (gere com `openssl rand -hex 32`) |
| `MOD_URL` | `http://host.docker.internal:25580` | URL HTTP do mod Forge |
| `MOD_TOKEN` | `liberthia-dev-token` | Token X-Liberthia-Token configurado no `liberthia-server.toml` |
| `POSTGRES_PASSWORD` | `liberthia_pw` | Senha do banco PostgreSQL |
| `PGADMIN_PASSWORD` | `liberthia_pw` | Senha do pgAdmin (acessar em http://localhost:5050) |

## Schema do banco

O arquivo `init.sql` é executado automaticamente na primeira inicialização do container PostgreSQL via `/docker-entrypoint-initdb.d/`. Contém:

- **DDL** de todas as tabelas (`CREATE TABLE IF NOT EXISTS`)
- **Seed** de presets cosmic horror:
  - 5 entidades de possession (Yh'kuath, Vorashen, Mor'Ghaur, Last Pilgrim, Iss'thar)
  - 4 cursed items (Olho de Vorash, Coração Rachado, Coroa do Silêncio, Sino de Ossos)
  - 4 glifos pré-posicionados (Olho, Vazio, Sangue, Silêncio)
  - 2 dimensional rifts (Fenda do Vazio, Portão dos Perdidos)
  - 2 memory echoes (Círculo dos Esquecidos, Memória da Batalha)
  - 4 dias especiais no calendário cósmico
  - 3 cutscenes agendadas (Sussurro da Meia-Noite, Bênção da Aurora, Fantasma Aleatório)
  - 4 forbidden words rules (vorashen, corra, socorro, deus/divino)
  - 1 time-locked box exemplo (Caixa da Aurora)
  - Config do Madness Meter com mensagens, sons e FX

Pra rodar manualmente em DB existente:

```bash
psql "postgresql://liberthia:senha@host:5432/liberthia" -f deploy/init.sql
```

É **idempotente** — pode rodar várias vezes, usa `IF NOT EXISTS` e `ON CONFLICT DO NOTHING`.

## Backup do banco

```bash
# Backup
docker exec liberthia_postgres pg_dump -U liberthia liberthia > backup.sql

# Restore
docker exec -i liberthia_postgres psql -U liberthia liberthia < backup.sql
```

## Inspeção do banco (pgAdmin)

http://localhost:5050  
- email: `admin@liberthia.local`
- pwd: `${PGADMIN_PASSWORD}`

Add server:
- Host: `postgres`
- Port: `5432`
- Database: `liberthia`
- Username: `liberthia`
- Password: `${POSTGRES_PASSWORD}`

## Tabelas

### Engines (configs + state)
- `forbidden_rules`, `forbidden_invocations`
- `madness_config`, `madness_state`
- `possession_entities`, `possession_active`
- `cursed_items`, `cursed_bindings`
- `rifts`
- `glyphs`
- `time_boxes`
- `memory_echoes`, `memory_echoes_active`
- `calendar_config`, `calendar_special_days`
- `cutscenes`

### Histórico e dados de player
- `chat_log` — toda mensagem de chat
- `command_log` — todo comando executado
- `player_snapshots` — backup de inventário/posição (auto a cada 5min)
- `map_chunks` — cache PNG de chunks do mapa (BYTEA)

## Observações de produção

- **Backups regulares**: rodar `pg_dump` em cron, salvar fora do container
- **TLS**: colocar nginx/caddy/traefik na frente do `frontend` pra HTTPS
- **Secrets**: usar `docker secret` em vez de env vars com `${POSTGRES_PASSWORD}`
- **Backend replicas = 1**: as engines usam `@Scheduled` e cooldowns em memória. Múltiplas réplicas processariam o mesmo evento N vezes. Pra escalar: precisaria de coordenação (Redis lock, leader election, etc.)
- **Frontend escalável**: nginx é stateless, escala livremente
- **PostgreSQL pinned no manager**: postgres tem volume local, então fixa em um node específico no swarm. Pra HA: use postgres com replicação ou serviço gerenciado (Neon, RDS, etc.)

## Troubleshooting

**Backend não conecta no mod**:  
`MOD_URL=http://host.docker.internal:25580` funciona no Docker Desktop (Mac/Windows). No Linux puro, usa o IP da rede do host: `MOD_URL=http://172.17.0.1:25580` ou habilita host networking.

**Backend OOM**:  
Aumenta `JAVA_OPTS=-Xmx1g` e `resources.limits.memory: 1.5G`.

**Migration do banco**:  
JPA está com `ddl-auto: update` — adiciona colunas novas automaticamente. Mas pra mudanças destrutivas (drop column, rename), faça manualmente via `psql`.

**Reset total do banco**:
```bash
docker volume rm liberthia_postgres_data
docker stack deploy -c deploy/docker-stack.yml liberthia
```
