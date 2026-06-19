# Spell Animation Pipeline — Liberthia Mod

> Como criar animações de feitiços estilo Iron's Spells N Spellbooks no nosso mod.
> v0.1.151 r119 — todos os patterns são originais, escritos no zero usando APIs públicas Forge/Minecraft.

Este documento mostra o **pipeline completo de VFX** que já está implementado no codebase, e como adicionar
animações novas. Cada camada existe em arquivos reais — use as referências pra estudar o código.

## Pipeline em 5 camadas

```
┌──────────────────────────────────────────────────────────┐
│ 1. SPRITESHEET PNG  →  ParticleSheet (.mcmeta)            │
│ 2. ParticleType  →  ConfigurableParticleOptions           │
│ 3. ProjectileEntity  →  spawn no level                    │
│ 4. EntityRenderer  →  billboard glow 3D                   │
│ 5. ScreenShake S2C  →  câmera trema no impacto            │
└──────────────────────────────────────────────────────────┘
```

Cada uma é independente — você pode usar só algumas se o feitiço não precisa de projétil
(ex: AOE pulse usa só camada 1+2; healing aura usa camada 1+2+5).

---

## Camada 1 — Spritesheet animated particles

**Onde:** `src/main/resources/assets/liberthia/textures/particle/spell_trail.png`
**Meta:** `spell_trail.png.mcmeta`

O spritesheet é um PNG vertical de N frames 16×16 empilhados (256 de altura = 16 frames).
O `.mcmeta` informa o Minecraft de animar entre eles:

```json
{
  "animation": {
    "frametime": 2,
    "interpolate": true
  }
}
```

**Como gerar:** Use Python+PIL — ver `build/gen_spell_textures.py` que produz os 16 frames
com fade-in fade-out gradient e blur radial.

**Resultado:** uma partícula que muda 16x ao longo da vida. Aparência de "estrela palpitante"
ou "flama crackling".

---

## Camada 2 — ConfigurableParticleOptions

**Onde:** `src/main/java/br/com/murilo/liberthia/particle/engine/ConfigurableParticleOptions.java`

Já existe um sistema de partículas custom com 11 parâmetros (cor RGBA, scale, gravity, lifetime,
glow, billboard, depth). Não invente um novo — REUSE.

```java
// Exemplo: pulse de cast roxo brilhante
ConfigurableParticleOptions castPulse = new ConfigurableParticleOptions(
        ModParticles.ENGINE_PARTICLE.get(),
        0.66F, 0.4F, 1.0F, 0.9F,   // RGBA (purple-pink)
        0.4F, 0.05F,                // scale start, scale shrink/tick
        14,                         // lifetime ticks
        0.0F, 0.85F,                // gravity, drag
        0.5F,                       // glow intensity
        false, true, true);         // collide, billboard, emissive

level.sendParticles(castPulse, hand.x, hand.y, hand.z,
        15,           // count
        0.3, 0.3, 0.3, // dx, dy, dz spread
        0.1);          // speed
```

**Padrões úteis:**
- Cast pulse: `15 particles, lifetime 14, glow 0.5`
- Hit explosion: `40 particles, lifetime 20, glow 1.0`
- Sustained beam: usar `LineData` (ver camada 4)

---

## Camada 3 — ProjectileEntity custom

**Onde:** `src/main/java/br/com/murilo/liberthia/magic/spell/SpellProjectileEntity.java`

Subclasse de `AbstractHurtingProjectile` com hooks de:
- `tick()` — spawn trail particles em cada movimento
- `onHit()` — explosão final + dano
- NBT save/load — escola, dano, raio

**Como spawnar:**

```java
SpellProjectileEntity proj = new SpellProjectileEntity(
        level, caster, motion,
        SpellSchool.FIRE, 25F);
proj.withConfig(
    60,          // lifetime ticks
    false,       // homing
    2.5F,        // explosion radius
    true);       // ignite blocks
level.addFreshEntity(proj);
```

**Trail:** o `tick()` interno chama `spawnTrailParticles()` em cada tick, com cor da school
e intensidade modulada pelo lifetime (mais intensa no começo).

**Para spell sem projétil** (ex: instant teleport, AOE pulse): NÃO use ProjectileEntity. Aplique
o efeito direto e spawn particles em um loop:

```java
// AOE pulse — 360°
for (int i = 0; i < 360; i += 10) {
    double ang = Math.toRadians(i);
    for (double r = 0; r < 8; r += 0.5) {
        level.sendParticles(...);
    }
}
```

