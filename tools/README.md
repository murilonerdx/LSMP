# Liberthia GUI/Block Studio

Editor visual completo pra criar **blocos + GUIs + Block Entities** sem abrir
Blockbench, GIMP, ou ficar editando Java na mão.

```bash
python tools/gen_gui_visual.py
```

## O que ele faz

1. **Selector de pasta do mod** (topo) — escolhe a raíz do mod, auto-detecta
   `ModBlocks.java`, `ModItems.java`, `ModBlockEntities.java`, `ModMenuTypes.java`,
   `ClientModEvents.java`, `ModCreativeTabs.java`, `lang/*.json`.
2. **Tipo de máquina** (radio buttons):
   - **Sem Energia** — só inventário
   - **Fornalha** — combustível + input → output
   - **Crafting** — múltiplos inputs → output instantâneo
   - **Transformação** — input + FE + tempo → output (mais comum)
   - **Gerador** — fuel → produz FE
3. **Configuração visual:**
   - Nome (auto-deriva snake_case, BE class, ModBlocks const)
   - Dimensões da GUI
   - FE capacity / FE por tick / Ticks por processo
   - Tema com **4 cores ARGB** clicáveis (transparência incluída)
   - Textura base do bloco (minecraft:block/iron_block, etc.)
4. **Canvas click-and-drag:**
   - Click + arrasta slot/barra/seta pra mover
   - Click direito pra deletar elemento
   - Click esquerdo seleciona → painel direito mostra propriedades
   - Color picker por slot individual
5. **Geração com auto-patch:**
   - Botão `⚙ Generate + AutoPatch` (canto inferior direito)
   - Cria 7 arquivos: `<Name>Menu.java`, `<Name>Screen.java`,
     `<Name>BlockEntity.java`, `<Name>Block.java`, blockstate JSON,
     block model JSON, item model JSON
   - **Insere automaticamente** as linhas de registro em:
     - `ModBlocks.java` (DeferredRegister entry)
     - `ModItems.java` (BlockItem entry)
     - `ModBlockEntities.java` (BE type entry)
     - `ModMenuTypes.java` (MenuType entry)
     - `ModCreativeTabs.java` (creative tab output)
     - `ClientModEvents.java` (MenuScreens.register)
     - `lang/pt_br.json` e `lang/en_us.json`

## Como usar — passo a passo

```
1. python tools/gen_gui_visual.py
2. Topo: confirma a pasta do mod (ou click "..." pra escolher)
3. Esquerda: digita Nome="DustGrinder"
   → auto: snake=dust_grinder, BE=DustGrinderBlockEntity, const=DUST_GRINDER
4. Tipo de Máquina: marca "Transformação"
5. Aparência: textura "minecraft:block/cyan_concrete" (ou outra)
6. Tema: click nas swatches pra escolher cores
7. Canvas: click+arrasta os 2 slots iniciais pra onde quer
8. Toolbar: + Slot pra adicionar mais
9. Toolbar: ⚡ Energy ON / ➡ Progress ON conforme precisar
10. Click no slot → painel direito → renomear pra "DUST_INPUT", mudar cor
11. 🔍 Preview Code (mostra tudo que vai gerar) ← opcional
12. ⚙ Generate + AutoPatch → confirma backup → vai
13. Mensagem mostra os 7 arquivos criados + status do patch em cada arquivo
14. ./gradlew build pra compilar
```

## Atalhos de teclado

- `Ctrl+S` — salva config como JSON
- `Ctrl+G` — gera + auto-patch
- Click direito num elemento — deleta com confirmação

## Modos de máquina (templates de tick)

Cada tipo gera um `static void tick()` específico:

| Tipo | Slots gerados | Tick comportamento |
|------|---------------|---------------------|
| Sem Energia | INPUT, OUTPUT | Não tickeia |
| Fornalha | FUEL, INPUT, OUTPUT | TODO burn time + processo |
| Crafting | múltiplos | Match recipe + consume + insert |
| Transformação | INPUT, OUTPUT | FE/tick × progress, TODO da fórmula |
| Gerador | FUEL | Queima fuel, gera FE/tick |

Os `TODO` no template indicam onde implementar a lógica específica.

## Limitações

- **Não gera texturas PNG** do bloco — só referencia path. Pra textura
  customizada, edita no GIMP/aseprite e salva em
  `src/main/resources/assets/<modid>/textures/block/<snake>.png`
- **Não gera receitas** — você adiciona em `src/main/resources/data/<modid>/recipes/`
- O auto-patch é heurístico (regex). Pode falhar se seu arquivo tem layout
  exótico. Sempre faça commit antes de rodar Auto-patch pra poder reverter.

## Cores comuns do mod

```
Roxo escuro (DM):     0xFF1B0830
Roxo médio:           0xFF6B2A8C
Roxo claro/accent:    0xFFAA40E8
Lilás brilhante:      0xFFE0A0FF
Dourado (catalyst):   0xFFE0C040
Verde-DM (extract):   0xFF553090
Azul-éter:            0xFF40A0E0
Vermelho perigo:      0xFFE04040
Branco-WM:            0xFFE0E8FF
Amarelo-YM:           0xFFFFE040
Sangue:               0xFFB02020
Sagrado/holy:         0xFFFFD060
```

## Modo CLI alternativo

Se preferir editar JSON em editor de texto:

```bash
# Modo prompt-by-prompt
python tools/gen_gui.py --interactive

# Direto de JSON
python tools/gen_gui.py tools/example_gui.json
```

`gen_gui.py` é a versão sem auto-patch — só gera Menu+Screen e imprime
no terminal as linhas pra colar manualmente.
