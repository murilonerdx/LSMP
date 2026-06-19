package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.magic.spell.vfx.Pack2VfxCatalog;
import br.com.murilo.liberthia.magic.spell.vfx.Pack2VfxCatalog.Type;
import br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * r171: <b>Two-Sprite Spells</b> — REWRITE de todos os spells r168+r169
 * usando exatamente 2 sprites do pack-2 que combinam visualmente.
 *
 * <h2>Filosofia</h2>
 * <ul>
 *   <li><b>Cada spell usa EXATAMENTE 2 sprites Pack-2</b> — nada mais.</li>
 *   <li><b>Sem mixing Pack-1</b> — evita conflitos de cor (Pack-1 tem cores
 *       hardcoded; Pack-2 tem cores tinted via HSV).</li>
 *   <li><b>Cor é LOCKED na escola</b> — FIRE=orange, ICE=cyan, etc. Usa o
 *       mapeamento de {@link Pack2VfxCatalog#colorForSchool}.</li>
 *   <li><b>Pattern padrão:</b> windup sprite na mão do caster → delay → impact
 *       sprite no alvo. Damage + effects aplicados no impact tick.</li>
 * </ul>
 *
 * <h2>Onde editar manualmente</h2>
 * <p>Quer trocar o sprite de "solar_flare"? Edita a linha:
 * <pre>
 *   reg("solar_flare", ..., Type.MANDALA, Type.SOLAR_FLARE, ...);
 *                              ↑ windup    ↑ impact
 * </pre>
 *
 * <p>As pastas dos sprites estão em
 * {@code src/main/resources/assets/liberthia/textures/vfx_pack2/partNN/cKK/}.
 *
 * <p>Substitui os registros de NewSpellsR168 e NewSpellsR169 — chamado DEPOIS
 * deles em {@link SpellLibrary} static block, então sobrescreve via
 * {@link SpellLibrary#registerExternal}.
 */
public final class TwoSpriteSpells {

    private TwoSpriteSpells() {}

    // ════════════════════════════════════════════════════════════════════════
    // ENTRY POINT — chamado em SpellLibrary.static após r168+r169
    // ════════════════════════════════════════════════════════════════════════
    public static void registerAll() {
        // ─── FIRE / SOLAR ───────────────────────────────────────────────────
        reg("solar_flare",      "Lança Solar",         SpellSchool.FIRE,      Rarity.EPIC,     45, 100, 22F, 24F,
                Type.MANDALA,        Type.SOLAR_FLARE,
                "Mandala de cast → explosão solar no alvo.");
        reg("ember_storm",      "Tempestade de Brasas",SpellSchool.FIRE,      Rarity.RARE,     30,  80, 14F,  8F,
                Type.SPARKLE_RING,   Type.ERUPTION,
                "Anel de brasas → erupção sustentada no alvo.");
        reg("phoenix_dive",     "Mergulho da Fênix",   SpellSchool.FIRE,      Rarity.EPIC,     40, 140, 28F, 16F,
                Type.SOLAR_FLARE,    Type.FIREWORK,
                "Flare solar → finale firework.");
        reg("magma_pillar",     "Pilar de Magma",      SpellSchool.FIRE,      Rarity.RARE,     35, 120, 18F, 12F,
                Type.RUNIC_SEAL,     Type.SMOKE_CLOUD,
                "Selo no chão → coluna de fumaça vertical.");
        reg("solar_seal",       "Selo Solar",          SpellSchool.FIRE,      Rarity.RARE,     35, 200,  0F,  0F,
                Type.MANDALA,        Type.RUNIC_SEAL,
                "Mandala + Selo runic riding caster (buff sustained).");
        reg("flame_lance",      "Lança Flamejante",    SpellSchool.FIRE,      Rarity.UNCOMMON, 22,  50, 16F, 22F,
                Type.SLASH_TRAIL,    Type.CROSS_STRIKE,
                "Trail slash → cross-strike no alvo.");
        reg("infernal_nova",    "Nova Infernal",       SpellSchool.FIRE,      Rarity.EPIC,     55, 160, 30F,  6F,
                Type.ERUPTION,       Type.SOLAR_FLARE,
                "Erupção dupla + flare = nova massiva.");
        reg("sunburst",         "Sunburst",            SpellSchool.FIRE,      Rarity.RARE,     30,  90, 20F, 14F,
                Type.SOLAR_FLARE,    Type.STAR_BURST,
                "Flare solar → sparkle no alvo.");
        reg("blaze_chain",      "Corrente Flamejante", SpellSchool.FIRE,      Rarity.RARE,     28, 100, 12F, 18F,
                Type.SLASH_TRAIL,    Type.STAR_BURST,
                "Chain — slash entre 4 alvos.");
        reg("fire_mandala",     "Mandala de Fogo",     SpellSchool.FIRE,      Rarity.EPIC,     50, 240, 24F, 10F,
                Type.MANDALA,        Type.SOLAR_FLARE,
                "Mandala sustained → flares periódicas 5s.");

        // ─── ICE / FROST ────────────────────────────────────────────────────
        reg("frost_nova",       "Nova de Gelo",        SpellSchool.ICE,       Rarity.EPIC,     40, 130, 18F,  8F,
                Type.SPARKLE_RING,   Type.MANDALA,
                "Ring + mandala expandindo do caster.");
        reg("ice_lance",        "Lança de Gelo",       SpellSchool.ICE,       Rarity.UNCOMMON, 18,  40, 12F, 20F,
                Type.SLASH_TRAIL,    Type.CROSS_STRIKE,
                "Lança rápida cyan → strike no alvo.");
        reg("blizzard_field",   "Campo de Nevasca",    SpellSchool.ICE,       Rarity.EPIC,     55, 200, 12F, 12F,
                Type.RUNIC_SEAL,     Type.SPARKLE_RING,
                "Selo no chão → sparkles caindo sustained.");
        reg("glacial_spike",    "Lança Glacial",       SpellSchool.ICE,       Rarity.RARE,     32, 110, 22F, 18F,
                Type.CROSS_STRIKE,   Type.SPARKLE_RING,
                "Cross-strike sharp → ring de sparkles.");
        reg("frost_armor",      "Armadura de Gelo",    SpellSchool.ICE,       Rarity.RARE,     30, 200,  0F,  0F,
                Type.MANDALA,        Type.RUNIC_SEAL,
                "Mandala + selo riding caster (buff).");
        reg("snow_storm",       "Nevasca",             SpellSchool.ICE,       Rarity.RARE,     28, 100, 10F, 10F,
                Type.SMOKE_CLOUD,    Type.SPARKLE_RING,
                "Nuvem azul → sparkles caindo.");
        reg("absolute_zero",    "Zero Absoluto",       SpellSchool.ICE,       Rarity.EPIC,     60, 240, 26F, 10F,
                Type.MOON_ARC,       Type.SPARKLE_RING,
                "Crescente de gelo → sparkles de cristal.");
        reg("frost_seal",       "Selo Gélido",         SpellSchool.ICE,       Rarity.RARE,     35, 180,  0F,  0F,
                Type.RUNIC_SEAL,     Type.SPARKLE_RING,
                "Selo riding → sparkle pulses.");
        reg("ice_prison",       "Prisão de Gelo",      SpellSchool.ICE,       Rarity.RARE,     30, 140, 12F, 16F,
                Type.MANDALA,        Type.RUNIC_SEAL,
                "Mandala no alvo → selo prendendo.");

        // ─── LIGHTNING ──────────────────────────────────────────────────────
        reg("lightning_bolt2",  "Raio Cêntrico",       SpellSchool.LIGHTNING, Rarity.RARE,     30,  70, 18F, 24F,
                Type.STAR_BURST,     Type.CROSS_STRIKE,
                "Sparkles → cross impact + arc próximos.");
        reg("storm_call",       "Chamado da Tempestade",SpellSchool.LIGHTNING, Rarity.EPIC,    50, 200, 14F, 18F,
                Type.RUNIC_SEAL,     Type.STAR_BURST,
                "Selo no chão → 5 raios sequenciais.");
        reg("arc_chain",        "Cadeia Elétrica",     SpellSchool.LIGHTNING, Rarity.RARE,     28,  90, 14F, 14F,
                Type.SLASH_TRAIL,    Type.STAR_BURST,
                "Slash arc → starburst entre alvos.");
        reg("thunder_clap",     "Trovão",              SpellSchool.LIGHTNING, Rarity.RARE,     35, 100, 16F, 10F,
                Type.SPARKLE_RING,   Type.STAR_BURST,
                "Ring expansivo + starburst.");
        reg("shock_wave",       "Onda de Choque",      SpellSchool.LIGHTNING, Rarity.UNCOMMON, 20,  60, 12F,  8F,
                Type.SLASH_TRAIL,    Type.STAR_BURST,
                "Slash linear + starburst em cone.");
        reg("static_field",     "Campo Estático",      SpellSchool.LIGHTNING, Rarity.RARE,     30, 140,  8F, 12F,
                Type.RUNIC_SEAL,     Type.STAR_BURST,
                "Selo no chão → sparkles per tick.");
        reg("plasma_orb",       "Esfera de Plasma",    SpellSchool.LIGHTNING, Rarity.RARE,     32,  80, 16F, 18F,
                Type.SPARKLE_RING,   Type.ERUPTION,
                "Ring de plasma → eruption no alvo.");
        reg("ball_lightning",   "Bola de Raio",        SpellSchool.LIGHTNING, Rarity.EPIC,     45, 160, 24F, 16F,
                Type.SOLAR_FLARE,    Type.SPARKLE_RING,
                "Flare amarelo + ring elétrico.");

        // ─── BLOOD ─────────────────────────────────────────────────────────
        reg("blood_spike",      "Espinho de Sangue",   SpellSchool.BLOOD,     Rarity.UNCOMMON, 20,  50, 14F, 18F,
                Type.TENDRIL_SCATTER,Type.CROSS_STRIKE,
                "Tendril vermelho → cross impact (drain).");
        reg("crimson_orb",      "Esfera Carmim",       SpellSchool.BLOOD,     Rarity.RARE,     28,  80, 18F, 18F,
                Type.SPARKLE_RING,   Type.ERUPTION,
                "Ring carmim → eruption no alvo.");
        reg("blood_ritual",     "Ritual de Sangue",    SpellSchool.BLOOD,     Rarity.EPIC,     50, 240, 30F, 12F,
                Type.RUNIC_SEAL,     Type.HORROR_SIGIL,
                "Selo blood → sigil horror.");
        reg("hemorrhage",       "Hemorragia",          SpellSchool.BLOOD,     Rarity.RARE,     25, 100,  0F, 16F,
                Type.TENDRIL_SCATTER,Type.HORROR_SIGIL,
                "Tendril red riding target.");
        reg("vampiric_aura",    "Aura Vampírica",      SpellSchool.BLOOD,     Rarity.EPIC,     45, 200,  0F,  0F,
                Type.HORROR_SIGIL,   Type.RUNIC_SEAL,
                "Aura riding caster (drain + heal).");
        reg("blood_chain",      "Corrente de Sangue",  SpellSchool.BLOOD,     Rarity.RARE,     30, 110, 12F, 16F,
                Type.SLASH_TRAIL,    Type.TENDRIL_SCATTER,
                "Chain — slash tendril per tick.");
        reg("crimson_dome",     "Domo Carmim",         SpellSchool.BLOOD,     Rarity.EPIC,     55, 240, 24F, 10F,
                Type.RUNIC_SEAL,     Type.HORROR_SIGIL,
                "Selo no chão + horror sigil sustained.");

        // ─── ELDRITCH ──────────────────────────────────────────────────────
        reg("void_bloom",       "Florescer do Vazio",  SpellSchool.ELDRITCH,  Rarity.EPIC,     50, 200, 22F, 16F,
                Type.MANDALA,        Type.HORROR_SIGIL,
                "Mandala roxa → horror sigil.");
        reg("eye_of_madness",   "Olho da Loucura",     SpellSchool.ELDRITCH,  Rarity.RARE,     35, 120, 14F, 20F,
                Type.HORROR_SIGIL,   Type.MANDALA,
                "Sigil riding alvo + mandala impacto.");
        reg("dimensional_rift", "Fenda Dimensional",   SpellSchool.ELDRITCH,  Rarity.EPIC,     55, 240, 24F, 18F,
                Type.MOON_ARC,       Type.TENDRIL_SCATTER,
                "Arco rift → tendrils sugando.");
        reg("nightmare",        "Pesadelo",            SpellSchool.ELDRITCH,  Rarity.RARE,     30, 140, 12F, 16F,
                Type.HORROR_SIGIL,   Type.TENDRIL_SCATTER,
                "Sigil + tendril sustained no alvo.");
        reg("void_seal",        "Selo do Vazio",       SpellSchool.ELDRITCH,  Rarity.RARE,     40, 180,  0F,  0F,
                Type.RUNIC_SEAL,     Type.HORROR_SIGIL,
                "Selo + sigil riding caster.");
        reg("phantom_strike",   "Golpe Fantasma",      SpellSchool.ELDRITCH,  Rarity.RARE,     28,  90, 18F, 14F,
                Type.SLASH_TRAIL,    Type.CROSS_STRIKE,
                "Slash purple → cross impact.");
        reg("cosmic_horror",    "Horror Cósmico",      SpellSchool.ELDRITCH,  Rarity.EPIC,     60, 300, 35F, 14F,
                Type.HORROR_SIGIL,   Type.MOON_ARC,
                "Sigil + arc rift = massive AoE.");

        // ─── HOLY ──────────────────────────────────────────────────────────
        reg("divine_seal",      "Selo Divino",         SpellSchool.HOLY,      Rarity.EPIC,     40, 200,  0F,  0F,
                Type.RUNIC_SEAL,     Type.MANDALA,
                "Selo + mandala white riding caster.");
        reg("radiant_burst",    "Explosão Radiante",   SpellSchool.HOLY,      Rarity.RARE,     30, 100, 18F, 14F,
                Type.SOLAR_FLARE,    Type.MANDALA,
                "Flare + mandala = burst sagrado.");
        reg("angelic_wings",    "Asas Angelicais",     SpellSchool.HOLY,      Rarity.EPIC,     50, 400,  0F,  0F,
                Type.STAR_BURST,     Type.SOLAR_FLARE,
                "Star + flare riding caster (voa).");
        reg("holy_lance",       "Lança Sagrada",       SpellSchool.HOLY,      Rarity.RARE,     28,  80, 20F, 22F,
                Type.SLASH_TRAIL,    Type.CROSS_STRIKE,
                "Slash branco → cross impact.");
        reg("sanctuary",        "Santuário",           SpellSchool.HOLY,      Rarity.EPIC,     55, 240,  0F,  0F,
                Type.RUNIC_SEAL,     Type.MANDALA,
                "Selo + mandala riding caster (invuln).");

        // ─── NATURE ────────────────────────────────────────────────────────
        reg("nature_seal",      "Selo da Natureza",    SpellSchool.NATURE,    Rarity.RARE,     30, 180,  0F,  0F,
                Type.RUNIC_SEAL,     Type.SPARKLE_RING,
                "Selo verde + sparkles riding caster.");
        reg("thorn_garden",     "Jardim de Espinhos",  SpellSchool.NATURE,    Rarity.RARE,     35, 140, 14F, 10F,
                Type.TENDRIL_SCATTER,Type.RUNIC_SEAL,
                "Tendril verde + selo riding caster.");
        reg("verdant_burst",    "Explosão Verdejante", SpellSchool.NATURE,    Rarity.UNCOMMON, 22,  60, 12F, 12F,
                Type.SPARKLE_RING,   Type.MANDALA,
                "Sparkle ring + mandala verde.");
        reg("life_seal",        "Selo da Vida",        SpellSchool.NATURE,    Rarity.EPIC,     60, 400,  0F,  0F,
                Type.RUNIC_SEAL,     Type.MANDALA,
                "Selo da vida + mandala riding caster.");

        // ════════════════════════════════════════════════════════════════════════
        // r169 batch — 20 more
        // ════════════════════════════════════════════════════════════════════════
        reg("meteor_strike",    "Golpe de Meteoro",    SpellSchool.FIRE,      Rarity.EPIC,     55, 200, 32F, 28F,
                Type.SMOKE_TRAIL,    Type.ERUPTION,
                "Trail descendo → erupção no impacto.");
        reg("frost_comet",      "Cometa de Gelo",      SpellSchool.ICE,       Rarity.RARE,     38, 140, 22F, 24F,
                Type.SPARKLE_RING,   Type.STAR_BURST,
                "Ring descendo → starburst de cristal.");
        reg("lightning_javelin","Dardo Elétrico",      SpellSchool.LIGHTNING, Rarity.RARE,     32,  90, 24F, 26F,
                Type.SLASH_TRAIL,    Type.STAR_BURST,
                "Trail rápido → starburst alvo.");
        reg("blood_geyser",     "Gêiser de Sangue",    SpellSchool.BLOOD,     Rarity.EPIC,     48, 180, 26F, 14F,
                Type.RUNIC_SEAL,     Type.TENDRIL_SCATTER,
                "Selo + tendril gêiser sustained.");
        reg("holy_beam",        "Feixe Sagrado",       SpellSchool.HOLY,      Rarity.EPIC,     45, 160, 28F, 30F,
                Type.SOLAR_FLARE,    Type.STAR_BURST,
                "Flare + starburst no alvo.");
        reg("volcanic_rupture", "Ruptura Vulcânica",   SpellSchool.FIRE,      Rarity.EPIC,     58, 220, 18F, 14F,
                Type.RUNIC_SEAL,     Type.ERUPTION,
                "Selo + erupções aleatórias 8s.");
        reg("polar_vortex",     "Vortex Polar",        SpellSchool.ICE,       Rarity.EPIC,     52, 200, 14F, 12F,
                Type.MOON_ARC,       Type.SPARKLE_RING,
                "Arc vortex + sparkles puxando.");
        reg("chain_storm",      "Tempestade Encadeada",SpellSchool.LIGHTNING, Rarity.EPIC,     58, 240, 18F, 20F,
                Type.SLASH_TRAIL,    Type.STAR_BURST,
                "Slash + starburst em 8 raios.");
        reg("blood_mist",       "Névoa de Sangue",     SpellSchool.BLOOD,     Rarity.RARE,     35, 160,  8F, 12F,
                Type.SMOKE_CLOUD,    Type.TENDRIL_SCATTER,
                "Cloud red + tendril sustained 12s.");
        reg("bramble_trap",     "Armadilha de Espinhos",SpellSchool.NATURE,   Rarity.RARE,     32, 140, 10F, 10F,
                Type.RUNIC_SEAL,     Type.TENDRIL_SCATTER,
                "Selo + tendrils de espinhos.");
        reg("radiant_aegis",    "Égide Radiante",      SpellSchool.HOLY,      Rarity.EPIC,     45, 300,  0F,  0F,
                Type.RUNIC_SEAL,     Type.MANDALA,
                "Selo + mandala white riding caster.");
        reg("solar_wings",      "Asas Solares",        SpellSchool.HOLY,      Rarity.EPIC,     55, 360,  0F,  0F,
                Type.SOLAR_FLARE,    Type.STAR_BURST,
                "Flare + star riding caster (voa).");
        reg("sanguine_pact",    "Pacto Sanguíneo",     SpellSchool.BLOOD,     Rarity.EPIC,     60, 280,  0F,  0F,
                Type.HORROR_SIGIL,   Type.RUNIC_SEAL,
                "Pacto: -30%HP por buff (riding).");
        reg("void_cocoon",      "Casulo do Vazio",     SpellSchool.ELDRITCH,  Rarity.EPIC,     50, 240,  0F,  0F,
                Type.HORROR_SIGIL,   Type.MANDALA,
                "Sigil + mandala riding (invisible).");
        reg("spirit_bloom",     "Florescer Espiritual",SpellSchool.NATURE,    Rarity.RARE,     30, 100,  0F,  6F,
                Type.SPARKLE_RING,   Type.MANDALA,
                "Sparkle + mandala (cura aliados).");
        reg("ember_trail",      "Rastro de Brasas",    SpellSchool.FIRE,      Rarity.RARE,     25,  80,  8F, 10F,
                Type.SPARKLE_RING,   Type.SMOKE_TRAIL,
                "Sparkle + trail onde anda 8s.");
        reg("storm_surge",      "Surto Tempestuoso",   SpellSchool.LIGHTNING, Rarity.RARE,     28,  80, 14F, 10F,
                Type.SPARKLE_RING,   Type.STAR_BURST,
                "Ring + starburst radial.");
        reg("curse_of_ages",    "Maldição das Eras",   SpellSchool.ELDRITCH,  Rarity.EPIC,     45, 200,  8F, 16F,
                Type.HORROR_SIGIL,   Type.TENDRIL_SCATTER,
                "Sigil + tendril 20s.");
        reg("mirror_of_madness","Espelho da Loucura",  SpellSchool.ELDRITCH,  Rarity.EPIC,     55, 240,  0F, 12F,
                Type.MANDALA,        Type.HORROR_SIGIL,
                "Mandala espelho + sigil (mobs vs mobs).");
        reg("verdant_shroud",   "Manto Verdejante",    SpellSchool.NATURE,    Rarity.EPIC,     50, 240,  0F,  0F,
                Type.MANDALA,        Type.SPARKLE_RING,
                "Mandala + sparkle riding caster.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Registration helper
    // ════════════════════════════════════════════════════════════════════════
    private static void reg(String id, String name, SpellSchool school, Rarity rar,
                             int mana, int cd, float dmg, float range,
                             Type windup, Type impact, String lore) {
        SpellDef d = SpellDef.builder(id)
                .name(name).school(school).rarity(rar)
                .mana(mana).cooldown(cd).damage(dmg).range(range)
                .lore(lore)
                .cast(ctx -> performCast(ctx, school, windup, impact))
                .build();
        SpellLibrary.registerExternal(d);
    }

    /**
     * UNIFIED cast logic — same shape for all 70 spells.
     *
     * <ol>
     *   <li>Pick the target/aim point.</li>
     *   <li>Spawn {@code windup} sprite at caster (1 sprite, 0 delay).</li>
     *   <li>10 ticks later, spawn {@code impact} sprite at target.</li>
     *   <li>Apply damage + school-default effects.</li>
     * </ol>
     *
     * <p>Self-buff spells (damage=0, range=0) play both sprites riding caster.
     */
    private static boolean performCast(CastContext ctx, SpellSchool school,
                                        Type windup, Type impact) {
        int color = Pack2VfxCatalog.colorForSchool(school);
        net.minecraft.server.level.ServerLevel sl = ctx.level;
        net.minecraft.world.entity.LivingEntity caster = ctx.caster;

        boolean isSelfBuff = ctx.def.damage == 0F && ctx.def.range == 0F;
        if (isSelfBuff) {
            // Both sprites ride caster
            spawnRiding(sl, caster, windup, color);
            ScheduledVfx.schedule(sl, 10, () -> spawnRiding(sl, caster, impact, color));
            applySchoolBuff(school, caster, ctx.def);
            playSound(sl, caster.position(), school, 1.2F);
            return true;
        }

        // Pick target: entity > block
        LivingEntity target = ctx.pickTarget(ctx.def.range);
        Vec3 targetPos = target != null ? target.position().add(0, 1, 0) : ctx.pickHit(ctx.def.range);
        Vec3 casterPos = caster.position().add(0, 1.0, 0);

        // Windup at caster's hand
        Pack2VfxCatalog.spawn(sl, casterPos, caster, windup, color, 1.3F);
        playSound(sl, casterPos, school, 0.8F);

        // Impact 10 ticks later
        ScheduledVfx.schedule(sl, 10, () -> {
            // Try to follow the target if it's still alive
            Vec3 finalImpact = (target != null && target.isAlive())
                    ? target.position().add(0, 1, 0) : targetPos;
            Pack2VfxCatalog.spawn(sl, finalImpact, caster, impact, color, 1.5F);

            // Apply damage + effects
            if (target != null && target.isAlive()) {
                applySingleTargetDamage(target, ctx, school);
            } else {
                applyAoeDamage(ctx, finalImpact, school);
            }
            playSound(sl, finalImpact, school, 1.3F);
        });
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════

    private static void spawnRiding(net.minecraft.server.level.ServerLevel sl,
                                    LivingEntity host, Type type, int color) {
        var ent = new br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxEntity(
                sl, host, host.position(), type, color, 1.5F);
        ent.startRiding(host, true);
        sl.addFreshEntity(ent);
    }

    private static void applySingleTargetDamage(LivingEntity target, CastContext ctx, SpellSchool school) {
        DamageSource ds = SchoolDamageSource.of(school).toVanilla(ctx.level, ctx.caster);
        target.hurt(ds, ctx.def.damage);
        applyDebuffsForSchool(school, target);
    }

    private static void applyAoeDamage(CastContext ctx, Vec3 center, SpellSchool school) {
        DamageSource ds = SchoolDamageSource.of(school).toVanilla(ctx.level, ctx.caster);
        double radius = Math.max(2.0, ctx.def.range / 4.0);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, ctx.def.damage);
            applyDebuffsForSchool(school, le);
        }
    }

    private static void applyDebuffsForSchool(SpellSchool school, LivingEntity target) {
        switch (school) {
            case FIRE -> target.setSecondsOnFire(5);
            case ICE  -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                target.setTicksFrozen(100);
            }
            case LIGHTNING -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            case BLOOD -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            case ELDRITCH -> {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            }
            case HOLY -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            case NATURE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        }
    }

    private static void applySchoolBuff(SpellSchool school, LivingEntity caster, SpellDef def) {
        switch (school) {
            case FIRE -> {
                caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, def.cooldownTicks, 0));
                caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, def.cooldownTicks / 2, 1));
            }
            case ICE -> {
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, def.cooldownTicks / 2, 1));
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, def.cooldownTicks / 2, 0));
            }
            case LIGHTNING -> {
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, def.cooldownTicks / 2, 1));
                caster.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, def.cooldownTicks / 2, 0));
            }
            case BLOOD -> {
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, def.cooldownTicks / 2, 2));
                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, def.cooldownTicks / 2, 2));
            }
            case ELDRITCH -> {
                caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, def.cooldownTicks / 2, 0));
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, def.cooldownTicks / 2, 2));
            }
            case HOLY -> {
                caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, def.cooldownTicks, 2));
                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, def.cooldownTicks, 2));
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, def.cooldownTicks, 1));
            }
            case NATURE -> {
                caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, def.cooldownTicks, 2));
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, def.cooldownTicks, 1));
            }
        }
    }

    private static void playSound(net.minecraft.server.level.ServerLevel sl, Vec3 pos,
                                   SpellSchool school, float volume) {
        SoundEvent ev = switch (school) {
            case FIRE      -> SoundEvents.BLAZE_SHOOT;
            case ICE       -> SoundEvents.GLASS_BREAK;
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case BLOOD     -> SoundEvents.PLAYER_HURT;
            case ELDRITCH  -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
            case HOLY      -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case NATURE    -> SoundEvents.AZALEA_BREAK;
        };
        sl.playSound(null, new net.minecraft.core.BlockPos((int)pos.x, (int)pos.y, (int)pos.z),
                ev, SoundSource.PLAYERS, volume, 1.0F);
    }
}
