# Source BlockBench Models

Pasta para armazenar os arquivos `.bbmodel` originais (formato nativo do BlockBench) usados pra editar models do mod.

## Workflow

1. Abre o `.json` correspondente no BlockBench:
   `src/main/resources/assets/liberthia/models/item/<nome>.json`

2. Edita à vontade (move cubos, ajusta UVs, adiciona elements, etc).

3. Pra salvar: `File` → `Save Model As...` → escolhe **"Both"**:
   - **`.bbmodel`** → salva NESTA pasta (`models-source/<nome>.bbmodel`)
     - Preserva groups, comentários, configurações de display, etc
     - **Não é lido pelo Minecraft** — só fica pra você editar depois
   - **`.json`** → sobreescreve em `src/main/resources/assets/liberthia/models/item/<nome>.json`
     - Esse é o que o Minecraft de fato carrega no jogo

4. Rebuilda o mod: `./gradlew build` (gera `build/libs/liberthia-X.X.X.jar`)

## Por que manter os dois?

- O `.json` é menor e otimizado pro Minecraft, mas perde metadados (groups, comentários, etc).
- O `.bbmodel` mantém tudo organizado pra futuras edições.

## Items atualmente editados

(adicione aqui conforme for salvando)

- `clear_matter_injector.bbmodel` — seringa 3D
- `yellow_matter_ingot.bbmodel` — lingote com bevel
- `speed_upgrade.bbmodel` — cristal vermelho
- `efficiency_upgrade.bbmodel` — engrenagem azul
- `capacity_upgrade.bbmodel` — baú com cristal amber
