package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.world.item.Rarity;
import br.com.murilo.liberthia.magic.spell.vfx.Pack2VfxCatalog;
import br.com.murilo.liberthia.magic.spell.vfx.Pack2VfxCatalog.Type;
import br.com.murilo.liberthia.magic.spell.vfx.SpellVfx;
import br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * r168: <b>50 new spells</b> using the Pack-2 VFX framework.
 *
 * <p>Each spell uses {@link SpellVfx} to choreograph 2-4 layered VFX events
 * (windup → impact → finale), creating visually distinctive, "hypnotizing"
 * casting effects.
 *
 * <p>Distribution: 10 Fire/Solar, 9 Ice/Frost, 8 Lightning, 7 Blood, 7 Eldritch,
 * 5 Holy, 4 Nature/Utility = 50.
 *
 * <p>All spells get cooldown / mana / damage tuned to school identity. They
 * register via {@link SpellLibrary#registerExternal} so they appear in the
 * Spell Factory + Inscription Table.
 */
public final class NewSpellsR168 {

    private NewSpellsR168() {}

    // ── Helpers ──────────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface CastImpl { boolean run(CastContext ctx); }

    /** Quick spell definition shortcut. */
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

    /** Pick the first living entity in line of sight up to range. */
    private static LivingEntity pickEntity(CastContext ctx) {
        return ctx.pickTarget(ctx.def.range);
    }

    /** AoE damage at point, applying source + extras. */
    private static int aoeAt(CastContext ctx, Vec3 center, double radius, float dmg,
                              MobEffectInstance... effects) {
        DamageSource ds = SchoolDamageSource.of(ctx.def.school).toVanilla(ctx.level, ctx.caster);
        int hit = 0;
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius))) {
            if (le == ctx.caster) continue;
            le.hurt(ds, dmg);
            for (MobEffectInstance e : effects) {
                le.addEffect(new MobEffectInstance(e.getEffect(), e.getDuration(), e.getAmplifier()));
            }
            hit++;
        }
        return hit;
    }

    private static int colorFor(SpellSchool s) {
        return Pack2VfxCatalog.colorForSchool(s);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ENTRY POINT
    // ════════════════════════════════════════════════════════════════════════

    public static void registerAll() {
        registerFireSet();
        registerIceSet();
        registerLightningSet();
        registerBloodSet();
        registerEldritchSet();
        registerHolySet();
        registerNatureSet();
    }

    // ════════════════════════════════════════════════════════════════════════
    // FIRE / SOLAR — 10 spells
    // ════════════════════════════════════════════════════════════════════════

    private static void registerFireSet() {
        def("solar_flare", "Lança Solar", SpellSchool.FIRE, Rarity.EPIC, 45, 100, 22F, 24F,
            "Burst de plasma solar. Dano massivo + cega o alvo.",
            NewSpellsR168::castSolarFlare);

        def("ember_storm", "Tempestade de Brasas", SpellSchool.FIRE, Rarity.RARE, 30, 80, 14F, 8F,
            "Chove brasas em raio de 8 blocos. Ignita por 6s.",
            NewSpellsR168::castEmberStorm);

        def("phoenix_dive", "Mergulho da Fênix", SpellSchool.FIRE, Rarity.EPIC, 40, 140, 28F, 16F,
            "Mergulha como uma fênix. Explode no impacto + imunidade ao fogo 10s.",
            NewSpellsR168::castPhoenixDive);

        def("magma_pillar", "Pilar de Magma", SpellSchool.FIRE, Rarity.RARE, 35, 120, 18F, 12F,
            "Ergue um pilar de magma. Atinge tudo em cima dele.",
            NewSpellsR168::castMagmaPillar);

        def("solar_seal", "Selo Solar", SpellSchool.FIRE, Rarity.RARE, 35, 200, 0F, 0F,
            "Selo runico sob seus pés. Cura 8HP/s + Fire Resistance por 30s.",
            NewSpellsR168::castSolarSeal);

        def("flame_lance", "Lança Flamejante", SpellSchool.FIRE, Rarity.UNCOMMON, 22, 50, 16F, 22F,
            "Lança rápida de fogo. Atravessa armadura leve.",
            NewSpellsR168::castFlameLance);

        def("infernal_nova", "Nova Infernal", SpellSchool.FIRE, Rarity.EPIC, 55, 160, 30F, 6F,
            "Explosão massiva ao redor do caster. Knockback brutal.",
            NewSpellsR168::castInfernalNova);

        def("sunburst", "Sunburst", SpellSchool.FIRE, Rarity.RARE, 30, 90, 20F, 14F,
            "Raio solar. Cega + Queima. +50% vs undead.",
            NewSpellsR168::castSunburst);

        def("blaze_chain", "Corrente Flamejante", SpellSchool.FIRE, Rarity.RARE, 28, 100, 12F, 18F,
            "Faisca pula entre 4 alvos próximos.",
            NewSpellsR168::castBlazeChain);

        def("fire_mandala", "Mandala de Fogo", SpellSchool.FIRE, Rarity.EPIC, 50, 240, 24F, 10F,
            "Desenha mandala ardente. Dano sustained por 5s na área.",
            NewSpellsR168::castFireMandala);
    }

    // ── FIRE casts ───────────────────────────────────────────────────────────

    private static boolean castSolarFlare(CastContext ctx) {
        LivingEntity tgt = pickEntity(ctx);
        if (tgt == null) return false;
        Vec3 tp = tgt.position().add(0, 1, 0);
        int c = colorFor(SpellSchool.FIRE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.5).pack2(Type.MANDALA, c, 1.2F)
            .sound(SoundEvents.BLAZE_SHOOT, 1.4F, 0.6F)
            .delay(15)
            .at(tp).pack2(Type.SOLAR_FLARE, c, 2.5F)
            .pack2(Type.ERUPTION, c, 2.0F)
            .sound(SoundEvents.GENERIC_EXPLODE, 1.2F, 1.4F)
            .execute(ctx.level, ctx.caster);
        tgt.hurt(SchoolDamageSource.fire(80).toVanilla(ctx.level, ctx.caster), ctx.def.damage * 1.5F);
        tgt.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        tgt.setSecondsOnFire(8);
        return true;
    }

    private static boolean castEmberStorm(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.FIRE);
        SpellVfx.builder()
            .at(center.add(0, 0.1, 0)).pack1(SpriteVfxRegistry.Type.CASTING, 2.5F)
            .delay(10)
            .run(p -> {
                for (int i = 0; i < 8; i++) {
                    final int ii = i;
                    br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(p.level, ii * 5, () -> {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * ctx.def.range;
                        Vec3 sp = center.add(Math.cos(a) * r, 3 + Math.random() * 2, Math.sin(a) * r);
                        Pack2VfxCatalog.spawn(p.level, sp, p.caster, Type.ERUPTION, c, 1.2F);
                    });
                }
            })
            .execute(ctx.level, ctx.caster);
        int n = aoeAt(ctx, center, ctx.def.range, ctx.def.damage * 1.4F);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(ctx.def.range))) {
            if (le != ctx.caster) le.setSecondsOnFire(6);
        }
        return n > 0 || true;
    }

    private static boolean castPhoenixDive(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        ctx.caster.setDeltaMovement(look.scale(2.0).add(0, 0.6, 0));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        int c = colorFor(SpellSchool.FIRE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.5).pack2(Type.FIREWORK, c, 2.0F)
            .pack1(SpriteVfxRegistry.Type.SUNBURN, 1.8F)
            .sound(SoundEvents.GENERIC_EXPLODE, 1.5F, 1.6F)
            .execute(ctx.level, ctx.caster);
        // Damage burst at impact next tick
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(4))) {
            if (le == ctx.caster) continue;
            le.hurt(SchoolDamageSource.fire(100).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
            le.setSecondsOnFire(6);
            Vec3 kb = le.position().subtract(ctx.caster.position()).normalize().scale(1.2);
            le.setDeltaMovement(kb.x, 0.5, kb.z);
            le.hurtMarked = true;
        }
        return true;
    }

    private static boolean castMagmaPillar(CastContext ctx) {
        Vec3 base = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.FIRE);
        for (int dy = 0; dy < 4; dy++) {
            final int yy = dy;
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, dy * 2, () -> {
                Pack2VfxCatalog.spawn(ctx.level, base.add(0, yy + 0.5, 0), ctx.caster,
                        Type.SMOKE_CLOUD, c, 1.6F);
            });
        }
        SpellVfx.builder()
            .at(base.add(0, 0.05, 0)).pack1(SpriteVfxRegistry.Type.CASTING, 2.0F)
            .delay(8)
            .at(base.add(0, 2, 0)).pack2(Type.ERUPTION, c, 3.0F)
            .sound(SoundEvents.BLAZE_BURN, 1.4F, 0.6F)
            .execute(ctx.level, ctx.caster);
        aoeAt(ctx, base, 3.0, ctx.def.damage);
        return true;
    }

    private static boolean castSolarSeal(CastContext ctx) {
        Vec3 c0 = ctx.caster.position();
        int c = colorFor(SpellSchool.FIRE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.1).pack2Ride(ctx.caster, Type.RUNIC_SEAL, c, 2.5F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 2.2F)
            .sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, 0.7F)
            .execute(ctx.level, ctx.caster);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
        return true;
    }

    private static boolean castFlameLance(CastContext ctx) {
        int c = colorFor(SpellSchool.FIRE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.5).pack2(Type.SLASH_TRAIL, c, 1.5F)
            .sound(SoundEvents.FIRE_AMBIENT, 1.0F, 1.6F)
            .execute(ctx.level, ctx.caster);
        LivingEntity tgt = pickEntity(ctx);
        if (tgt != null) {
            tgt.hurt(SchoolDamageSource.fire(50).toVanilla(ctx.level, ctx.caster), ctx.def.damage * 1.2F);
            tgt.setSecondsOnFire(4);
            SpellVfx.builder()
                .atEntity(tgt, 1).pack2(Type.CROSS_STRIKE, c, 1.4F)
                .execute(ctx.level, ctx.caster);
        }
        return true;
    }

    private static boolean castInfernalNova(CastContext ctx) {
        Vec3 c0 = ctx.caster.position();
        int c = colorFor(SpellSchool.FIRE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.0).pack2(Type.ERUPTION, c, 4.0F)
            .pack2(Type.SOLAR_FLARE, c, 5.0F)
            .pack1(SpriteVfxRegistry.Type.SUNBURN, 3.0F)
            .sound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.7F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(SchoolDamageSource.fire(100).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
            le.setSecondsOnFire(8);
            Vec3 kb = le.position().subtract(c0).normalize().scale(2.0);
            le.setDeltaMovement(kb.x, 0.8, kb.z);
            le.hurtMarked = true;
        }
        return true;
    }

    private static boolean castSunburst(CastContext ctx) {
        LivingEntity tgt = pickEntity(ctx);
        if (tgt == null) return false;
        int c = colorFor(SpellSchool.FIRE);
        float dmg = tgt.getMobType() == net.minecraft.world.entity.MobType.UNDEAD
                ? ctx.def.damage * 1.5F : ctx.def.damage;
        tgt.hurt(SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(ctx.level, ctx.caster), dmg);
        tgt.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        tgt.setSecondsOnFire(5);
        SpellVfx.builder()
            .atEntityHead(tgt).pack2(Type.SOLAR_FLARE, c, 2.0F)
            .delay(8).pack2(Type.STAR_BURST, c, 1.8F)
            .sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.4F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castBlazeChain(CastContext ctx) {
        LivingEntity first = pickEntity(ctx);
        if (first == null) return false;
        int c = colorFor(SpellSchool.FIRE);
        LivingEntity current = first;
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        DamageSource ds = SchoolDamageSource.fire(60).toVanilla(ctx.level, ctx.caster);
        for (int step = 0; step < 4 && current != null; step++) {
            hit.add(current);
            current.hurt(ds, ctx.def.damage * (1F - step * 0.15F));
            current.setSecondsOnFire(4);
            final Vec3 hp = current.position().add(0, 1, 0);
            final int delay = step * 6;
            SpellVfx.builder()
                .at(hp).pack2(Type.SLASH_TRAIL, c, 1.3F)
                .delay(delay)
                .execute(ctx.level, ctx.caster);
            LivingEntity next = null;
            double bestD = 16.0;
            for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                    current.getBoundingBox().inflate(6))) {
                if (le == ctx.caster || hit.contains(le)) continue;
                double d = le.distanceToSqr(current);
                if (d < bestD) { bestD = d; next = le; }
            }
            current = next;
        }
        return true;
    }

    private static boolean castFireMandala(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.FIRE);
        SpellVfx.builder()
            .at(center.add(0, 0.05, 0)).pack1(SpriteVfxRegistry.Type.CASTING, 3.0F)
            .delay(8).pack2(Type.MANDALA, c, 4.0F)
            .sound(SoundEvents.BLAZE_AMBIENT, 1.5F, 0.7F)
            .execute(ctx.level, ctx.caster);
        // sustained DoT 5s
        for (int t = 0; t < 100; t += 10) {
            final int tk = t;
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, tk, () -> {
                aoeAt(ctx, center, 4.0, ctx.def.damage * 0.3F);
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(center, center).inflate(4))) {
                    if (le != ctx.caster) le.setSecondsOnFire(2);
                }
            });
        }
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ICE / FROST — 9 spells
    // ════════════════════════════════════════════════════════════════════════

    private static void registerIceSet() {
        def("frost_nova", "Nova de Gelo", SpellSchool.ICE, Rarity.EPIC, 40, 130, 18F, 8F,
            "Explosão de gelo ao redor. Congela todos os alvos 4s.",
            NewSpellsR168::castFrostNova);

        def("ice_lance", "Lança de Gelo", SpellSchool.ICE, Rarity.UNCOMMON, 18, 40, 12F, 20F,
            "Lança de gelo afiada. Slowness IV no impacto.",
            NewSpellsR168::castIceLance);

        def("blizzard_field", "Campo de Nevasca", SpellSchool.ICE, Rarity.EPIC, 55, 200, 12F, 12F,
            "Tempestade gelada sustained 12s. Slowness contínuo.",
            NewSpellsR168::castBlizzardField);

        def("glacial_spike", "Lança Glacial", SpellSchool.ICE, Rarity.RARE, 32, 110, 22F, 18F,
            "Spike de gelo do chão. Mass damage + knockup.",
            NewSpellsR168::castGlacialSpike);

        def("frost_armor", "Armadura de Gelo", SpellSchool.ICE, Rarity.RARE, 30, 200, 0F, 0F,
            "Cobre-se de gelo. +50% defesa, atacantes ficam slow.",
            NewSpellsR168::castFrostArmor);

        def("snow_storm", "Nevasca", SpellSchool.ICE, Rarity.RARE, 28, 100, 10F, 10F,
            "Chuva de gelo em área. Slowness II.",
            NewSpellsR168::castSnowStorm);

        def("absolute_zero", "Zero Absoluto", SpellSchool.ICE, Rarity.EPIC, 60, 240, 26F, 10F,
            "Cone de frio absoluto. Congela 6s + Wither II.",
            NewSpellsR168::castAbsoluteZero);

        def("frost_seal", "Selo Gélido", SpellSchool.ICE, Rarity.RARE, 35, 180, 0F, 0F,
            "Selo gélido sob você. Mata em torno: slowness perma 30s.",
            NewSpellsR168::castFrostSeal);

        def("ice_prison", "Prisão de Gelo", SpellSchool.ICE, Rarity.RARE, 30, 140, 12F, 16F,
            "Aprisiona alvo em gelo. Imobilizado 5s.",
            NewSpellsR168::castIcePrison);
    }

    private static boolean castFrostNova(CastContext ctx) {
        int c = colorFor(SpellSchool.ICE);
        Vec3 cp = ctx.caster.position();
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.5).pack2(Type.SPARKLE_RING, c, 3.5F)
            .pack2(Type.MANDALA, c, 2.5F)
            .pack1(SpriteVfxRegistry.Type.FREEZING, 2.5F)
            .sound(SoundEvents.GLASS_BREAK, 1.5F, 0.6F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(SchoolDamageSource.ice(80).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
            le.setTicksFrozen(140);
        }
        return true;
    }

    private static boolean castIceLance(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.ICE);
        t.hurt(SchoolDamageSource.ice(60).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.5).pack2(Type.SLASH_TRAIL, c, 1.5F)
            .delay(6).atEntityHead(t).pack2(Type.CROSS_STRIKE, c, 1.6F)
            .sound(SoundEvents.GLASS_BREAK, 1.2F, 1.5F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castBlizzardField(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.ICE);
        SpellVfx.builder()
            .at(center.add(0, 0.05, 0)).pack1(SpriteVfxRegistry.Type.CASTING, 3.5F)
            .execute(ctx.level, ctx.caster);
        for (int t = 0; t < 240; t += 8) {
            final int tk = t;
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, tk, () -> {
                for (int i = 0; i < 3; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * ctx.def.range;
                    Vec3 sp = center.add(Math.cos(a) * r, 2 + Math.random() * 3, Math.sin(a) * r);
                    Pack2VfxCatalog.spawn(ctx.level, sp, ctx.caster, Type.SPARKLE_RING, c, 0.7F);
                }
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(center, center).inflate(ctx.def.range))) {
                    if (le == ctx.caster) continue;
                    if (tk % 20 == 0) le.hurt(
                            SchoolDamageSource.ice(40).toVanilla(ctx.level, ctx.caster),
                            ctx.def.damage * 0.5F);
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                }
            });
        }
        return true;
    }

    private static boolean castGlacialSpike(CastContext ctx) {
        Vec3 pos = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.ICE);
        // r169: TENDRIL_SCATTER era eldritch — substituído por CROSS_STRIKE + STAR_BURST (gelo)
        SpellVfx.builder()
            .at(pos).pack2(Type.CROSS_STRIKE, c, 2.5F)
            .delay(6).pack2(Type.SPARKLE_RING, c, 2.5F)
            .pack2(Type.STAR_BURST, c, 1.8F)
            .sound(SoundEvents.GLASS_BREAK, 1.6F, 0.7F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos, pos).inflate(3.0))) {
            if (le == ctx.caster) continue;
            le.hurt(SchoolDamageSource.ice(70).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
            le.setTicksFrozen(80);
            le.setDeltaMovement(le.getDeltaMovement().add(0, 0.9, 0));
            le.hurtMarked = true;
        }
        return true;
    }

    private static boolean castFrostArmor(CastContext ctx) {
        int c = colorFor(SpellSchool.ICE);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 0));
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.5).pack2Ride(ctx.caster, Type.MANDALA, c, 1.8F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 2.0F)
            .sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.5F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castSnowStorm(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.ICE);
        SpellVfx.builder()
            .at(center.add(0, 3, 0)).pack2(Type.SMOKE_CLOUD, c, 3.0F)
            .delay(10).pack2(Type.SPARKLE_RING, c, 2.5F)
            .sound(SoundEvents.PLAYER_HURT_FREEZE, 1.0F, 0.7F)
            .execute(ctx.level, ctx.caster);
        aoeAt(ctx, center, ctx.def.range, ctx.def.damage,
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
        return true;
    }

    private static boolean castAbsoluteZero(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        int c = colorFor(SpellSchool.ICE);
        Vec3 cone = ctx.caster.getEyePosition().add(look.scale(5));
        SpellVfx.builder()
            .at(cone).pack2(Type.MOON_ARC, c, 3.5F)
            .pack2(Type.SPARKLE_RING, c, 2.5F)
            .pack1(SpriteVfxRegistry.Type.FREEZING, 3.5F)
            .sound(SoundEvents.GLASS_BREAK, 2.0F, 0.4F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(cone, cone).inflate(5))) {
            if (le == ctx.caster) continue;
            Vec3 to = le.position().subtract(ctx.caster.position()).normalize();
            if (to.dot(look) < 0.5) continue; // only forward cone
            le.hurt(SchoolDamageSource.ice(80).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
            le.setTicksFrozen(120);
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
        }
        return true;
    }

    private static boolean castFrostSeal(CastContext ctx) {
        int c = colorFor(SpellSchool.ICE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.1).pack2Ride(ctx.caster, Type.RUNIC_SEAL, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 2.5F)
            .sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.5F)
            .execute(ctx.level, ctx.caster);
        // Aura tick — slow nearby enemies every 20t
        for (int t = 0; t < 600; t += 20) {
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(6))) {
                    if (le == ctx.caster) continue;
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
            });
        }
        return true;
    }

    private static boolean castIcePrison(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.ICE);
        t.hurt(SchoolDamageSource.ice(60).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 9));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 3));
        t.setTicksFrozen(100);
        SpellVfx.builder()
            .atEntityHead(t).pack2(Type.MANDALA, c, 2.5F)
            .pack2(Type.RUNIC_SEAL, c, 2.0F)
            .sound(SoundEvents.GLASS_BREAK, 1.3F, 0.7F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // LIGHTNING — 8 spells
    // ════════════════════════════════════════════════════════════════════════

    private static void registerLightningSet() {
        def("lightning_bolt2", "Raio Cêntrico", SpellSchool.LIGHTNING, Rarity.RARE, 30, 70, 18F, 24F,
            "Raio direto. Atordoa o alvo + arc damage para próximos.",
            NewSpellsR168::castLightningBolt2);

        def("storm_call", "Chamado da Tempestade", SpellSchool.LIGHTNING, Rarity.EPIC, 50, 200, 14F, 18F,
            "Convoca 5 raios em sequência na área.",
            NewSpellsR168::castStormCall);

        def("arc_chain", "Cadeia Elétrica", SpellSchool.LIGHTNING, Rarity.RARE, 28, 90, 14F, 14F,
            "Arc-jump entre até 6 alvos.",
            NewSpellsR168::castArcChain);

        def("thunder_clap", "Trovão", SpellSchool.LIGHTNING, Rarity.RARE, 35, 100, 16F, 10F,
            "Clap massivo. Atordoa + ensurdece + knockback.",
            NewSpellsR168::castThunderClap);

        def("shock_wave", "Onda de Choque", SpellSchool.LIGHTNING, Rarity.UNCOMMON, 20, 60, 12F, 8F,
            "Pulso elétrico cônico. Knockback.",
            NewSpellsR168::castShockWave);

        def("static_field", "Campo Estático", SpellSchool.LIGHTNING, Rarity.RARE, 30, 140, 8F, 12F,
            "Aura elétrica 10s. Quem entra leva choque.",
            NewSpellsR168::castStaticField);

        def("plasma_orb", "Esfera de Plasma", SpellSchool.LIGHTNING, Rarity.RARE, 32, 80, 16F, 18F,
            "Esfera lenta de plasma. Atordoa + queima.",
            NewSpellsR168::castPlasmaOrb);

        def("ball_lightning", "Bola de Raio", SpellSchool.LIGHTNING, Rarity.EPIC, 45, 160, 24F, 16F,
            "Convoca esfera elétrica gigante. Massive AoE.",
            NewSpellsR168::castBallLightning);
    }

    private static boolean castLightningBolt2(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(ctx.level);
        if (bolt != null) {
            bolt.moveTo(t.position());
            bolt.setVisualOnly(true);
            ctx.level.addFreshEntity(bolt);
        }
        int c = colorFor(SpellSchool.LIGHTNING);
        t.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        // arc to nearby
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                t.getBoundingBox().inflate(4))) {
            if (le == ctx.caster || le == t) continue;
            le.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                    ctx.def.damage * 0.5F);
        }
        SpellVfx.builder()
            .atEntityHead(t).pack2(Type.STAR_BURST, c, 2.0F)
            .pack2(Type.CROSS_STRIKE, c, 1.5F)
            .sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 1.5F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castStormCall(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.LIGHTNING);
        SpellVfx.builder()
            .at(center.add(0, 0.05, 0)).pack1(SpriteVfxRegistry.Type.CASTING, 3.5F)
            .execute(ctx.level, ctx.caster);
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, idx * 12, () -> {
                double a = (idx / 5.0) * Math.PI * 2;
                Vec3 sp = center.add(Math.cos(a) * 4, 0, Math.sin(a) * 4);
                LightningBolt b = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(ctx.level);
                if (b != null) {
                    b.moveTo(sp);
                    b.setVisualOnly(true);
                    ctx.level.addFreshEntity(b);
                }
                Pack2VfxCatalog.spawn(ctx.level, sp.add(0, 1, 0), ctx.caster, Type.STAR_BURST, c, 1.8F);
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

    private static boolean castArcChain(CastContext ctx) {
        LivingEntity first = pickEntity(ctx);
        if (first == null) return false;
        int c = colorFor(SpellSchool.LIGHTNING);
        LivingEntity cur = first;
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        DamageSource ds = SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster);
        for (int s = 0; s < 6 && cur != null; s++) {
            hit.add(cur);
            cur.hurt(ds, ctx.def.damage * (1F - s * 0.12F));
            final Vec3 hp = cur.position().add(0, 1, 0);
            final int dl = s * 4;
            SpellVfx.builder().at(hp).pack2(Type.STAR_BURST, c, 1.3F).delay(dl)
                .execute(ctx.level, ctx.caster);
            LivingEntity next = null;
            double bestD = 36.0;
            for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                    cur.getBoundingBox().inflate(7))) {
                if (le == ctx.caster || hit.contains(le)) continue;
                double d = le.distanceToSqr(cur);
                if (d < bestD) { bestD = d; next = le; }
            }
            cur = next;
        }
        return true;
    }

    private static boolean castThunderClap(CastContext ctx) {
        int c = colorFor(SpellSchool.LIGHTNING);
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.0).pack2(Type.SPARKLE_RING, c, 4.0F)
            .pack2(Type.ERUPTION, c, 3.0F)
            .sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.8F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                    ctx.def.damage);
            le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            Vec3 kb = le.position().subtract(ctx.caster.position()).normalize().scale(1.6);
            le.setDeltaMovement(kb.x, 0.5, kb.z);
            le.hurtMarked = true;
        }
        return true;
    }

    private static boolean castShockWave(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        int c = colorFor(SpellSchool.LIGHTNING);
        Vec3 front = ctx.caster.position().add(look.scale(3));
        SpellVfx.builder()
            .at(front).pack2(Type.SLASH_TRAIL, c, 2.5F)
            .sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2F, 1.3F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            Vec3 to = le.position().subtract(ctx.caster.position()).normalize();
            if (to.dot(look) < 0.3) continue;
            le.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                    ctx.def.damage);
            le.setDeltaMovement(look.scale(1.4).add(0, 0.4, 0));
            le.hurtMarked = true;
        }
        return true;
    }

    private static boolean castStaticField(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.LIGHTNING);
        SpellVfx.builder()
            .at(center).pack2(Type.RUNIC_SEAL, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 2.5F)
            .execute(ctx.level, ctx.caster);
        for (int t = 0; t < 200; t += 10) {
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(center, center).inflate(4))) {
                    if (le == ctx.caster) continue;
                    le.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                            ctx.def.damage * 0.4F);
                    Pack2VfxCatalog.spawn(ctx.level, le.position().add(0, 1, 0),
                            ctx.caster, Type.STAR_BURST, c, 0.8F);
                }
            });
        }
        return true;
    }

    private static boolean castPlasmaOrb(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.LIGHTNING);
        t.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        t.setSecondsOnFire(4);
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.4).pack2(Type.SPARKLE_RING, c, 1.5F)
            .delay(8).atEntityHead(t).pack2(Type.ERUPTION, c, 2.0F)
            .pack2(Type.STAR_BURST, c, 2.2F)
            .sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4F, 1.2F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castBallLightning(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.LIGHTNING);
        SpellVfx.builder()
            .at(center.add(0, 1.5, 0)).pack2(Type.SOLAR_FLARE, c, 4.0F)
            .pack2(Type.SPARKLE_RING, c, 3.5F)
            .pack2(Type.MANDALA, c, 3.0F)
            .sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 1.0F)
            .execute(ctx.level, ctx.caster);
        aoeAt(ctx, center, ctx.def.range, ctx.def.damage,
                new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // BLOOD — 7 spells
    // ════════════════════════════════════════════════════════════════════════

    private static void registerBloodSet() {
        def("blood_spike", "Espinho de Sangue", SpellSchool.BLOOD, Rarity.UNCOMMON, 20, 50, 14F, 18F,
            "Espinho de sangue do alvo. Drena 30% como cura.",
            NewSpellsR168::castBloodSpike);

        def("crimson_orb", "Esfera Carmim", SpellSchool.BLOOD, Rarity.RARE, 28, 80, 18F, 18F,
            "Esfera de sangue. Drena vida no impacto.",
            NewSpellsR168::castCrimsonOrb);

        def("blood_ritual", "Ritual de Sangue", SpellSchool.BLOOD, Rarity.EPIC, 50, 240, 30F, 12F,
            "Ritual sangrento. Custa 6HP, dano massivo + Weakness IV.",
            NewSpellsR168::castBloodRitual);

        def("hemorrhage", "Hemorragia", SpellSchool.BLOOD, Rarity.RARE, 25, 100, 0F, 16F,
            "Faz alvo sangrar 8s. Damage by Wither V.",
            NewSpellsR168::castHemorrhage);

        def("vampiric_aura", "Aura Vampírica", SpellSchool.BLOOD, Rarity.EPIC, 45, 200, 0F, 0F,
            "Aura 20s: roubas vida dos inimigos próximos.",
            NewSpellsR168::castVampiricAura);

        def("blood_chain", "Corrente de Sangue", SpellSchool.BLOOD, Rarity.RARE, 30, 110, 12F, 16F,
            "Acorrenta alvo a você. Drena enquanto conectado.",
            NewSpellsR168::castBloodChain);

        def("crimson_dome", "Domo Carmim", SpellSchool.BLOOD, Rarity.EPIC, 55, 240, 24F, 10F,
            "Domo de sangue 8s. Inimigos dentro sangram.",
            NewSpellsR168::castCrimsonDome);
    }

    private static boolean castBloodSpike(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.BLOOD);
        t.hurt(SchoolDamageSource.of(SpellSchool.BLOOD).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        ctx.caster.heal(ctx.def.damage * 0.3F);
        SpellVfx.builder()
            .atEntityHead(t).pack2(Type.TENDRIL_SCATTER, c, 1.6F)
            .pack2(Type.CROSS_STRIKE, c, 1.4F)
            .sound(SoundEvents.PLAYER_HURT, 1.0F, 0.7F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castCrimsonOrb(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.BLOOD);
        t.hurt(SchoolDamageSource.of(SpellSchool.BLOOD).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        ctx.caster.heal(ctx.def.damage * 0.5F);
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.4).pack2(Type.SPARKLE_RING, c, 1.2F)
            .delay(10).atEntityHead(t).pack2(Type.ERUPTION, c, 2.0F)
            .pack2(Type.TENDRIL_SCATTER, c, 1.8F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castBloodRitual(CastContext ctx) {
        int c = colorFor(SpellSchool.BLOOD);
        ctx.caster.hurt(ctx.level.damageSources().magic(), 6);
        Vec3 center = ctx.caster.position();
        SpellVfx.builder()
            .at(center).pack2(Type.RUNIC_SEAL, c, 4.0F)
            .pack2(Type.HORROR_SIGIL, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.CASTING, 3.5F)
            .sound(SoundEvents.WITHER_AMBIENT, 1.2F, 0.7F)
            .execute(ctx.level, ctx.caster);
        aoeAt(ctx, center, ctx.def.range, ctx.def.damage,
                new MobEffectInstance(MobEffects.WEAKNESS, 200, 3),
                new MobEffectInstance(MobEffects.WITHER, 100, 1));
        return true;
    }

    private static boolean castHemorrhage(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.BLOOD);
        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 4));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
        SpellVfx.builder()
            .atEntity(t, 1.2).pack2Ride(t, Type.TENDRIL_SCATTER, c, 1.5F)
            .sound(SoundEvents.PLAYER_HURT, 1.0F, 0.5F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castVampiricAura(CastContext ctx) {
        int c = colorFor(SpellSchool.BLOOD);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.1).pack2Ride(ctx.caster, Type.HORROR_SIGIL, c, 2.0F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 2.5F)
            .sound(SoundEvents.WITHER_AMBIENT, 1.0F, 0.6F)
            .execute(ctx.level, ctx.caster);
        for (int t = 0; t < 400; t += 20) {
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(6))) {
                    if (le == ctx.caster) continue;
                    le.hurt(SchoolDamageSource.of(SpellSchool.BLOOD).toVanilla(ctx.level, ctx.caster),
                            2F);
                    ctx.caster.heal(1F);
                }
            });
        }
        return true;
    }

    private static boolean castBloodChain(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.BLOOD);
        for (int tick = 0; tick < 100; tick += 10) {
            final int tk = tick;
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, tk, () -> {
                if (!t.isAlive() || t.distanceTo(ctx.caster) > 20) return;
                t.hurt(SchoolDamageSource.of(SpellSchool.BLOOD).toVanilla(ctx.level, ctx.caster),
                        ctx.def.damage * 0.2F);
                ctx.caster.heal(ctx.def.damage * 0.2F);
                Pack2VfxCatalog.spawn(ctx.level, t.position().add(0, 1, 0), ctx.caster,
                        Type.SLASH_TRAIL, c, 1.2F);
            });
        }
        return true;
    }

    private static boolean castCrimsonDome(CastContext ctx) {
        Vec3 center = ctx.caster.position();
        int c = colorFor(SpellSchool.BLOOD);
        SpellVfx.builder()
            .at(center.add(0, 0.1, 0)).pack2(Type.RUNIC_SEAL, c, 4.5F)
            .delay(10).pack2(Type.HORROR_SIGIL, c, 3.5F)
            .sound(SoundEvents.WITHER_DEATH, 1.0F, 0.6F)
            .execute(ctx.level, ctx.caster);
        for (int t = 0; t < 160; t += 20) {
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, t, () -> {
                aoeAt(ctx, center, ctx.def.range, ctx.def.damage * 0.25F,
                        new MobEffectInstance(MobEffects.WITHER, 60, 1));
            });
        }
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ELDRITCH — 7 spells
    // ════════════════════════════════════════════════════════════════════════

    private static void registerEldritchSet() {
        def("void_bloom", "Florescer do Vazio", SpellSchool.ELDRITCH, Rarity.EPIC, 50, 200, 22F, 16F,
            "Flor eldritch desabrocha no alvo. AoE + Nausea.",
            NewSpellsR168::castVoidBloom);

        def("eye_of_madness", "Olho da Loucura", SpellSchool.ELDRITCH, Rarity.RARE, 35, 120, 14F, 20F,
            "Olho cósmico fixa alvo. Confusion + Wither.",
            NewSpellsR168::castEyeOfMadness);

        def("dimensional_rift", "Fenda Dimensional", SpellSchool.ELDRITCH, Rarity.EPIC, 55, 240, 24F, 18F,
            "Abre rift. Suga inimigos para o vazio.",
            NewSpellsR168::castDimensionalRift);

        def("nightmare", "Pesadelo", SpellSchool.ELDRITCH, Rarity.RARE, 30, 140, 12F, 16F,
            "Mostra horrores. Confusion VIII + Darkness.",
            NewSpellsR168::castNightmare);

        def("void_seal", "Selo do Vazio", SpellSchool.ELDRITCH, Rarity.RARE, 40, 180, 0F, 0F,
            "Selo do vazio sob você. Inimigos próximos: dread.",
            NewSpellsR168::castVoidSeal);

        def("phantom_strike", "Golpe Fantasma", SpellSchool.ELDRITCH, Rarity.RARE, 28, 90, 18F, 14F,
            "Striker fantasmal. Atravessa armadura.",
            NewSpellsR168::castPhantomStrike);

        def("cosmic_horror", "Horror Cósmico", SpellSchool.ELDRITCH, Rarity.EPIC, 60, 300, 35F, 14F,
            "Convoca horror cósmico. Mass damage + insanity.",
            NewSpellsR168::castCosmicHorror);
    }

    private static boolean castVoidBloom(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.ELDRITCH);
        SpellVfx.builder()
            .atEntityHead(t).pack2(Type.MANDALA, c, 2.5F)
            .delay(10).pack2(Type.TENDRIL_SCATTER, c, 3.0F)
            .pack2(Type.HORROR_SIGIL, c, 2.5F)
            .sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.0F, 0.8F)
            .execute(ctx.level, ctx.caster);
        aoeAt(ctx, t.position(), 4.0, ctx.def.damage,
                new MobEffectInstance(MobEffects.CONFUSION, 200, 1),
                new MobEffectInstance(MobEffects.WITHER, 120, 1));
        return true;
    }

    private static boolean castEyeOfMadness(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.ELDRITCH);
        t.hurt(SchoolDamageSource.of(SpellSchool.ELDRITCH).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 2));
        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
        SpellVfx.builder()
            .atEntityHead(t).pack2(Type.HORROR_SIGIL, c, 2.5F)
            .pack2Ride(t, Type.MANDALA, c, 1.8F)
            .sound(SoundEvents.ENDERMAN_STARE, 1.2F, 0.8F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castDimensionalRift(CastContext ctx) {
        Vec3 pos = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.ELDRITCH);
        SpellVfx.builder()
            .at(pos.add(0, 1.5, 0)).pack2(Type.MOON_ARC, c, 4.0F)
            .pack1(SpriteVfxRegistry.Type.VORTEX, 2.5F)
            .sound(SoundEvents.PORTAL_AMBIENT, 1.5F, 0.4F)
            .execute(ctx.level, ctx.caster);
        for (int t = 0; t < 100; t += 10) {
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(pos, pos).inflate(ctx.def.range))) {
                    if (le == ctx.caster) continue;
                    Vec3 pull = pos.subtract(le.position()).normalize().scale(1.0);
                    le.setDeltaMovement(le.getDeltaMovement().add(pull.x, 0.05, pull.z));
                    le.hurtMarked = true;
                    le.hurt(SchoolDamageSource.of(SpellSchool.ELDRITCH).toVanilla(ctx.level, ctx.caster),
                            ctx.def.damage * 0.15F);
                }
            });
        }
        return true;
    }

    private static boolean castNightmare(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.ELDRITCH);
        t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 7));
        t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400, 0));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 2));
        SpellVfx.builder()
            .atEntityHead(t).pack2(Type.HORROR_SIGIL, c, 2.5F)
            .delay(8).pack2(Type.TENDRIL_SCATTER, c, 2.0F)
            .sound(SoundEvents.ENDERMAN_SCREAM, 0.8F, 0.6F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castVoidSeal(CastContext ctx) {
        int c = colorFor(SpellSchool.ELDRITCH);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.05).pack2Ride(ctx.caster, Type.RUNIC_SEAL, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.VORTEX, 2.5F)
            .sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.4F)
            .execute(ctx.level, ctx.caster);
        for (int t = 0; t < 600; t += 20) {
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(6))) {
                    if (le == ctx.caster) continue;
                    le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                    le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                }
            });
        }
        return true;
    }

    private static boolean castPhantomStrike(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.ELDRITCH);
        t.hurt(SchoolDamageSource.of(SpellSchool.ELDRITCH).toVanilla(ctx.level, ctx.caster),
                ctx.def.damage);
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.4).pack1(SpriteVfxRegistry.Type.PHANTOM, 1.5F)
            .delay(6).atEntityHead(t).pack2(Type.SLASH_TRAIL, c, 1.6F)
            .pack2(Type.CROSS_STRIKE, c, 1.4F)
            .sound(SoundEvents.PHANTOM_BITE, 1.2F, 1.0F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castCosmicHorror(CastContext ctx) {
        Vec3 pos = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.ELDRITCH);
        SpellVfx.builder()
            .at(pos.add(0, 2, 0)).pack2(Type.HORROR_SIGIL, c, 4.5F)
            .pack2(Type.MOON_ARC, c, 4.0F)
            .pack2(Type.TENDRIL_SCATTER, c, 3.5F)
            .pack1(SpriteVfxRegistry.Type.VORTEX, 3.0F)
            .sound(SoundEvents.WARDEN_SONIC_BOOM, 1.5F, 0.5F)
            .execute(ctx.level, ctx.caster);
        aoeAt(ctx, pos, ctx.def.range, ctx.def.damage,
                new MobEffectInstance(MobEffects.WITHER, 400, 3),
                new MobEffectInstance(MobEffects.CONFUSION, 400, 2),
                new MobEffectInstance(MobEffects.DARKNESS, 400, 0));
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // HOLY — 5 spells
    // ════════════════════════════════════════════════════════════════════════

    private static void registerHolySet() {
        def("divine_seal", "Selo Divino", SpellSchool.HOLY, Rarity.EPIC, 40, 200, 0F, 0F,
            "Selo sagrado. Cura 12HP/s + Regen IV por 20s.",
            NewSpellsR168::castDivineSeal);

        def("radiant_burst", "Explosão Radiante", SpellSchool.HOLY, Rarity.RARE, 30, 100, 18F, 14F,
            "Explosão divina. +100% vs undead. Cura aliados.",
            NewSpellsR168::castRadiantBurst);

        def("angelic_wings", "Asas Angelicais", SpellSchool.HOLY, Rarity.EPIC, 50, 400, 0F, 0F,
            "Voa por 30s. Slow fall + speed boost.",
            NewSpellsR168::castAngelicWings);

        def("holy_lance", "Lança Sagrada", SpellSchool.HOLY, Rarity.RARE, 28, 80, 20F, 22F,
            "Lança de luz. Atravessa armadura completamente.",
            NewSpellsR168::castHolyLance);

        def("sanctuary", "Santuário", SpellSchool.HOLY, Rarity.EPIC, 55, 240, 0F, 0F,
            "Cria santuário 12s. Imunidade total + cura sustained.",
            NewSpellsR168::castSanctuary);
    }

    private static boolean castDivineSeal(CastContext ctx) {
        int c = colorFor(SpellSchool.HOLY);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.05).pack2Ride(ctx.caster, Type.RUNIC_SEAL, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 2.5F)
            .pack1(SpriteVfxRegistry.Type.MAGIC_BUBBLES, 1.8F)
            .sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.4F)
            .execute(ctx.level, ctx.caster);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 3));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
        return true;
    }

    private static boolean castRadiantBurst(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.HOLY);
        SpellVfx.builder()
            .at(center).pack2(Type.SOLAR_FLARE, c, 3.5F)
            .pack2(Type.MANDALA, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.SUNBURN, 2.5F)
            .sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.6F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(ctx.def.range))) {
            if (le == ctx.caster) {
                le.heal(ctx.def.damage * 0.5F);
                continue;
            }
            float dmg = le.getMobType() == net.minecraft.world.entity.MobType.UNDEAD
                    ? ctx.def.damage * 2F : ctx.def.damage;
            le.hurt(SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(ctx.level, ctx.caster), dmg);
            le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }
        return true;
    }

    private static boolean castAngelicWings(CastContext ctx) {
        int c = colorFor(SpellSchool.HOLY);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 4));
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.0).pack2Ride(ctx.caster, Type.STAR_BURST, c, 2.0F)
            .pack1(SpriteVfxRegistry.Type.SUNBURN, 2.5F)
            .sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 1.8F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castHolyLance(CastContext ctx) {
        LivingEntity t = pickEntity(ctx);
        if (t == null) return false;
        int c = colorFor(SpellSchool.HOLY);
        t.hurt(SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(ctx.level, ctx.caster), ctx.def.damage * 1.5F);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        SpellVfx.builder()
            .atEntity(ctx.caster, 1.5).pack2(Type.SLASH_TRAIL, c, 2.0F)
            .delay(8).atEntityHead(t).pack2(Type.CROSS_STRIKE, c, 2.0F)
            .pack2(Type.STAR_BURST, c, 1.8F)
            .sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.5F)
            .execute(ctx.level, ctx.caster);
        return true;
    }

    private static boolean castSanctuary(CastContext ctx) {
        int c = colorFor(SpellSchool.HOLY);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.05).pack2Ride(ctx.caster, Type.RUNIC_SEAL, c, 4.0F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 3.0F)
            .pack1(SpriteVfxRegistry.Type.MAGIC_BUBBLES, 2.5F)
            .sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 1.0F)
            .execute(ctx.level, ctx.caster);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 4));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 4));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 240, 3));
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    // NATURE / UTILITY — 4 spells
    // ════════════════════════════════════════════════════════════════════════

    private static void registerNatureSet() {
        def("nature_seal", "Selo da Natureza", SpellSchool.NATURE, Rarity.RARE, 30, 180, 0F, 0F,
            "Selo verdejante. Cura sustained + Regen III + Resistance II.",
            NewSpellsR168::castNatureSeal);

        def("thorn_garden", "Jardim de Espinhos", SpellSchool.NATURE, Rarity.RARE, 35, 140, 14F, 10F,
            "Espinhos rondam você 15s. Dano sustained.",
            NewSpellsR168::castThornGarden);

        def("verdant_burst", "Explosão Verdejante", SpellSchool.NATURE, Rarity.UNCOMMON, 22, 60, 12F, 12F,
            "Explosão de folhas. Cura aliados, dano em inimigos.",
            NewSpellsR168::castVerdantBurst);

        def("life_seal", "Selo da Vida", SpellSchool.NATURE, Rarity.EPIC, 60, 400, 0F, 0F,
            "Selo de vida. Cura ALL próximos + Resistance + Regen 30s.",
            NewSpellsR168::castLifeSeal);
    }

    private static boolean castNatureSeal(CastContext ctx) {
        int c = colorFor(SpellSchool.NATURE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.05).pack2Ride(ctx.caster, Type.RUNIC_SEAL, c, 3.0F)
            .pack1(SpriteVfxRegistry.Type.MAGIC_BUBBLES, 2.0F)
            .sound(SoundEvents.BEACON_ACTIVATE, 1.0F, 1.5F)
            .execute(ctx.level, ctx.caster);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
        return true;
    }

    private static boolean castThornGarden(CastContext ctx) {
        int c = colorFor(SpellSchool.NATURE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.05).pack2Ride(ctx.caster, Type.TENDRIL_SCATTER, c, 2.5F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 2.0F)
            .execute(ctx.level, ctx.caster);
        for (int t = 0; t < 300; t += 15) {
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(4))) {
                    if (le == ctx.caster) continue;
                    le.hurt(SchoolDamageSource.of(SpellSchool.NATURE).toVanilla(ctx.level, ctx.caster),
                            ctx.def.damage * 0.2F);
                }
            });
        }
        return true;
    }

    private static boolean castVerdantBurst(CastContext ctx) {
        Vec3 center = ctx.pickHit(ctx.def.range);
        int c = colorFor(SpellSchool.NATURE);
        SpellVfx.builder()
            .at(center).pack2(Type.SPARKLE_RING, c, 2.5F)
            .pack2(Type.MANDALA, c, 2.0F)
            .pack1(SpriteVfxRegistry.Type.MAGIC_BUBBLES, 1.8F)
            .sound(SoundEvents.AZALEA_BREAK, 1.2F, 1.3F)
            .execute(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(ctx.def.range))) {
            if (le == ctx.caster) {
                le.heal(ctx.def.damage);
                continue;
            }
            le.hurt(SchoolDamageSource.of(SpellSchool.NATURE).toVanilla(ctx.level, ctx.caster),
                    ctx.def.damage);
        }
        return true;
    }

    private static boolean castLifeSeal(CastContext ctx) {
        int c = colorFor(SpellSchool.NATURE);
        SpellVfx.builder()
            .atEntity(ctx.caster, 0.05).pack2(Type.RUNIC_SEAL, c, 5.0F)
            .pack2(Type.MANDALA, c, 4.0F)
            .pack1(SpriteVfxRegistry.Type.PROTECTION, 3.5F)
            .pack1(SpriteVfxRegistry.Type.MAGIC_BUBBLES, 3.0F)
            .sound(SoundEvents.BEACON_POWER_SELECT, 1.5F, 1.0F)
            .execute(ctx.level, ctx.caster);
        for (int t = 0; t < 600; t += 20) {
            br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.schedule(ctx.level, t, () -> {
                for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                        ctx.caster.getBoundingBox().inflate(10))) {
                    if (le instanceof net.minecraft.world.entity.monster.Monster) continue;
                    le.heal(2F);
                    le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
                    le.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
                }
            });
        }
        return true;
    }
}