---

## Camada 4 — EntityRenderer (3D billboard glow)

**Onde:** `src/main/java/br/com/murilo/liberthia/magic/spell/client/SpellProjectileRenderer.java`

Renderer que desenha 3 quads em **billboard** (sempre faceando a câmera):
1. Inner core — brilhante, scale 0.6
2. Middle glow — semi-transparente, scale 1.0
3. Outer halo — pulsing, scale 1.5 + sin(time)

Usa `PoseStack` + `MultiBufferSource` direto, com `RenderType.entityTranslucent`.

**Pattern crítico:** sempre normalize a posição via `camera.getPosition()` pra render relativo
ao viewer, senão a partícula "salta" quando você anda.

```java
// O quad é desenhado FACEANDO a câmera:
poseStack.mulPose(camera.rotation());
```

**LineData** (`vfx/SpellBeam.java`): variante pra desenhar feixes contínuos — interpola posição
start→end ao longo do lifetime, dando aparência de raio sustentado.

---

## Camada 5 — Screen shake S2C packet

**Onde:**
- Server side: `src/main/java/br/com/murilo/liberthia/magic/spell/ScreenShakeS2CPacket.java`
- Client side: `src/main/java/br/com/murilo/liberthia/magic/spell/ScreenShakeClient.java`

Server envia packet com intensity (0-2) e duration (1-40 ticks). Client aplica delta de
yaw/pitch na câmera via `ViewportEvent.ComputeCameraAngles`.

**Como disparar:**

```java
var pkt = new ScreenShakeS2CPacket(1.0F, 25); // intensity, ticks
for (var sp : level.players()) {
    if (sp.distanceToSqr(origin) <= 64*64) {
        ModNetwork.sendToPlayer(sp, pkt);
    }
}
```

**Decay:** intensidade decai linearmente por tick. Visualmente: tela vibra forte no início,
diminui até parar.

**Pattern Apolão (r119):** ver `SpellLibrary.broadcastScreenShake()` — usado em spells >1000 dmg
pra dar feel cinematográfico.

---

## Pipeline completo — exemplo "fireball"

1. Cast → `UniversalSpellScrollItem.use()` valida mana/cooldown
2. Chama `SpellLibrary.projectile(ctx, fireDamageSource, 2.5F, true)`
3. `projectile()` spawna:
   - `SpellProjectileEntity` no level (camada 3)
   - Cast pulse particles na mão do caster (camada 2)
   - `HelixSpawner.spawnRing()` na mão pra windup (camada 2)
   - Som dual ENDER_DRAGON_FLAP + AMETHYST_BLOCK_CHIME
4. Cada tick do projectile:
   - `SpellTrailParticle` é spawnada com cor FIRE (camada 1+2)
5. Renderer client-side desenha 3-layer glow (camada 4)
6. Quando o projectile hits: explosão + screen shake leve (camada 5)

---

## Apolão tier — efeitos cinemáticos

Spells de >1000 dano (`solar_apocalypse`, `apocalypse`, etc.) usam **TODAS as camadas
+ multi-stage VFX**. Pattern:

```java
private static boolean castSolarApocalypse(CastContext ctx) {
    Vec3 origin = ctx.pickHit(48);

    // Camada 5: screen shake forte 30 ticks
    broadcastScreenShake(ctx, 30, 1.0F);

    // Camada 2: anel concentrado de FLAME (radial 360°, raio 0→12)
    for (int i = 0; i < 360; i += 5) {
        double ang = Math.toRadians(i);
        for (double r = 0; r < 12; r += 1) {
            ctx.level.sendParticles(ParticleTypes.FLAME, ...);
        }
    }

    // Explosão real do MC (block damage + KB)
    ctx.level.explode(...);

    // Som — multiplexa GENERIC_EXPLODE + LIGHTNING_BOLT_THUNDER
    playCastSound(ctx, SoundEvents.GENERIC_EXPLODE, 0.5F);
    playCastSound(ctx, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F);

    // AABB damage 12-block radius
    for (LivingEntity le : ctx.level.getEntitiesOfClass(...)) {
        le.hurt(ds, 1500F);
        le.setSecondsOnFire(40);
    }

    return true;
}
```

