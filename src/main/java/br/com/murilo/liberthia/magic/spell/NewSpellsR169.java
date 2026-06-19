package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.magic.spell.vfx.Pack2VfxCatalog;
import br.com.murilo.liberthia.magic.spell.vfx.Pack2VfxCatalog.Type;
import br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx;
import br.com.murilo.liberthia.magic.spell.vfx.SpellVfx;
import br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * r169: <b>20 more spells</b> with carefully matched VFX choreography.
 *
 * <p>Each spell uses a UNIQUE combination of pack-2 sprites + pack-1 effects
 * to feel visually distinct. The pulse cadence (delay between effects) and
 * scale variation create rhythm and impact.
 *
 * <h2>Design rules followed</h2>
 * <ul>
 *   <li>VFX themes match spell themes (no ice spells using eldritch tendrils)</li>
 *   <li>Each spell has 3-5 timed steps (windup → cast → impact → finale)</li>
 *   <li>Sustained AoE spells use {@link ScheduledVfx#schedule} for persistent effects</li>
 *   <li>Colors picked from school palette via {@link Pack2VfxCatalog#colorForSchool}</li>
 * </ul>
 */
public final class NewSpellsR169 {

    private NewSpellsR169() {}

    @FunctionalInterface
    private interface CastImpl { boolean run(CastContext ctx); }

    private static void def(String id, String name, SpellSchool school, Rarity rar,
                             int mana, int cd, float dmg, float range,
                             String lore, CastImpl impl) {
        SpellDef d = SpellDef.builder(id)
                .name(name).school(school).rarity(rar)
                .mana(mana).cooldown(cd).damage(dmg).range(range)
                .lore(lore)
                .cast(impl::run)
                .build();
        SpellLibrary.registerExternal(d);
    }

    private static int colorFor(SpellSchool s) { return Pack2VfxCatalog.colorForSchool(s); }

    private static LivingEntity pick(CastContext ctx) { return ctx.pickTarget(ctx.def.range); }

    private static void aoe(CastContext ctx, Vec3 center, double radius, float dmg,
                             MobEffectInstance... fx) {
        DamageSource ds = SchoolDamageSource.of(ctx.def.school).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, dmg);
            for (MobEffectInstance e : fx) {
                le.addEffect(new MobEffectInstance(e.getEffect(), e.getDuration(), e.getAmplifier()));
            }
        }
    }

    // ── ENTRY POINT ──────────────────────────────────────────────────────────

    public static void registerAll() {
        // 1-5: Projectile-style with trail+impact
        def("meteor_strike",   "Golpe de Meteoro",     SpellSchool.FIRE,      Rarity.EPIC,   55, 200, 32F, 28F,
            "Meteoro cai do céu. Rastro de fumaça + erupção massiva no impacto.",
            NewSpellsR169::castMeteorStrike);
        def("frost_comet",     "Cometa de Gelo",       SpellSchool.ICE,       Rarity.RARE,   38, 140, 22F, 24F,
            "Cometa congelado atravessa céus. Congela área ao impactar.",
            NewSpellsR169::castFrostComet);
        def("lightning_javelin","Dardo Elétrico",      SpellSchool.LIGHTNING, Rarity.RARE,   32,  90, 24F, 26F,
            "Javelin de raio. Cross-strike no impacto + arc para próximos.",
            NewSpellsR169::castLightningJavelin);
        def("blood_geyser",    "Gêiser de Sangue",     SpellSchool.BLOOD,     Rarity.EPIC,   48, 180, 26F, 14F,
            "Gêiser de sangue do chão. Tentáculos sustained 8s.",
            NewSpellsR169::castBloodGeyser);
        def("holy_beam",       "Feixe Sagrado",        SpellSchool.HOLY,      Rarity.EPIC,   45, 160, 28F, 30F,
            "Feixe de luz divina. Cega + Glowing + cura aliados.",
            NewSpellsR169::castHolyBeam);

        // 6-10: Sustained AoE / fields
        def("volcanic_rupture","Ruptura Vulcânica",    SpellSchool.FIRE,      Rarity.EPIC,   58, 220, 18F, 14F,
            "Fenda vulcânica abre no chão. Erupções aleatórias 8s.",
            NewSpellsR169::castVolcanicRupture);
        def("polar_vortex",    "Vortex Polar",         SpellSchool.ICE,       Rarity.EPIC,   52, 200, 14F, 12F,
            "Vortex de gelo. Puxa inimigos pro centro e congela.",
            NewSpellsR169::castPolarVortex);
        def("chain_storm",     "Tempestade Encadeada", SpellSchool.LIGHTNING, Rarity.EPIC,   58, 240, 18F, 20F,
            "Tempestade de raios em cadeia. 8 raios sequenciais.",
            NewSpellsR169::castChainStorm);
        def("blood_mist",      "Névoa de Sangue",      SpellSchool.BLOOD,     Rarity.RARE,   35, 160, 8F, 12F,
            "Névoa sangrenta. Vampirismo + slowness em raio 12s.",
            NewSpellsR169::castBloodMist);
        def("bramble_trap",    "Armadilha de Espinhos",SpellSchool.NATURE,    Rarity.RARE,   32, 140, 10F, 10F,
            "Espinhos brotam do chão. Imobiliza inimigos 4s.",
            NewSpellsR169::castBrambleTrap);

        // 11-15: Self-buff / channeled
        def("radiant_aegis",   "Égide Radiante",       SpellSchool.HOLY,      Rarity.EPIC,   45, 300, 0F, 0F,
            "Escudo solar 20s. Reflete 50% do dano recebido.",
            NewSpellsR169::castRadiantAegis);
        def("solar_wings",     "Asas Solares",         SpellSchool.HOLY,      Rarity.EPIC,   55, 360, 0F, 0F,
            "Asas de fogo solar. Voa 25s + queima inimigos próximos.",
            NewSpellsR169::castSolarWings);
        def("sanguine_pact",   "Pacto Sanguíneo",      SpellSchool.BLOOD,     Rarity.EPIC,   60, 280, 0F, 0F,
            "Pacto: troca 30%HP por Strength III + Speed II 30s.",
            NewSpellsR169::castSanguinePact);
        def("void_cocoon",     "Casulo do Vazio",      SpellSchool.ELDRITCH,  Rarity.EPIC,   50, 240, 0F, 0F,
            "Casulo de vazio. Invisibilidade + slowness immune 10s.",
            NewSpellsR169::castVoidCocoon);
        def("spirit_bloom",    "Florescer Espiritual", SpellSchool.NATURE,    Rarity.RARE,   30, 100, 0F, 6F,
            "Aura floral. Cura aliados 2HP/s + Regen II.",
            NewSpellsR169::castSpiritBloom);

        // 16-20: Special / unique mechanics
        def("ember_trail",     "Rastro de Brasas",     SpellSchool.FIRE,      Rarity.RARE,   25,  80, 8F, 10F,
            "Deixa rastro de fogo onde você anda. 8s.",
            NewSpellsR169::castEmberTrail);
        def("storm_surge",     "Surto Tempestuoso",    SpellSchool.LIGHTNING, Rarity.RARE,   28,  80, 14F, 10F,
            "Pulso radial de raios. Knockback médio.",
            NewSpellsR169::castStormSurge);
        def("curse_of_ages",   "Maldição das Eras",    SpellSchool.ELDRITCH,  Rarity.EPIC,   45, 200, 8F, 16F,
            "Maldição antiga. -1HP por seg durante 20s + Mining Fatigue.",
            NewSpellsR169::castCurseOfAges);
        def("mirror_of_madness","Espelho da Loucura",  SpellSchool.ELDRITCH,  Rarity.EPIC,   55, 240, 0F, 12F,
            "Espelho cósmico. Inimigos atacam uns aos outros 8s.",
            NewSpellsR169::castMirrorOfMadness);
        def("verdant_shroud",  "Manto Verdejante",     SpellSchool.NATURE,    Rarity.EPIC,   50, 240, 0F, 0F,
            "Manto floral. Resistance II + Regen III + cura aliados próximos.",
            NewSpellsR169::castVerdantShroud);
    }

    // ════════════════════════════════════════════════════════════════════════
    // CAST IMPLEMENTATIONS
    // ════════════════════════════════════════════════════════════════════════

    // ── 1. Meteor Strike ─────────────────────────────────────────────────────
    private static boolean castMeteorStrike(CastContext ctx) {
        Vec3 impact = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.FIRE);
        // Trail descending from sky
        for (int t = 0; t < 20; t += 4) {
            final int tk = t;
            ScheduledVfx.schedule(ctx.level, tk, () -> {
                Vec3 trail = impact.add(0, 8 - tk * 0.4, 0);
                Pack2VfxCatalog.spawn(ctx.level, trail, ctx.caster, Type.SMOKE_TRAIL, c, 1.5F);
            });
        }
        SpellVfx.builder()
            .delay(20)
            .at(impact).pack2(Type.ERUPTION, c, 3.5F)
            .pack2(Type.SMOKE_CLOUD, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.SUNBURN, 2.5F)
            .sound(SoundEvents.GENERIC_EXPLODE, 2.5F, 0.5F)
            .execute(ctx.level, ctx.caster);
        ScheduledVfx.schedule(ctx.level, 20, () -> {
            aoe(ctx, impact, 5.0, ctx.def.damage,
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(impact, impact).inflate(5))) {
                if (le != ctx.caster) le.setSecondsOnFire(8);
            }
        });
        return true;
    }

    // ── 2. Frost Comet ───────────────────────────────────────────────────────
    private static boolean castFrostComet(CastContext ctx) {
        Vec3 impact = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.ICE);
        for (int t = 0; t < 16; t += 3) {
            final int tk = t;
            ScheduledVfx.schedule(ctx.level, tk, () -> {
                Vec3 trail = impact.add(0, 6 - tk * 0.4, 0);
                Pack2VfxCatalog.spawn(ctx.level, trail, ctx.caster, Type.SPARKLE_RING, c, 1.0F);
            });
        }
        SpellVfx.builder()
            .delay(16)
            .at(impact).pack2(Type.STAR_BURST, c, 2.5F)
            .pack2(Type.SPARKLE_RING, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.FREEZING, 2.5F)
            .sound(SoundEvents.GLASS_BREAK, 1.8F, 0.6F)
            .execute(ctx.level, ctx.caster);
        ScheduledVfx.schedule(ctx.level, 16, () -> {
            for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(impact, impact).inflate(4))) {
                if (le == ctx.caster) continue;
                le.hurt(SchoolDamageSource.ice(80).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
                le.setTicksFrozen(120);
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            }
        });
        return true;
    }

    // ── 3. Lightning Javelin ─────────────────────────────────────────────────
    private static boolean castLightningJavelin(CastContext ctx) {
        LivingEntity t = pick(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.LIGHTNING);
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.5).pack2(Type.SLASH_TRAIL, c, 1.5F)
            .delay(4).atEntityHead(t).pack2(Type.CROSS_STRIKE, c, 2.0F)
            .pack2(Type.STAR_BURST, c, 1.5F)
            .sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5F, 1.3F)
            .execute(ctx.level, ctx.caster);
        t.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        // Arc to 2 nearby
        java.util.List<LivingEntity> nearby = ctx.level.getEntitiesOfClass(LivingEntity.class,
                t.getBoundingBox().inflate(4));
        int arced = 0;
        for (LivingEntity n : nearby) {
            if (n == ctx.caster || n == t) continue;
            if (arced >= 2) break;
            n.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                    ctx.def.damage * 0.5F);
            Pack2VfxCatalog.spawn(ctx.level, n.position().add(0, 1, 0), ctx.caster,
                    Type.STAR_BURST, c, 1.2F);
            arced++;
        }
        return true;
    }

    // ── 4. Blood Geyser ──────────────────────────────────────────────────────
    private static boolean castBloodGeyser(CastContext ctx) {
        Vec3 base = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.BLOOD);
        SpellVfx.builder()
            .at(base.add(0, 0.05, 0)).pack2(Type.RUNIC_SEAL, c, 2.5F)
            .delay(6).pack2(Type.ERUPTION, c, 2.5F)
            .pack2(Type.TENDRIL_SCATTER, c, 3.0F)
            .sound(SoundEvents.PLAYER_HURT, 1.4F, 0.5F)
            .execute(ctx.level, ctx.caster);
        // Sustained tendrils + drain for 8s
        for (int t = 0; t < 160; t += 20) {
            ScheduledVfx.schedule(ctx.level, t, () -> {
                aoe(ctx, base, 4.0, ctx.def.damage * 0.25F,
                        new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(base, base).inflate(4))) {
                    if (le != ctx.caster) ctx.caster.heal(0.5F);
                }
            });
        }
        return true;
    }

    // ── 5. Holy Beam ─────────────────────────────────────────────────────────
    private static boolean castHolyBeam(CastContext ctx) {
        Vec3 hit = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.HOLY);
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.5).pack2(Type.SOLAR_FLARE, c, 1.5F)
            .delay(8).at(hit).pack2(Type.STAR_BURST, c, 2.5F)
            .pack2(Type.SOLAR_FLARE, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.SUNBURN, 2.5F)
            .sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.5F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(hit, hit).inflate(4))) {
            if (le == ctx.caster) {
                le.heal(ctx.def.damage * 0.5F);
                continue;
            }
            float dmg = le.getMobType() == net.minecraft.world.entity.MobType.UNDEAD
                    ? ctx.def.damage * 1.8F : ctx.def.damage;
            le.hurt(SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(ctx.level, ctx.caster), dmg);
            le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }
        return true;
    }

    // ── 6. Volcanic Rupture ──────────────────────────────────────────────────
    private static boolean castVolcanicRupture(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.FIRE);
        SpellVfx.builder()
            .at(center.add(0, 0.05, 0)).pack2(Type.RUNIC_SEAL, c, 3.5F)
            .pack1(SpriteVfxRegistry.Type.CASTING, 3.5F)
            .sound(SoundEvents.BLAZE_BURN, 1.5F, 0.5F)
            .execute(ctx.level, ctx.caster);
        // Random eruptions every 15t for 8s (160t)
        for (int t = 10; t < 160; t += 15) {
            final int tk = t;
            ScheduledVfx.schedule(ctx.level, tk, () -> {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 4.0;
                Vec3 sp = center.add(Math.cos(a) * r, 1, Math.sin(a) * r);
                Pack2VfxCatalog.spawn(ctx.level, sp, ctx.caster, Type.ERUPTION, c, 1.8F);
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(sp, sp).inflate(2))) {
                    if (le == ctx.caster) continue;
                    le.hurt(SchoolDamageSource.fire(60).toVanilla(ctx.level, ctx.caster),
                            ctx.def.damage * 0.5F);
                    le.setSecondsOnFire(4);
                }
            });
        }
        return true;
    }

    // ── 7. Polar Vortex ──────────────────────────────────────────────────────
    private static boolean castPolarVortex(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.ICE);
        SpellVfx.builder()
            .at(center.add(0, 1.5, 0)).pack1(SpriteVfxRegistry.Type.VORTEX, 3.0F)
            .pack2(Type.SPARKLE_RING, c, 3.5F)
            .sound(SoundEvents.PLAYER_HURT_FREEZE, 1.5F, 0.5F)
            .execute(ctx.level, ctx.caster);
        // Pull + slow over 10s
        for (int t = 0; t < 200; t += 10) {
            ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(center, center).inflate(ctx.def.range))) {
                    if (le == ctx.caster) continue;
                    Vec3 pull = center.subtract(le.position()).normalize().scale(0.8);
                    le.setDeltaMovement(le.getDeltaMovement().add(pull.x, 0.05, pull.z));
                    le.hurtMarked = true;
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                    if (Math.random() < 0.3) le.setTicksFrozen(60);
                }
            });
        }
        return true;
    }

    // ── 8. Chain Storm ───────────────────────────────────────────────────────
    private static boolean castChainStorm(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.LIGHTNING);
        SpellVfx.builder()
            .at(center).pack1(SpriteVfxRegistry.Type.CASTING, 3.0F)
            .execute(ctx.level, ctx.caster);
        for (int i = 0; i < 8; i++) {
            final int idx = i;
            ScheduledVfx.schedule(ctx.level, idx * 8, () -> {
                double a = (idx / 8.0) * Math.PI * 2;
                Vec3 sp = center.add(Math.cos(a) * 5, 0, Math.sin(a) * 5);
                LightningBolt b = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(ctx.level);
                if (b != null) {
                    b.moveTo(sp);
                    b.setVisualOnly(true);
                    ctx.level.addFreshEntity(b);
                }
                Pack2VfxCatalog.spawn(ctx.level, sp.add(0, 1, 0), ctx.caster, Type.SLASH_TRAIL, c, 1.6F);
                Pack2VfxCatalog.spawn(ctx.level, sp.add(0, 1, 0), ctx.caster, Type.STAR_BURST, c, 1.3F);
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(sp, sp).inflate(3))) {
                    if (le != ctx.caster) le.hurt(
                            SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                            ctx.def.damage);
                }
            });
        }
        return true;
    }

    // ── 9. Blood Mist ────────────────────────────────────────────────────────
    private static boolean castBloodMist(CastContext ctx) {
        Vec3 center = ctx.caster.position();
        int c = colorFor(SpellSchool.BLOOD);
        SpellVfx.builder()
            .at(center.add(0, 1, 0)).pack2(Type.SMOKE_CLOUD, c, 3.5F)
            .pack1(SpriteVfxRegistry.Type.MIDNIGHT, 2.5F)
            .sound(SoundEvents.WITHER_AMBIENT, 1.0F, 0.5F)
            .execute(ctx.level, ctx.caster);
        // Sustained drain + slow 12s
        for (int t = 0; t < 240; t += 15) {
            ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
                    if (le == ctx.caster) continue;
                    le.hurt(SchoolDamageSource.of(SpellSchool.BLOOD).toVanilla(ctx.level, ctx.caster),
                            ctx.def.damage * 0.5F);
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    ctx.caster.heal(0.5F);
                }
            });
        }
        return true;
    }

    // ── 10. Bramble Trap ─────────────────────────────────────────────────────
    private static boolean castBrambleTrap(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.NATURE);
        SpellVfx.builder()
            .at(center.add(0, 0.05, 0)).pack2(Type.RUNIC_SEAL, c, 3.0F)
            .delay(6).pack2(Type.TENDRIL_SCATTER, c, 2.5F)
            .sound(SoundEvents.AZALEA_BREAK, 1.5F, 0.7F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(SchoolDamageSource.of(SpellSchool.NATURE).toVanilla(ctx.level, ctx.caster),
                    ctx.def.damage);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 9)); // imobiliza
            le.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
        }
        return true;
    }

    // ── 11. Radiant Aegis ────────────────────────────────────────────────────
    private static boolean castRadiantAegis(CastContext ctx) {
        int c = colorFor(SpellSchool.HOLY);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.1).pack2Ride(ctx.caster, Type.RUNIC_SEAL, c, 2.8F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 2.5F)
            .sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.0F)
            .execute(ctx.level, ctx.caster);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 2));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 2));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
        return true;
    }

    // ── 12. Solar Wings ──────────────────────────────────────────────────────
    private static boolean castSolarWings(CastContext ctx) {
        int c = colorFor(SpellSchool.HOLY);
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.0).pack2Ride(ctx.caster, Type.SOLAR_FLARE, c, 2.5F)
            .pack1(SpriteVfxRegistry.Type.SUNBURN, 2.0F)
            .sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 1.6F)
            .execute(ctx.level, ctx.caster);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 500, 0));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 500, 2));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.JUMP, 500, 4));
        // Burn nearby every 10t for 25s
        for (int t = 0; t < 500; t += 10) {
            ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(3))) {
                    if (le == ctx.caster) continue;
                    le.setSecondsOnFire(2);
                    le.hurt(SchoolDamageSource.fire(20).toVanilla(ctx.level, ctx.caster), 1F);
                }
            });
        }
        return true;
    }

    // ── 13. Sanguine Pact ────────────────────────────────────────────────────
    private static boolean castSanguinePact(CastContext ctx) {
        int c = colorFor(SpellSchool.BLOOD);
        float hpCost = ctx.caster.getMaxHealth() * 0.3F;
        ctx.caster.hurt(ctx.level.damageSources().magic(), hpCost);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.1).pack2Ride(ctx.caster, Type.HORROR_SIGIL, c, 1.8F)
            .pack2(Type.RUNIC_SEAL, c, 2.5F)
            .pack1(SpriteVfxRegistry.Type.MIDNIGHT, 2.0F)
            .sound(SoundEvents.PLAYER_HURT, 1.5F, 0.5F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    // ── 14. Void Cocoon ──────────────────────────────────────────────────────
    private static boolean castVoidCocoon(CastContext ctx) {
        int c = colorFor(SpellSchool.ELDRITCH);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.0).pack2Ride(ctx.caster, Type.HORROR_SIGIL, c, 1.5F)
            .pack1(SpriteVfxRegistry.Type.VORTEX, 1.8F)
            .sound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.6F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    // ── 15. Spirit Bloom ─────────────────────────────────────────────────────
    private static boolean castSpiritBloom(CastContext ctx) {
        int c = colorFor(SpellSchool.NATURE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.5).pack2(Type.SPARKLE_RING, c, 3.0F)
            .pack2(Type.MANDALA, c, 2.0F)
            .pack1(SpriteVfxRegistry.Type.MAGIC_BUBBLES, 2.0F)
            .sound(SoundEvents.AZALEA_LEAVES_BREAK, 1.2F, 1.4F)
            .execute(ctx.level, ctx.caster);
        // Pulse heal aliados every 20t for 8s
        for (int t = 0; t < 160; t += 20) {
            ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
                    if (le instanceof net.minecraft.world.entity.monster.Monster) continue;
                    le.heal(2F);
                    le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1));
                }
            });
        }
        return true;
    }

    // ── 16. Ember Trail ──────────────────────────────────────────────────────
    private static boolean castEmberTrail(CastContext ctx) {
        int c = colorFor(SpellSchool.FIRE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.1).pack2(Type.SPARKLE_RING, c, 1.5F)
            .sound(SoundEvents.FIRE_AMBIENT, 1.0F, 1.2F)
            .execute(ctx.level, ctx.caster);
        // Trail fire on caster position every 10t for 8s
        for (int t = 0; t < 160; t += 10) {
            ScheduledVfx.schedule(ctx.level, t, () -> {
                Vec3 pos = ctx.caster.position();
                Pack2VfxCatalog.spawn(ctx.level, pos.add(0, 0.05, 0), ctx.caster,
                        Type.SMOKE_TRAIL, c, 1.0F);
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(1.5))) {
                    if (le == ctx.caster) continue;
                    le.setSecondsOnFire(3);
                    le.hurt(SchoolDamageSource.fire(40).toVanilla(ctx.level, ctx.caster),
                            ctx.def.damage * 0.3F);
                }
            });
        }
        return true;
    }

    // ── 17. Storm Surge ──────────────────────────────────────────────────────
    private static boolean castStormSurge(CastContext ctx) {
        int c = colorFor(SpellSchool.LIGHTNING);
        Vec3 cp = ctx.caster.position();
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.0).pack2(Type.SPARKLE_RING, c, 3.5F)
            .pack2(Type.STAR_BURST, c, 2.5F)
            .sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5F, 1.0F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                    ctx.def.damage);
            Vec3 kb = le.position().subtract(cp).normalize().scale(1.2);
            le.setDeltaMovement(kb.x, 0.4, kb.z);
            le.hurtMarked = true;
        }
        return true;
    }

    // ── 18. Curse of Ages ────────────────────────────────────────────────────
    private static boolean castCurseOfAges(CastContext ctx) {
        LivingEntity t = pick(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.ELDRITCH);
        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 1));
        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 400, 3));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
        SpellVfx.builder()
            .atEntityHead(t).pack2(Type.HORROR_SIGIL, c, 2.0F)
            .pack1(SpriteVfxRegistry.Type.MIDNIGHT, 1.8F)
            .sound(SoundEvents.WITHER_AMBIENT, 1.0F, 0.4F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    // ── 19. Mirror of Madness ────────────────────────────────────────────────
    private static boolean castMirrorOfMadness(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.ELDRITCH);
        SpellVfx.builder()
            .at(center).pack2(Type.MANDALA, c, 3.0F)
            .pack2(Type.HORROR_SIGIL, c, 2.5F)
            .pack1(SpriteVfxRegistry.Type.NEBULA, 2.5F)
            .sound(SoundEvents.ENDERMAN_SCREAM, 1.0F, 0.7F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 4));
            // Force them to target each other if mob
            if (le instanceof net.minecraft.world.entity.Mob mob) {
                java.util.List<LivingEntity> others = ctx.level.getEntitiesOfClass(LivingEntity.class,
                        le.getBoundingBox().inflate(6));
                others.remove(le);
                others.remove(ctx.caster);
                if (!others.isEmpty()) {
                    LivingEntity victim = others.get(ctx.level.random.nextInt(others.size()));
                    mob.setTarget(victim);
                }
            }
        }
        return true;
    }

    // ── 20. Verdant Shroud ───────────────────────────────────────────────────
    private static boolean castVerdantShroud(CastContext ctx) {
        int c = colorFor(SpellSchool.NATURE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.1).pack2Ride(ctx.caster, Type.MANDALA, c, 2.2F)
            .pack2(Type.SPARKLE_RING, c, 2.5F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 2.5F)
            .pack1(SpriteVfxRegistry.Type.MAGIC_BUBBLES, 2.0F)
            .sound(SoundEvents.BEACON_ACTIVATE, 1.2F, 1.3F)
            .execute(ctx.level, ctx.caster);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2));
        // heal allies pulse every 20t
        for (int t = 0; t < 400; t += 20) {
            ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(8))) {
                    if (le instanceof net.minecraft.world.entity.monster.Monster) continue;
                    le.heal(1F);
                }
            });
        }
        return true;
    }
}
