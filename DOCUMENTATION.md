# LIBERTHIA MOD - Documentacao Completa

> **Versao:** v1 | **Minecraft:** 1.20.1 | **Forge:** 47.4.18 | **Java:** 17
> **Mod ID:** `liberthia` | **Autor:** Murilo (murilonerdx)

---

## Indice

1. [Visao Geral](#1-visao-geral)
2. [Arquitetura do Projeto](#2-arquitetura-do-projeto)
3. [Sistema de Infeccao](#3-sistema-de-infeccao)
4. [Blocos](#4-blocos)
5. [Itens](#5-itens)
6. [Entidades](#6-entidades)
7. [Efeitos (MobEffects)](#7-efeitos-mobeffects)
8. [Capabilities (Dados Persistentes)](#8-capabilities-dados-persistentes)
9. [Sistema de DNA e Mutacoes](#9-sistema-de-dna-e-mutacoes)
10. [Sistema de Materia (Escura, Clara, Amarela)](#10-sistema-de-materia-escura-clara-amarela)
11. [Maquinas e Bancadas](#11-maquinas-e-bancadas)
12. [Interface (HUD/Overlays)](#12-interface-hudoverlays)
13. [Sistema de Protecao](#13-sistema-de-protecao)
14. [Buracos Negros](#14-buracos-negros)
15. [World Generation](#15-world-generation)
16. [Configuracao](#16-configuracao)
17. [Rede e Sincronizacao](#17-rede-e-sincronizacao)
18. [Backend Externo](#18-backend-externo)
19. [Sons](#19-sons)
20. [Comandos](#20-comandos)
21. [Receitas](#21-receitas)
22. [Registro Completo de Arquivos](#22-registro-completo-de-arquivos)

---

## 1. Visao Geral

Liberthia e um mod de sobrevivencia anomala para Minecraft Forge 1.20.1. O jogador enfrenta uma infeccao de **Materia Escura** que corrompe o mundo, transforma mobs, altera o DNA do jogador e pode criar **buracos negros** se nao for contida.

O mod introduz tres tipos de materia:
- **Materia Escura** - A fonte da infeccao. Corrompe blocos, mobs e jogadores.
- **Materia Clara** - Agente de purificacao. Cura infeccao, restaura blocos.
- **Materia Amarela** - Barreira e estabilizacao. Bloqueia propagacao.

O jogador deve usar ferramentas, armaduras, maquinas e estrategia para sobreviver e conter a infeccao.

---

## 2. Arquitetura do Projeto

```
br.com.murilo.liberthia/
  LiberthiaMod.java              - Classe principal (@Mod)
  backend/                       - Cliente HTTP para backend externo
  block/                         - Blocos de maquinas + BlockEntities
  capability/                    - IInfectionData, IMatterEnergy + Providers
  client/                        - Overlays, HUD, renderers, telas
  command/                       - Comandos do servidor
  config/                        - LiberthiaConfig (Client + Server)
  effect/                        - Efeitos customizados (MobEffect)
  entity/                        - Entidades (mobs, projéteis, explosoes)
  event/                         - Event handlers (Forge + Mod bus)
  item/                          - Items, ferramentas, armaduras, materiais
  logic/                         - Blocos de infeccao + InfectionLogic central
  menu/                          - Menus de maquinas (Container)
  network/                       - Packets S2C para sincronizar dados
  registry/                      - DeferredRegister (Blocks, Items, Entities, etc.)
```

### Ordem de Registro (LiberthiaMod constructor)
1. `ModBlocks` -> 2. `ModItems` -> 3. `ModFluids` -> 4. `ModSounds` -> 5. `ModEntities` -> 6. `ModCapabilities` -> 7. `ModEffects` -> 8. `ModBlockEntities` -> 9. `ModMenuTypes` -> 10. `ModCreativeTabs`

---

## 3. Sistema de Infeccao

O coracao do mod. Gerenciado por `InfectionLogic.java` (~1400 linhas).

### 3.1 Ciclo de Tick do Jogador (`InfectionLogic.tick`)

Executado a cada tick para cada `ServerPlayer`:

1. **Imunidade** - Se imune, zera infeccao
2. **Scan de Exposicao** - `scanExposureGeneric()` analisa area 9x5x9 ao redor
3. **Timer de Pilula** - Depleta 1/tick normal, 100/tick se exposto a materia escura
4. **Aplicar Exposicao** - Incrementa infeccao baseado na pressao escura
5. **Reducao por Materia Clara** - -2 infeccao se tocando, -1 se proxima
6. **Atualizar Perfil de DNA** - Modifica energias dark/clear/yellow
7. **Sinergia de Materia** - Calcula imunidade e mutacoes
8. **Efeitos Derivados** - Aplica penalidades de vida, debuffs
9. **Auto-Debuffs** - Fome, lentidao, cegueira proporcional ao nivel
10. **Efeitos de Mutacao** - Buffs/debuffs especificos por mutacao
11. **Isolamento** - Stage >= 4 infecta jogadores proximos
12. **Espalhar Corrupcao** - Corrompe blocos ao redor do jogador
13. **Espalhar Materias Neutras** - Clear/Yellow se expandem naturalmente
14. **Atividade de Fluido Escuro** - Verifica condicoes para buraco negro
15. **Dano Periodico** - Stage >= 2: 1 dano magico a cada 120 ticks
16. **Sincronizacao** - Envia packet S2C a cada 20 ticks
17. **Snapshot Backend** - Envia dados para backend a cada 200 ticks
18. **Instabilidade Ambiental** - Spawna buracos negros, mobs, glitches

### 3.2 Niveis de Infeccao (Stages)

| Stage | Infeccao | Nome | Efeitos |
|-------|----------|------|---------|
| 0 | 0% | LIMPO | Nenhum |
| 1 | 1-24% | EXPOSICAO | Fome leve |
| 2 | 25-49% | CONTAMINACAO | Fome + Lentidao de mineracao + Dano periodico |
| 3 | 50-74% | DEGRADACAO | + Fraqueza + Lentidao de movimento + Confusao |
| 4 | 75-89% | DEGRADACAO+ | + Cegueira + Isolamento (infecta outros) |
| 5 | 90-100% | COLAPSO | Todos os debuffs + Mutacoes automaticas |

### 3.3 Scan de Exposicao

`scanExposureGeneric()` calcula pressao escura baseado em:

| Fonte | Peso |
|-------|------|
| Fluido de Materia Escura | 4.0 |
| Bloco de Materia Escura | 3.0 |
| Crescimento de Infeccao | 2.0 |
| Solo Corrompido | 1.0 |
| Minerio de Materia Escura | 0.5 |

Bonus: raytrace frontal (20 blocos), cache de chunk, densidade do chunk.

### 3.4 Tick de Mobs (`tickLiving`)

Mobs tambem sao afetados pela infeccao:
- Exposicao incrementa infeccao proporcionalmente
- Pressao >= 8: 1 dano magico a cada 40 ticks
- Infeccao >= 50: Wither + 2 dano/20 ticks
- **Infeccao = 100 + exposicao total**: Mob se converte em `CorruptedZombie`

### 3.5 Instabilidade Ambiental

Verificada a cada 200 ticks com base na densidade do chunk:

| Densidade | Efeito |
|-----------|--------|
| >= 0.90 | Spawna Buraco Negro |
| >= 0.80 | Spawna Corrupted Zombie (15% chance) |
| >= 0.75 | Explosao escura |
| >= 0.60 | Spawna Spore Spitter (12% chance) |
| >= 0.60 | Coloca SporeBloom naturalmente |
| >= 0.50 | Converte blocos em Glitch Blocks (max 10/chunk) |

---

## 4. Blocos

### 4.1 Blocos de Infeccao

#### Dark Matter Block (`dark_matter_block`)
- **Classe:** `DarkMatterBlock`
- **Visual:** Preto puro, `emissiveRendering=true`, `lightLevel=0`, bloqueia TODA luz (`getLightBlock=15`)
- **Comportamento:**
  - `onPlace`: Agenda tick em 12.000 ticks (~10 min) para derreter em fluido
  - `randomTick`: 1-4 tentativas de espalhamento baseado em densidade
  - **Espalhamento (40% gate):** Verifica barreira de agua, protecao, e converte:
    - Grama/Terra -> Solo Corrompido
    - Pedra/Areia/Cascalho/Deepslate/Neve -> Materia Escura
    - Troncos/Folhas/Flores -> Fluido de Materia Escura
  - **Reacao com Materia Clara:** Explosao r=4, remove ambos, infecta entidades +5, dropa materia amarela
  - **Lancamento de Esporos:** 2-6% chance, requer 10+ blocos locais, lanca a 32-64 blocos
- **Textura:** Preto puro com veias roxas quase invisiveis

#### Corrupted Soil (`corrupted_soil`)
- **Classe:** `CorruptedSoilBlock`
- **Comportamento:**
  - 2 tentativas de espalhamento (35% gate cada)
  - Infeccao profunda: corrompe 4 camadas abaixo e 2 acima da superficie
  - 6% chance de brotar colunas de Infection Growth (ate 3 de altura)
  - 4% chance de lancar esporos
- **Converte:** Grande lista de blocos incluindo pedras, minerios, deepslate, blocos do Nether/End

#### Infection Growth (`infection_growth`)
- **Classe:** `InfectionGrowthBlock`
- **Propriedade:** `AGE` 0-3
- **Comportamento:**
  - Cresce em idade com 12%+densidade*10% de chance
  - Espalha horizontalmente (max 2 tentativas, 2-4 blocos)
  - Cresce em arvores de infeccao (tronco ate 5 blocos, copa com galhos)
  - Age >= 3 + densidade >= 0.75 + arvore >= 5 blocos: converte em Materia Escura
  - Espacamento minimo: 8 blocos entre crescimentos

#### Corrupted Stone (`corrupted_stone`)
- **Classe:** `CorruptedStoneBlock`
- 15% chance de espalhar para pedras adjacentes
- Converte: stone, cobblestone, deepslate, andesite, diorite, granite, tuff

#### Infection Vein (`infection_vein`)
- **Classe:** `InfectionVeinBlock`
- **Propriedade:** `AGE` 0-3
- Somente espalha abaixo de Y=50 (subterraneo)
- Age 3: 10% chance de virar Materia Escura
- Espalha para blocos tipo pedra adjacentes

#### Spore Bloom (`spore_bloom`)
- **Classe:** `SporeBloomBlock`
- Sem colisao, precisa de Solo Corrompido ou Materia Escura embaixo
- Detecta jogadores em 8 blocos e lanca esporos de materia escura neles
- Auto-destroi se base for removida

#### Corrupted Log (`corrupted_log`)
- **Classe:** `CorruptedLogBlock`
- **Propriedade:** `AXIS` (preserva orientacao do tronco original)
- 8% chance de infectar troncos adjacentes
- 4% chance de corromper solo abaixo (descendo ate 10 blocos)

#### Glitch Block (`glitch_block`)
- **Classe:** `GlitchBlock`
- **Propriedade:** `PHASE` 0-7 (8 variantes visuais)
- **Visual:** Preto com linhas de scan glitchadas, pixels aleatorios brilhantes, emissivo
- **Comportamento:**
  - Cicla fase rapidamente via randomTick (+1 a +3 por tick)
  - Reverte para pedra se densidade < 30%
  - Particulas: ENCHANT + REVERSE_PORTAL (client-side)
  - Maximo 10 por chunk, spawna naturalmente quando densidade >= 50%

### 4.2 Blocos de Materia

#### Clear Matter Block (`clear_matter_block`)
- **Classe:** `ClearMatterBlock`
- **Luz:** 11
- **Comportamento:**
  - Restaura blocos originais via `MatterHistoryManager` (ate 16 por tick)
  - Aura de purificacao 9x5x9: Solo Corrompido -> Terra, reduz idade de Growth
  - Reduz infeccao de entidades em 6 blocos por 2/tick
  - Destroi Materia Escura/Fluido adjacente (5% chance de auto-degradar)

#### Yellow Matter Block (`yellow_matter_block`)
- Barreira passiva — bloqueia TODA propagacao de infeccao

#### Dark Matter Fluid (`dark_matter_fluid_block`)
- **Classe:** `DarkMatterFluidBlock`
- Fluido com densidade 1000, viscosidade 1000, cor quase preta
- 2+densidade*8 tentativas de espalhamento por tick
- Lanca esporos se >= 5 blocos de fluido em area 5x5x5

### 4.3 Blocos Especiais

#### White Matter TNT (`white_matter_tnt`)
- **Classe:** `WhiteMatterTNTBlock`
- **Ativacao:** Click direito ou sinal de redstone
- Spawna `WhiteMatterExplosionEntity` que:
  - Limpa infeccao em raio de 8 blocos
  - Aplica CLEAR_SHIELD por 1200 ticks em raio de 16 blocos

#### Wormhole Block (`wormhole_block`)
- **Classe:** `WormholeBlock`
- **Portal de Materia Escura** com sistema de vinculacao:
  1. Click direito com Dark Matter Shard no primeiro wormhole -> armazena posicao no NBT do shard
  2. Click direito com o mesmo shard no segundo wormhole -> vincula os dois
  3. Entrar no wormhole teleporta para o mais proximo (raio 64 blocos)
- Aplica Nausea breve ao teleportar
- Particulas: REVERSE_PORTAL + PORTAL (client-side)
- **Luz:** 10, emissivo, sem oclusao

#### Purity Beacon (`purity_beacon`)
- Luz 15, protecao passiva de area

#### White Matter Bomb (`white_matter_bomb`)
- Bomba de purificacao (funcionalidade basica)

### 4.4 Minerios

| Minerio | Bloco | Resistencia | Luz | XP |
|---------|-------|-------------|-----|-----|
| `dark_matter_ore` | Overworld | 3.0/3.0 | 2 | 3-7 |
| `deepslate_dark_matter_ore` | Deepslate | 4.5/3.0 | 2 | 3-7 |
| `white_matter_ore` | Raro | 3.0/3.0 | 5 | 3-7 |

### 4.5 Maquinas

| Maquina | Slots | Tempo | Detalhes |
|---------|-------|-------|----------|
| Dark Matter Forge | 4 (fuel, 2 input, output) | 200 ticks | Usa combustivel escuro, infecta jogadores proximos |
| Matter Infuser | 5 (dark, clear, yellow, catalyst, output) | 300 ticks | Combina 3 materias |
| Purification Bench | 2 (input, output) | 200 ticks | Purifica materia escura |
| Research Table | 3 (material, paper, output) | 100 ticks | Pesquisa com papel/livro |
| Containment Chamber | 4 (2 input, containment, output) | 400 ticks | Instavel sem materia clara |
| Matter Transmuter | 3 (input, catalyst, output) | 300 ticks | Transmuta entre materias |

---

## 5. Itens

### 5.1 Ferramentas de Materia Escura

| Item | Durabilidade | Velocidade | Ataque | Especial |
|------|-------------|-----------|--------|---------|
| Dark Matter Sword | 1200 | - | 7.0 | Aplica Dark Infection, dropa shard ao matar |
| Dark Matter Pickaxe | 1200 | 6.0 | 4.0 | Velocidade 2x em areas infectadas |
| Dark Matter Axe | 1200 | 6.0 | 8.0 | Derruba arvore inteira (BFS 20 blocos) |

### 5.2 Ferramentas de Materia Clara

| Item | Durabilidade | Velocidade | Ataque | Especial |
|------|-------------|-----------|--------|---------|
| Clear Matter Sword | 800 | - | 5.5 | Aplica CLEAR_SHIELD ao atacante, reduz infeccao do alvo |
| Clear Matter Pickaxe | 800 | 7.0 | 2.5 | Restaura blocos originais via MatterHistoryManager |
| Clear Matter Axe | 800 | 7.0 | 2.5 | Restaura troncos corrompidos |

### 5.3 Ferramentas de Materia Amarela

| Item | Durabilidade | Velocidade | Ataque | Especial |
|------|-------------|-----------|--------|---------|
| Yellow Matter Sword | 1200 | - | 6.0 | 30% chance de colocar bloco amarelo nos pes do alvo |
| Yellow Matter Pickaxe | 1200 | 8.0 | 3.0 | Velocidade 2x em areas infectadas |
| Yellow Matter Axe | 1200 | 8.0 | 3.0 | Derruba arvore 3x3 (BFS 20 blocos) |

### 5.4 Escudo de Materia Amarela

- **Durabilidade:** 600
- Quebra 3x mais rapido com 10.000+ particulas
- Com 20.000 particulas ou 100% densidade: **quebra e spawna buraco negro**

### 5.5 Armaduras

#### Yellow Matter Armor
- Entre ferro e diamante em protecao
- Material customizado com durabilidade: H=187, C=272, L=255, B=220

#### Clear Matter Armor
- Material customizado
- Protecao basica contra infeccao

#### Containment Suit
- **Classe:** `ContainmentSuitItem`
- Set completo fornece CLEAR_SHIELD automatico
- **Carga:** 6000 ticks (5 minutos), depleta em zonas de infeccao
- Tooltip mostra carga restante
- Durabilidade: H=275, C=400, L=375, B=325

### 5.6 Itens Medicos

#### Clear Matter Pill (`clear_matter_pill`)
- **Stack:** 16
- Reduz infeccao em 10, remove Dark Infection e Radiation Sickness
- Aplica CLEAR_SHIELD por 1200 ticks
- Timer de pilula: 30 minutos — apos esgotar, pilulas param de funcionar
- Mensagem: "As pilulas nao fazem mais efeito. Voce precisa de uma injecao de cura!"

#### Clear Matter Injector (`clear_matter_injector`)
- **Stack:** 16
- Reduz infeccao em 30, penalidade de vida em 2
- **Limpa TODAS as mutacoes**
- Remove 9 efeitos negativos diferentes
- Aplica imunidade por 3 minutos (3600 ticks)

#### White Matter Syringe (`white_matter_syringe`)
- **Stack:** 4, Raridade RARE
- **Cura total:** Infeccao para 0, limpa mutacoes, concede imunidade
- Reduz penalidade permanente de vida em 4
- Aplica CLEAR_SHIELD (5 min) + Regeneracao II + Absorcao II
- Remove todos os efeitos negativos
- Cooldown: 200 ticks, brilha (isFoil)

### 5.7 Baldes de Materia

| Balde | Comportamento |
|-------|--------------|
| Dark Matter Bucket | `BucketItem` padrao para fluido de Materia Escura |
| Clear Matter Bucket | Coloca bloco de Materia Clara ao usar |
| Yellow Matter Bucket | Coloca bloco de Materia Amarela ao usar |

### 5.8 Ferramentas de Utilidade

| Item | Stack | Descricao |
|------|-------|-----------|
| Geiger Counter | 1 | Mede radiacao, mostra particulas/densidade, alerta de buraco negro |
| White Light Wand | 1 (dur. 100) | Varinha de luz branca |
| White Matter Finder | 1 | Localiza minerio de materia branca |
| Safe Siphon | 1 | Sifao seguro para extracao |
| Cleansing Grenade | 16 | Granada de purificacao (projetil) |
| Clear Matter Shield | 4 | Escudo defensivo de materia clara |

### 5.9 Itens de Lore

#### Worker Badge (`worker_badge`)
- **Stack:** 1, brilha (isFoil)
- **Sistema de NBT vinculado ao jogador:**
  - Primeiro uso: vincula UUID e nome do jogador
  - Armazena: max infeccao vista, mutacoes descobertas, ticks de exposicao
  - Click direito mostra status completo no chat:
    - Trabalhador, Infeccao %, Stage, Mutacoes, Max Infeccao, Imunidade
  - Tooltip mostra estatisticas pessoais quando vinculado

#### Host Journal (`host_journal`)
- Diario de lore (stack 1)

### 5.10 Materiais de Crafting

| Item | Stack | Uso |
|------|-------|-----|
| Dark Matter Shard | 64 | Combustivel, ingrediente, vinculacao de wormhole |
| Yellow Matter Ingot | 64 | Crafting de armaduras e ferramentas amarelas |
| Holy Essence | 1 | Catalisador em maquinas |
| Stabilized Dark Matter | 64 | Material intermediario |
| Void Crystal | 16 | Material intermediario |
| Singularity Core | 1 | Material avancado |
| Matter Core | 64 | Material combinado |
| Purified Essence | 64 | Resultado de purificacao |
| Research Notes | 64 | Resultado de pesquisa |
| Protection Ruby | 1 (RARE) | Rubi de protecao de area |

---

## 6. Entidades

### 6.1 Corrupted Zombie (`corrupted_zombie`)
- **HP:** 30, **Velocidade:** 0.27, **Dano:** 5
- Aplica +5 infeccao ao atacar
- Lanca esporos a cada 80 ticks
- Queima em zonas de materia clara
- Dropa `dark_matter_shard`
- **Renderer:** Usa modelo de Zombie vanilla com textura customizada

### 6.2 Spore Spitter (`spore_spitter`)
- **HP:** 15, **Velocidade:** 0.35
- Tipo artropode (spider model)
- Dispara esporos a cada 40-80 ticks
- **Renderer:** Usa modelo de Spider vanilla com textura customizada

### 6.3 Dark Matter Spore (`dark_matter_spore`)
- **Projetil** lancado por blocos infectados e mobs
- Tamanho: 0.5x0.5, tracking range 20
- Viaja ate o alvo definido e espalha infeccao

### 6.4 Black Hole Entity (`black_hole`)
- Tamanho: 2.0x2.0
- Spawna quando: 2000+ particulas OU 90%+ densidade
- Atrai entidades e blocos proximos

### 6.5 White Matter Explosion (`white_matter_explosion`)
- **Fuse:** 80 ticks
- Emite particulas END_ROD durante contagem
- **Detonacao:**
  - Remove infeccao em raio de 8 blocos
  - Aplica CLEAR_SHIELD (1200 ticks) em raio de 16 blocos
  - 100 particulas END_ROD em explosao
  - Toca som CLEAR_HUM

### 6.6 Cleansing Grenade (`cleansing_grenade`)
- Projetil de purificacao (tamanho 0.25x0.25)

---

## 7. Efeitos (MobEffects)

| Efeito | Classe | Descricao |
|--------|--------|-----------|
| `dark_infection` | `DarkInfectionEffect` | Infeccao escura — debuffs progressivos |
| `radiation_sickness` | `RadiationSicknessEffect` | Doenca por radiacao |
| `clear_shield` | `ClearShieldEffect` | Escudo de protecao contra infeccao |

---

## 8. Capabilities (Dados Persistentes)

### 8.1 IInfectionData

Anexado a **todos os LivingEntity**. Campos:

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `infection` | int | Nivel de infeccao (0-100) |
| `immune` | boolean | Se esta imune |
| `mutations` | String | Lista de mutacoes separadas por virgula |
| `permanentHealthPenalty` | int | Penalidade permanente de vida (0-10) |
| `stage` | int | Estagio da infeccao (0-5) |
| `maxInfectionReached` | int | Maximo de infeccao ja alcancado |
| `pillTimer` | int | Timer de efeito de pilula |
| `dirty` | boolean | Flag de sincronizacao |

### 8.2 IMatterEnergy

Anexado apenas a **Players**. Campos:

| Campo | Tipo | Max | Descricao |
|-------|------|-----|-----------|
| `darkEnergy` | int | 1000 | Energia de materia escura |
| `clearEnergy` | int | 1000 | Energia de materia clara |
| `yellowEnergy` | int | 1000 | Energia de materia amarela |
| `stabilized` | boolean | - | Se a energia esta estabilizada |

---

## 9. Sistema de DNA e Mutacoes

### 9.1 Perfil de DNA

A cada tick, a exposicao a diferentes materias modifica as energias do jogador. O perfil de DNA e calculado em `applyMatterSynergy()`:

### 9.2 Caminhos para Imunidade

**Clear+Yellow Immunity:**
- `clearEnergy >= 450`
- `yellowEnergy >= 450`
- `darkEnergy <= 100`
- `|clearEnergy - yellowEnergy| <= 50`
- Resultado: Imunidade total

**Dark Dominance:**
- `darkEnergy >= 700`
- `darkEnergy > clearEnergy`
- `darkEnergy > yellowEnergy`
- Resultado: Imunidade MAS depleta energia clara em 3/tick
- Causa 2 dano magico a entidades proximas a cada 40 ticks

### 9.3 Sinergias de DNA

| Combinacao | Mutacao | Efeitos |
|-----------|---------|---------|
| Dark + Yellow | - | Repulsao escura |
| Dark + Clear | LUCID_CORRUPTION | Buff de ataque, drena 1 infeccao/20t |
| Clear + Yellow | TACTICAL_REASON | Resistencia + Velocidade |

### 9.4 Mutacoes

| Mutacao | Trigger | Efeito |
|---------|---------|--------|
| HEAVY_STEPS | Auto (infeccao >= 90) | Reducao de pulo (35%) |
| RADIO_EYES | Auto (infeccao >= 90) | Visao noturna |
| HUNGRY_VOID | Manual | Fome extrema |
| DARK_VEIL | Manual | Cegueira |
| RADIANT_SKIN | Manual | Resistencia |
| AQUATIC_ADAPTATION | Manual | Respiracao aquatica |
| SWIFT_SIGHT | Manual | Velocidade + Exaustao |
| STATIC_DISCHARGE | Manual | Dano quando chove |
| LUCID_CORRUPTION | Sinergia Dark+Clear | Buff de ataque |
| TACTICAL_REASON | Sinergia Clear+Yellow | Resistencia + Velocidade |

---

## 10. Sistema de Materia (Escura, Clara, Amarela)

### 10.1 Materia Escura

- **Fonte da infeccao** — corrompe tudo que toca
- **Fluido:** Densidade 1000, viscosidade 1000, cor quase preta (#050505)
- **Bloco solido** derrete em fluido apos 10 minutos
- **Reacao com Materia Clara:** Explosao + Drop de Materia Amarela
- **Barrada por:** Materia Amarela, agua, materia clara

### 10.2 Materia Clara

- **Agente de purificacao** — restaura blocos, cura infeccao
- **Aura de purificacao** em raio 9x5x9
- **MatterHistoryManager:** Registra blocos originais antes de serem corrompidos
- **Picareta de Materia Clara** pode restaurar blocos originais

### 10.3 Materia Amarela

- **Barreira** — bloqueia TODA propagacao de infeccao
- **Protecao passiva** em raio 5x3x5
- **Escudo Amarelo** com mecanica de buraco negro

### 10.4 MatterHistoryManager

Sistema que registra qual bloco existia originalmente antes de ser corrompido. Permite restauracao com ferramentas de materia clara.

---

## 11. Maquinas e Bancadas

### 11.1 Dark Matter Forge

**Combustiveis:**
| Item | Duracao |
|------|---------|
| Dark Matter Shard | 200 ticks |
| Dark Matter Block | 800 ticks |
| Dark Matter Bucket | 1600 ticks |

**Receitas:**
| Input 1 | Input 2 | Output |
|---------|---------|--------|
| Dark Matter Shard | Iron Ingot | Stabilized Dark Matter |
| Dark Matter Shard | Netherite Scrap | Void Crystal |
| Stabilized Dark Matter | Void Crystal | Singularity Core |
| Dark Matter Block | Holy Essence | Purified Essence x2 |

**Perigo:** A cada 40 ticks enquanto ativa, infecta jogadores em 8 blocos (+1 infeccao).

### 11.2 Matter Infuser

**Receitas:**
| Dark | Clear | Yellow/Catalyst | Output |
|------|-------|-----------------|--------|
| Dark Block | Clear Block | Yellow Block + Singularity Core | Matter Core |
| Dark Shard | Clear Block | Holy Essence | Purified Essence x3 |
| - | Clear Block | Yellow Ingot + Holy Essence | Clear Matter Pill x4 |

### 11.3 Purification Bench

| Input | Output |
|-------|--------|
| Dark Matter Shard | Yellow Matter Ingot |
| Dark Matter Block | Clear Matter Block |

### 11.4 Research Table

Slot 1: Material de pesquisa. Slot 2: Papel, Livro ou WritableBook.

| Material | Paper/Book | Output |
|----------|-----------|--------|
| Dark Matter Shard | Paper | Written Book |
| Holy Essence | Book | Written Book |
| Clear Matter Block | Paper | Clear Matter Pill x4 |

### 11.5 Containment Chamber

**Sistema de Estabilidade:** Sem materia clara no slot de contencao, estabilidade cai 5/tick. Em 0, causa explosao (3.0F).

| Input 1 | Input 2 | Output |
|---------|---------|--------|
| Dark Matter Block | Dark Matter Shard | Dark Matter Shard x4 |
| Yellow Matter Block | Holy Essence | Holy Essence x3 |

### 11.6 Matter Transmuter

| Input | Catalisador | Output |
|-------|-------------|--------|
| Dark Matter | Holy Essence | Clear Matter |
| Clear Matter | Yellow Ingot | Yellow Matter |
| Clear Matter | Dark Matter Shard | Dark Matter |

---

## 12. Interface (HUD/Overlays)

### 12.1 Infection HUD (`InfectionHudOverlay`)
- **Sempre visivel**
- Barra de infeccao com cor baseada no stage
- Texto: Nivel %, Estado, Tipo de mutacao
- Penalidade permanente de vida
- Tinta roxa pulsante quando infeccao > 50%
- **Geiger (quando segurando):** Criticidade, Sps, barra de sinal

### 12.2 Matter Energy HUD (`MatterEnergyHudOverlay`)
- Posicao fixa (10, 70)
- 3 barras compactas: Escura (roxo), Clara (ciano), Amarela (dourado)
- Indicador de estabilizacao (diamante)
- So aparece com energia > 0

### 12.3 DNA Mutation Overlay (`DnaMutationOverlay`)
- Escala 0.75x, posicao configuravel
- Lista apenas mutacoes ativas com descricoes em portugues
- So aparece quando ha mutacoes

### 12.4 Radiation Guide Overlay (`RadiationGuideOverlay`)
- **So aparece quando segurando Geiger Counter**
- Painel no canto inferior direito
- Escaneia a cada 10 ticks (performance)
- Conta blocos de infeccao em raio 16
- Calcula densidade em raio 8
- **Indicador direcional:** Seta apontando para maior concentracao
- Barra de radiacao colorida (verde -> amarelo -> vermelho -> roxo)
- **Alerta de Buraco Negro:** "[BH WARNING]" piscante quando particulas > 1500 ou densidade > 80%

### 12.5 Infection Status Overlay (`InfectionStatusOverlay`)
- **So aparece com inventario aberto**
- Painel detalhado (150x110) no canto superior direito
- Mostra: infeccao %, stage, exposicao, protecao de armadura %, penalidade de vida
- Lista efeitos ativos e mutacoes com traducoes

### 12.6 HUD Config Screen (`HudConfigScreen`)
- Tela drag-and-drop para reposicionar HUDs
- Renderiza previews dummy dos 3 paineis
- Salva posicoes em `LiberthiaConfig.CLIENT`

---

## 13. Sistema de Protecao

### 13.1 ProtectionUtils

| Metodo | Raio | Descricao |
|--------|------|-----------|
| `hasClearMatterProtection` | 7x3x7 | Detecta materia clara proxima |
| `hasYellowMatterProtection` | 5x3x5 | Detecta materia amarela proxima |
| `isWaterBarrier` | 3x3x3 | Detecta agua/lava (exceto fluido escuro) |
| `hasGrowthTooClose` | customizavel | Verifica espacamento minimo entre growths |

### 13.2 Barreiras

A infeccao e bloqueada por:
1. **Materia Amarela** — hard stop em qualquer propagacao
2. **Materia Clara** — protecao ativa + purificacao
3. **Agua/Lava** — barreira natural (3x3x3)
4. **Containment Suit** — protecao pessoal com carga limitada
5. **Clear Shield (efeito)** — protecao temporaria

---

## 14. Buracos Negros

### Condicoes de Spawn

| Fonte | Condicao |
|-------|---------|
| Instabilidade Ambiental | Densidade >= 90% no chunk |
| Fluido Escuro | 6+ blocos de fluido + 2000+ particulas |
| Escudo Amarelo | 20.000+ particulas ou 100% densidade ao quebrar |

### Contagem de Particulas (Pesos)

| Bloco | Peso |
|-------|------|
| Dark Matter Fluid | 25 |
| Dark Matter Block | 14 |
| Infection Growth | 11 |
| Dark Matter Ore | 9 |
| Corrupted Soil | 7 |

### Comportamento

O `BlackHoleEntity` (2.0x2.0) atrai entidades e blocos proximos quando spawnado.

---

## 15. World Generation

### 15.1 Minerios

**Dark Matter Ore** e gerado no Overworld via `placed_feature/dark_matter_ore.json` com biome modifier Forge.

Tags de blocos: `mineable/pickaxe`, `needs_iron_tool`.

### 15.2 Surtos Periodicos (`WorldSpawnerEvents`)

- Ativado por `worldSpawnsEnabled` (default: true)
- Intervalo configuravel: `spawnIntervalTicks` (default: 2400 = 2 min)
- A cada intervalo no Overworld:
  - Avalia regiao para surto de materia escura
  - 15% chance de tocar som `DARK_PULSE`
  - Coloca focos de infeccao com growth trees, fluido escuro, solo corrompido
  - Bloqueado por proximidade de agua/lava

---

## 16. Configuracao

### 16.1 Client Config (`liberthia-client.toml`)

| Chave | Default | Descricao |
|-------|---------|-----------|
| `hud.infection_x` | 10 | Posicao X da barra de infeccao |
| `hud.infection_y` | 10 | Posicao Y da barra de infeccao |
| `hud.exposure_x` | 10 | Posicao X do alerta de exposicao |
| `hud.exposure_y` | 50 | Posicao Y do alerta de exposicao |
| `hud.dna_x` | 10 | Posicao X do painel de DNA |
| `hud.dna_y` | 95 | Posicao Y do painel de DNA |

### 16.2 Server Config (`liberthia-server.toml`)

| Chave | Default | Descricao |
|-------|---------|-----------|
| `backend.enabled` | false | Ativa envio de snapshots |
| `backend.base_url` | "" | URL do backend |
| `backend.snapshot_path` | "/api/v1/infection/snapshot" | Endpoint |
| `backend.connect_timeout_ms` | 3000 | Timeout de conexao |
| `backend.request_timeout_ms` | 5000 | Timeout de requisicao |
| `world.world_spawns_enabled` | true | Surtos periodicos |
| `world.spawn_interval_ticks` | 2400 | Intervalo de surtos |

---

## 17. Rede e Sincronizacao

### Canal: `liberthia:main` (protocolo "1")

| Packet | ID | Direcao | Dados |
|--------|----|---------|----- |
| `S2CInfectionSyncPacket` | 0 | Server -> Client | infection, penalty, stage, exposure (raw/blocked), armorProtection, mutations |
| `S2CMatterEnergySyncPacket` | 1 | Server -> Client | darkEnergy, clearEnergy, yellowEnergy, stabilized |

### Frequencia de Sync
- Infeccao: a cada 20 ticks ou quando `dirty=true`
- Materia: a cada 20 ticks ou quando `dirty=true`

---

## 18. Backend Externo

### BackendClient

Cliente HTTP assincrono (`java.net.http.HttpClient`) em thread daemon separada.

**Endpoint:** POST `{baseUrl}{snapshotPath}`

**Payload JSON:**
```json
{
  "playerUuid": "uuid",
  "playerName": "name",
  "dimension": "minecraft:overworld",
  "infection": 50,
  "stage": 2,
  "permanentHealthPenalty": 4,
  "x": 100.0,
  "y": 64.0,
  "z": -200.0,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

Enviado a cada 200 ticks (~10 segundos) quando backend habilitado.

---

## 19. Sons

| Som | Uso |
|-----|-----|
| `dark_whisper` | Ambiente em areas infectadas |
| `dark_pulse` | Surtos periodicos, stage alto |
| `clear_hum` | Purificacao, pilulas, seringas |
| `geiger_tick` | Medicao de radiacao |
| `infection_alert` | Alertas de infeccao |
| `isolation_warning` | Stage >= 4, isolamento |

Todos em formato `.ogg` em `assets/liberthia/sounds/`.

---

## 20. Comandos

### `/purge_infection`

Registrado em `PurgeInfectionCommand` via `CommandEvents`. Limpa infeccao de jogadores/area.

---

## 21. Receitas

### 21.1 Crafting Table

| Resultado | Tipo | Ingredientes |
|-----------|------|-------------|
| Clear Matter Block | Shaped | Amethyst Shards + Quartz + Glass |
| Clear Matter Injector | Shaped | Clear Matter Block + Yellow Ingots + Glass Bottle |
| Clear Matter Pill x4 | Shapeless | Clear Matter Block |
| Clear Matter Shield | Shaped | Clear Matter Blocks + Stick |
| Geiger Counter | Shaped | Yellow Ingot + Iron Ingots + Redstone |
| Yellow Matter Block | Shaped | 9x Yellow Matter Ingot |
| Yellow Matter Ingot x9 | Shapeless | Yellow Matter Block |
| Yellow Armor (H/C/L/B) | Shaped | Yellow Matter Ingots |

### 21.2 Receitas de Maquinas

*Veja secao 11 (Maquinas e Bancadas) para todas as receitas de maquinas.*

---

## 22. Registro Completo de Arquivos

### Java (128 arquivos)

```
LiberthiaMod.java
backend/BackendClient.java
block/ContainmentChamberBlock.java, DarkMatterForgeBlock.java, MatterInfuserBlock.java
      MatterTransmuterBlock.java, PurificationBenchBlock.java, ResearchTableBlock.java
      WhiteMatterBombBlock.java
block/entity/ContainmentChamberBlockEntity.java, DarkMatterForgeBlockEntity.java
             MatterInfuserBlockEntity.java, MatterTransmuterBlockEntity.java
             PurificationBenchBlockEntity.java, ResearchTableBlockEntity.java
capability/IInfectionData.java, IMatterEnergy.java, InfectionData.java
           InfectionProvider.java, MatterEnergyData.java, MatterEnergyProvider.java
client/ClientEvents.java, ClientInfectionState.java, ClientMatterEnergyState.java
       ClientModEvents.java, DnaMutationOverlay.java, HudConfigScreen.java
       InfectionHudOverlay.java, InfectionStatusOverlay.java, KeyBindings.java
       MatterEnergyHudOverlay.java, RadiationGuideOverlay.java
client/renderer/BlackHoleRenderer.java, CleansingGrenadeRenderer.java
                CorruptedZombieRenderer.java, DarkMatterSporeRenderer.java
                SporeSpitterRenderer.java, WhiteMatterExplosionRenderer.java
client/screen/ContainmentChamberScreen.java, DarkMatterForgeScreen.java
              MatterInfuserScreen.java, MatterTransmuterScreen.java
              PurificationBenchScreen.java, ResearchTableScreen.java
command/ModCommands.java, PurgeInfectionCommand.java
config/LiberthiaConfig.java
effect/ClearShieldEffect.java, DarkInfectionEffect.java, RadiationSicknessEffect.java
entity/BlackHoleEntity.java, CleansingGrenadeEntity.java, CorruptedZombieEntity.java
       DarkMatterSporeEntity.java, SporeSpitterEntity.java, WhiteMatterExplosionEntity.java
event/CommandEvents.java, InfectionEvents.java, ModConfigEvents.java
      ModEntityEvents.java, WorldSpawnerEvents.java
item/CleansingGrenadeItem.java, ClearMatterArmorMaterial.java, ClearMatterAxeItem.java
     ClearMatterBucketItem.java, ClearMatterInjectorItem.java, ClearMatterPickaxeItem.java
     ClearMatterPillItem.java, ClearMatterShieldItem.java, ClearMatterSwordItem.java
     ClearMatterToolMaterial.java, ContainmentSuitArmorMaterial.java, ContainmentSuitItem.java
     DarkMatterAxeItem.java, DarkMatterPickaxeItem.java, DarkMatterShardItem.java
     DarkMatterSwordItem.java, DarkMatterToolMaterial.java, GeigerCounterItem.java
     HostJournalItem.java, SafeSiphon.java, WhiteLightWand.java, WhiteMatterBombItem.java
     WhiteMatterFinder.java, WhiteMatterSyringeItem.java, WorkerBadgeItem.java
     YellowMatterArmorMaterial.java, YellowMatterAxeItem.java, YellowMatterBucketItem.java
     YellowMatterPickaxeItem.java, YellowMatterShieldItem.java, YellowMatterSwordItem.java
     YellowMatterToolMaterial.java
logic/ClearMatterBlock.java, CorruptedLogBlock.java, CorruptedSoilBlock.java
      CorruptedStoneBlock.java, DarkMatterBlock.java, DarkMatterFluidBlock.java
      ExposureData.java, GlitchBlock.java, InfectionGrowthBlock.java, InfectionLogic.java
      InfectionVeinBlock.java, MatterHistoryManager.java, ProtectionUtils.java
      PurityBeaconBlock.java, SporeBloomBlock.java, WhiteMatterTNTBlock.java, WormholeBlock.java
menu/ContainmentChamberMenu.java, DarkMatterForgeMenu.java, MatterInfuserMenu.java
     MatterTransmuterMenu.java, PurificationBenchMenu.java, ResearchTableMenu.java
network/ModNetwork.java, S2CInfectionSyncPacket.java, S2CMatterEnergySyncPacket.java
registry/ModBlockEntities.java, ModBlocks.java, ModCapabilities.java, ModCreativeTabs.java
         ModEffects.java, ModEntities.java, ModFluids.java, ModItems.java
         ModMenuTypes.java, ModSounds.java
```

### Resources

```
assets/liberthia/
  blockstates/          (24 arquivos JSON)
  lang/en_us.json, pt_br.json
  models/block/         (29 arquivos JSON, incluindo 8 glitch_block variants)
  models/item/          (57 arquivos JSON)
  sounds/               (6 arquivos .ogg)
  sounds.json
  textures/block/       (30+ arquivos .png)
  textures/gui/         (6 arquivos .png)
  textures/item/        (50+ arquivos .png)
  textures/models/armor/ (4 arquivos .png)

data/liberthia/
  forge/biome_modifier/ (1 arquivo)
  loot_tables/blocks/   (10 arquivos)
  recipes/              (11 arquivos)
  worldgen/             (2 arquivos)

data/minecraft/tags/blocks/ (2 arquivos)
```

---

> **Nota:** Esta documentacao cobre o estado completo do mod ate a versao v1 (branch `v1`). O mod compila com sucesso com apenas warnings de deprecacao do `ResourceLocation`.