**Princípios visuais:**
- 30+ ticks de duração visual (NÃO 1 frame)
- Multi-cor: combine 2-3 ParticleTypes diferentes (FLAME + SOUL_FIRE_FLAME + END_ROD)
- Multi-stage: trama várias ondas (ring 0..2, ring 2..4, ring 4..6) com offset temporal
- Som duplo: low-freq + high-freq simultâneos pra sensação cheia
- Dano alto + status effects (WITHER, BLINDNESS) pra reforçar feel "lendário"

---

## Composições (r119) — adicionar VFX por modifier

`SpellModifier` afetam stats (já implementado em `SpellComposition`), mas ALGUNS também
fazem sentido visualmente:

- **AOE** — adicionar pulso radial de partículas após hit
- **MULTISHOT** — spawnar 3 projéteis em leque
- **CHAIN** — desenhar `SpellBeam.drawChain()` entre alvos
- **APOLAO** — adicionar pulse de partícula extra-bright + shake

Hook: `CompositionEffectApplier.applyPostCast()` é chamado depois do base cast. Adicione
VFX ali quando o modifier está presente.

---

## Performance — particle budget

- **Casual cast:** ~20-30 particles. OK em qualquer client.
- **AOE:** ~100-200 particles. OK mas notável.
- **Apolão:** 500-1000+ particles em 1-2 segundos. Aceitável só pra spells raros.

Use `if (level.random.nextDouble() < 0.5)` pra reduzir contagem em particle loops grandes
quando aplicável. Para particles "background" (não core do VFX), pular 50% é imperceptível.

---

## Cookbook rápido

| Quero fazer... | Use... |
|---|---|
| Projétil mágico que voa e explode | `SpellLibrary.projectile()` helper |
| Dash/teleporte instantâneo | `setDeltaMovement` + portal particles loop |
| AOE radial pulse | `for (ang 0..360) for (r 0..N)` loop de sendParticles |
| Beam contínuo entre 2 pontos | `SpellBeam.drawSegment()` (LineData) |
| Chain pulando entre alvos | `SpellBeam.drawChain()` |
| Helix/ring no caster | `HelixSpawner.spawnRing()` ou `spawnHelix()` |
| Tela tremer no impacto | `ScreenShakeS2CPacket` + `ModNetwork.sendToPlayer` |
| Aura sustentada (5+ segundos) | `ScheduledVfx.AreaPulseEffect` (engine de tasks ticked) |
| Sustained beam (canalizado) | `ScheduledVfx.SustainedBeamEffect` |

---

## Referências do código existente

| Arquivo | O que estuda |
|---|---|
| `magic/spell/SpellLibrary.java` | Catálogo de 50+ spells implementados |
| `magic/spell/SpellProjectileEntity.java` | Projétil custom com trail |
| `magic/spell/client/SpellProjectileRenderer.java` | Renderer 3-layer billboard |
| `magic/spell/particle/SpellTrailParticle.java` | Particle de trail animada |
| `magic/spell/particle/SpellTrailParticleType.java` | Registry da particle |
| `magic/spell/vfx/SpellBeam.java` | Beam/chain helpers (LineData) |
| `magic/spell/vfx/HelixSpawner.java` | Helix/ring spawners |
| `magic/spell/vfx/ScheduledVfx.java` | Engine de efeitos sustentados |
| `magic/spell/ScreenShakeS2CPacket.java` | Packet S2C shake |
| `magic/spell/ScreenShakeClient.java` | Aplicação client-side do shake |
| `magic/spell/CastBarOverlay.java` | HUD overlay (cast bar) |
| `particle/engine/ConfigurableParticleOptions.java` | Particle multi-param |

---

## Próximos passos pra novos spells

1. Adicione `SpellDef` no `SpellLibrary.registerXxx()` com `cast = Library::castMySpell`
2. Implemente o método `castMySpell(CastContext ctx)` usando as 5 camadas acima conforme
   necessário
3. Registre o item no `ModItems` via helper `spell("my_spell", Rarity.RARE)`
4. Adicione modelo `models/item/spell_my_spell.json` (parent generated)
5. Gere textura procedural ou desenhe à mão em `textures/item/spell_my_spell.png`
6. Adicione lang entry: `"item.liberthia.spell_my_spell": "Meu Feitiço"`
7. (Opcional) Adicione recipe JSON em `data/liberthia/glyph_recipes/my_spell.json`

Tudo isso já é feito em batch pelo Python script `build/gen_*_assets.py` — adapte os scripts
existentes.

---

**Última atualização:** r119 (2026)
**Autor do framework:** original code Liberthia mod
