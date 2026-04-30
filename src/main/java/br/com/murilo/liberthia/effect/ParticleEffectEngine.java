package br.com.murilo.liberthia.effect;

import br.com.murilo.liberthia.particle.engine.ConfigurableParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.Comparator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import java.util.List;

public final class ParticleEffectEngine {

    private ParticleEffectEngine() {
    }

    // ============================================================
// ROOT FROM GROUND - raízes sobem do chão e prendem entidades
// ============================================================

    public static void rootFromGroundTimed(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            int durationTicks,
            double radius,
            double rootHeight,
            boolean affectPlayers
    ) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();
        RandomSource random = caster.getRandom();

        int safeDuration = Math.max(1, durationTicks);

        playConfiguredSound(level, caster, config);

        TimedParticleEffectManager.add(new TimedParticleEffectManager.ActiveEffect() {
            private int age = 0;

            @Override
            public boolean tick() {
                if (!caster.isAlive() || caster.level() != level) {
                    return true;
                }

                Vec3 center = caster.position();

                List<LivingEntity> targets = getRootTargets(level, caster, center, radius, affectPlayers);

                for (LivingEntity target : targets) {
                    rootTarget(level, caster, config, target);

                    spawnRootParticlesAroundTarget(
                            level,
                            particleOptions,
                            target,
                            config,
                            rootHeight,
                            random,
                            age
                    );
                }

                spawnRootCircle(level, particleOptions, center, config, radius, random, age);

                age++;

                return age >= safeDuration;
            }
        });
    }

    private static List<LivingEntity> getRootTargets(
            ServerLevel level,
            LivingEntity caster,
            Vec3 center,
            double radius,
            boolean affectPlayers
    ) {
        AABB area = new AABB(
                center.x - radius,
                center.y - 1.0D,
                center.z - radius,
                center.x + radius,
                center.y + 3.0D,
                center.z + radius
        );

        return level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive()
                        && entity != caster
                        && !entity.isSpectator()
                        && (affectPlayers || !(entity instanceof Player))
        );
    }

    private static void rootTarget(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            LivingEntity target
    ) {
        Vec3 motion = target.getDeltaMovement();

        target.setDeltaMovement(
                motion.x * 0.05D,
                Math.min(0.0D, motion.y),
                motion.z * 0.05D
        );

        target.hurtMarked = true;

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 8, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8, 0, false, false));

        if (config.damage() > 0.0F && target.tickCount % 10 == 0) {
            if (caster instanceof Player player) {
                target.hurt(level.damageSources().playerAttack(player), config.damage());
            } else {
                target.hurt(level.damageSources().mobAttack(caster), config.damage());
            }
        }

        for (MobEffectInstance effect : config.effects()) {
            target.addEffect(new MobEffectInstance(effect));
        }
    }

    private static void spawnRootParticlesAroundTarget(
            ServerLevel level,
            ConfigurableParticleOptions particleOptions,
            LivingEntity target,
            ParticleEffectConfig config,
            double rootHeight,
            RandomSource random,
            int age
    ) {
        int roots = Math.max(3, config.count() / 10);

        Vec3 base = target.position();

        for (int i = 0; i < roots; i++) {
            double angle = (Math.PI * 2.0D) * (i / (double) roots) + age * 0.08D;
            double radius = 0.35D + random.nextDouble() * 0.35D;

            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            double yProgress = (age % 20) / 20.0D;
            double y = yProgress * rootHeight;

            Vec3 particlePos = base.add(x, 0.05D + y, z);

            Vec3 velocity = new Vec3(
                    x * -0.015D,
                    0.035D + random.nextDouble() * 0.025D,
                    z * -0.015D
            );

            spawnOne(level, particleOptions, particlePos, velocity);
        }
    }

    private static void spawnOne(
            ServerLevel level,
            ConfigurableParticleOptions particleOptions,
            Vec3 particlePos,
            Vec3 velocity
    ) {
        level.sendParticles(
                particleOptions,
                particlePos.x,
                particlePos.y,
                particlePos.z,
                0,
                velocity.x,
                velocity.y,
                velocity.z,
                1.0D
        );
    }

    private static Vec3 randomDirection(RandomSource random) {
        double x = random.nextDouble() * 2.0D - 1.0D;
        double y = random.nextDouble() * 2.0D - 1.0D;
        double z = random.nextDouble() * 2.0D - 1.0D;

        Vec3 vec = new Vec3(x, y, z);

        if (vec.lengthSqr() < 0.0001D) {
            return new Vec3(0.0D, 1.0D, 0.0D);
        }

        return vec.normalize();
    }

    private static void spawnRootCircle(
            ServerLevel level,
            ConfigurableParticleOptions particleOptions,
            Vec3 center,
            ParticleEffectConfig config,
            double radius,
            RandomSource random,
            int age
    ) {
        if (age % 3 != 0) {
            return;
        }

        int amount = Math.max(8, config.count() / 4);

        for (int i = 0; i < amount; i++) {
            double angle = (Math.PI * 2.0D) * (i / (double) amount);
            double distance = radius * (0.75D + random.nextDouble() * 0.25D);

            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;

            Vec3 pos = center.add(x, 0.05D, z);

            Vec3 velocity = new Vec3(
                    (random.nextDouble() - 0.5D) * 0.025D,
                    0.025D + random.nextDouble() * 0.025D,
                    (random.nextDouble() - 0.5D) * 0.025D
            );

            spawnOne(level, particleOptions, pos, velocity);
        }
    }

    // ============================================================
