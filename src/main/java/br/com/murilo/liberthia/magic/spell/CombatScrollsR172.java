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
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * r172: <b>8 scrolls de combate</b> com danos VARIADOS (5-10), mana/cd
 * acessíveis. Complementam o roster (mais opções de dano médio/baixo, como
 * pedido). Todos com efeito real.
 */
public final class CombatScrollsR172 {

    private CombatScrollsR172() {}

    @FunctionalInterface private interface Impl { boolean run(CastContext ctx); }

    private static void def(String id, String name, SpellSchool school, Rarity rar,
                            int mana, int cd, float dmg, float range, String lore, Impl impl) {
        SpellLibrary.registerExternal(SpellDef.builder(id)
                .name(name).school(school).rarity(rar)
                .mana(mana).cooldown(cd).damage(dmg).range(range)
                .lore(lore).cast(impl::run).build());
    }

    private static DamageSource ds(CastContext ctx) {
        return SchoolDamageSource.of(ctx.def.school).toVanilla(ctx.level, ctx.caster);
    }

    private static LivingEntity nearest(CastContext ctx) {
        LivingEntity t = ctx.pickTarget(ctx.def.range);
        if (t != null) return t;
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class,
                ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le != ctx.caster) return le;
        }
        return null;
    }

    private static void hit(CastContext ctx, LivingEntity t, net.minecraft.core.particles.ParticleOptions p) {
        ctx.level.sendParticles(p, t.getX(), t.getY() + 1, t.getZ(), 16, 0.3, 0.4, 0.3, 0.04);
    }

    public static void registerAll() {
        def("arc_bolt", "Raio Arcano", SpellSchool.ELDRITCH, Rarity.COMMON,
            10, 30, 7F, 20F, "Dardo arcano simples no alvo mirado.",
            CombatScrollsR172::castArcBolt);
        def("cinder_burst", "Explosão de Cinzas", SpellSchool.FIRE, Rarity.UNCOMMON,
            16, 60, 9F, 6F, "Pequena explosão de brasas em volta. Incendeia.",
            CombatScrollsR172::castCinderBurst);
        def("frost_shard", "Estilhaço Gélido", SpellSchool.ICE, Rarity.COMMON,
            12, 40, 8F, 18F, "Estilhaço de gelo. Lentidão + congela um pouco.",
            CombatScrollsR172::castFrostShard);
        def("thunder_clap", "Estrondo", SpellSchool.LIGHTNING, Rarity.UNCOMMON,
            16, 60, 8F, 7F, "Estrondo elétrico radial. Empurra inimigos.",
            CombatScrollsR172::castThunderClap);
        def("venom_spit", "Cuspe Venenoso", SpellSchool.NATURE, Rarity.COMMON,
            10, 40, 5F, 16F, "Cospe veneno no alvo. Veneno II + dano leve.",
            CombatScrollsR172::castVenomSpit);
        def("blood_lash", "Açoite de Sangue", SpellSchool.BLOOD, Rarity.UNCOMMON,
            14, 50, 9F, 14F, "Açoite sangrento. Cura você em 50% do dano.",
            CombatScrollsR172::castBloodLash);
        def("minor_smite", "Castigo Menor", SpellSchool.HOLY, Rarity.UNCOMMON,
            14, 50, 10F, 18F, "Luz divina no alvo. +dano vs undead + Glowing.",
            CombatScrollsR172::castMinorSmite);
        def("void_grip", "Garra do Vazio", SpellSchool.ELDRITCH, Rarity.UNCOMMON,
            14, 60, 6F, 14F, "Puxa o alvo + dano + Confusão.",
            CombatScrollsR172::castVoidGrip);
    }

    private static boolean castArcBolt(CastContext ctx) {
        LivingEntity t = nearest(ctx);
        if (t == null) return false;
        t.hurt(ds(ctx), ctx.def.damage);
        hit(ctx, t, ParticleTypes.WITCH);
        ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    private static boolean castCinderBurst(CastContext ctx) {
        Vec3 c = ctx.pickHit(ctx.def.range);
        DamageSource d = SchoolDamageSource.fire(60).toVanilla(ctx.level, ctx.caster);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(d, ctx.def.damage);
            le.setSecondsOnFire(4);
        }
        ctx.level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.5, c.z, 30, ctx.def.range * 0.4, 0.4, ctx.def.range * 0.4, 0.05);
        ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean castFrostShard(CastContext ctx) {
        LivingEntity t = nearest(ctx);
        if (t == null) return false;
        t.hurt(SchoolDamageSource.ice(60).toVanilla(ctx.level, ctx.caster), ctx.def.damage);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        t.setTicksFrozen(t.getTicksFrozen() + 60);
        hit(ctx, t, ParticleTypes.SNOWFLAKE);
        ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    private static boolean castThunderClap(CastContext ctx) {
        Vec3 cp = ctx.caster.position();
        DamageSource d = ds(ctx);
        for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class, ctx.caster.getBoundingBox().inflate(ctx.def.range))) {
            if (le == ctx.caster) continue;
            le.hurt(d, ctx.def.damage);
            Vec3 kb = le.position().subtract(cp).normalize().scale(0.9);
            le.setDeltaMovement(kb.x, 0.4, kb.z);
            le.hurtMarked = true;
        }
        ctx.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cp.x, cp.y + 1, cp.z, 30, ctx.def.range * 0.4, 0.4, ctx.def.range * 0.4, 0.1);
        ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.3F);
        return true;
    }

    private static boolean castVenomSpit(CastContext ctx) {
        LivingEntity t = nearest(ctx);
        if (t == null) return false;
        t.hurt(ds(ctx), ctx.def.damage);
        t.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
        hit(ctx, t, ParticleTypes.SNEEZE);
        ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.9F, 1.3F);
        return true;
    }

    private static boolean castBloodLash(CastContext ctx) {
        LivingEntity t = nearest(ctx);
        if (t == null) return false;
        t.hurt(ds(ctx), ctx.def.damage);
        ctx.caster.heal(ctx.def.damage * 0.5F);
        hit(ctx, t, ParticleTypes.DAMAGE_INDICATOR);
        ctx.level.sendParticles(ParticleTypes.HEART, ctx.caster.getX(), ctx.caster.getY() + 1, ctx.caster.getZ(), 3, 0.3, 0.3, 0.3, 0.01);
        ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.7F, 0.7F);
        return true;
    }

    private static boolean castMinorSmite(CastContext ctx) {
        LivingEntity t = nearest(ctx);
        if (t == null) return false;
        float dmg = t.getMobType() == MobType.UNDEAD ? ctx.def.damage * 1.6F : ctx.def.damage;
        t.hurt(SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(ctx.level, ctx.caster), dmg);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        hit(ctx, t, ParticleTypes.END_ROD);
        ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    private static boolean castVoidGrip(CastContext ctx) {
        LivingEntity t = nearest(ctx);
        if (t == null) return false;
        t.hurt(ds(ctx), ctx.def.damage);
        t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
        Vec3 pull = ctx.caster.position().subtract(t.position()).normalize().scale(1.1);
        t.setDeltaMovement(pull.x, 0.2, pull.z);
        t.hurtMarked = true;
        hit(ctx, t, ParticleTypes.PORTAL);
        ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.7F);
        return true;
    }
}
