package br.com.murilo.liberthia.magic;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * v0.1.22 r36: <b>Spell Executor</b> — recebe um {@link Spell} + caster, executa
 * o efeito server-side. Switch por tipo. Inclui VFX próprio por feitiço (cor
 * baseada em {@code Spell.color}).
 *
 * <h2>Tipos suportados</h2>
 * Todos os {@link SpellType} têm implementação aqui — não vanilla particle
 * spam, mas efeitos COREOGRAFADOS por feitiço.
 */
public final class SpellExecutor {

    private SpellExecutor() {}

    /**
     * r58 FIX: calcula posição da MÃO do player (visivelmente offset dos olhos)
     * pra spawn de spells. Spell sai da mão visível, NÃO obstrui visão.
     *
     * <p>Cálculo aprimorado: side offset maior (0.7) + down offset maior (-0.7)
     * pra deixar visivelmente "fora da tela" do player.
     */
    public static Vec3 handOrigin(ServerPlayer sp) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        // Vetor RIGHT relativo ao olhar (cross product look x up)
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).normalize();
        // r58: side maior pra ficar VISIVELMENTE no lado
        double side = sp.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.7 : -0.7;
        // r58: down maior pra abaixar até a mão (não o peito)
        double down = -0.7;
        // Avança um pouco pra ficar à frente do corpo
        double forward = 0.5;
        return eye.add(right.scale(side))
                 .add(0, down, 0)
                 .add(look.scale(forward));
    }

    /** Tenta executar — checa mana, cooldown, depois roteia por type. */
    public static boolean cast(ServerPlayer caster, Spell spell) {
        // Cooldown check via item cooldown (usa item Grimoire como key)
        var grimoire = br.com.murilo.liberthia.registry.ModItems.GRIMOIRE.get();
        if (caster.getCooldowns().isOnCooldown(grimoire)) {
            caster.displayClientMessage(Component.literal("§7Grimoire em cooldown..."), true);
            return false;
        }
        // Mana check
        if (!PlayerSpellKnowledge.consumeMana(caster, spell.manaCost)) {
            caster.displayClientMessage(Component.literal(
                    "§c⚠ Mana insuficiente §7(" + PlayerSpellKnowledge.getMana(caster)
                            + "/" + spell.manaCost + ")"), true);
            return false;
        }
        // Execute
        try {
            doCast(caster, spell);
            caster.getCooldowns().addCooldown(grimoire, spell.cooldownTicks);
            return true;
        } catch (Throwable t) {
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                    "[Grimoire] cast error for {}: {}", spell.id, t.toString());
            return false;
        }
    }

    private static void doCast(ServerPlayer sp, Spell spell) {
        ServerLevel level = (ServerLevel) sp.level();
        // VFX universal antes do cast — burst no caster
        Vector3f c = colorToVec3(spell.color);
        DustParticleOptions dust = new DustParticleOptions(c, 1.5F);
        level.sendParticles(dust, sp.getX(), sp.getY() + 1.5, sp.getZ(),
                20, 0.4, 0.4, 0.4, 0);

        switch (spell.type) {
            case PROJECTILE -> castProjectile(sp, level, spell, c);
            case BEAM -> castBeam(sp, level, spell, c);
            case AOE -> castAOE(sp, level, spell, c);
            case SELF_BUFF -> castSelfBuff(sp, level, spell);
            case HEAL -> castHeal(sp, level, spell);
            case TELEPORT -> castTeleport(sp, level, spell);
            case SUMMON -> castSummon(sp, level, spell);
            case DRAIN -> castDrain(sp, level, spell, c);
            case CURSE -> castCurse(sp, level, spell);
        }
    }

    private static void castProjectile(ServerPlayer sp, ServerLevel level,
                                        Spell spell, Vector3f c) {
        // r54: spell sai da MÃO, não dos olhos (não obstrui visão)
        Vec3 start = handOrigin(sp);
        Vec3 look = sp.getLookAngle();
        Vec3 end = start.add(look.scale(spell.range));
        BlockHitResult bhit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 stopPos = bhit.getType() == HitResult.Type.BLOCK ? bhit.getLocation() : end;

        // VFX trail
        DustParticleOptions dust = new DustParticleOptions(c, 1.2F);
        for (double d = 0; d <= start.distanceTo(stopPos); d += 0.4) {
            Vec3 p = start.add(look.scale(d));
            level.sendParticles(dust, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
        }
        // Dano em entidades ao longo do raio
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, stopPos).inflate(1.2))) {
            if (le == sp) continue;
            Vec3 toE = le.position().subtract(start);
            double along = toE.dot(look);
            if (along < 0 || along > spell.range) continue;
            Vec3 closest = start.add(look.scale(along));
            if (le.position().distanceTo(closest) < 1.5) {
                le.hurt(sp.damageSources().magic(), spell.damage);
            }
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    private static void castBeam(ServerPlayer sp, ServerLevel level,
                                  Spell spell, Vector3f c) {
        // Beam = projectile mas com lifetime — duração de 40 ticks com damage tick a cada 5
        // (simplificação: aplica AoE em cone na direção do olhar)
        Vec3 look = sp.getLookAngle();
        Vec3 origin = handOrigin(sp); // r54: sai da mão
        DustParticleOptions dust = new DustParticleOptions(c, 0.8F);
        for (double d = 0.5; d <= spell.range; d += 0.5) {
            Vec3 p = origin.add(look.scale(d));
            level.sendParticles(dust, p.x, p.y, p.z, 3, 0.15, 0.15, 0.15, 0);
        }
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(spell.range))) {
            if (le == sp) continue;
            Vec3 toE = le.position().subtract(sp.position()).normalize();
            if (toE.dot(look) > 0.85) { // cone 30°
                le.hurt(sp.damageSources().magic(), spell.damage);
                le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 0.5F, 1.5F);
    }

    private static void castAOE(ServerPlayer sp, ServerLevel level, Spell spell, Vector3f c) {
        // r41: cosmic_collapse é uma AOE ESPECIAL — usa scheduler de 6 phases cinematográficas
        if ("cosmic_collapse".equals(spell.id)) {
            // Bloqueia se já tem ritual ativo
            if (br.com.murilo.liberthia.cosmic.collapse.CosmicCollapseScheduler.isActive(sp)) {
                sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§c⚠ Você já está canalizando o Colapso."), true);
                return;
            }
            // Centro = onde o player está olhando (até 16 blocos), ou abaixo dele
            net.minecraft.world.phys.HitResult hit = sp.pick(spell.range, 0, false);
            Vec3 center = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                    ? hit.getLocation()
                    : sp.position().add(sp.getLookAngle().scale(8));
            br.com.murilo.liberthia.cosmic.collapse.CosmicCollapseScheduler.startRitual(sp, center);
            return;
        }
        DustParticleOptions dust = new DustParticleOptions(c, 1.5F);
        // Ring expanding
        for (int ring = 0; ring < 30; ring++) {
            double a = (ring / 30.0) * Math.PI * 2;
            level.sendParticles(dust,
                    sp.getX() + Math.cos(a) * spell.range,
                    sp.getY() + 0.3,
                    sp.getZ() + Math.sin(a) * spell.range,
                    2, 0.2, 0.2, 0.2, 0);
        }
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(spell.range))) {
            if (le == sp) continue;
            le.hurt(sp.damageSources().magic(), spell.damage);
            // Knockback
            Vec3 away = le.position().subtract(sp.position()).normalize().scale(0.8);
            le.setDeltaMovement(away.x, 0.4, away.z);
            le.hurtMarked = true;
        }
        if ("lbrp".equals(spell.id)) {
            // Banishing especial: TP 40b longe
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(spell.range))) {
                if (le == sp || le instanceof Player) continue;
                Vec3 away = le.position().subtract(sp.position()).normalize();
                le.teleportTo(le.getX() + away.x * 40, le.getY(), le.getZ() + away.z * 40);
            }
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.8F, 1.5F);
    }

    private static void castSelfBuff(ServerPlayer sp, ServerLevel level, Spell spell) {
        // Buffs baseados em id
        switch (spell.id) {
            case "bael_invisibility" -> {
                sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
            }
            case "metatron_shield" -> {
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 4));
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
                sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 2));
            }
            default -> sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        }
        level.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1, sp.getZ(),
                40, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, sp.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    private static void castHeal(ServerPlayer sp, ServerLevel level, Spell spell) {
        sp.heal(spell.damage); // damage field = heal amount em HEAL
        sp.removeAllEffects(); // remove debuffs
        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        level.sendParticles(ParticleTypes.HEART, sp.getX(), sp.getY() + 1, sp.getZ(),
                15, 0.3, 0.5, 0.3, 0);
        level.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.5F, 1.8F);
    }

    private static void castTeleport(ServerPlayer sp, ServerLevel level, Spell spell) {
        Vec3 look = sp.getLookAngle();
        Vec3 hand = handOrigin(sp); // r54: trail sai da mão
        Vec3 end = hand.add(look.scale(spell.range));
        BlockHitResult bhit = level.clip(new ClipContext(
                hand, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, sp));
        Vec3 dest = bhit.getType() == HitResult.Type.BLOCK
                ? bhit.getLocation().subtract(look.scale(0.5))
                : end;
        // Particle trail
        for (double d = 0; d <= hand.distanceTo(dest); d += 0.5) {
            Vec3 p = hand.add(look.scale(d));
            level.sendParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0);
        }
        sp.teleportTo(dest.x, dest.y, dest.z);
        level.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void castSummon(ServerPlayer sp, ServerLevel level, Spell spell) {
        if ("raise_dead".equals(spell.id)) {
            var z = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
            if (z != null) {
                Vec3 spawnPos = sp.position().add(sp.getLookAngle().scale(2));
                z.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, sp.getYRot(), 0);
                z.setCustomName(Component.literal("§5Undead Servant"));
                z.setCustomNameVisible(true);
                z.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1));
                z.setPersistenceRequired();
                level.addFreshEntity(z);
            }
        }
        level.sendParticles(ParticleTypes.SOUL,
                sp.getX(), sp.getY() + 1, sp.getZ(), 30, 0.5, 0.5, 0.5, 0.05);
    }

    private static void castDrain(ServerPlayer sp, ServerLevel level,
                                   Spell spell, Vector3f c) {
        DustParticleOptions dust = new DustParticleOptions(c, 1.0F);
        int drained = 0;
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(spell.range))) {
            if (le == sp) continue;
            le.hurt(sp.damageSources().magic(), spell.damage);
            // Particle trail entity→caster
            Vec3 dir = sp.position().subtract(le.position()).normalize();
            for (int k = 0; k < 5; k++) {
                Vec3 trail = le.position().add(dir.scale(k * 0.5));
                level.sendParticles(dust, trail.x, trail.y + 1, trail.z, 1, 0, 0, 0, 0);
            }
            drained++;
        }
        sp.heal(drained * spell.damage * 0.5F);
    }

    private static void castCurse(ServerPlayer sp, ServerLevel level, Spell spell) {
        // r179 FIX: Entity.pick() só detecta blocos — usar raycast de entidade real.
        LivingEntity le = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, spell.range);
        if (le == null) {
            sp.displayClientMessage(Component.literal("§7Mire em uma entidade."), true);
            return;
        }
        switch (spell.id) {
            case "foliot_grasp" -> {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            }
            case "witness_curse" -> {
                // Adiciona o "Whispering Veil" pendant virtual via NBT
                le.getPersistentData().putBoolean("liberthia.witness_cursed", true);
                le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 0));
            }
            default -> le.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
        }
        level.sendParticles(ParticleTypes.SQUID_INK,
                le.getX(), le.getY() + 1, le.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
    }

    private static Vector3f colorToVec3(int color) {
        return new Vector3f(
                ((color >> 16) & 0xFF) / 255F,
                ((color >> 8) & 0xFF) / 255F,
                (color & 0xFF) / 255F);
    }
}
