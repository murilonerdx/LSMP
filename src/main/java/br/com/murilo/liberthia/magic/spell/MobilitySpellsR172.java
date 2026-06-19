package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * r172: <b>15 feitiços ACESSÍVEIS</b> — mobilidade + utilidade no estilo das
 * "Asas da Fonte / Salto de Fase / Passo do Vento", porém com custo de mana,
 * cooldown e dano BAIXOS (early/mid-game friendly). Todos com EFEITO REAL
 * (dash, slow-fall, teleport, buffs, mini-combate) e VFX vanilla leve.
 *
 * <p>Registrados em {@link SpellLibrary} via {@link SpellLibrary#registerExternal}.
 * Cada um vira um {@code spell_<id>} scroll castável (ver ModItems).
 */
public final class MobilitySpellsR172 {

    private MobilitySpellsR172() {}

    @FunctionalInterface private interface Impl { boolean run(CastContext ctx); }

    private static void def(String id, String name, SpellSchool school, Rarity rar,
                            int mana, int cd, float dmg, float range, String lore, Impl impl) {
        SpellLibrary.registerExternal(SpellDef.builder(id)
                .name(name).school(school).rarity(rar)
                .mana(mana).cooldown(cd).damage(dmg).range(range)
                .lore(lore).cast(impl::run).build());
    }

    private static void sound(CastContext ctx, net.minecraft.sounds.SoundEvent s, float vol, float pitch) {
        ctx.level.playSound(null, ctx.caster.blockPosition(), s, SoundSource.PLAYERS, vol, pitch);
    }

    private static void burst(CastContext ctx, net.minecraft.core.particles.ParticleOptions p, int n) {
        ctx.level.sendParticles(p, ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(),
                n, 0.4, 0.6, 0.4, 0.08);
    }

    private static DamageSource ds(CastContext ctx) {
        return SchoolDamageSource.of(ctx.def.school).toVanilla(ctx.level, ctx.caster);
    }

    public static void registerAll() {
        def("air_dash", "Investida Aérea", SpellSchool.NATURE, Rarity.COMMON,
            8, 40, 0F, 0F, "Avança no ar na direção do olhar + queda suave breve.",
            MobilitySpellsR172::castAirDash);
        def("feather_grace", "Graça da Pena", SpellSchool.HOLY, Rarity.COMMON,
            10, 120, 0F, 0F, "Queda lenta + super-salto por 30s. Mobilidade pura.",
            MobilitySpellsR172::castFeatherGrace);
        def("gust_leap", "Salto da Brisa", SpellSchool.NATURE, Rarity.COMMON,
            8, 50, 0F, 0F, "Rajada te lança pra cima + queda suave. Escala paredes.",
            MobilitySpellsR172::castGustLeap);
        def("shadow_step", "Passo Sombrio", SpellSchool.ELDRITCH, Rarity.UNCOMMON,
            14, 80, 0F, 8F, "Teleporta 8b à frente + invisibilidade breve (2s).",
            MobilitySpellsR172::castShadowStep);
        def("swift_current", "Correnteza Veloz", SpellSchool.ICE, Rarity.COMMON,
            12, 100, 0F, 0F, "Velocidade II + Graça do Golfinho 20s. Corre na água.",
            MobilitySpellsR172::castSwiftCurrent);
        def("stone_skin", "Pele de Pedra", SpellSchool.NATURE, Rarity.UNCOMMON,
            15, 140, 0F, 0F, "Resistência II + 4 de Absorção por 20s. Defesa barata.",
            MobilitySpellsR172::castStoneSkin);
        def("ember_spark", "Faísca de Brasa", SpellSchool.FIRE, Rarity.COMMON,
            6, 25, 4F, 16F, "Faísca rápida e barata. Pequeno dano + incendeia 3s.",
            MobilitySpellsR172::castEmberSpark);
        def("frost_touch", "Toque Gélido", SpellSchool.ICE, Rarity.COMMON,
            8, 40, 5F, 5F, "Toque curto que congela e lentifica o alvo mais próximo.",
            MobilitySpellsR172::castFrostTouch);
        def("static_jolt", "Solavanco Estático", SpellSchool.LIGHTNING, Rarity.COMMON,
            8, 35, 5F, 5F, "Pulso elétrico curto. Empurra inimigos em volta.",
            MobilitySpellsR172::castStaticJolt);
        def("minor_heal", "Cura Menor", SpellSchool.HOLY, Rarity.COMMON,
            12, 100, 6F, 0F, "Cura 6 de vida + Regeneração I. Cura barata e rápida.",
            MobilitySpellsR172::castMinorHeal);
        def("thorn_whip", "Chicote de Espinhos", SpellSchool.NATURE, Rarity.UNCOMMON,
            10, 50, 5F, 12F, "Puxa o alvo até você + dano leve + Veneno.",
            MobilitySpellsR172::castThornWhip);
        def("glide", "Planar", SpellSchool.HOLY, Rarity.COMMON,
            10, 80, 0F, 0F, "Plana suavemente 12s, empurrado pela direção do olhar.",
            MobilitySpellsR172::castGlide);
        def("blink_short", "Piscar Curto", SpellSchool.ELDRITCH, Rarity.COMMON,
            10, 60, 0F, 6F, "Teleporte curto de 6b. Versão barata do Blink.",
            MobilitySpellsR172::castBlinkShort);
        def("warmth", "Aconchego", SpellSchool.FIRE, Rarity.COMMON,
            12, 120, 0F, 0F, "Resist. ao Fogo 30s + descongela + cura 3. Anti-frio.",
            MobilitySpellsR172::castWarmth);
        def("nimble_reflexes", "Reflexos Ágeis", SpellSchool.LIGHTNING, Rarity.UNCOMMON,
            14, 120, 0F, 0F, "Velocidade III + Salto II + Pressa II por 15s.",
            MobilitySpellsR172::castNimbleReflexes);
    }

    // ── Implementations (efeitos REAIS) ─────────────────────────────────────

    private static boolean castEmberSpark(CastContext ctx) {
        Vec3 motion = ctx.lookVec().scale(1.8);
        SpellProjectileEntity proj = new SpellProjectileEntity(
                ctx.level, ctx.caster, motion, ctx.def.school, ctx.def.damage);
        int life = Math.max(40, (int) (ctx.def.range / 1.8) + 10);
        proj.withConfig(life, false, 0F, false);
        ctx.level.addFreshEntity(proj);
        Vec3 o = ctx.castOrigin();
        ctx.level.sendParticles(ParticleTypes.FLAME, o.x, o.y, o.z, 8, 0.1, 0.1, 0.1, 0.02);
        sound(ctx, SoundEvents.BLAZE_SHOOT, 0.8F, 1.4F);
        return true;
    }

    private static boolean castAirDash(CastContext ctx) {
        Vec3 look = ctx.lookVec();
        ctx.caster.setDeltaMovement(look.x * 1.6, Math.max(0.25, look.y * 1.2 + 0.2), look.z * 1.6);
        ctx.caster.hurtMarked = true;
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
        ctx.caster.fallDistance = 0;
        burst(ctx, ParticleTypes.CLOUD, 25);
        sound(ctx, SoundEvents.PHANTOM_FLAP, 1.0F, 1.4F);
        return true;
    }

    private static boolean castFeatherGrace(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 2));
        burst(ctx, ParticleTypes.END_ROD, 30);
        sound(ctx, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
        return true;
    }

    private static boolean castGustLeap(CastContext ctx) {
        ctx.caster.setDeltaMovement(ctx.caster.getDeltaMovement().x, 1.05, ctx.caster.getDeltaMovement().z);
        ctx.caster.hurtMarked = true;
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
        ctx.caster.fallDistance = 0;
        burst(ctx, ParticleTypes.CLOUD, 30);
        sound(ctx, SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 1.4F);
        return true;
    }

    private static boolean castShadowStep(CastContext ctx) {
        Vec3 dest = ctx.caster.position().add(ctx.lookVec().scale(ctx.def.range));
        burst(ctx, ParticleTypes.PORTAL, 30);
        ctx.caster.teleportTo(dest.x, dest.y, dest.z);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0));
        burst(ctx, ParticleTypes.PORTAL, 30);
        sound(ctx, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.1F);
        return true;
    }

    private static boolean castSwiftCurrent(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 400, 0));
        burst(ctx, ParticleTypes.BUBBLE, 25);
        sound(ctx, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0F, 1.4F);
        return true;
    }

    private static boolean castStoneSkin(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
        burst(ctx, ParticleTypes.CRIT, 20);
        sound(ctx, SoundEvents.STONE_PLACE, 1.0F, 0.7F);
        return true;
    }

    private static boolean castFrostTouch(CastContext ctx) {
        LivingEntity t = ctx.pickTarget(ctx.def.range);
        if (t == null) {
            for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                    ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
                if (le != ctx.caster) { t = le; break; }
            }
        }
        if (t == null) return false;
        t.hurt(SchoolDamageSource.ice(60).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
        t.setTicksFrozen(t.getTicksFrozen() + 80);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        ctx.level.sendParticles(ParticleTypes.SNOWFLAKE, t.getX(), t.getY() + 1, t.getZ(),
                18, 0.3, 0.4, 0.3, 0.05);
        sound(ctx, SoundEvents.GLASS_HIT, 0.9F, 1.2F);
        return true;
    }

    private static boolean castStaticJolt(CastContext ctx) {
        Vec3 cp = ctx.caster.position();
        DamageSource d = ds(ctx);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(d, ctx.def.damage);
            Vec3 kb = le.position().subtract(cp).normalize().scale(0.7);
            le.setDeltaMovement(kb.x, 0.35, kb.z);
            le.hurtMarked = true;
        }
        burst(ctx, ParticleTypes.ELECTRIC_SPARK, 25);
        sound(ctx, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8F, 1.5F);
        return true;
    }

    private static boolean castMinorHeal(CastContext ctx) {
        ctx.caster.heal(ctx.def.damage);
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
        burst(ctx, ParticleTypes.HEART, 8);
        ctx.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ctx.caster.getX(), ctx.caster.getY() + 1,
                ctx.caster.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
        sound(ctx, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.5F);
        return true;
    }

    private static boolean castThornWhip(CastContext ctx) {
        LivingEntity t = ctx.pickTarget(ctx.def.range);
        if (t == null) return false;
        t.hurt(ds(ctx), ctx.def.damage);
        t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        Vec3 pull = ctx.caster.position().subtract(t.position()).normalize().scale(1.2);
        t.setDeltaMovement(pull.x, 0.25, pull.z);
        t.hurtMarked = true;
        ctx.level.sendParticles(ParticleTypes.COMPOSTER, t.getX(), t.getY() + 1, t.getZ(),
                15, 0.3, 0.4, 0.3, 0.02);
        sound(ctx, SoundEvents.AZALEA_BREAK, 1.0F, 0.8F);
        return true;
    }

    private static boolean castGlide(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 240, 0));
        Vec3 look = ctx.lookVec();
        ctx.caster.setDeltaMovement(ctx.caster.getDeltaMovement().add(look.x * 0.4, 0, look.z * 0.4));
        ctx.caster.hurtMarked = true;
        burst(ctx, ParticleTypes.CLOUD, 15);
        sound(ctx, SoundEvents.PHANTOM_FLAP, 0.8F, 1.6F);
        return true;
    }

    private static boolean castBlinkShort(CastContext ctx) {
        Vec3 dest = ctx.caster.position().add(ctx.lookVec().scale(ctx.def.range));
        burst(ctx, ParticleTypes.REVERSE_PORTAL, 20);
        ctx.caster.teleportTo(dest.x, dest.y, dest.z);
        ctx.caster.fallDistance = 0;
        burst(ctx, ParticleTypes.REVERSE_PORTAL, 20);
        sound(ctx, SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.3F);
        return true;
    }

    private static boolean castWarmth(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        ctx.caster.setTicksFrozen(0);
        ctx.caster.heal(3F);
        ctx.caster.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        burst(ctx, ParticleTypes.FLAME, 18);
        sound(ctx, SoundEvents.FIRE_AMBIENT, 1.0F, 1.0F);
        return true;
    }

    private static boolean castNimbleReflexes(CastContext ctx) {
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 1));
        ctx.caster.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 1));
        burst(ctx, ParticleTypes.ELECTRIC_SPARK, 22);
        sound(ctx, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 2.0F);
        return true;
    }
}
