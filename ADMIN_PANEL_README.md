# Liberthia Admin Panel — Setup Completo

Sistema 3-tier de administração via web pro mod Liberthia.

```
┌──────────────────┐    HTTP+WS      ┌────────────────────┐    HTTP+SSE     ┌─────────────────┐
│ React Frontend   │ ←─────────────→ │ Spring Boot Backend │ ←──────────────→ │ Mod (Forge)      │
│  (porta 5173)    │                 │  (porta 8080)        │                 │  (porta 25580)   │
└──────────────────┘                 └────────────────────┘                 └─────────────────┘
```

## Setup rápido (3 terminais)

### 1️⃣ Mod (servidor MC)

Builda e copia o jar pra `mods/`:

```bash
cd liberthia_mod
./gradlew build
cp build/libs/liberthia-0.1.1.jar /caminho/pro/server/mods/
```

Roda o servidor. **Quando entrar no jogo como op**, executa em chat:

```
/liberthia admin link
```

Vai aparecer:

```
=== Liberthia Admin Panel ===

Mod URL (local):  http://192.168.0.10:25580   ← clicável
Token: abc123-xyz789  ← clica pra copiar

Cole essas infos no backend/src/main/resources/application.yml:
  mod.url:   http://192.168.0.10:25580
  mod.token: abc123-xyz789

→ Painel (dev): http://192.168.0.10:5173
```

Comandos disponíveis:
- `/liberthia admin link` — mostra URL completa + token (clicável)
- `/liberthia admin token` — só o token (clicável pra copiar)
- `/liberthia admin status` — verifica se a API tá ligada

OPs também recebem dica automática `Use /liberthia admin link` ao entrar no servidor.

### 2️⃣ Backend (Spring Boot)

```bash
cd liberthia_mod/backend
# Cola token no application.yml
nano src/main/resources/application.yml
# Vai ficar tipo:
# mod:
#   url: http://192.168.0.10:25580
#   token: abc123-xyz789

./gradlew bootRun
```

Sobe em `http://localhost:8080`. Faz proxy + cache + WebSocket fan-out.

### 3️⃣ Frontend (React + Vite)

```bash
cd liberthia_mod/frontend
npm install
npm run dev
```

Abre `http://localhost:5173` no browser.

---

## Endpoints (referência)

| Método | Path | Descrição |
|---|---|---|
| GET | `/api/server/info` | MOTD, TPS, dimensões, players online |
| GET | `/api/players` | Lista de players online |
| GET | `/api/player/{uuid}/inventory` | Inventário (main+armor+offhand) |
| POST | `/api/player/{uuid}/give` | `{item, count, enchantments[]}` |
| POST | `/api/player/{uuid}/remove` | `{slot, count}` |
| POST | `/api/player/{uuid}/clear` | Limpar inventário |
| POST | `/api/player/{uuid}/effect` | `{effect, duration, amplifier}` |
| POST | `/api/player/{uuid}/teleport` | `{x, y, z, dimension?}` |
| POST | `/api/player/{uuid}/kick` | `{reason}` |
| GET | `/api/matter/{uuid}` | DM/WM/YM do player |
| POST | `/api/matter/{uuid}` | `{dm?, wm?, ym?}` |
| GET | `/api/items` | Lista todos items registrados (~1800) |
| GET | `/api/enchantments` | Lista todos encantamentos |
| POST | `/api/command` | `{command}` — executa em op level 4 |
| GET | `/api/events/sse` | Server-Sent Events stream |

## Test rápido sem frontend (curl)

```bash
# Saúde
curl http://server:25580/health

# Listar players (precisa do token)
curl -H "X-Liberthia-Token: SEU_TOKEN" http://server:25580/api/players

# Dar diamante encantado
curl -H "X-Liberthia-Token: SEU_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"item":"minecraft:diamond_sword","count":1,"enchantments":[{"id":"minecraft:sharpness","level":5}]}' \
     http://server:25580/api/player/UUID/give
```

## Auth & Segurança

- Mod ↔ Backend: token compartilhado em `X-Liberthia-Token` header
- Backend ↔ Frontend: **sem auth** (decisão de design — frontend assume rede privada)
- **NÃO exponha** o frontend pra internet pública sem pôr autenticação na frente
- Token é gerado no primeiro start e salvo em `world/serverconfig/liberthia-server.toml`

## Gerenciamento de token sem editar compose (`/mod-config`)

Histórico: o token do `docker-compose.env` (MOD_TOKEN) e o gerado pelo mod em `world/serverconfig/liberthia-server.toml` viviam divergindo. A página `/mod-config` (link "🔑 Mod Token" no menu admin) resolve isso sem rebuild.

