package br.com.murilo.liberthia.loom.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.22 r33: Os 3 efeitos do retorno da dimensão Loom.
 *
 * <ul>
 *   <li><b>Obsession</b>: glitch overlay (client-side), náusea ocasional</li>
 *   <li><b>Madness</b>: TODOS mobs ao redor ficam estáticos olhando, depois
 *       atacam em massa e cegam o jogador</li>
 *   <li><b>Dimensional Infection</b>: TPs aleatórios + spawn de Dimensional Worms</li>
 * </ul>
 */
public final class LoomEffects {

    private LoomEffects() {}

    /** <b>Obsession (Obsessão / Paranoia)</b> — efeito de glitch + nausea ocasional. */
    public static class ObsessionEffect extends MobEffect {
        public ObsessionEffect() {
            super(MobEffectCategory.HARMFUL, 0x6E2DA0);
        }
        @Override
        public void applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity instanceof Player p)) return;
            if (entity.level().isClientSide) return;
            if (entity.tickCount % 100 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, true, false));
            }
            // particles roxas constantes
            if (entity.level() instanceof ServerLevel sl && entity.tickCount % 5 == 0) {
                sl.sendParticles(ParticleTypes.PORTAL,
                        entity.getX(), entity.getY() + 1.6, entity.getZ(),
                        2, 0.3, 0.2, 0.3, 0.05);
            }
        }
        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
    }

    /** <b>Madness (Loucura)</b> — todos mobs próximos ficam ESTÁTICOS olhando + atacam em ondas. */
    public static class MadnessEffect extends MobEffect {
        public MadnessEffect() {
            super(MobEffectCategory.HARMFUL, 0xFF0033);
        }
        @Override
        public void applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity instanceof Player p)) return;
            if (entity.level().isClientSide) return;
            ServerLevel sl = (ServerLevel) entity.level();

            // A cada 1s, todos mobs num raio 16b: olham pro player + ficam parados 1s
            if (entity.tickCount % 20 == 0) {
                AABB box = entity.getBoundingBox().inflate(16);
                for (Mob m : sl.getEntitiesOfClass(Mob.class, box)) {
                    m.getLookControl().setLookAt(p);
                    m.setDeltaMovement(0, m.getDeltaMovement().y, 0);
                    if (m instanceof Monster) {
                        // Força target o player
                        ((Monster) m).setTarget(p);
                    }
                }
            }
            // A cada 5s, ondas de ataque: mobs hostis tentam avançar + ataque agudo + blinda
            if (entity.tickCount % 100 == 0) {
                AABB box = entity.getBoundingBox().inflate(24);
                for (Monster m : sl.getEntitiesOfClass(Monster.class, box)) {
                    Vec3 toPlayer = p.position().subtract(m.position()).normalize();
                    m.setDeltaMovement(toPlayer.x * 0.5, m.getDeltaMovement().y, toPlayer.z * 0.5);
                    m.hasImpulse = true;
                }
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false));
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        p.getX(), p.getY() + 1.5, p.getZ(),
                        12, 0.5, 0.5, 0.5, 0);
            }
        }
        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
    }

    /** <b>Dimensional Infection</b> — TPs aleatórios + spawna Dimensional Worms. */
    public static class DimensionalInfectionEffect extends MobEffect {
        public DimensionalInfectionEffect() {
            super(MobEffectCategory.HARMFUL, 0x999999);
        }
        @Override
        public void applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity instanceof Player p)) return;
            if (entity.level().isClientSide) return;
            ServerLevel sl = (ServerLevel) entity.level();
            // TP aleatório a cada ~40 ticks (2s)
            if (entity.tickCount % 40 == 0) {
                double dist = 8 + sl.random.nextDouble() * 16;
                double angle = sl.random.nextDouble() * Math.PI * 2;
                double x = entity.getX() + Math.cos(angle) * dist;
                double z = entity.getZ() + Math.sin(angle) * dist;
                double y = entity.getY();
                p.teleportTo(x, y, z);
                sl.sendParticles(ParticleTypes.PORTAL, x, y + 1, z,
                        20, 0.5, 1, 0.5, 0.3);
                // 30% chance de spawnar 1 Worm
                if (sl.random.nextFloat() < 0.3F) {
                    var worm = br.com.murilo.liberthia.registry.ModEntities.LOOM_WORM.get().create(sl);
                    if (worm != null) {
                        worm.moveTo(x + sl.random.nextDouble() * 2 - 1, y,
                                z + sl.random.nextDouble() * 2 - 1,
                                sl.random.nextFloat() * 360, 0);
                        sl.addFreshEntity(worm);
                    }
                }
            }
            // Particles cinzas + ash em volta do player
            if (entity.tickCount % 10 == 0) {
                sl.sendParticles(ParticleTypes.ASH,
                        entity.getX(), entity.getY() + 1, entity.getZ(),
                        4, 0.5, 0.3, 0.5, 0.02);
            }
        }
        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
    }
}
