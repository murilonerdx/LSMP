package br.com.murilo.liberthia.magic.custom;

import br.com.murilo.liberthia.magic.PlayerSpellKnowledge;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * v0.1.22 r42: Executor de custom spells. Cada shape tem implementação
 * própria + VFX usando o sprite escolhido.
 *
 * <h2>Sprite particles</h2>
 * Pra cada shape, o sprite escolhido é spawn ao longo do caminho:
 * <ul>
 *   <li>PROJECTILE: 30 particles ao longo do raio até o impacto</li>
 *   <li>BEAM: 60 particles ao longo do beam (dura 3s)</li>
 *   <li>LASER: 80 particles intensas instantâneas</li>
 *   <li>AOE: 40 particles em ring expandindo</li>
 *   <li>SELF: 20 particles em volta do caster</li>
 *   <li>TOUCH: 10 particles do caster ao alvo</li>
 * </ul>
 */
public final class CustomSpellExecutor {

    private CustomSpellExecutor() {}

    /** Casta o spell. Verifica mana + cooldown. Retorna true se executou. */
    public static boolean cast(ServerPlayer caster, CustomSpell spell) {
        // Cooldown check (usa item Grimoire como key compartilhada se tem)
        var grimoire = br.com.murilo.liberthia.registry.ModItems.GRIMOIRE.get();
        if (caster.getCooldowns().isOnCooldown(grimoire)) {
            caster.displayClientMessage(Component.literal("§7Grimoire em cooldown..."), true);
            return false;
        }

        // Mana check
        int cost = spell.manaCost();
        if (!PlayerSpellKnowledge.consumeMana(caster, cost)) {
            caster.displayClientMessage(Component.literal(
                    "§c⚠ Mana insuficiente §7(" + PlayerSpellKnowledge.getMana(caster)
                            + "/" + cost + ")"), true);
            return false;
        }

        // Execute
        try {
            ServerLevel level = caster.serverLevel();
            switch (spell.shape) {
                case PROJECTILE -> castProjectile(caster, level, spell);
                case BEAM -> castBeam(caster, level, spell);
                case LASER -> castLaser(caster, level, spell);
                case AOE -> castAOE(caster, level, spell);
                case SELF -> castSelf(caster, level, spell);
                case TOUCH -> castTouch(caster, level, spell);
            }
            caster.getCooldowns().addCooldown(grimoire, spell.cooldownTicks());
            // Cast announce na actionbar
            caster.displayClientMessage(Component.literal("§5✦ §r").append(
                    Component.literal(spell.name).withStyle(s -> s.withColor(spell.effectiveColor()))), true);
            return true;
        } catch (Throwable t) {
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                    "[CustomSpell] cast error for {}: {}", spell.name, t.toString());
            return false;
        }
    }

    /** Spawn particles do sprite escolhido. */
    private static void spawnSprite(ServerLevel level, CustomSpell spell,
                                     double x, double y, double z, int count) {
        SimpleParticleType type = particleTypeFor(spell.sprite);
        level.sendParticles(type, x, y, z, count, 0.05, 0.05, 0.05, 0);
    }

    private static SimpleParticleType particleTypeFor(SpellSprite sprite) {
        return switch (sprite) {
            case FIRE_BLAST -> ModParticles.SPELL_FIRE_BLAST.get();
            case ICE_LANCE -> ModParticles.SPELL_ICE_LANCE.get();
            case VOID_ORB -> ModParticles.SPELL_VOID_ORB.get();
            case LIGHT_RAY -> ModParticles.SPELL_LIGHT_RAY.get();
            case BLOOD_SHOT -> ModParticles.SPELL_BLOOD_SHOT.get();
            case ARCANE_MISSILE -> ModParticles.SPELL_ARCANE_MISSILE.get();
            case EARTH_SPIKE -> ModParticles.SPELL_EARTH_SPIKE.get();
            case LIGHTNING_BOLT -> ModParticles.SPELL_LIGHTNING_BOLT.get();
            case SHADOW_DART -> ModParticles.SPELL_SHADOW_DART.get();
            case NATURE_THORN -> ModParticles.SPELL_NATURE_THORN.get();
        };
    }

    // ────────── Shape impls ──────────

    private static void castProjectile(ServerPlayer sp, ServerLevel level, CustomSpell spell) {
        // r54: sai da MÃO, não dos olhos (não obstrui visão)
        Vec3 start = br.com.murilo.liberthia.magic.SpellExecutor.handOrigin(sp);
        Vec3 look = sp.getLookAngle();
        Vec3 end = start.add(look.scale(spell.range()));
        BlockHitResult bhit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 stopPos = bhit.getType() == HitResult.Type.BLOCK ? bhit.getLocation() : end;

        // Trail of sprites along path — 30 particles
        double total = start.distanceTo(stopPos);
        for (double d = 0; d <= total; d += Math.max(0.2, total / 30)) {
            Vec3 p = start.add(look.scale(d));
            spawnSprite(level, spell, p.x, p.y, p.z, 1);
        }

        // Hit detection along path
        boolean hit = false;
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, stopPos).inflate(1.2))) {
            if (le == sp) continue;
            Vec3 toE = le.position().subtract(start);
            double along = toE.dot(look);
            if (along < 0 || along > spell.range()) continue;
            Vec3 closest = start.add(look.scale(along));
            if (le.position().distanceTo(closest) < 1.5) {
                applyHit(sp, le, spell);
                hit = true;
                // Impact burst at hit location
                spawnSprite(level, spell, le.getX(), le.getY() + 1, le.getZ(), 12);
                break;
            }
        }
        if (!hit) {
            // Impact at stopPos
            spawnSprite(level, spell, stopPos.x, stopPos.y, stopPos.z, 8);
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, 1.2F + spell.power * 0.1F);
    }

    private static void castBeam(ServerPlayer sp, ServerLevel level, CustomSpell spell) {
        Vec3 start = br.com.murilo.liberthia.magic.SpellExecutor.handOrigin(sp); // r54
        Vec3 look = sp.getLookAngle();

        // Beam particles along 24b path
        for (double d = 0.5; d <= spell.range(); d += 0.5) {
            Vec3 p = start.add(look.scale(d));
            spawnSprite(level, spell, p.x, p.y, p.z, 1);
        }

        // Damage all entities in cone (30°) along beam direction
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(spell.range()))) {
            if (le == sp) continue;
            Vec3 toE = le.position().subtract(sp.position()).normalize();
            if (toE.dot(look) > 0.85) { // cone ~30°
                applyHit(sp, le, spell);
            }
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 0.4F, 1.6F);
    }

    private static void castLaser(ServerPlayer sp, ServerLevel level, CustomSpell spell) {
        Vec3 start = br.com.murilo.liberthia.magic.SpellExecutor.handOrigin(sp); // r54
        Vec3 look = sp.getLookAngle();
        Vec3 end = start.add(look.scale(spell.range()));
        BlockHitResult bhit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 stopPos = bhit.getType() == HitResult.Type.BLOCK ? bhit.getLocation() : end;

        // INTENSE laser — denser particles, dust trail along beam
        double total = start.distanceTo(stopPos);
        Vector3f color = colorToVec3(spell.effectiveColor());
        DustParticleOptions dust = new DustParticleOptions(color, 1.4F);
        for (double d = 0; d <= total; d += 0.25) {
            Vec3 p = start.add(look.scale(d));
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        // Sprite particles overlay (more spaced)
        for (double d = 0; d <= total; d += 0.8) {
            Vec3 p = start.add(look.scale(d));
            spawnSprite(level, spell, p.x, p.y, p.z, 1);
        }

        // Hit-scan: damage ALL entities along the laser
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, stopPos).inflate(1.5))) {
            if (le == sp) continue;
            Vec3 toE = le.position().subtract(start);
            double along = toE.dot(look);
            if (along < 0 || along > spell.range()) continue;
            Vec3 closest = start.add(look.scale(along));
            if (le.position().distanceTo(closest) < 1.8) {
                applyHit(sp, le, spell);
            }
        }
        // Big impact at end
        spawnSprite(level, spell, stopPos.x, stopPos.y, stopPos.z, 16);
        level.playSound(null, sp.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.5F, 2.0F);
    }

    private static void castAOE(ServerPlayer sp, ServerLevel level, CustomSpell spell) {
        // Expanding ring of sprites
        double r = spell.range();
        for (int i = 0; i < 40; i++) {
            double a = (i / 40.0) * Math.PI * 2;
            spawnSprite(level, spell,
                    sp.getX() + Math.cos(a) * r,
                    sp.getY() + 0.5,
                    sp.getZ() + Math.sin(a) * r,
                    1);
        }
        // Damage in radius + knockback
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(spell.range()))) {
            if (le == sp) continue;
            applyHit(sp, le, spell);
            Vec3 away = le.position().subtract(sp.position()).normalize().scale(0.6);
            le.setDeltaMovement(away.x, 0.3, away.z);
            le.hurtMarked = true;
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.6F, 1.5F);
    }

    private static void castSelf(ServerPlayer sp, ServerLevel level, CustomSpell spell) {
        // Buff: apply element effect on caster
        sp.addEffect(new MobEffectInstance(spell.element.appliedEffect,
                200 + spell.power * 80, spell.power - 1, false, true));
        // Heal a bit too
        sp.heal(spell.damage() * 0.5F);
        // 20 sprites em volta
        for (int i = 0; i < 20; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 1 + Math.random() * 1.5;
            spawnSprite(level, spell,
                    sp.getX() + Math.cos(a) * r,
                    sp.getY() + Math.random() * 2,
                    sp.getZ() + Math.sin(a) * r, 1);
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static void castTouch(ServerPlayer sp, ServerLevel level, CustomSpell spell) {
        // First entity within 4b
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(spell.range()))) {
            if (le == sp) continue;
            applyHit(sp, le, spell);
            // Trail caster → target
            Vec3 from = sp.position().add(0, 1, 0);
            Vec3 to = le.position().add(0, 1, 0);
            for (int i = 0; i <= 10; i++) {
                double t = i / 10.0;
                Vec3 p = from.add(to.subtract(from).scale(t));
                spawnSprite(level, spell, p.x, p.y, p.z, 1);
            }
            level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.PLAYERS, 1.0F, 1.5F);
            return;
        }
        sp.displayClientMessage(Component.literal("§7Sem alvo próximo."), true);
    }

    // ────────── Apply hit ──────────

    private static void applyHit(ServerPlayer caster, LivingEntity target, CustomSpell spell) {
        target.hurt(caster.damageSources().magic(), spell.damage());
        if (!spell.element.isBuff) {
            target.addEffect(new MobEffectInstance(spell.element.appliedEffect,
                    100 + spell.power * 40, spell.power - 1, false, true));
        }
        // Element-specific extras
        switch (spell.element) {
            case FIRE -> target.setSecondsOnFire(8);
            case BLOOD -> {
                // Lifesteal 30%
                caster.heal(spell.damage() * 0.3F);
            }
            case LIGHT -> {
                if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
                    target.hurt(caster.damageSources().magic(), spell.damage() * 0.5F); // bonus
                }
            }
            case LIGHTNING -> {
                if (target.level() instanceof ServerLevel sl) {
                    var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) {
                        bolt.moveTo(target.getX(), target.getY(), target.getZ());
                        bolt.setVisualOnly(true);
                        sl.addFreshEntity(bolt);
                    }
                }
            }
            default -> {}
        }
    }

    private static Vector3f colorToVec3(int color) {
        return new Vector3f(
                ((color >> 16) & 0xFF) / 255F,
                ((color >> 8) & 0xFF) / 255F,
                (color & 0xFF) / 255F);
    }
}
