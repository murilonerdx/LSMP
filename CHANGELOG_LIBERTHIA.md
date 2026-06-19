# Liberthia — Resumo das modificações da sessão

Documento gerado em 2026-05-19, **última atualização: 2026-05-26** cobrindo todas as alterações feitas no mod, backend e frontend.

---

## 🎓 v0.1.166 — r162 Mage Class System + Texturas Fix (2026-05-26)

### Crisis Fix

**Texturas dos feiticos sumiram em massa.** Causa: o `gen_spell_textures.py` salvava com prefix `factory_` duplicado (ex: `factory_spell_scroll_factory_fireball.png`) mas os models procuram `factory_spell_scroll_fireball.png`. Resultado: TODAS as 116 texturas apareciam como xadrez roxo-preto missing-texture.

**Fix:**
- Python rename batch: 116 arquivos PNG sem duplo prefix
- Script `gen_spell_textures.py` corrigido em 4 pontos (`.replace("factory_", "")`)
- Proximo run nao quebra de novo

### Mage Class System (13 classes)

**5 originais (spec user):** Necromante, Ofuscador, Piromante, Vazio, Parachoque
**8 novas:** Curandeiro, Mestre do Grupo, Senhor Critico, Forca Bruta, Mago Poderoso, Escapista, Veloz, Mestre Dash, Aviador

- Cada classe: 10 niveis, bonus = base + (level-1) × step
- 5 XP por cast → auto-levelup (level² × 100 XP threshold)
- Cada cast aplica efeito secundario (poison/fire/wither/heal/slow/...)
- Hook em `SpellFactory.applyDamageAndEffects()` antes do crit roll

### Class Pedestal Block

- `liberthia:class_pedestal` — right-click abre menu de classes via chat clicavel
- Shift+right-click remove classe
- Recipe: amethyst+gold+ender_eye+beacon+smooth_stone
- Self-drop ao quebrar

### Beta Items JSON

- Sincronizado com TODOS os 736 itens/blocos registrados + 117 factory spells
- 5 entries malformadas (com schema `id`/`tier`/`status` em vez de `name`/`itemId`/`kind`/`description`) convertidas pra canonical
- Total: **879 items no JSON**, 0 com `name` faltando

---

## 🛠️ v0.1.165 — r158 Spell Designer GUI Tool (2026-05-26)

**Ferramenta standalone Python/Tkinter** para criação visual de feitiços — sem código, sem digitação.

### Novidades

- **`tools/spell_designer.py`** — GUI completa (~900 linhas) com:
  - Form de identidade (ID auto-derivado), school picker visual com 7 botões coloridos, type/category dropdowns
  - Sliders pra Mana / Cooldown / Damage / Range
  - Color picker pra VFX primary/secondary com swatch live
  - Particle preset dropdown (5 presets de `data/liberthia/particle_config/`)
  - Effects panel com **37 mod-native effects** disponíveis pra combo
  - Sprite Gallery em 5 abas (Schools / Runes / Elements / Threads / Focuses)
  - Composer canvas 256×256 (preview 16× do final 16×16)
  - Layer list com reorder (▲▼), remove (✖), play sequence (▶)
- **`tools/gen_designer_defaults.py`** — gera assets default:
  - **7 school glyphs** com símbolo único por escola (flame, crystal, bolt, drop, eye, cross, leaf)
  - **10 runes** (pentagram, hexagram, circle, triangle, square, cross, spiral, infinity, arrow, wave)
  - **10 elements** (earth, water, wind, void, steam, mud, lava, frost, spark, thorn)
  - **6 threads** (silver, golden, crimson, azure, shadow, ethereal)
  - **5 focuses** (concentration, destruction, protection, manipulation, transmutation)
  - **5 particle presets** (minimal / standard / intense / devastation / subtle_aura)
- **`tools/spell_designer.bat`** — launcher Windows duplo-click
- **`tools/SPELL_DESIGNER_README.md`** — doc completa com ASCII art da UI

### Save flow automatizado

Quando user clica 💾 SAVE SPELL, a ferramenta automaticamente:
1. Cria `data/liberthia/spells/factory_<id>.json`
2. Compõe textura 16×16 com layers + parchment + seal colorido → `textures/item/factory_spell_scroll_<id>.png`
3. Cria model JSON em `assets/liberthia/models/item/`
4. **Atualiza `FactorySpellIndex.java`** — regenera IDX map ordenado alfabeticamente
5. Atualiza overrides do master `factory_spell_scroll.json`
6. Adiciona lang entries em `en_us.json` + `pt_br.json`

### Bug Fix de init order

- `_set_school("FIRE")` em `_build_form` chamava `_refresh_preview` antes do `preview_canvas` existir.
- Fix: guards `hasattr(self, "preview_canvas")` em `_set_preview` e `hasattr(self, "effects_list")` em `_refresh_effects`.

---

## 🩹 v0.1.164 — r157 Magic Level + Crash Fixes (2026-05-26)

**3 crash fixes críticos** + sistema de progressão de magia v2 + categoria Vazio Roxo + balanceamento.

### Crash Fixes

| # | Bug | Fix |
|---|---|---|
| 1 | **ConcurrentModificationException** em `ScheduledVfx.onServerTick` (meteoros/tempestades crashavam server quando spell agendava outra spell durante o tick) | Pattern `PENDING_ADDS` + iteração por índice + flush no início do tick (effects podem agora chamar `spawn*()` dentro do próprio `tick()`) |
| 2 | **SpellTrailParticle** "school 0 null sprite" floodando o log | Removido prefix `particle/` no `ResourceLocation` + flag `WARNED[]` por school anti-log-spam |
| 3 | **Beams não causavam dano** em entidades no caminho do raio (só crosshair target) | `castBeam()` agora itera AABB do segmento e projeta cada entidade sobre o raio — damage em TODAS entidades dentro de 1 bloco do beam |
| 4 | **Registry crash** "did not copy to correct id" — `whispers` registrado em DOIS lugares (ModMobEffects r151 + ModEffects r156) | Renomeado `r156.WHISPERS` → `cosmic_whispers`; texture/lang clonados |

