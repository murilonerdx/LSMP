package br.com.murilo.liberthia.magic.spell.composition;

import br.com.murilo.liberthia.magic.spell.CastContext;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * v0.1.151 r119: <b>CompositionEffectApplier</b> — aplica os efeitos extras
 * dos modifiers DEPOIS que o base spell rodou.
 *
 * <p>Pattern: o base spell (definido em SpellLibrary) faz o trabalho principal
 * (dano + projétil + VFX). Os modifiers são *augments* que ligam em hooks:
 * <ul>
 *   <li>{@code applyPostCast} — depois do cast principal (sustains, AoE extra)</li>
 *   <li>{@code applyOnHit} — em cada alvo atingido (ignite, freeze, lifesteal)</li>
 * </ul>
 *
 * <p>Modifiers como AMPLIFY/RANGE/MULTISHOT já alteram stats e não precisam
 * de hook (refletido via SpellComposition.finalDamage()).
 */
public final class CompositionEffectApplier {

    private CompositionEffectApplier() {}

    /**
     * Aplica modifiers que afetam o caster ou área pós-cast.
     * Chamar APÓS o {@code SpellDef.cast.execute(ctx)} retornar true.
     */
    public static void applyPostCast(CastContext ctx, SpellComposition comp) {
        if (comp == null || !comp.isComposed()) return;
        Map<SpellModifier, Integer> stacks = comp.stackCounts();

        // SUSTAIN — efeito dura mais. Já refletido no cooldown via finalCooldownTicks.
        // (sustain de buffs como Magic Shield: pré-aplica MobEffect com duração extra.)

        // AOE — aplica explosão de partículas + dano em AABB pequena (3 blocos)
        int aoeStacks = stacks.getOrDefault(SpellModifier.AOE, 0);
        if (aoeStacks > 0) {
            float radius = 4F * aoeStacks;
            Vec3 origin = ctx.caster.position().add(ctx.lookVec().scale(comp.finalRange() * 0.6F));
            AABB box = new AABB(origin.x - radius, origin.y - radius, origin.z - radius,
                                origin.x + radius, origin.y + radius, origin.z + radius);
            float dmg = comp.finalDamage() * 0.5F;
            for (LivingEntity le : ctx.level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (le == ctx.caster) continue;
                le.hurt(ctx.caster.damageSources().magic(), dmg);
            }
        }

        // ── r173: Runas de buff pós-cast no próprio caster ──
        int ward = stacks.getOrDefault(SpellModifier.WARD, 0);
        if (ward > 0) {
            ctx.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160 * ward, 1));
            ctx.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160 * ward, 1));
        }
        int hasten = stacks.getOrDefault(SpellModifier.HASTEN, 0);
        if (hasten > 0) {
            ctx.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200 * hasten, 1));
        }
    }

    /**
     * Aplica modifiers que afetam UM alvo atingido (chamado pelo SpellProjectileEntity
     * ou pelos beam spells).
     */
    public static void applyOnHit(CastContext ctx, LivingEntity target, SpellComposition comp) {
        if (comp == null || !comp.isComposed() || target == null) return;
        Map<SpellModifier, Integer> stacks = comp.stackCounts();

        // IGNITE
        int ig = stacks.getOrDefault(SpellModifier.IGNITE, 0);
        if (ig > 0) target.setSecondsOnFire(8 * ig);

        // FREEZE — root + slow máximo + Frost Walker effect
        int fz = stacks.getOrDefault(SpellModifier.FREEZE, 0);
        if (fz > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80 * fz, 9));
            target.setTicksFrozen(140 * fz);
        }

        // KNOCKBACK
        int kb = stacks.getOrDefault(SpellModifier.KNOCKBACK, 0);
        if (kb > 0) {
            // r137 fix #10: guard contra zero-vector (caster e target na mesma posicao =>
            // normalize() retorna NaN, push(NaN,NaN,NaN) crasha entity physics)
            Vec3 diff = target.position().subtract(ctx.caster.position());
            if (diff.lengthSqr() > 1e-6) {
                Vec3 dir = diff.normalize();
                target.push(dir.x * kb, 0.4 * kb, dir.z * kb);
            } else {
                // fallback: empurra na direcao do look do caster
                target.push(ctx.lookVec().x * kb, 0.4 * kb, ctx.lookVec().z * kb);
            }
        }

        // LIFESTEAL
        int ls = stacks.getOrDefault(SpellModifier.LIFESTEAL, 0);
        if (ls > 0) {
            float heal = comp.finalDamage() * 0.25F * ls;
            ctx.caster.heal(heal);
        }

        // PENETRATE — aplica dano "real" que ignora armor (via DamageSource.bypassArmor já é truque do mod;
        // aqui aplicamos um damage extra que IGNORA armor via setInvulnerable false)
        int pen = stacks.getOrDefault(SpellModifier.PENETRATE, 0);
        if (pen > 0) {
            // r137 fix #13: se AOE tambem esta ativo, target ja levou o splash dano de AOE
            // alem do dano base do spell. PENETRATE sozinho aplicava +0.5x finalDamage, dobrando.
            // Reduz bonus pela metade quando combinado com AOE pra balancear (sem zerar).
            int aoeStacks = stacks.getOrDefault(SpellModifier.AOE, 0);
            float penalty = aoeStacks > 0 ? 0.25F : 0.5F;
            float bonusBypass = comp.finalDamage() * penalty;
            target.hurt(ctx.caster.damageSources().magic(), bonusBypass);
        }

        // ── r173: novas Runas (efeitos reais via MobEffects vanilla) ──
        int poison = stacks.getOrDefault(SpellModifier.POISON, 0);
        if (poison > 0) target.addEffect(new MobEffectInstance(MobEffects.POISON, 120 * poison, 1));

        int wither = stacks.getOrDefault(SpellModifier.WITHER, 0);
        if (wither > 0) target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120 * wither, 1));

        int slow = stacks.getOrDefault(SpellModifier.SLOW, 0);
        if (slow > 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160 * slow, 2));

        int weaken = stacks.getOrDefault(SpellModifier.WEAKEN, 0);
        if (weaken > 0) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160 * weaken, 1));

        int blind = stacks.getOrDefault(SpellModifier.BLIND, 0);
        if (blind > 0) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));

        int levitate = stacks.getOrDefault(SpellModifier.LEVITATE, 0);
        if (levitate > 0) target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80 * levitate, 1));

        int gravity = stacks.getOrDefault(SpellModifier.GRAVITY, 0);
        if (gravity > 0) {
            Vec3 diff = ctx.caster.position().subtract(target.position());
            if (diff.lengthSqr() > 1e-6) {
                Vec3 dir = diff.normalize();
                target.push(dir.x * 0.6 * gravity, 0.15, dir.z * 0.6 * gravity);
            }
        }

        int smite = stacks.getOrDefault(SpellModifier.SMITE, 0);
        if (smite > 0 && target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
            target.hurt(ctx.caster.damageSources().magic(), comp.finalDamage() * 0.6F * smite);
        }
    }
}