### Fluxo recomendado

1. **No servidor MC** — pega o token do mod:
   - Linha do log no start: `[AdminAPI] full token (compartilha com o backend): xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
   - Ou abre `world/serverconfig/liberthia_admin_url.txt` (gerado automaticamente, regenerado a cada start)
2. **No painel admin** — abre `/mod-config` → cola o token no Card 2 → **Salvar** → **Confirmar**.
3. Backend grava em `backend_config.MOD_TOKEN`, ativa **lock automático** em `backend_config.MOD_TOKEN_LOCKED`, reconstrói o `WebClient` — próximas chamadas já usam o token novo.
4. **Card 3 → 🔌 Testar agora** — confirma TPS/players/MOTD reais (pula cache + circuit breaker).

### Lock manual — pra token NÃO mudar sozinho

Salvar pelo painel ATIVA automaticamente o lock. Significa:

- O auto-register do mod (POST `/api/mod/register` a cada 60 s) **não consegue mais sobrescrever** o token.
- Backend loga: `[ModRegistry] mod tentou registrar token novo mas LOCK manual está ativo — IGNORANDO`.
- Lock sobrevive restart do backend (persistido no DB).

Pra liberar (voltar pro modo "mod é fonte de verdade"): no Card 1, clica **🔓 Destravar** (ou `POST /api/admin/mod-config/unlock`).

### Endpoints

| Método | Path | Descrição |
|---|---|---|
| GET | `/api/admin/mod-config/token` | Retorna `{tokenFingerprint, source: DB|ENV, manualLock, lastUpdatedAt, lastRegisteredAt, currentMatchesRegistered, hasOverride}` |
| POST | `/api/admin/mod-config/token` | Body `{token: "uuid"}` — salva no DB + ativa lock + rebuild WebClient |
| POST | `/api/admin/mod-config/test-connection` | Faz call real `/api/server/info` no mod, retorna TPS/players ou erro |
| POST | `/api/admin/mod-config/unlock` | Desativa lock, deixa auto-register voltar a sobrescrever |

### Quando usar cada modo

| Cenário | Recomendação |
|---|---|
| Servidor MC fixo, ambiente estável | Lock **ativado** (salva uma vez, esquece) |
| Trocando servidor MC com frequência, dev | Lock **desligado** (mod re-registra e backend acompanha) |
| Mod gerou token novo e tudo quebrou | Pega token novo do log do MC → cola no painel → salva (re-trava) |

## Troubleshooting

**Backend não conecta no mod**
- Confirma que mod tá rodando: `curl http://IP:25580/health`
- Token tá certo no `application.yml`?
- Bind address do mod é `0.0.0.0` (config) pra aceitar conexões remotas?
- Firewall do servidor MC libera porta 25580?

**Frontend mostra "WS off"**
- Backend tá rodando?
- Console do browser tem erro de WebSocket?
- Se backend tá em `localhost:8080`, vite proxy direciona OK

**Comando `/liberthia admin link` não existe**
- Você precisa ser op (permissão 2+)
- Mod buildado e tá no `mods/` do servidor

**Token apagado / não lembro**
- Roda `/liberthia admin token` no chat
- Ou olha o arquivo `world/serverconfig/liberthia-server.toml` (chave `admin_api.token`)
- Ou abre `world/serverconfig/liberthia_admin_url.txt` (regenerado a cada start)

**Backend mostra `AUTH FAIL` constante em `/api/players` mesmo após salvar token**
- O `WebClient` cacheado pode estar usando o token antigo — abre `/mod-config` → Card 3 → "🔌 Testar agora" pra forçar rebuild
- Se o token do DB difere do que o mod usa: pega o atual do `liberthia_admin_url.txt`, cola no painel, salva → ativa lock automático
- Se quer aceitar o que o mod manda: clica **🔓 Destravar** no Card 1 e espera o próximo auto-register (≤ 60 s)

**Token "muda sozinho" depois de eu salvar manualmente**
- Era bug clássico (mod re-registrava sobrescrevendo). Fix em v0.1.44: salvar pelo painel ativa lock automático. Confirma no Card 1 que aparece **🔒 travado**.

## Build prod

```bash
# Mod: jar normal
cd liberthia_mod && ./gradlew build

# Backend
cd backend && ./gradlew bootJar
java -jar build/libs/liberthia-admin-backend-0.1.0.jar

# Frontend
cd frontend && npm run build
# serve dist/ com nginx/apache
```
