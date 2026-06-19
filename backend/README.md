# Liberthia Admin Backend

Spring Boot que faz proxy entre o mod Forge (HTTP local) e o frontend React.

## Setup

1. Edita `src/main/resources/application.yml`:
   - `mod.url` → onde o mod tá rodando (default `http://localhost:25580`)
   - `mod.token` → o token que aparece no log do mod no startup (também salvo em `liberthia-server.toml`)

2. Build & run:
   ```bash
   ./gradlew bootRun
   ```

   Sobe em `http://localhost:8080`.

## Endpoints proxiados

Todos os `/api/*` espelham o mod. Veja o plano principal em `.claude/plans/jolly-napping-fairy.md`.

## WebSocket

`ws://localhost:8080/ws` recebe broadcast de todos os eventos live do mod (login/logout/death/server_started).

## Run jar

```bash
./gradlew bootJar
java -jar build/libs/liberthia-admin-backend-0.1.0.jar
```
