# Protocolo de Atualização do Mod — Lembrete pra IA

**Objetivo**: toda vez que adicionar/remover/modificar items, blocos, features ou recipes no mod Liberthia, **a IA (Claude) deve gerar ou atualizar 4 JSONs** que ficam nesta pasta e são **importáveis no painel admin via "Importar JSON"**.

## Arquivos canônicos (sempre em `tools/`)

| Arquivo | Painel admin destino | Botão |
|---|---|---|
| `beta-items-update.json` | `/tester-admin` → Items beta | "Importar JSON" |
| `rewards-update.json` | `/tester-admin` → Rewards | "Importar JSON" |
| `changelog-update.json` | `/tester-admin` → Changelog | "Importar JSON" |
| `roadmap-update.json` | `/tester-admin` → Roadmap | "Importar JSON" |

## Quando atualizar

A IA deve atualizar o(s) arquivo(s) correspondente(s) **na MESMA mensagem** que:

- Adiciona/remove item em `ModItems.java`
- Adiciona/remove bloco em `ModBlocks.java`
- Cria/altera receita em `data/liberthia/recipes/`
- Implementa feature nova (evento, mecânica, sistema)
- Adiciona item-lore (drop, ritual, ore, mob)

## Formato dos JSONs (canonical schema)

### `beta-items-update.json`

```json
{
  "version": "0.1.12",
  "generatedAt": "2026-05-19T18:00:00Z",
  "items": [
    {
      "name": "Matter Cure",
      "itemId": "liberthia:matter_cure",
      "kind": "ITEM",
      "category": "consumable",
      "description": "Cura emergencial: zera DM/WM/YM do player + Regen II + Resistance I por 10s.",
      "lore": "Quando o corpo já não responde aos remédios convencionais...",
      "giveCommand": "/give @p liberthia:matter_cure 1",
      "propertiesJson": "{\"stack\":8,\"useTime\":32}",
      "effectsJson": "[{\"effect\":\"regeneration\",\"duration\":200,\"amplifier\":1}]",
      "recipeJson": "{\"type\":\"shaped\",\"pattern\":[\" G \",\"PEP\",\" B \"],\"key\":{\"G\":\"minecraft:glowstone_dust\",\"P\":\"liberthia:purified_essence\",\"E\":\"liberthia:clear_matter_pill\",\"B\":\"minecraft:glass_bottle\"},\"result\":{\"item\":\"liberthia:matter_cure\",\"count\":1}}",
      "imageUrl": "",
      "enabled": true
    }
  ]
}
```

**Campos obrigatórios**: `name`
**Campos opcionais**: todos os outros (chave natural: `itemId`, fallback `name`)

**Valores de `kind`**: `ITEM` | `BLOCK` | `FEATURE` | `ARTIFACT`

### `rewards-update.json`

```json
{
  "version": "0.1.12",
  "generatedAt": "2026-05-19T18:00:00Z",
  "rewards": [
    {
      "name": "Yellow Matter Ingot x4",
      "description": "4 ingots de Yellow Matter — pra craftar armadura completa.",
      "imageUrl": "",
      "category": "item",
      "costPoints": 250,
      "giveCommand": "/give {player} liberthia:yellow_matter_ingot 4",
      "perTesterLimit": 0,
      "enabled": true
    }
  ]
}
```

**Substituições no `giveCommand`**: `{player}` → nome do tester ao resgatar.
**Chave natural**: `name`

### `changelog-update.json`

> Schema REAL do backend — usa campos divididos por seção (`itemsAdded`, `bugsFixed`,
> `buffs`, `debuffs`, `integrations`, `credits`, `notes`), não um único `body`.