// ORBIT DAMAGE - órbita ao redor do jogador com hitbox móvel
// ============================================================

    public static void orbitDamageTimed(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            int durationTicks,
            double radius,
            double height,
            double rotationsPerSecond,
            int arms,
            double hitRadius,
            int hitCooldownTicks
    ) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();

        int safeDuration = Math.max(1, durationTicks);
        int safeArms = Math.max(1, arms);
        int safeCooldown = Math.max(1, hitCooldownTicks);

        Map<UUID, Integer> hitCooldowns = new HashMap<>();

        playConfiguredSound(level, caster, config);

        TimedParticleEffectManager.add(new TimedParticleEffectManager.ActiveEffect() {
            private int age = 0;

            @Override
            public boolean tick() {
                if (!caster.isAlive() || caster.level() != level) {
                    return true;
                }

                tickCooldowns(hitCooldowns);

                Vec3 base = caster.position().add(0.0D, 0.20D, 0.0D);

                double angularSpeed = rotationsPerSecond * Math.PI * 2.0D / 20.0D;
                double baseAngle = age * angularSpeed;

                int particlesPerArm = Math.max(2, config.count() / 16);

                for (int arm = 0; arm < safeArms; arm++) {
                    double armAngle = baseAngle + ((Math.PI * 2.0D) * arm / safeArms);

                    for (int i = 0; i < particlesPerArm; i++) {
                        double progress = i / (double) Math.max(1, particlesPerArm - 1);

                        double wave = Math.sin((age * 0.15D) + progress * Math.PI * 2.0D) * 0.20D;
                        double currentRadius = radius + wave;

                        double angle = armAngle + progress * 0.65D;

                        double x = Math.cos(angle) * currentRadius;
                        double z = Math.sin(angle) * currentRadius;
                        double y = 0.25D + progress * height;

                        Vec3 particlePos = base.add(x, y, z);

                        Vec3 tangent = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle))
                                .normalize()
                                .scale(config.speed());

                        Vec3 velocity = tangent.add(0.0D, config.upwardSpeed(), 0.0D);

                        spawnOne(level, particleOptions, particlePos, velocity);

                        damageEntitiesAtMovingPoint(
                                level,
                                caster,
                                config,
                                particlePos,
                                hitRadius,
                                hitCooldowns,
                                safeCooldown
                        );
                    }
                }

                age++;

                return age >= safeDuration;
            }
        });
    }

    private static void damageEntitiesAtMovingPoint(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            Vec3 point,
            double hitRadius,
            Map<UUID, Integer> hitCooldowns,
            int hitCooldownTicks
    ) {
        if (config.damage() <= 0.0F && config.effects().isEmpty()) {
            return;
        }

        AABB area = new AABB(
                point.x - hitRadius,
                point.y - hitRadius,
                point.z - hitRadius,
                point.x + hitRadius,
                point.y + hitRadius,
                point.z + hitRadius
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive()
                        && entity != caster
                        && !entity.isSpectator()
        );

        for (LivingEntity target : targets) {
            UUID id = target.getUUID();

            if (hitCooldowns.getOrDefault(id, 0) > 0) {
                continue;
            }

            Vec3 direction = target.position().subtract(point);

            if (direction.lengthSqr() < 0.0001D) {
                direction = new Vec3(0.0D, 0.0D, 1.0D);
            } else {
                direction = direction.normalize();
            }

            applyDamageAndEffects(level, caster, target, config, direction);
            hitCooldowns.put(id, hitCooldownTicks);
        }
    }

    private static void tickCooldowns(Map<UUID, Integer> cooldowns) {
        cooldowns.replaceAll((id, value) -> value - 1);
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    // ============================================================
    // 1. SLASH - efeito instantâneo em corte frontal
    // ============================================================

    public static void slash(ServerLevel level, Player caster, ParticleEffectConfig config) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 eye = caster.getEyePosition();
        Vec3 start = eye.add(look.scale(0.85D));

        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() < 0.0001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        RandomSource random = caster.getRandom();

        for (int i = 0; i < config.count(); i++) {
            double progress = i / (double) config.count();

            double forwardDistance = 0.25D + progress * config.range();
            double arc = Math.sin(progress * Math.PI);

            double sideOffset = (random.nextDouble() - 0.5D) * config.spread() * 2.0D * arc;
            double verticalOffset = (random.nextDouble() - 0.35D) * config.spread() * arc;

            Vec3 particlePos = start
                    .add(look.scale(forwardDistance))
                    .add(side.scale(sideOffset))
                    .add(0.0D, verticalOffset, 0.0D);

            double speedForward = config.speed() + random.nextDouble() * config.speed();
            double speedSide = (random.nextDouble() - 0.5D) * config.speed();
            double speedUp = config.upwardSpeed() + (random.nextDouble() - 0.5D) * config.upwardSpeed();

            Vec3 velocity = look.scale(speedForward)
                    .add(side.scale(speedSide))
                    .add(0.0D, speedUp, 0.0D);

            spawnOne(level, particleOptions, particlePos, velocity);
        }

        damageEntitiesInSlash(level, caster, config, look);
        playConfiguredSound(level, caster, config);
    }

    // ============================================================
    // 2. BURST - explosão esférica instantânea
    // ============================================================

    public static void burst(ServerLevel level, LivingEntity caster, ParticleEffectConfig config) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();
        RandomSource random = caster.getRandom();

        Vec3 center = caster.position().add(0.0D, caster.getBbHeight() * 0.55D, 0.0D);

        for (int i = 0; i < config.count(); i++) {
            Vec3 direction = randomDirection(random);
            double distance = random.nextDouble() * config.spread();

            Vec3 particlePos = center.add(direction.scale(distance));

            Vec3 velocity = direction.scale(config.speed())
                    .add(0.0D, config.upwardSpeed(), 0.0D);

            spawnOne(level, particleOptions, particlePos, velocity);
        }

        damageEntitiesInRadius(level, caster, config, center);
        playConfiguredSound(level, caster, config);
    }

    // ============================================================
    // 3. AURA - círculo instantâneo em volta do caster
    // ============================================================

    public static void aura(ServerLevel level, LivingEntity caster, ParticleEffectConfig config) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();
        RandomSource random = caster.getRandom();

        Vec3 center = caster.position().add(0.0D, 0.15D, 0.0D);

        for (int i = 0; i < config.count(); i++) {
            double angle = (Math.PI * 2.0D) * (i / (double) config.count());
            double radius = config.spread() * (0.65D + random.nextDouble() * 0.35D);

            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = random.nextDouble() * caster.getBbHeight();

            Vec3 particlePos = center.add(x, y, z);

            Vec3 velocity = new Vec3(
                    x * 0.012D,
                    config.upwardSpeed(),
                    z * 0.012D
            );

            spawnOne(level, particleOptions, particlePos, velocity);
        }

        damageEntitiesInRadius(level, caster, config, caster.position());
        playConfiguredSound(level, caster, config);
    }

    // ============================================================
    // 4. ORBIT TIMED - partículas girando em volta por N ticks
    // ============================================================

    public static void orbitTimed(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            int durationTicks,
            double radius,
            double height,
            double rotationsPerSecond,
            int arms
    ) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();

        int safeDuration = Math.max(1, durationTicks);
        int safeArms = Math.max(1, arms);

        playConfiguredSound(level, caster, config);

        TimedParticleEffectManager.add(new TimedParticleEffectManager.ActiveEffect() {
            private int age = 0;

            @Override
            public boolean tick() {
                if (!caster.isAlive() || caster.level() != level) {
                    return true;
                }

                double angularSpeed = rotationsPerSecond * Math.PI * 2.0D / 20.0D;
                double baseAngle = age * angularSpeed;

                Vec3 baseCenter = caster.position().add(0.0D, 0.20D, 0.0D);

                int particlesPerTick = Math.max(1, config.count() / 10);

                for (int arm = 0; arm < safeArms; arm++) {
                    double armOffset = (Math.PI * 2.0D) * (arm / (double) safeArms);

                    for (int i = 0; i < particlesPerTick; i++) {
                        double heightProgress = i / (double) Math.max(1, particlesPerTick - 1);
                        double y = heightProgress * height;

                        double angle = baseAngle + armOffset + heightProgress * Math.PI * 2.0D;

                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        Vec3 particlePos = baseCenter.add(x, y, z);

                        Vec3 tangent = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle))
                                .normalize()
                                .scale(config.speed());

                        Vec3 velocity = tangent.add(0.0D, config.upwardSpeed(), 0.0D);

                        spawnOne(level, particleOptions, particlePos, velocity);
                    }
                }

                if (age % 10 == 0) {
                    damageEntitiesInRadius(level, caster, config, caster.position());
                }

                age++;
                return age >= safeDuration;
            }
        });
    }

    // ============================================================
    // 5. GROUND SLAM - pancada no chão com anel, dano e quebra opcional
    // ============================================================

    public static void groundSlam(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            double radius,
            boolean breakBlocks,
            float maxHardnessToBreak
    ) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();
        RandomSource random = caster.getRandom();

        BlockPos groundPos = findGroundBelow(level, caster.position());
        Vec3 center = Vec3.atCenterOf(groundPos).add(0.0D, 0.05D, 0.0D);

        int rings = 4;
        int particlesPerRing = Math.max(8, config.count() / rings);

        for (int ring = 1; ring <= rings; ring++) {
            double ringProgress = ring / (double) rings;
            double ringRadius = radius * ringProgress;

            for (int i = 0; i < particlesPerRing; i++) {
                double angle = (Math.PI * 2.0D) * (i / (double) particlesPerRing);
                double noise = (random.nextDouble() - 0.5D) * 0.20D;

                double x = Math.cos(angle) * (ringRadius + noise);
                double z = Math.sin(angle) * (ringRadius + noise);

                Vec3 particlePos = center.add(x, 0.02D + random.nextDouble() * 0.15D, z);

                Vec3 outward = new Vec3(x, 0.0D, z);
                if (outward.lengthSqr() < 0.0001D) {
                    outward = new Vec3(0.0D, 0.0D, 1.0D);
                } else {
                    outward = outward.normalize();
                }

                Vec3 velocity = outward.scale(config.speed())
                        .add(0.0D, config.upwardSpeed() + random.nextDouble() * 0.08D, 0.0D);

                spawnOne(level, particleOptions, particlePos, velocity);
            }
        }

        damageEntitiesInRadius(level, caster, config, center);

        if (breakBlocks) {
            breakBlocksInDisk(level, groundPos.below(), radius, 1, maxHardnessToBreak, caster);
        }

        playConfiguredSound(level, caster, config);
    }

    // ============================================================
    // 6. BLOCK BREAKER WAVE - quebra blocos em onda circular
    // ============================================================

    public static void blockBreakerWave(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            double radius,
            int depth,
            float maxHardnessToBreak
    ) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();
        RandomSource random = caster.getRandom();

        BlockPos centerBlock = findGroundBelow(level, caster.position()).below();
        Vec3 center = Vec3.atCenterOf(centerBlock).add(0.0D, 1.0D, 0.0D);

        int particles = Math.max(16, config.count());

        for (int i = 0; i < particles; i++) {
            double angle = (Math.PI * 2.0D) * (i / (double) particles);
            double distance = random.nextDouble() * radius;

            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;

            Vec3 particlePos = center.add(x, random.nextDouble() * 0.35D, z);

            Vec3 direction = new Vec3(x, 0.0D, z);
            if (direction.lengthSqr() < 0.0001D) {
                direction = randomDirection(random);
            } else {
                direction = direction.normalize();
            }

            Vec3 velocity = direction.scale(config.speed())
                    .add(0.0D, config.upwardSpeed() + random.nextDouble() * 0.10D, 0.0D);

            spawnOne(level, particleOptions, particlePos, velocity);
        }

        breakBlocksInDisk(level, centerBlock, radius, Math.max(1, depth), maxHardnessToBreak, caster);
        damageEntitiesInRadius(level, caster, config, center);
        playConfiguredSound(level, caster, config);
    }

    // ============================================================
    // 7. VORTEX PULL TIMED - vórtice que puxa entidades
    // ============================================================

    public static void vortexPullTimed(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            int durationTicks,
            double radius,
            double height,
            double pullStrength
    ) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();
        int safeDuration = Math.max(1, durationTicks);

        playConfiguredSound(level, caster, config);

        TimedParticleEffectManager.add(new TimedParticleEffectManager.ActiveEffect() {
            private int age = 0;

            @Override
            public boolean tick() {
                if (!caster.isAlive() || caster.level() != level) {
                    return true;
                }

                Vec3 center = caster.position().add(0.0D, height * 0.45D, 0.0D);

                int particlesPerTick = Math.max(2, config.count() / 8);

                for (int i = 0; i < particlesPerTick; i++) {
                    double progress = i / (double) particlesPerTick;

                    double shrinkingRadius = radius * (1.0D - progress * 0.65D);
                    double angle = age * 0.35D + progress * Math.PI * 6.0D;

                    double x = Math.cos(angle) * shrinkingRadius;
                    double z = Math.sin(angle) * shrinkingRadius;
                    double y = progress * height;

                    Vec3 particlePos = caster.position().add(x, y, z);

                    Vec3 toCenter = center.subtract(particlePos);
                    if (toCenter.lengthSqr() > 0.0001D) {
                        toCenter = toCenter.normalize();
                    }

                    Vec3 swirl = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle))
                            .scale(config.speed());

                    Vec3 velocity = toCenter.scale(config.speed() * 0.65D)
                            .add(swirl)
                            .add(0.0D, config.upwardSpeed(), 0.0D);

                    spawnOne(level, particleOptions, particlePos, velocity);
                }

                pullEntitiesToward(level, caster, center, radius, pullStrength);

                if (age % 12 == 0) {
                    damageEntitiesInRadius(level, caster, config, center);
                }

                age++;
                return age >= safeDuration;
            }
        });
    }

    // ============================================================
    // 8. METEOR RAIN TIMED - chuva de partículas caindo do céu
    // ============================================================

    public static void meteorRainTimed(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            int durationTicks,
            double radius,
            double spawnHeight,
            boolean impactAtEnd
    ) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();
        RandomSource random = caster.getRandom();

        int safeDuration = Math.max(1, durationTicks);

        playConfiguredSound(level, caster, config);

        TimedParticleEffectManager.add(new TimedParticleEffectManager.ActiveEffect() {
            private int age = 0;

            @Override
            public boolean tick() {
                if (!caster.isAlive() || caster.level() != level) {
                    return true;
                }

                Vec3 center = caster.position();

                int particlesPerTick = Math.max(1, config.count() / 12);

                for (int i = 0; i < particlesPerTick; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double distance = Math.sqrt(random.nextDouble()) * radius;

                    double x = Math.cos(angle) * distance;
                    double z = Math.sin(angle) * distance;

                    Vec3 particlePos = center.add(x, spawnHeight, z);

                    Vec3 velocity = new Vec3(
                            (random.nextDouble() - 0.5D) * config.speed(),
                            -Math.abs(config.speed() * 2.5D + random.nextDouble() * config.speed()),
                            (random.nextDouble() - 0.5D) * config.speed()
                    );

                    spawnOne(level, particleOptions, particlePos, velocity);
                }

                if (age % 10 == 0) {
                    damageEntitiesInRadius(level, caster, config, center);
                }

                age++;

                if (age >= safeDuration) {
                    if (impactAtEnd) {
                        groundSlam(level, caster, config, radius * 0.65D, false, 0.0F);
                    }

                    return true;
                }

                return false;
            }
        });
    }


    private static void damageEntitiesInSlash(
            ServerLevel level,
            Player caster,
            ParticleEffectConfig config,
            Vec3 look
    ) {
        if (config.damage() <= 0.0F && config.effects().isEmpty()) {
            return;
        }

        AABB area = caster.getBoundingBox().inflate(config.range() + config.damageRadius());

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive()
                        && entity != caster
                        && !entity.isSpectator()
        );

        Vec3 origin = caster.getEyePosition();

        for (LivingEntity target : targets) {
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 toTarget = targetCenter.subtract(origin);

            double forward = toTarget.dot(look);

            if (forward < 0.0D || forward > config.range()) {
                continue;
            }

            Vec3 closestPoint = origin.add(look.scale(forward));
            double distanceToLine = targetCenter.distanceTo(closestPoint);

            if (distanceToLine > config.damageRadius()) {
                continue;
            }

            applyDamageAndEffects(level, caster, target, config, look);
        }
    }

    private static void damageEntitiesInRadius(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            Vec3 center
    ) {
        if (config.damage() <= 0.0F && config.effects().isEmpty()) {
            return;
        }

        AABB area = new AABB(
                center.x - config.damageRadius(),
                center.y - config.damageRadius(),
                center.z - config.damageRadius(),
                center.x + config.damageRadius(),
                center.y + config.damageRadius(),
                center.z + config.damageRadius()
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive()
                        && entity != caster
                        && !entity.isSpectator()
        );

        for (LivingEntity target : targets) {
            Vec3 direction = target.position().subtract(center);

            if (direction.lengthSqr() < 0.0001D) {
                direction = new Vec3(0.0D, 0.0D, 1.0D);
            } else {
                direction = direction.normalize();
            }

            applyDamageAndEffects(level, caster, target, config, direction);
        }
    }

    private static void applyDamageAndEffects(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            ParticleEffectConfig config,
            Vec3 knockbackDirection
    ) {
        if (config.damage() > 0.0F) {
            if (caster instanceof Player player) {
                target.hurt(level.damageSources().playerAttack(player), config.damage());
            } else {
                target.hurt(level.damageSources().mobAttack(caster), config.damage());
            }
        }

        for (var effect : config.effects()) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
        }

        if (config.knockback() > 0.0D) {
            Vec3 direction = knockbackDirection.normalize();
            target.push(
                    direction.x * config.knockback(),
                    0.12D,
                    direction.z * config.knockback()
            );
        }
    }

    private static void pullEntitiesToward(
            ServerLevel level,
            LivingEntity caster,
            Vec3 center,
            double radius,
            double strength
    ) {
        if (strength <= 0.0D) {
            return;
        }

        AABB area = new AABB(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive()
                        && entity != caster
                        && !entity.isSpectator()
        );

        for (LivingEntity target : targets) {
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 pull = center.subtract(targetCenter);

            if (pull.lengthSqr() < 0.0001D) {
                continue;
            }

            pull = pull.normalize().scale(strength);

            target.push(
                    pull.x,
                    Math.min(0.18D, Math.max(-0.08D, pull.y)),
                    pull.z
            );

            target.hurtMarked = true;
        }
    }

    private static void playConfiguredSound(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config
    ) {
        if (config.sound() == null) {
            return;
        }

        level.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                config.sound(),
                SoundSource.PLAYERS,
                config.soundVolume(),
                config.soundPitch()
        );
    }

    private static BlockPos findGroundBelow(ServerLevel level, Vec3 origin) {
        BlockPos start = BlockPos.containing(origin);

        for (int i = 0; i <= 12; i++) {
            BlockPos check = start.below(i);
            BlockState state = level.getBlockState(check);

            if (!state.isAir()) {
                return check.above();
            }
        }

        return start;
    }
    private static LivingEntity findHostileMeteorTarget(
            ServerLevel level,
            LivingEntity caster,
            double minSafeRadius,
            double maxRadius,
            RandomSource random
    ) {
        Vec3 center = caster.position();

        double minSq = minSafeRadius * minSafeRadius;
        double maxSq = maxRadius * maxRadius;

        AABB area = new AABB(
                center.x - maxRadius,
                center.y - 12.0D,
                center.z - maxRadius,
                center.x + maxRadius,
                center.y + 24.0D,
                center.z + maxRadius
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> {
                    if (!entity.isAlive()) {
                        return false;
                    }

                    if (entity == caster) {
                        return false;
                    }

                    if (entity.isSpectator()) {
                        return false;
                    }

                    if (!isHostileForMeteor(entity)) {
                        return false;
                    }

                    double distSq = horizontalDistanceSqr(entity.position(), center);

                    return distSq >= minSq && distSq <= maxSq;
                }
        );

        if (targets.isEmpty()) {
            return null;
        }

        targets.sort(Comparator.comparingDouble(entity -> horizontalDistanceSqr(entity.position(), center)));

        int poolSize = Math.min(4, targets.size());

        return targets.get(random.nextInt(poolSize));
    }

    private static boolean isHostileForMeteor(LivingEntity entity) {
        if (entity instanceof Enemy) {
            return true;
        }

        if (entity instanceof Mob mob && mob.getTarget() != null) {
            return true;
        }

        return false;
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;

        return dx * dx + dz * dz;
    }

    private static Vec3 chooseMeteorImpactPointForTarget(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            double minSafeRadius,
            RandomSource random
    ) {
        Vec3 casterCenter = caster.position();

        /*
         * Pequeno offset para não cair exatamente no centro do mob toda vez.
         */
        double offsetX = (random.nextDouble() - 0.5D) * 1.2D;
        double offsetZ = (random.nextDouble() - 0.5D) * 1.2D;

        Vec3 candidate = target.position().add(offsetX, 0.0D, offsetZ);

        /*
         * Segurança: se o mob estiver perto demais do caster,
         * empurra o ponto de impacto para fora da zona segura.
         */
        if (horizontalDistanceSqr(candidate, casterCenter) < minSafeRadius * minSafeRadius) {
            Vec3 away = new Vec3(
                    candidate.x - casterCenter.x,
                    0.0D,
                    candidate.z - casterCenter.z
            );

            if (away.lengthSqr() < 0.0001D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                away = away.normalize();
            }

            candidate = casterCenter.add(away.scale(minSafeRadius + 1.0D));
        }

        BlockPos surface = findSurfaceNear(
                level,
                candidate.x,
                target.getY() + 8.0D,
                candidate.z
        );

        return Vec3.atCenterOf(surface).add(0.0D, 0.05D, 0.0D);
    }

    // ============================================================
