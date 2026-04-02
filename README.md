# Liberthia — Forge 1.20.1

Projeto inicial de mod para Minecraft **1.20.1** em **Java 17** usando **Forge 47.4.18**.

## O que já está implementado

- Matéria Escura como **fluido** com **balde**
- Blocos sólidos:
  - Matéria Escura Cristalizada
  - Matéria Clara
  - Matéria Amarela
- Sistema de **infecção persistente** via capability do player
- **HUD** com barra e estágio de infecção
- **overlay escuro** que piora conforme a infecção
- **penalidade permanente de vida máxima**
- **dano periódico** em níveis altos de infecção
- **injeção de Matéria Clara**
- **spawn de focos de Matéria Escura** no mundo do servidor
- **sons próprios** `.ogg`
- **cliente HTTP** para backend externo configurável por TOML
- documentação dos papéis/“agentes” em `AGENTS.md`

## Configuração do backend

O Forge gera o arquivo:

```toml
config/liberthia-server.toml
```

Os campos principais são:

```toml
[backend]
enabled = true
base_url = "https://seu-backend.com"
snapshot_path = "/api/v1/infection/snapshot"
connect_timeout_ms = 3000
request_timeout_ms = 5000
```

O mod envia snapshots assíncronos simples do estado do player infectado.

## Estrutura importante

- `src/main/java/.../capability` — persistência da infecção
- `src/main/java/.../event` — ticks, exposição, sincronização e spawn
- `src/main/java/.../network` — sync HUD cliente/servidor
- `src/main/java/.../backend` — integração HTTP
- `src/main/resources/assets/liberthia` — texturas, modelos, sons, lang
- `src/main/resources/data/liberthia` — receitas e loot tables

## Observação honesta

O container desta sessão não tem Gradle/MDK pronto nem acesso de build ao Maven, então eu **não consegui executar a compilação final aqui dentro**. Mesmo assim, a base foi escrita mirando a API Forge 1.20.1 e evitando chamadas obsoletas, como o construtor deprecated de `ResourceLocation`, substituído por `ResourceLocation.fromNamespaceAndPath(...)`.

## Próximo passo recomendado

1. importar o projeto na IDE com Java 17
2. rodar `gradlew genIntellijRuns` ou `gradlew genEclipseRuns`
3. ajustar balanceamento
4. adicionar worldgen por datapack/biome modifiers se quiser geração mais “natural”

## Pacote

Este zip inclui:
- código-fonte
- assets pixel art
- sons
- docs
- build scripts