### Magic Level System v2

- **10 níveis** (cap)
- Threshold de kills por level: `level × 100` (1→2: 100, 9→10: 900)
- **+5% mana/source max por level** (Level 10 = +50%)
- **−5% custo por level** (Level 10 = −50%, cap)
- Bonus de uso: 30 usos da mesma spell → +20 kill credits
- Display no Grimoire (nível + barra de progresso + bônus)
- LevelUp toast com som de `PLAYER_LEVELUP`
- Hooks: `LivingDeathEvent`, `PlayerLoggedInEvent`
- Aplicado em `DynamicSpellItem.execute()` via `MagicLevelData.getCostMult()`

### Buffs

- **Dash spells**: velocidade 4×, uplift mínimo 0.45, zera fall damage, atropela inimigos (AABB damage), trail mais denso + 3 sub-trails escalonados

### Nova Categoria: Vazio Roxo (Amethyst Void)

**10 spells** com paleta ametista (#7A3DFF, #A020F0):
- **Vazio Roxo** (masterpiece) — projectile slow + hungry_void + vertigo + cegueira dimensional
- Lança de Ametista, Singularidade do Vazio, Estilhaços de Ametista
- Poço Gravitacional (aura magnet+time_dilation), Ruptura Dimensional
- Égide do Vazio (buff), Tempestade de Cristais (rain)
- Dash do Vazio, **Coração do Vazio Roxo** (variante suprema 40 dmg, 600t cd)

### Vanilla → Mod-Native Effects

Spells `group_*` agora usam mod-native em vez de `minecraft:*`:
- `haste`/`speed` → `magnet_fist`
- `strength`/`resistance`/`regen` → `liberthia_blessing`
- `slowness` → `time_dilation`
- `invisibility` → `shadow_double`
- `glowing` → `stardust`
- `jump_boost` → `crystal_bloom`
- `water_breathing`/`slow_falling` → `ghost_touch`

### Cobertura

- **116 spells** total — 100% com texturas procedurais
- **20 custom effects** — 100% com texturas

---

## 🎆 v0.1.163 — r156 Mega Magic Batch (2026-05-26)

**Maior expansão de magia até hoje.** Adiciona 64 novos feitiços (total 106), 20 custom MobEffects, sistema de drop de scrolls por raridade, e fixes críticos.

### Bug Fixes

| # | Bug | Fix |
|---|---|---|
| 1 | Dark Matter Portal — quebrar bloco do frame deixava portal central intacto | `LoomPortalBlock.updateShape()` quebra portal block se neighbor frame for removido. Cascateia em todos 6 blocks. |
| 2 | Magic Fire (7 cores) — `getShape() = Shapes.empty()` impedia raycast/break | `getShape()` retorna `SHAPE` (half-box); collision via `getCollisionShape() = Shapes.empty()` |
| 3 | Fine Weave items (4 variantes) — fundo transparente quebrado | Re-render com fundo black opaco + tint procedural por tipo |

### Novas Features

- **Arcane Workbench** (registrado em r155 Phase 2): bloco GUI 9-slot (base+7mods+output) que compõe scrolls
- **20 Custom MobEffects** novos:
  - Cegueira Dimensional, Pegadas do Diabo, Aura da Lua Sangrenta
  - Duplo Sombrio, Sussurros, Vertigem, Vazio Faminto
  - Toque Fantasma, Vínculo de Alma, Inventário Assombrado
  - Caminhar no Espelho, Dilatação Temporal, Gravidade Reversa
  - Punho Magnético, Enxame Pútrido, Pesadelo
  - Bênção de Liberthia, Floração Cristal, Aura Sinistra, Poeira Estelar
- **64 novos feitiços**:
  - 18 spells que exploram os 20 efeitos novos
  - 18 utility/healing/CC (group_heal, mass_root, dispell, ...)
  - 10 destruição por meteoros (meteor_00..09)
  - 5 dash variants (dash_00..04 — FIRE/ICE/BLOOD/HOLY/ELDRITCH)
  - 5 imortalidade (immortal_00..04, resistência 10s+)
  - 8 movimento (movement_00..07 — voo, agua, salto, ...)
- **SpellDropHandler** — drop sistemático por tier de mob:
  - Mob comum (Monster): 3% UNCOMMON
  - HP>50/Raider: 10% RARE+
  - Boss (Wither/Dragon): 100% EPIC
- **106 textures procedurais** de scroll geradas via `gen_spell_textures.py`
- **Manual** atualizado com chapter "Sistema de Magia" + página Purification Bench

### Arquivos novos

- `effect/r156/CustomEffectsR156.java` (20 inner MobEffect classes)
- `magic/drops/SpellDropHandler.java`
- `gen_r156_batch.py` (gerador procedural de assets)
- 64 spell JSONs em `data/liberthia/spells/`
- 20 effect textures em `assets/liberthia/textures/mob_effect/`
- 64 spell scroll textures + 64 model JSONs

### Arquivos modificados

- `loom/LoomPortalBlock.java` — neighbor collapse
- `atmospheric/MagicFireBlock.java` — getShape fix
- `registry/ModEffects.java` — 20 novos efeitos registrados
- `manual/ManualContent.java` — chapter "Sistema de Magia" com 9 páginas
- `magic/factory/FactorySpellIndex.java` — 106 mapeamentos
- `assets/liberthia/textures/item/{false,ghost,mirror,sky}_weave.png` — fundo preto
- `lang/en_us.json` + `lang/pt_br.json` — 40 entries de efeito

---

## 🐛 v0.1.162 — r137 Bug Fixes (Code Review Round 1) (2026-05-26)

Auditoria sistemática do código pós-r119 identificou **12 bugs HIGH severity**. Todos corrigidos, projeto compila limpo (`./gradlew compileJava` → BUILD SUCCESSFUL, 0 erros).

### Bugs corrigidos

| # | Arquivo | Bug | Fix aplicada |
|---|---|---|---|
| 1 | `AgronomicSourcelinkBlockEntity.java` | `break` interno só saía do loop dz → contava até 33 crops (6.6× o cap intencional de 5), gerava 99 Source/tick ao invés de 15 | Labeled break `outer:` quebra os 3 loops de uma vez |
| 2 | `AutoMinerBlock.java` | `tryConsume(10 Source)` rodava antes de validar bloco → gastava Source mesmo quando todas 4 tentativas batiam em air/bedrock | Reorder: valida bloco PRIMEIRO, consome Source SÓ se for minerar |
| 3 | `SpellMirrorItem.java` | `mirror.hurtAndBreak(1, p, p2 -> p2.broadcastBreakEvent(hand))` chamava broadcast com hand do MIRROR (que tá no inventário, não na mão) | Lambda vazia `p -> {}` — item de inv não tem hand |
| 4 | `SpellMirrorItem.java` | 2 players com Spell Mirror se atacando podia recursar infinitamente (reflect → counter-reflect → reflect → …) | Flag NBT `liberthia.mirror_reflecting` durante exec do handler, skip se já ativo |
| 5 | `VoidInfectionEffect.java` | `sl.getGameTime() % 20` (tempo absoluto do mundo) podia nunca disparar damage tick nos primeiros 20 ticks ou pular timing após carregar save | Usa `inst.getDuration()` (tempo relativo ao efeito) — decrementa de 200 a 0, dispara em intervalos certos |
| 6 | `MobJarItem.java` | Liberar 2 jars com mesmo mob (via inventory dupe) → 2 entities com mesma UUID, despawn dedup matava uma | `entity.setUUID(UUID.randomUUID())` antes de `addFreshEntity` + log warn se NBT corrompido |
| 7 | `SourceSyncTicker.java` | Cache `Map<UUID, Integer>` static persistia entre worlds em single-player; voltar pro main menu + abrir novo world fazia primeiro tick ter `changed=false` (HUD ficava 0/0 até periodic sync) | `@SubscribeEvent onServerStopping` limpa caches |
| 9 | `SpellWeaverBlockEntity.java` | NPE em `onContentsChanged`: `level.isClientSide` quando `level==null` (Minecraft chama `load()` antes de setLevel ao deserializar) | Guard `level != null && !level.isClientSide` |
| 10 | `CompositionEffectApplier.java` (KNOCKBACK) | `position().subtract(caster.position()).normalize()` em zero-vector → NaN; `target.push(NaN, NaN, NaN)` quebrava entity physics | Guard `lengthSqr() > 1e-6`, fallback empurra na direção do `ctx.lookVec()` |
| 12 | `VoidLaserTracker.java` | Stack 5000 dmg (`DAMAGE_PER_TICK=250 × 20`) persistia entre morte/respawn/logout — player voltava pronto pra one-shot bosses | 3 novos listeners: `LivingDeathEvent`, `PlayerRespawnEvent`, `PlayerLoggedOutEvent` → todos chamam `clear()` |
| 13 | `CompositionEffectApplier.java` (PENETRATE+AOE) | AOE aplicava splash 0.5× finalDamage E PENETRATE aplicava bonus 0.5× finalDamage no mesmo target → +1.0× dano extra (dobrava) | Quando `aoeStacks > 0`, PENETRATE reduz pra 0.25× ao invés de 0.5× |
| 14 | `MiniBlackHoleEntity.java` | `pullEntities()` rodava todos os 40 ticks da vida, INCLUSIVE 10 ticks depois da explosão (puxava entities pro buraco já colapsado) | `if (age < EXPLODE_TICK) pullEntities()` |
| 15 | `SpellWeaverBlockEntity.java` | Output extraído via hopper/automation NÃO consumia base+glyphs (só `SpellWeaverMenu` chamava `consumeIngredientsOnExtract`) | Override `extractItem` do `ItemStackHandler` — hook centralizado |

### Padrões usados nas fixes

- **Reorder ops**: validar precondições ANTES de side effects (cost consume, broadcast)
- **Defensive guards**: `level != null`, `lengthSqr() > eps`, `inst != null`
- **Reentrancy flag**: NBT booleano pra prevenir recursão em event handlers
- **Lifecycle hooks**: death/respawn/logout pra limpar state per-player
- **Static cache cleanup**: `ServerStoppingEvent` evita leak entre saves single-player

### Build verificado

```
./gradlew compileJava
> BUILD SUCCESSFUL in 19s
> 36 warnings (apenas deprecation pré-existentes de ResourceLocation)
> 0 errors
```

---

## 🎯 v0.1.161 — r136 Source Sync Fix + HUD Overlap Cleanup (2026-05-25)

User reportou (screenshot) 2 problemas críticos: HUD mostrando "Source 100/100" sem decrescer ao castar feitiço, e múltiplos HUDs sobrepostos.

### Bug raiz #1 — Source não sincronizava server→client

`SourceData` armazena valor em `Player.getPersistentData()` (server-side NBT). Cliente lê via `SourceData.get(p)` que NÃO tem o NBT remoto — sempre retornava 0 ou valor antigo. HUD ficava travado em "100/100" enquanto server descontava normalmente. Tentativa de castar dizia "Source insuficiente" do server, mas client mostrava cheio.

### Solução — Packet S2C + Ticker periódico

**Novos arquivos:**
- `observation/source/SourceSyncS2CPacket.java` — packet que carrega `(int cur, int max)`, handler client-side atualiza `ClientSourceCache`
- `observation/source/SourceSyncTicker.java` — `@SubscribeEvent onPlayerTick`:
  - Compara cur/max atuais com último valor sincronizado
  - Se mudou OU `tickCount % 10 == 0` (failsafe periódico 0.5s), manda packet
  - Hook `PlayerLoggedInEvent` envia state inicial

**Modificados:**
- `network/ModNetwork.java` — registra `SourceSyncS2CPacket` no `packetId++`
- `observation/client/ClientSourceCache.java` (novo) — `Map<UUID, Integer> cur/max`, getters lockless

### Bug #2 — 2 HUDs sobrepostos mostrando mesmo valor

`ManaBarOverlay` (barra horizontal estilo Iron's Spells, posicionada acima da hotbar) e `SourceHud` (panel detalhado em BOTTOM_LEFT) ambos exibiam Source. Visualmente conflitavam.

### Solução — Disable + compactar

- `ManaBarOverlay.onRegisterOverlays` → comentado `event.registerAbove(...)` (desativa registro)
- `SourceHud` compactado de 160×52 pra 140×44, reposicionado y=H-44-65 (acima hotbar mas sem encostar)

---

## 🎨 v0.1.102 — Texturas GUI dos sets de matéria + lore items (2026-05-24)

Substituição em massa de **29 sprites de inventário/crafting** vindos do pack `texturas lsmp/`.

### Itens trocados

| Categoria | Itens (sprites GUI substituídos) |
|---|---|
| Pílulas | `blood_cure_pill`, `clear_matter_pill`, `daily_pill`, `dark_matter_pill`, `yellow_matter_pill` |
| Armadura Clear | helmet, chestplate, leggings, boots |
| Armadura Dark | helmet, chestplate, leggings, boots |
| Armadura Yellow | helmet, chestplate, leggings, boots |
| Ingots | `clear_matter_ingot`, `dark_matter_ingot` |
| Baldes | `clear_matter_bucket`, `yellow_matter_bucket` |
| Lore / artifacts | `boss_crown`, `cursed_idol`, `equilibrium_crystal`, `gravity_anchor`, `heart_of_the_mother`, `refined_containment_pendant`, `white_matter_pendant`, `worker_clone` |

### Decisões técnicas

- **3 chestplates (clear/dark/yellow)** tinham modelo 3D Blockbench customizado em `texture_size 64×32` com UVs específicas. Como o novo sprite é 16×16 flat, o modelo 3D foi convertido pra `item/generated` — agora rendeiza como sprite flat consistente em GUI, mão e chão. **A armadura no corpo do player NÃO foi afetada** (textura separada em `textures/models/armor/*_layer_*.png` ficou intacta).
- `worker_clone.json` apontava pra `minecraft:item/totem_of_undying`; agora aponta pro PNG local recém-importado.
- Demais 25 itens já usavam `item/generated` → simples overwrite de PNG.

### Não incluído neste batch (observação)

Os tools de cada set (`*_sword`, `*_pickaxe`, `*_axe`) **NÃO** foram atualizados — não vieram no pack. Visualmente os tools (dezembro) e a nova armadura (maio) não combinam mais. Quando o user mandar essas texturas, aplico do mesmo jeito.

---

## 📡 v0.1.101 — Antenna r28: FACING + tuning + Quantum Terminal + SVC voice relay (2026-05-24)

Camada de comunicação cross-dim ganhou direcionalidade, computador receptor e relay de voz pelo Simple Voice Chat.

### Dimensional Antenna r28

- **`FACING` property** (`HorizontalDirectionalBlock.FACING`) — antena orientada horizontalmente, modelo 3D com dish inclinada 22.5° apontando pra direção do facing
- **Shift + right-click mão vazia rotaciona** 90° clockwise + som `LEVER_CLICK`
- **Direction-based tuning** (`AntennaNetwork.calculateTuning`): dot-product de Vec3(facing) × Vec3(A→B), média dos dois ângulos × 100. Same-dim usa alinhamento real; cross-dim fixo 50%
- **Barra de tuning% na GUI** (verde ≥75 / laranja ≥40 / vermelho <40) + seta indicando facing (`/\`/`>`/`\/`/`<`)
- **Modelo 3D 3-elementos**: base (16×6×16) + pole (2×6×2 central) + dish (8×5×4 com `rotation.angle 22.5 axis x`)
- Blockstate variants pra 4 facings (y=0/90/180/270)

### Quantum Terminal — computador receptor

Novo bloco `liberthia:quantum_terminal` (crafting: amethyst + dark matter shard + redstone + glass + iron).

- **`QuantumTerminalBlockEntity`** com FE buffer 10k, consumo 1 FE/sec, ON/OFF depende de FE > 0
- **Listener pattern**: implementa `AntennaNetwork.BroadcastListener` — registra/desregistra automaticamente quando active. Quando broadcast acontece em qualquer freq, todos os listeners inscritos recebem
- **Log** de até 50 mensagens recebidas (NBT-persistente, thread-safe via `synchronized LinkedList`)
- **GUI 256×192** (`QuantumTerminalScreen`): título + badge ON/OFF + barra FE + input de freq + botão "Tunar" + área grande de log (mensagens mais recentes em cima) + input de msg + botão "Enviar"
- **Send broadcast direto** via `AntennaNetwork.broadcastMessage` (custa 10 FE por mensagem) — terminal funciona como transmissor leve, não precisa de antena pra enviar
- Pacote único `TerminalActionC2SPacket` cobre `SET_FREQ` e `SEND_MSG` (1 byte action + payload). Anti-cheat: range check 9 blocos
- 4 texturas geradas via PIL: front (scan lines em screen + status LEDs + power button), side (vents + rivets), top (checkerboard cooling grille + antenna mount), bottom (rubber feet)

### Simple Voice Chat relay (`AntennaVoiceRelay`)

Hook em `LiberthiaVoicePlugin.onMicPacket`:

1. Quando player fala E está num raio de 12 blocos de uma antena ativa registrada
2. Pega a freq dessa antena
3. Pra cada OUTRA antena tunada na mesma freq:
   - Cria/reutiliza `LocationalAudioChannel` na posição da antena receptora
   - `channel.send(opusData)` — forward direto sem decodificar/re-encodar
4. Audio chega como locational nos players próximos das antenas receptoras

**Cache**: `Map<senderUuid+@+receiverGlobalPos, LocationalAudioChannel>` evita criar canal novo a cada frame Opus de 20ms.

**Cleanup**: hook em `LiberthiaVoicePlugin.resetSession` (logout do player), `AntennaNetwork.unregister` (antena destruída) e `AntennaNetwork.clearAll` (server stop).

### Arquivos novos / alterados

**Java:**
- `block/QuantumTerminalBlock.java` (novo)
- `block/entity/QuantumTerminalBlockEntity.java` (novo)
- `menu/QuantumTerminalMenu.java` (novo)
- `client/screen/QuantumTerminalScreen.java` (novo)
- `network/packet/TerminalActionC2SPacket.java` (novo)
- `voice/AntennaVoiceRelay.java` (novo)
- `voice/AntennaNetwork.java` (+ `calculateTuning`, `BroadcastListener`, `addListener`/`removeListener`, listener fan-out em `broadcastMessage`)
- `voice/LiberthiaVoicePlugin.java` (+ chamada `AntennaVoiceRelay.relayMicPacket` em `onMicPacket`, cleanup em `resetSession`)
- `block/DimensionalAntennaBlock.java` (+ `FACING`, rotate via shift+rclick, `rotate()`/`mirror()`)
- `block/entity/DimensionalAntennaBlockEntity.java` (+ `calculateBestTuning`, `getFacing`, `lookupFacing`, slots 6-7 do ContainerData)
- `menu/DimensionalAntennaMenu.java` (data 6→8 slots; `getTuningPercent`, `getFacing`)
- `client/screen/DimensionalAntennaScreen.java` (+ barra tuning, seta facing)
- `registry/ModBlocks.java`, `ModBlockEntities.java`, `ModItems.java`, `ModMenuTypes.java`, `ModCreativeTabs.java`, `network/ModNetwork.java`, `client/ClientModEvents.java`

**Resources:**
- `assets/liberthia/blockstates/quantum_terminal.json` (4 facing variants)
- `assets/liberthia/models/block/quantum_terminal.json` (modelo 3D com nub + LED antena)
- `assets/liberthia/models/item/quantum_terminal.json`
- `assets/liberthia/textures/block/quantum_terminal_{front,side,top,bottom}.png`
- `assets/liberthia/blockstates/dimensional_antenna.json` (4 facing variants)
- `assets/liberthia/models/block/dimensional_antenna.json` (3-element 3D model com dish rotacionada)
- `data/liberthia/recipes/quantum_terminal.json`
- `data/liberthia/loot_tables/blocks/quantum_terminal.json`
- Lang `en_us` + `pt_br` (Quantum Terminal / Terminal Quântico)

---

## 🛡️ v0.1.23 — Possession à prova de bug + Cinto universal + /reset + 9 efeitos com textura

Release consolidada de 7 sprints internas (builds **0.1.74 → 0.1.80**) focada em **estabilidade da possessão** e **usabilidade**.

### Possession — 4 rounds de fixes (r10 → r13)

**r10 — Lock total do possuído:**
- Client: trava rotação capturando yaw/pitch no `START` do tick e restaurando no `END` (anula mouse delta local)
- Consome buffers de `keyDrop`/`keySwapOffhand`/`keyAttack`/`keyUse`/`keyHotbarSlots` cada tick
- Mouse buttons **cancelados** enquanto locked
- Server bloqueia `AttackEntity`/`ItemToss`/`PlayerInteract.*` via 6 novos handlers em `PossessionManager`

**r11 — Sanity reset pós-end:**
- Criei e removi `onPossessedAttack` (cancelava `AttackEntityEvent` legítimo via `PossessionAttackC2SPacket`)
- Solução: `Set<UUID> ATTACK_BYPASS` gerenciado por try/finally no packet handler
- `sanityResetPlayer()`: `invulnerableTime/hurtTime/hurtDuration = 0`, `connection.teleport` resync, `closeContainer`, `onUpdateAbilities`, `inventoryMenu.broadcastChanges` (resync Curios)

**r13 — State sync defensivo:**
- **Removido `onPossessedAttack`** (causa-raiz mais provável do bug "players não tomam dano pós-posse")
- Server reenvia `EndPossessionS2CPacket` em **todos os 4 packets** (`Attack`/`Move`/`Use`/`BreakBlock`) quando sender não tem posse ativa → força client a sair de estado active
- Reset de flags client no `LOGIN` (`ClientPlayerNetworkEvent.LoggingIn`) cobre clients que disconectaram em estado bugado

### Astaron Reliquia — proteção universal

Antes só ativava em hits diretos pesados (≥50% max HP). Agora cobre:

| Tipo de dano | Comportamento |
|---|---|
| **Afogamento** (`DamageTypeTags.IS_DROWNING`) | Procura terra seca em raio 32 via spiral search; aplica `Water Breathing 20s` + restaura air supply |
| **Fogo/Lava** (`DamageTypeTags.IS_FIRE`) | Busca por radius com score (distância do fogo + proximidade água a 8b); aplica `Fire Resistance 20s` + `clearFire` |
| **Dano fatal genérico** (≥ HP atual) | Respawn position ou world spawn |

Defesa em profundidade: handler `onDamage` (`LivingDamageEvent HIGHEST`) cobre caso onde `LivingHurtEvent` foi consumido antes. Helpers novos: `findDryGroundNear`, `findFireSafeSpot`, `hasFireNearby`, `hasWaterNearby`.

### Chaves inalienáveis — 4 layers

1. **Flag NBT no player** (`liberthia.astaron_key_given`/`liberthia.flame_key_given`) impede duplicação; spawna 1 vez ao equipar, **não respawna** se player dropar. Desequipar reseta.
2. **`ItemTossEvent` cancelado** pra ambas as keys: Q-drop/drag-out devolvem item ao inv
3. **`LivingDropsEvent`** remove keys do drop list na morte (não caem no chão)
4. **`EntityItemPickupEvent`** bloqueia outros players de pegarem (owner UUID check)

### `/liberthia reset` — comando de emergência

Op-level 2. `/liberthia reset` (self) ou `/liberthia reset <player>`.

Limpa: `PossessionManager.end` (ambos sentidos), `ATTACK_BYPASS`, `EndPossessionS2CPacket` + `LockedInputS2CPacket(false)` enviados pro client, `invulnerableTime`/`hurtTime`/`hurtDuration` = 0, `clearFire`, `PlayerImmortalityHandler.disable`, `VisionSwapManager.end`, `connection.teleport` resync, `closeContainer`, `onUpdateAbilities`, `inventoryMenu.broadcastChanges`.

### Clear Matter Injector (Seringa) — reescrita

Antes: right-click sem shift = no-op. Agora:
- **Right-click no AR** (com/sem shift): aplica em si próprio
- **Right-click em outro player/mob**: aplica nele
- Cura: **+50 White Matter (50% do cap)**, -50 Dark Matter, -30 infection, limpa mutations + 9 efeitos negativos, aplica `Clear Shield` 3min
- Cooldown 5s. Cada componente independente (cura nunca falha em players válidos)

### 9 texturas de efeitos novas

Geradas via `tools/generate_effect_textures.py` (Python/PIL 18×18 pixel art):

| Efeito | Visual |
|---|---|
| `sanguine_vitality` | Coração vermelho com brilho rosa |
| `blood_frenzy` | Lâmina vermelha + chamas laterais |
| `hemo_sickness` | Frasco de sangue podre + bolhas verdes |
| `infected_sight` | Olho almond com íris violeta + veias |
| `blood_step` | Pegada vermelha com toes |
| `feather_fall` | Pena branca/azul com espinha |
| `dark_matter_resistance` | Escudo violeta (0xAA60FF) |
| `clear_matter_resistance` | Escudo branco-perolado (0xB0E8FF) |
| `yellow_matter_resistance` | Escudo dourado (0xFFD23F) |

### Flesh Mother nerf

User pediu cooldown mínimo de 10s pra sonic e knockback. Antes era 4-9s (sonic) e 6-10s (pound). Agora **fixo 200 ticks (10s)** em todas as fases.

---

## 🎮 Mod (Forge 1.20.1) — v0.1.14 → v0.1.44

Bloco grande de evolução cobrindo refinamento de sistemas existentes + novas features.

### Items / Mecânicas novas

- **Matter Pill Brewer** (`liberthia:matter_pill_brewer`) — bloco alquímico que fabrica pílulas a partir de ingot purificado + glass bottle. 1 lote = 3 s, gera 3 pílulas do tipo correspondente (DM/CM/YM). Sem energia.
- **Pílulas dedicadas por matter**:
  - `dark_matter_pill` — purga DM do perfil
  - `clear_matter_pill` — purga WM (antes curava DM, corrigido)
  - `yellow_matter_pill` — purga YM
- **Matter Extractor + Matter Tank + Matter Pipes** (sistema multiblock inspirado no Create):
  - Extractor drena matéria de player parado em cima (raio 0, exigência estrita após v0.1.21)
  - Tank com BER que renderiza fluido visualmente dentro do bloco (preenchimento real, gradient por tipo)
  - 3 variantes de pipe (dark/clear/yellow), filtro by-pipe
  - Tank slot IN aceita balde vazio OU matter bucket cheio — tick processa automático em 2 direções
- **Matter Purifier** — purifica ingots raw em ingots purificados (gasta FE). Saída usada nas armaduras de Matter.
- **Sistema de itens infectados** — `Alchemizer` infecta items, `Purifier` (item) purifica de volta. NBT-tagged.
- **Curios artifacts** (Curios API):
  - 3 pendants (Dark / Clear / Yellow Matter Pendant) — suprimem efeitos proximidade do tipo
  - 3 relíquias do Astaron (Flame Key, Astaron Access Key, Pulso de Astaron, Pés Queimantes, Cinto Sigil)
  - Channel sword: espada Clear Matter c/ hold para canalizar lifesteal + dash; Purify option (600k FE consumido = remove inf. global do mundo)
- **Blood Tree (família completa)**: `blood_sapling`, `blood_log`, `blood_leaves`, `stripped_blood_log`, e variantes `sanguine_*` (família original). Sapling sozinho infecta jogadores próximos (threshold 1, era 3 — v0.1.44).
- **BloodLeavesBlock custom** — sobrescreve `randomTick` (no-op) e `updateShape` (força `PERSISTENT=true`) pra impedir decay quando vanilla não reconhece o tronco como log. Folhas continuam dropando saplings, podem ser quebradas — só não desaparecem mais.
- **Vision Swap** — White Matter Vision: jogador com WM ≥ N vê automaticamente através dos infectados.
- **Possession** — item de domínio remoto (controla mob/animal/player) com captura input via `ClientTickEvent` (raw key) + `connection.teleport` server-side pra sync.
- **Bossbar global** — `/liberthia boss <player>` faz tela escurecer + bossbar mundial.
- **Imortalidade total** — `/liberthia immortal <player>` impede morte mesmo por dano mágico/cmd.

### Bug-fixes importantes nessa onda

- **Pés Queimantes / Cinto / Keys somindo** (v0.1.43): items registrados sem `.durability(N)` tinham `maxDamage=0` — `damageStack()` deletava no primeiro drain. Fixed em 3 lugares: `.durability(19200)` no `ModItems`, safety check em `CuriosBridge.damageStack` (`if (stack.getMaxDamage() <= 0) return;`), grace period em `FlameKeyItem` + `AstaronAccessKeyItem` aumentado de 100 → 600 ticks.
- **Possession sem movimento**: `MovementInputUpdateEvent` só dispara em mudança. Trocado por leitura raw do `KeyMapping.isDown()` no `ClientTickEvent`.
- **Matter Extractor enchendo tank vizinho**: agora só extrai quando player está EM CIMA do bloco (não só "próximo").
- **Matter Pill cap**: limitado em 20 % máximo dos pontos do perfil — antes podia zerar perfil inteiro.
- **Pipe filter exclusivo do Item Pipe**: extractor/inserter agora mostram msg "Esse filtro vai no Item Pipe, não aqui".

### Tagged NBT consolidado

- `"Purified"` em items processados pelo Purifier
- `"MatterInfected"` em items processados pelo Alchemizer
- `"GraceTicks"` em keys/Curios pra evitar perda no primeiro drain
- `"liberthia.bloodtree.last_msg"` em persistent data do player para cooldown da mensagem ambiental

### Sistema de proximidade da Blood Tree

`BloodTreeProximityHandler` faz scan 17³ a cada 60 ticks por player vivo (skip creative/spectator). Threshold ≥ 1 bloco de blood/sanguine (qualquer um da família) → aplica Wither I 5 s + ganha DM (0.3 × min(count, 8) por tick = até 2.4 / check). Action bar message c/ cooldown 1 min.

---

## 🌐 Painel admin web (v0.1.20 → v0.1.44)

Sistema 3-tier (mod ↔ Spring Boot ↔ React + Vite) ficou robusto nessa rodada.

### Backend (Spring Boot)

- **`BackendConfig` (entidade JPA)** — KV persistente com 2 chaves novas:
  - `MOD_TOKEN` — token override do mod, sobrevive restart
  - `MOD_TOKEN_LOCKED` — quando `"true"`, auto-register do mod NÃO pode sobrescrever
- **`ModRegistry`**:
  - Boot: lê `MOD_TOKEN` do DB primeiro, env var como fallback
  - `applyToken()` (chamado pelo painel) → seta token + ativa lock + rebuild `WebClient`
  - `update()` (chamado pelo mod a cada 60 s) → se lock ativo, **IGNORA** o token vindo do mod e loga warning
  - `unlockToken()` — destrava (operador volta ao modo "mod manda")
  - URL fixa do env protege contra `liberthia-server.toml` com `admin_api.public_address` errado
  - `WebClient` com timeouts (connect 3 s, response 8 s, read/write 8 s) pra não pendurar tomcat threads quando o mod cai
- **`ModBridgeClient`** — circuit breaker (abre por 15 s após 3 falhas consecutivas, retorna 503 em 1 ms em vez de pendurar request)
- **`ModConfigController`** — `GET/POST /api/admin/mod-config/token`, `POST /test-connection`, `POST /unlock`. Tudo protegido por `AuthFilter` (Bearer JWT admin).
- **`BannedItemController`** — quando o circuito está aberto, loga `Mod offline (circuito aberto por mais Xs)` sem tentar a request, evitando spam

### Frontend (React + Vite + Tailwind)

- **`/mod-config` (`ModConfigPage.tsx`)** — tela completa:
  - **Card 1**: token atual (fingerprint), source badge (DB/ENV), lock badge (🔒/🔓), timestamps, banner verde "Token travado" + botão **🔓 Destravar** quando ativo
  - **Card 2**: input UUID com validação + confirmação dois-passos antes de salvar (toast "Token salvo + travado")
  - **Card 3**: botão "🔌 Testar agora" → faz call real no mod pulando cache/circuit breaker, mostra TPS/players/MOTD ou erro
  - Auto-refresh 5 s
- **`modConfigApi`** (`lib/api.ts`) — `getToken`, `setToken`, `testConnection`, `unlock`

### Comportamento "anti-mudança espontânea"

Problema histórico: o mod re-registrava a cada 60 s com SEU token (do `liberthia-server.toml`), e o backend sobrescrevia o que o admin tinha salvo manualmente — token "mudava sozinho".

Solução (v0.1.44): salvar pela tela ativa lock automático persistido em `backend_config.MOD_TOKEN_LOCKED`. Auto-register continua chegando, mas é silenciosamente ignorado (warning no log). Sobrevive restart do backend.

---

## 🎮 Mod (Forge 1.20.1) — v0.1.12 → v0.1.13

### Items adicionados

- **`liberthia:white_matter_pendant`** — Pingente cosmético de matéria branca refinada
  - Equipado em slot Curios (necklace/charm)
  - **Suprime** efeitos de proximidade de matter blocks enquanto equipado
  - Durabilidade: 7200 (4h de exposição contínua)
  - Recipe: `GSG / CWC / GDG` (4 gold + 2 dark matter shard + 1 clear matter pill + 1 yellow matter ingot)
  - Quando quebra/remove, os efeitos voltam a fluir normalmente

### Blocos adicionados (12 novos infectados)

Variantes de dirt/sand/stone/grass para cada uma das 3 matérias. Pisar → aplica efeito + ganha matéria no perfil.

| Bloco | Efeito ao pisar | Matter gain |
|---|---|---|
| `dm_infected_dirt` | Wither I (60t) | +0.5 DM |
| `dm_infected_sand` | Slowness I (80t) | +0.4 DM |
| `dm_infected_stone` | Blindness (40t) | +0.3 DM |
| `dm_infected_grass` | Hunger (80t) | +0.6 DM |
| `wm_bleached_dirt` | Weakness (80t) | +0.5 WM |
| `wm_bleached_sand` | Levitation (30t) | +0.4 WM |
| `wm_bleached_stone` | Glowing (100t) | +0.3 WM |
| `wm_bleached_grass` | Night Vision (200t) | +0.6 WM |
| `ym_unstable_dirt` | Hunger (100t) | +0.5 YM |
| `ym_unstable_sand` | Nausea (80t) | +0.4 YM |
| `ym_unstable_stone` | Unluck (200t) | +0.3 YM |
| `ym_unstable_grass` | Weakness (100t) | +0.6 YM |

Texturas geradas via `tools/generate-infected-textures.py` (16x16 pixel art com paletas únicas por matter).

### Items REMOVIDOS

- **Cleansing Grenade** (item + entity + renderer + ModEntities)
- **Glitch Block** (block + item + InfectionLogic refs + ProtectionUtils refs)
- **Liberthia Wrench** (item + EnergyCableBlock interaction)
- **White Matter Bomb** (block + item)

Stubs deixados nos arquivos `.java` pra preservar histórico e não quebrar builds incrementais.

### Mecânicas adicionadas

#### Player proximity matter effects
Player próximo (raio 5) de blocos de matéria recebe efeitos escalados:
- **1-4 blocos**: tier I (60t, amp 0)
- **5-9 blocos**: tier II (80t, amp 0)
- **10+ blocos**: tier III (120t, amp 1) + efeito severo adicional

Implementado em `MatterProfileEvents.applyProximityMatterEffects` rodando a cada 40 ticks (2s). White Matter Pendant suprime totalmente esses efeitos quando ativo.

#### Sample Vial tooltip blindado
Antes: tooltip do Sample Vial mostrava DM/WM/YM e mutação dominante no inventário.
Agora: tooltip só diz "Amostra coletada — Use o Matter Analyzer para identificar a composição". Detalhes só aparecem no GUI do Matter Analyzer.

#### Compass dimensional melhorado
- **16 rifts** gerados por dimensão (era 8)
- Range: 250-2500 blocos do spawn (era 400-1500)
- Capacidade: 30 baldes por rift (era 100) — força exploração
- **SHIFT+click no compass** cicla pro próximo rift mais próximo (não fica preso no mesmo)
- Mostra contagem de rifts ativos na mensagem

### Manual do Pesquisador

- **Forçado PT-BR sempre** — usuário com cliente EN via só 6 capítulos (parcial) em vez dos 39 completos
- **Layout refeito**: sidebar single-column com 11 capítulos por página, paginação `Cap N/M`, sem mais sobreposição entre lista e conteúdo
- 39 capítulos / 363 páginas cobrindo todo o catálogo

### Viewer 3D (frontend)

- Parser `.bbmodel` agora processa hierarquia `outliner` (groups com rotation/origin)
- Antes: partes rotacionadas de modelos complexos renderizavam no lugar errado
- Suporte mantido para `.gltf`, `.glb`, `.obj`

---

## 🔧 Backend (Spring Boot 3) — v9 → v105

### Connection reliability

- **WebClient timeout configurado** em `ModRegistry`: connectTimeout 3s, responseTimeout 8s, read/write 8s (antes era infinito → backend travava 30s por request quando mod offline)
- **Circuit breaker** já existente em `ModBridgeClient` agora abre em ~24s (3 falhas × 8s) em vez de ~90s
- Frontend recebe 503 mod_offline em ~1ms quando circuito aberto

### Bugs críticos corrigidos

- **NullPointerException em `/api/tester/beta-items`** (linha 83 de `TesterContentController`) — `Map.of("myVote", null)` lançava NPE porque `Map.of()` não aceita null. Trocado por `LinkedHashMap`.
- **Whisper pause não matava workers ativos** — agora `POST /api/voice/whisper/pause` chama `whisperSvc.killAllActive()` → `Process.destroyForcibly()` em todo `whisper-cli` em andamento. Resposta inclui `{killedActive: N}`.
- **Backend não compilava** (stale Gradle cache em `KvRepository` references) — fixado com `./gradlew clean build`.

### Bulk import (v103)

4 endpoints novos pra admin importar JSON gerado pela IA:
- `POST /api/admin/tester/beta-items/bulk-import`
- `POST /api/admin/tester/rewards/bulk-import`
- `POST /api/admin/tester/changelog/bulk-import`
- `POST /api/admin/tester/roadmap/bulk-import`

Cada um faz **upsert por chave natural** (itemId/name/title). Itens removidos do JSON NÃO são deletados do banco (preserva histórico).

### Curios bridge expandido

`CuriosBridge` agora suporta:
- Containment Glove (já existia)
- White Matter Pendant (novo) — com `damagePendant()` pra drenar durabilidade ao longo do tempo

---

## 🌐 Frontend (React + Vite) — v9 → v103

### Botão "Importar JSON" nos 4 admin tabs

`BulkImportButton.tsx` reutilizável com upload de arquivo + paste de JSON. Mostra summary `{created, updated, errors[]}` após o import. Wired em:
- Tester Admin → Items Beta
- Tester Admin → Rewards
- Tester Admin → Changelog
- Tester Admin → Roadmap

### Viewer 3D fix
Parser `.bbmodel` reescrito em 2 passes (mesh build + outliner traversal) — modelos complexos do BlockBench renderizam corretamente.

### ItemAutocomplete fix
Bug onde IDs custom (não no registry) eram perdidos no save — agora `onChange` propaga IMEDIATAMENTE ao digitar, não só ao selecionar do dropdown. Feedback visual com border verde (registrado), âmbar (custom válido), vermelho (formato inválido).

---

## 🛠 Tooling

### Scripts Python (em `tools/`)

- **`generate-items-update.py`** — Lê `ModItems.java` + `ModBlocks.java` + recipes do mod, gera 4 JSONs canônicos pros bulk-import
- **`generate-infected-textures.py`** — Gera as 12 texturas de blocos infectados (PNG 16×16) + blockstates + models + ajuste do `blood_bucket.png`
- **`generate-pendant-texture.py`** — Gera textura pixel-art do White Matter Pendant

### Documentação

- **`tools/MOD_UPDATE_PROTOCOL.md`** — Lembrete pra IA: toda vez que adicionar/remover item/bloco/feature, atualizar os 4 JSONs em `tools/`. Schemas canônicos documentados.
- **`docker-stack.fixed.yml`** — Compose corrigido com `order: start-first` (era stop-first → 60s de downtime durante deploy)

---

## 📦 Imagens Docker prontas pra deploy

| Imagem | Tag atual | Mudanças nessa rodada |
|---|---|---|
| `murilonerdx/lsmp-backend` | **v105** | NPE fix + Whisper pause kills workers + WebClient timeouts + BulkImport endpoints |
| `murilonerdx/lsmp-frontend` | **v103** | BulkImportButton + Viewer 3D outliner + ItemAutocomplete fix |

### Como atualizar produção

```bash
ssh root@srv614263
# Cola o conteúdo de docker-stack.fixed.yml em /root/docker-stack.yml
# (NÃO esqueça do order: start-first nos dois serviços)
docker stack deploy -c /root/docker-stack.yml lsmp
sleep 30
curl https://backend.astaroneremita.com/api/mod-status
```

---

## ⚠ Sobre o erro `Servidor Minecraft offline` recorrente

Causa raiz: o **mod no servidor MC** (em `lsmp.ddns.net:25580`) **não está aceitando conexões** durante esses intervalos. Não é o backend nosso. Checar no servidor MC:

1. Console deve mostrar `AdminHttpServer listening on 0.0.0.0:25580 (token=XXXX)` ao iniciar
2. Firewall do VPS do MC liberando porta 25580 inbound
3. Mod jar `liberthia-0.1.12.jar` presente em `mods/` do servidor

O backend já trata graciosamente: circuit breaker abre em ~24s, frontend mostra banner "Servidor MC offline", próximas calls retornam 503 em ~1ms sem travar.