// METEOR SHOWER - meteoros caem do céu, explodem e deixam fogo
// ============================================================

    public static void meteorShowerTimed(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            int durationTicks,
            int meteorCount,
            double minSafeRadius,
            double radius,
            double spawnHeight,
            float explosionPower,
            int fireRadius,
            int fireDurationVisualTicks
    ) {
        ConfigurableParticleOptions particleOptions = config.toParticleOptions();
        RandomSource random = caster.getRandom();

        int safeDuration = Math.max(1, durationTicks);
        int safeMeteorCount = Math.max(1, meteorCount);
        int spawnInterval = Math.max(1, safeDuration / safeMeteorCount);

        double safeMinRadius = Math.max(4.0D, minSafeRadius);
        double safeMaxRadius = Math.max(safeMinRadius + 2.0D, radius);

        playConfiguredSound(level, caster, config);

        TimedParticleEffectManager.add(new TimedParticleEffectManager.ActiveEffect() {
            private int age = 0;
            private int spawned = 0;
            private final ArrayList<ActiveMeteor> meteors = new ArrayList<>();

            @Override
            public boolean tick() {
                if (!caster.isAlive() || caster.level() != level) {
                    return true;
                }

                Vec3 casterCenter = caster.position();

                if (age % spawnInterval == 0 && spawned < safeMeteorCount) {
                    spawned++;

                    LivingEntity target = findHostileMeteorTarget(
                            level,
                            caster,
                            safeMinRadius,
                            safeMaxRadius,
                            random
                    );

                    Vec3 impactTarget;

                    if (target != null) {
                        impactTarget = target.position()
                                .add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                    } else {
                        impactTarget = chooseSafeMeteorImpactPoint(
                                level,
                                casterCenter,
                                safeMinRadius,
                                safeMaxRadius,
                                random
                        );
                    }

                    /*
                     * O meteoro nasce acima e um pouco para o lado.
                     * Se tiver alvo, ele nasce acima do alvo, não acima do player.
                     */
                    Vec3 start = impactTarget.add(
                            (random.nextDouble() - 0.5D) * 5.0D,
                            spawnHeight,
                            (random.nextDouble() - 0.5D) * 5.0D
                    );

                    Vec3 toTarget = impactTarget.subtract(start);

                    int travelTicks = 22 + random.nextInt(8);
                    Vec3 velocity = toTarget.scale(1.0D / travelTicks);

                    meteors.add(new ActiveMeteor(
                            start,
                            velocity,
                            travelTicks + 30,
                            target,
                            impactTarget
                    ));
                }

                for (int i = meteors.size() - 1; i >= 0; i--) {
                    ActiveMeteor meteor = meteors.get(i);

                    meteor.age++;

                    Vec3 aimPoint = meteor.impactTarget;
                    boolean hasLivingTarget = meteor.target != null && meteor.target.isAlive();

                    if (hasLivingTarget) {
                        double targetDistanceFromCaster = horizontalDistanceSqr(
                                meteor.target.position(),
                                casterCenter
                        );

                        /*
                         * Se o monstro chegar perto demais do evocador, o meteoro para de perseguir
                         * para evitar explosão em cima do player.
                         */
                        if (targetDistanceFromCaster >= safeMinRadius * safeMinRadius) {
                            aimPoint = meteor.target.position()
                                    .add(0.0D, meteor.target.getBbHeight() * 0.55D, 0.0D);
                        } else {
                            meteor.target = null;
                            hasLivingTarget = false;
                        }
                    }

                    Vec3 desired = aimPoint.subtract(meteor.position);

                    if (desired.lengthSqr() > 0.0001D) {
                        /*
                         * Velocidade mínima para não ficar lento demais.
                         */
                        double currentSpeed = Math.max(0.45D, meteor.velocity.length());

                        Vec3 desiredVelocity = desired.normalize().scale(currentSpeed);

                        /*
                         * Homing forte.
                         * 0.28 = segue bem.
                         * 0.40 = segue agressivo.
                         */
                        double homingStrength = hasLivingTarget ? 0.35D : 0.18D;

                        meteor.velocity = meteor.velocity.scale(1.0D - homingStrength)
                                .add(desiredVelocity.scale(homingStrength));
                    }

                    /*
                     * Gravidade bem fraca. Se for forte, ele para de seguir e só cai reto.
                     */
                    meteor.velocity = meteor.velocity.add(0.0D, -0.002D, 0.0D);

                    meteor.position = meteor.position.add(meteor.velocity);

                    spawnMeteorTrail(
                            level,
                            particleOptions,
                            meteor.position,
                            meteor.velocity,
                            random,
                            config
                    );

                    boolean tooCloseToCaster =
                            horizontalDistanceSqr(meteor.position, casterCenter)
                                    < safeMinRadius * safeMinRadius * 0.75D;

                    if (tooCloseToCaster && meteor.position.y <= casterCenter.y + 2.5D) {
                        meteors.remove(i);
                        continue;
                    }

                    boolean hitTarget = false;
                    Vec3 impactPosition = meteor.position;

                    if (meteor.target != null && meteor.target.isAlive()) {
                        Vec3 targetCenter = meteor.target.position()
                                .add(0.0D, meteor.target.getBbHeight() * 0.5D, 0.0D);

                        /*
                         * Hitbox do meteoro contra o monstro.
                         * Aumente para 2.0D se quiser acertar mais fácil.
                         */
                        if (meteor.position.distanceToSqr(targetCenter) <= 1.75D * 1.75D) {
                            hitTarget = true;
                            impactPosition = targetCenter;
                        }
                    }

                    boolean hitGround = hasMeteorImpacted(level, meteor.position);
                    boolean expired = meteor.age >= meteor.maxAge;

                    if (hitTarget || hitGround || expired) {
                        if (horizontalDistanceSqr(impactPosition, casterCenter) >= safeMinRadius * safeMinRadius) {
                            impactMeteor(
                                    level,
                                    caster,
                                    config,
                                    particleOptions,
                                    impactPosition,
                                    explosionPower,
                                    fireRadius,
                                    fireDurationVisualTicks,
                                    random
                            );
                        }

                        meteors.remove(i);
                    }
                }

                age++;

                return age >= safeDuration && spawned >= safeMeteorCount && meteors.isEmpty();
            }
        });
    }

    private static Vec3 chooseSafeMeteorImpactPoint(
            ServerLevel level,
            Vec3 casterCenter,
            double minSafeRadius,
            double maxRadius,
            RandomSource random
    ) {
        double safeMin = Math.max(5.0D, minSafeRadius);
        double safeMax = Math.max(safeMin + 2.0D, maxRadius);

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = safeMin + random.nextDouble() * (safeMax - safeMin);

            double x = casterCenter.x + Math.cos(angle) * distance;
            double z = casterCenter.z + Math.sin(angle) * distance;

            BlockPos surface = findSurfaceNear(level, x, casterCenter.y + 8.0D, z);

            Vec3 impact = Vec3.atCenterOf(surface).add(0.0D, 0.05D, 0.0D);

            double horizontalDistanceSq = new Vec3(impact.x, 0.0D, impact.z)
                    .distanceToSqr(new Vec3(casterCenter.x, 0.0D, casterCenter.z));

            if (horizontalDistanceSq >= safeMin * safeMin) {
                return impact;
            }
        }

        /*
         * Fallback: força cair diretamente atrás/longe do caster.
         */
        return casterCenter.add(safeMin + 2.0D, 0.0D, 0.0D);
    }

    private static BlockPos findSurfaceNear(
            ServerLevel level,
            double x,
            double startY,
            double z
    ) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        int topY = Math.min(level.getMaxBuildHeight() - 2, (int) Math.floor(startY) + 32);
        int bottomY = level.getMinBuildHeight() + 1;

        for (int y = topY; y >= bottomY; y--) {
            BlockPos ground = new BlockPos(blockX, y, blockZ);
            BlockPos airAbove = ground.above();

            BlockState groundState = level.getBlockState(ground);
            BlockState aboveState = level.getBlockState(airAbove);

            if (!groundState.isAir() && aboveState.isAir()) {
                return airAbove;
            }
        }

        return new BlockPos(blockX, (int) Math.floor(startY), blockZ);
    }

    private static void spawnMeteorTrail(
            ServerLevel level,
            ConfigurableParticleOptions particleOptions,
            Vec3 position,
            Vec3 velocity,
            RandomSource random,
            ParticleEffectConfig config
    ) {
        int trailAmount = 4;

        Vec3 backward = velocity.normalize().scale(-0.25D);

        for (int i = 0; i < trailAmount; i++) {
            Vec3 offset = new Vec3(
                    (random.nextDouble() - 0.5D) * 0.35D,
                    (random.nextDouble() - 0.5D) * 0.35D,
                    (random.nextDouble() - 0.5D) * 0.35D
            );

            Vec3 particlePos = position.add(backward.scale(i)).add(offset);

            Vec3 particleVelocity = backward.scale(0.08D)
                    .add(
                            (random.nextDouble() - 0.5D) * 0.04D,
                            random.nextDouble() * 0.05D,
                            (random.nextDouble() - 0.5D) * 0.04D
                    );

            spawnOne(level, particleOptions, particlePos, particleVelocity);
        }
    }

    private static boolean hasMeteorImpacted(ServerLevel level, Vec3 position) {
        if (position.y <= level.getMinBuildHeight() + 1) {
            return true;
        }

        BlockPos pos = BlockPos.containing(position);

        if (!level.isLoaded(pos)) {
            return true;
        }

        BlockState state = level.getBlockState(pos);

        if (!state.isAir()) {
            return true;
        }

        BlockState below = level.getBlockState(pos.below());

        return !below.isAir() && below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static void impactMeteor(
            ServerLevel level,
            LivingEntity caster,
            ParticleEffectConfig config,
            ConfigurableParticleOptions particleOptions,
            Vec3 impactPosition,
            float explosionPower,
            int fireRadius,
            int fireDurationVisualTicks,
            RandomSource random
    ) {
        Vec3 impact = new Vec3(
                impactPosition.x,
                Math.max(level.getMinBuildHeight() + 1, impactPosition.y),
                impactPosition.z
        );

        int burstAmount = Math.max(24, config.count());

        for (int i = 0; i < burstAmount; i++) {
            Vec3 direction = randomDirection(random);

            if (direction.y < 0.0D) {
                direction = new Vec3(direction.x, Math.abs(direction.y), direction.z);
            }

            Vec3 particlePos = impact.add(0.0D, 0.25D, 0.0D);
            Vec3 particleVelocity = direction.scale(config.speed() * 2.5D)
                    .add(0.0D, config.upwardSpeed() + 0.08D, 0.0D);

            spawnOne(level, particleOptions, particlePos, particleVelocity);
        }

        damageEntitiesInRadius(level, caster, config, impact);

        if (explosionPower > 0.0F) {
            level.explode(
                    caster,
                    impact.x,
                    impact.y,
                    impact.z,
                    explosionPower,
                    false,
                    Level.ExplosionInteraction.NONE
            );
        }

        placeFireDisk(level, BlockPos.containing(impact), fireRadius);

        level.playSound(
                null,
                impact.x,
                impact.y,
                impact.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.2F,
                0.75F + random.nextFloat() * 0.20F
        );
    }

    private static void placeFireDisk(ServerLevel level, BlockPos center, int radius) {
        if (radius <= 0) {
            return;
        }

        int safeRadius = Math.min(radius, 6);
        int radiusSq = safeRadius * safeRadius;

        for (BlockPos mutable : BlockPos.betweenClosed(
                center.offset(-safeRadius, -1, -safeRadius),
                center.offset(safeRadius, 1, safeRadius)
        )) {
            BlockPos pos = mutable.immutable();

            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();

            if ((dx * dx + dz * dz) > radiusSq) {
                continue;
            }

            BlockPos firePos = pos.above();

            if (!level.isLoaded(firePos)) {
                continue;
            }

            if (!level.getBlockState(firePos).isAir()) {
                continue;
            }

            BlockState below = level.getBlockState(pos);

            if (below.isAir()) {
                continue;
            }

            BlockState fire = Blocks.FIRE.defaultBlockState();

            if (fire.canSurvive(level, firePos)) {
                level.setBlockAndUpdate(firePos, fire);
            }
        }
    }

    private static final class ActiveMeteor {

        private Vec3 position;
        private Vec3 velocity;
        private int age;
        private final int maxAge;

        private LivingEntity target;
        private final Vec3 impactTarget;

        private ActiveMeteor(Vec3 position, Vec3 velocity, int maxAge) {
            this(position, velocity, maxAge, null, position);
        }

        private ActiveMeteor(
                Vec3 position,
                Vec3 velocity,
                int maxAge,
                LivingEntity target,
                Vec3 impactTarget
        ) {
            this.position = position;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.target = target;
            this.impactTarget = impactTarget;
            this.age = 0;
        }
    }

    private static void breakBlocksInDisk(
            ServerLevel level,
            BlockPos center,
            double radius,
            int depth,
            float maxHardnessToBreak,
            LivingEntity breaker
    ) {
        if (radius <= 0.0D || maxHardnessToBreak <= 0.0F) {
            return;
        }

        int blockRadius = (int) Math.ceil(radius);
        int safeDepth = Math.max(1, depth);

        double centerX = center.getX() + 0.5D;
        double centerZ = center.getZ() + 0.5D;
        double radiusSq = radius * radius;

        BlockPos min = center.offset(-blockRadius, -safeDepth + 1, -blockRadius);
        BlockPos max = center.offset(blockRadius, 0, blockRadius);

        for (BlockPos mutablePos : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = mutablePos.immutable();

            double dx = (pos.getX() + 0.5D) - centerX;
            double dz = (pos.getZ() + 0.5D) - centerZ;

            if ((dx * dx + dz * dz) > radiusSq) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            if (!state.getFluidState().isEmpty()) {
                continue;
            }

            if (level.getBlockEntity(pos) != null) {
                continue;
            }

            float hardness = state.getDestroySpeed(level, pos);

            if (hardness < 0.0F) {
                continue;
            }

            if (hardness > maxHardnessToBreak) {
                continue;
            }

            level.destroyBlock(pos, false, breaker);
        }
    }
}