```json
{
  "version": "0.1.12",
  "generatedAt": "2026-05-19T18:00:00Z",
  "entries": [
    {
      "version": "0.1.12",
      "title": "v0.1.12 — Bulk import + 3D viewer fix",
      "releaseDate": "2026-05-19T18:00:00Z",
      "summary": "Painéis admin agora aceitam JSONs via Importar; viewer 3D processa groups corretamente.",
      "itemsAdded": "- Matter Cure\n- Daily Cure Pill\n- Body Armor Upgrade Tier 2",
      "bugsFixed": "- ItemAutocomplete não propagava IDs custom\n- Viewer 3D ignorava hierarquia outliner",
      "buffs": "- Yellow Matter Sword: +2 dmg",
      "debuffs": "- Singularity Core: cooldown 10s → 30s",
      "integrations": "- Suporte preliminar a Numismatics",
      "credits": "- Bug do viewer 3D reportado por @murilonerdx",
      "notes": "Backup do mundo recomendado antes de atualizar.",
      "highlighted": false
    }
  ]
}
```

**Campos obrigatórios**: `version`, `title`
**Chave natural**: `title`

### `roadmap-update.json`

> Schema REAL do backend — `category` é enum específico, não status livre.

```json
{
  "version": "0.1.12",
  "generatedAt": "2026-05-19T18:00:00Z",
  "items": [
    {
      "title": "Matter Analyzer GUI completa",
      "description": "Tabs internos: análise / curas / histórico de exposição",
      "category": "PLANNED",
      "emoji": "🔬",
      "tag": "tool",
      "priority": 10,
      "targetVersion": "0.2.0"
    }
  ]
}
```

**Valores de `category`**: `IDEA` | `PLANNED` | `IN_DEV` | `NEXT` | `DONE` | `CANCELLED`
**Campos obrigatórios**: `title`
**Chave natural**: `title`
**`priority`**: integer (maior = aparece em cima dentro da categoria)

## Workflow pra IA (lembrar SEMPRE)

1. Quando adicionar item/bloco/feature/recipe:
   - **Edite o(s) JSON correspondente(s)** em `tools/` (não cria duplicata, atualiza o existente)
   - **Adicione o item novo** à lista preservando os anteriores
   - **Incremente `version`** pra match `gradle.properties`
   - **Atualize `generatedAt`** pra ISO timestamp atual

2. Mencione na resposta:
   - "Atualizei `tools/beta-items-update.json` com o item novo"
   - Lista os itens adicionados/removidos no JSON

3. Pra rodar atualização automática quando muito coisa mudou:
   - `python tools/generate-items-update.py` regenera o JSON do estado completo
   - Útil quando perdeu o histórico ou começou novo

## Como usar (do lado do usuário)

1. Baixa o JSON da pasta `tools/`
2. Acessa `/tester-admin` no painel
3. Vai na aba correspondente (Items Beta / Rewards / Changelog / Roadmap)
4. Clica em **"Importar JSON"**
5. Cola/upload o JSON
6. Confirma → backend faz `upsert` (cria ou atualiza por nome/título)

## Comportamento de upsert

- Backend deduplica por `itemId` (items), `name` (rewards), `title` (changelog), `title` (roadmap)
- Items existentes com mesma chave são **atualizados** (não duplica)
- Items removidos do JSON **NÃO são deletados** do banco (pra não perder histórico) — admin desabilita manualmente se quiser

## Versionamento

- Cada JSON tem `version` que deve match `gradle.properties: mod_version`
- Mantém histórico no banco com timestamp de quando foi importado

## Endpoints backend (referência)

| Método | Path | O que faz |
|---|---|---|
| POST | `/api/admin/tester/beta-items/bulk-import` | upsert lista de items beta |
| POST | `/api/admin/tester/rewards/bulk-import` | upsert lista de rewards |
| POST | `/api/admin/tester/changelog/bulk-import` | upsert lista de entradas |
| POST | `/api/admin/tester/roadmap/bulk-import` | upsert lista de roadmap items |

**Body**: o JSON inteiro do arquivo (com chaves `items`/`rewards`/`entries`).
**Resposta**: `{ok, created, updated, errors, errorMessages[]}`
