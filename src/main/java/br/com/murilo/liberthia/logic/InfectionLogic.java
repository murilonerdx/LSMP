package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.backend.BackendClient;
import br.com.murilo.liberthia.capability.IInfectionData;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.S2CInfectionSyncPacket;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModFluids;
import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class InfectionLogic {
    private static final UUID HEALTH_PENALTY_UUID = UUID.fromString("ce316bd0-ec11-44ca-98da-e77be719fe77");
    private static final int FULL_MOB_INFECTION_TICKS = 20 * 40;
    private static final int DNA_DOMINANCE_THRESHOLD = 700;
    private static final int DNA_BALANCE_TOLERANCE = 75;

    private static final int MAX_INFECTION_GROWTH_HEIGHT = 3;
    private static final int SURFACE_SPREAD_RADIUS = 5;
    private static final int FLUID_SCAN_RADIUS = 16;
    private static final int FLUID_SCAN_VERTICAL = 6;
    private static final int BLACK_HOLE_MIN_FLUID_BLOCKS = 6;
    private static final int BLACK_HOLE_PARTICLE_THRESHOLD = 2000;

    private static final Map<UUID, Integer> AMBIENT_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> MOB_EXPOSURE_TICKS = new ConcurrentHashMap<>();

    private InfectionLogic() {
    }

    public static void tick(ServerPlayer player, IInfectionData data) {
        boolean immuneNow = data.isImmune();
        if (immuneNow && data.getInfection() > 0) {
            data.setInfection(0);
        }
        ExposureData exposure = scanExposureGeneric(player);

        if (data.getPillTimer() > 0) {
            int depletion = 1;
            if (exposure.rawDarkPressure() > 0) {
                depletion = 100;
            }
            data.setPillTimer(Math.max(0, data.getPillTimer() - depletion));
        }

        if (!immuneNow) {
            applyExposure(player, data, exposure);
        }
        updateDnaProfile(player, exposure);
        applyMatterSynergy(player, data, exposure);

        if (exposure.effectiveDarkPressure() > 0 && player.tickCount % 20 == 0) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("chat.liberthia.exposure_alert")
                            .withStyle(net.minecraft.ChatFormatting.RED),
                    true
            );
        }

        int derivedPenalty = Math.min(10, (data.getMaxInfectionReached() / 20) * 2);
        if (derivedPenalty > data.getPermanentHealthPenalty()) {
            data.setPermanentHealthPenalty(derivedPenalty);
        }

        applyDerivedEffects(player, data);
        applySelfDebuffs(player, data, exposure);
        applyCustomEffects(player, data);
        applyMutationEffects(player, data);
        applyIsolation(player, data);
        spreadCorruption(player, data);
        spreadNeutralMatters(player, exposure);

        if (player.tickCount % 40 == 0) {
            processDarkFluidActivity(player.serverLevel(), player.blockPosition());
        }

        if (!immuneNow && data.getStage() >= 2 && player.level().getGameTime() % 80L == 0L) {
            player.hurt(player.damageSources().magic(), 1.0F);
        }

        if ((data.getStage() >= 3 || exposure.immersedInDark()) && player.level().getGameTime() % 200L == 0L) {
            player.level().playSound(null, player.blockPosition(), ModSounds.DARK_PULSE.get(), SoundSource.PLAYERS, 0.65F, 0.85F);
        }

        if (data.isDirty() || player.tickCount % 20 == 0) {
            sync(player, data, exposure);
            data.setDirty(false);
        }

        if (player.tickCount % 200 == 0) {
            BackendClient.sendSnapshot(player, data);
            checkEnvironmentalInstability(player);
        }
    }

    private static void checkEnvironmentalInstability(ServerPlayer player) {
        ServerLevel serverLevel = player.serverLevel();
        BlockPos origin = player.blockPosition();

        processDarkFluidActivity(serverLevel, origin);

        float density = getChunkInfectionDensity(serverLevel, origin);

        if (density >= 0.90f) {
            BlockPos target = origin.above(8)
                    .offset(serverLevel.random.nextInt(10) - 5, 0, serverLevel.random.nextInt(10) - 5);

            if (!hasNearbyBlackHole(serverLevel, target, 24.0D) && !isSpreadBlockedByProtectiveBlocks(serverLevel, target)) {
                spawnBlackHole(serverLevel, target);
            }
        } else if (density >= 0.75f) {
            triggerDarkExplosion(serverLevel, origin);
        }
    }

    private static int countDarkMatterFoci(Level level, BlockPos center, int radius) {
        int count = 0;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -8, -radius), center.offset(radius, 8, radius))) {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);

            if (state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                    || state.is(ModBlocks.INFECTION_GROWTH.get())
                    || state.is(ModBlocks.CORRUPTED_SOIL.get())) {
                count++;
            }

            if (fluidState.getType().isSame(ModFluids.DARK_MATTER.get())) {
                count += 2;
            }
        }

        return count;
    }

    public static float getChunkInfectionDensity(Level level, BlockPos pos) {
        net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkAt(pos);
        int count = 0;
        int samples = 0;

        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                BlockPos p = level.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        new BlockPos(chunk.getPos().getMinBlockX() + x, 0, chunk.getPos().getMinBlockZ() + z)
                );

                BlockState s = level.getBlockState(p.below());
                FluidState f = level.getFluidState(p);

                if (s.is(ModBlocks.DARK_MATTER_BLOCK.get())
                        || s.is(ModBlocks.CORRUPTED_SOIL.get())
                        || s.is(ModBlocks.INFECTION_GROWTH.get())
                        || f.getType().isSame(ModFluids.DARK_MATTER.get())) {
                    count++;
                }
                samples++;
            }
        }

        return samples == 0 ? 0.0f : (float) count / samples;
    }

    private static void triggerDarkExplosion(Level level, BlockPos pos) {
        level.explode(null, pos.getX(), pos.getY(), pos.getZ(), 8.0F, true, Level.ExplosionInteraction.BLOCK);

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        net.minecraft.world.level.chunk.LevelChunk chunk = serverLevel.getChunkAt(pos);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos sample = new BlockPos(
                        chunk.getPos().getMinBlockX() + x,
                        pos.getY(),
                        chunk.getPos().getMinBlockZ() + z
                );

                BlockPos surface = serverLevel.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        sample
                );

                BlockPos ground = surface.below();

                if (serverLevel.random.nextFloat() < 0.20f) {
                    advanceInfectionStage(serverLevel, ground, 0.35f, 0.08f, 0.02f);
                }
            }
        }

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(16))) {
            entity.getCapability(br.com.murilo.liberthia.registry.ModCapabilities.INFECTION).ifPresent(data -> {
                data.setInfection(100);
                data.setDirty(true);
            });
        }
    }

    private static void spawnBlackHole(Level level, BlockPos pos) {
        spawnBlackHole(level, pos, BLACK_HOLE_PARTICLE_THRESHOLD);
    }

    private static void spawnBlackHole(Level level, BlockPos pos, int particleMass) {
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (isSpreadBlockedByProtectiveBlocks(serverLevel, pos)) return;

        br.com.murilo.liberthia.entity.BlackHoleEntity blackHole =
                br.com.murilo.liberthia.registry.ModEntities.BLACK_HOLE.get().create(level);

        if (blackHole != null) {
            double massFactor = Math.max(1.0D, particleMass / 2000.0D);
            blackHole.setExplosionSizeMultiplier(0.55D * Math.min(4.0D, massFactor));
            blackHole.setPlayerDeathExplosionMultiplier(3.40D * Math.min(2.5D, massFactor));
            blackHole.setProximityExplosionMultiplier(2.60D * Math.min(2.5D, massFactor));
            blackHole.setTriggerPercent(Math.min(95, 45 + (particleMass / 120)));
            blackHole.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0);
            level.addFreshEntity(blackHole);
            level.playSound(null, pos, ModSounds.DARK_PULSE.get(), SoundSource.BLOCKS, 2.0F, 0.5F);
        }
    }

    public static void tickLiving(LivingEntity entity, IInfectionData data) {
        if (data.isImmune() || entity.level().isClientSide) return;

        ExposureData exposure = scanExposureGeneric(entity);
        int exposureTicks = updateMobExposureCounter(entity, exposure.effectiveDarkPressure() > 0 || exposure.immersedInDark());
        double nearestDarkDistance = findNearestDarkMatterDistance(entity.level(), entity.blockPosition(), 8);

        if (exposureTicks > 0) {
            int targetInfection = Math.min(100, (exposureTicks * 100) / FULL_MOB_INFECTION_TICKS);
            if (data.getInfection() < targetInfection) {
                data.setInfection(targetInfection);
            }
        }

        if (exposure.effectiveDarkPressure() >= 8 && entity.tickCount % 40 == 0) {
            entity.hurt(entity.damageSources().magic(), 1.0F);
        }

        if (nearestDarkDistance <= 8.0D) {
            int bonus = nearestDarkDistance <= 2.0D ? 3 : nearestDarkDistance <= 4.0D ? 2 : 1;
            if (entity.tickCount % Math.max(6, 20 - (bonus * 4)) == 0) {
                data.addInfection(bonus);
            }
            if (entity.tickCount % Math.max(20, 60 - (bonus * 10)) == 0) {
                entity.hurt(entity.damageSources().magic(), 1.0F + bonus);
            }
        }

        if (data.getInfection() >= 50) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            if (entity.tickCount % 20 == 0) {
                entity.hurt(entity.damageSources().magic(), 2.0F);
            }
        }

        spreadCorruption(entity, data);
    }

    private static int updateMobExposureCounter(LivingEntity entity, boolean exposedToDark) {
        UUID entityId = entity.getUUID();
        if (exposedToDark) {
            int updated = Math.min(FULL_MOB_INFECTION_TICKS, MOB_EXPOSURE_TICKS.getOrDefault(entityId, 0) + 1);
            MOB_EXPOSURE_TICKS.put(entityId, updated);
            return updated;
        }

        int cooled = Math.max(0, MOB_EXPOSURE_TICKS.getOrDefault(entityId, 0) - 2);
        if (cooled <= 0) {
            MOB_EXPOSURE_TICKS.remove(entityId);
            return 0;
        }
        MOB_EXPOSURE_TICKS.put(entityId, cooled);
        return cooled;
    }

    private static void spreadCorruption(LivingEntity entity, IInfectionData data) {
        if (data.getInfection() < 50 || entity.level().isClientSide || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float chance = Math.min(0.12f, 0.03f * data.getStage());
        if (serverLevel.random.nextFloat() >= chance) {
            return;
        }

        BlockPos baseGround = resolveGroundBlock(entity);
        advanceInfectionStage(
                serverLevel,
                baseGround,
                0.45f,
                data.getStage() >= 2 ? 0.12f : 0.04f,
                data.getStage() >= 4 ? 0.03f : 0.0f
        );

        int attempts = 1 + Math.min(2, data.getStage() / 2);
        for (int i = 0; i < attempts; i++) {
            BlockPos nearbyGround = findNearbySurface(serverLevel, entity.blockPosition(), 2);
            advanceInfectionStage(
                    serverLevel,
                    nearbyGround,
                    0.30f,
                    data.getStage() >= 2 ? 0.08f : 0.02f,
                    data.getStage() >= 4 ? 0.02f : 0.0f
            );
        }
    }

    private static void applyMutationEffects(ServerPlayer player, IInfectionData data) {
        if (data.getInfection() < 50) return;

        if (data.getInfection() >= 90 && data.getMutations().isEmpty()) {
            data.addMutation("HEAVY_STEPS");
            data.addMutation("RADIO_EYES");
        }

        if (data.hasMutation("HUNGRY_VOID")) {
            player.getFoodData().addExhaustion(0.1f);
        }
        if (data.hasMutation("DARK_VEIL")) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, false));
        }
        if (data.hasMutation("RADIANT_SKIN")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false));
        }
        if (data.hasMutation("RADIO_EYES")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));
        }
        if (data.hasMutation("AQUATIC_ADAPTATION")) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false));
        }
        if (data.hasMutation("SWIFT_SIGHT")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, true, false));
            player.getFoodData().addExhaustion(0.05f);
        }
        if (data.hasMutation("STATIC_DISCHARGE") && player.isInWaterOrRain()) {
            player.hurt(player.damageSources().magic(), 1.0f);
        }
    }

    private static void applyCustomEffects(ServerPlayer player, IInfectionData data) {
        if (data.getStage() >= 2) {
            int amplifier = Math.min(3, data.getStage() - 1);
            player.addEffect(new MobEffectInstance(ModEffects.DARK_INFECTION.get(), 100, amplifier, true, true, true));
        }

        if (data.getStage() >= 3) {
            int amplifier = data.getStage() >= 4 ? 1 : 0;
            player.addEffect(new MobEffectInstance(ModEffects.RADIATION_SICKNESS.get(), 100, amplifier, true, false, true));
        }
    }

    private static void applyIsolation(ServerPlayer player, IInfectionData data) {
        if (data.getStage() < 4) return;

        if (player.level().getGameTime() % 100L == 0L) {
            player.level().playSound(
                    null,
                    player.blockPosition(),
                    ModSounds.ISOLATION_WARNING.get(),
                    SoundSource.PLAYERS,
                    0.7F,
                    0.6F
            );
        }

        for (ServerPlayer nearby : player.serverLevel().players()) {
            if (nearby != player && nearby.distanceTo(player) < 5.0D) {
                nearby.getCapability(br.com.murilo.liberthia.registry.ModCapabilities.INFECTION).ifPresent(nearbyData -> {
                    nearbyData.addInfection(1);
                    nearbyData.setDirty(true);
                });
            }
        }
    }

    public static void sync(ServerPlayer player, IInfectionData data) {
        sync(player, data, scanExposureGeneric(player));
    }

    private static void sync(ServerPlayer player, IInfectionData data, ExposureData exposure) {
        ModNetwork.sendToPlayer(player, new S2CInfectionSyncPacket(
                data.getInfection(),
                data.getPermanentHealthPenalty(),
                data.getStage(),
                exposure.rawDarkPressure(),
                exposure.blockedExposure(),
                exposure.armorProtectionPercent(),
                data.getMutations()
        ));
    }

    public static void applyDerivedEffects(ServerPlayer player, IInfectionData data) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;

        AttributeModifier existing = attribute.getModifier(HEALTH_PENALTY_UUID);
        if (existing != null) {
            attribute.removeModifier(existing);
        }

        if (data.getPermanentHealthPenalty() > 0) {
            attribute.addPermanentModifier(new AttributeModifier(
                    HEALTH_PENALTY_UUID,
                    "liberthia_infection_penalty",
                    -data.getPermanentHealthPenalty(),
                    AttributeModifier.Operation.ADDITION
            ));
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static ExposureData scanExposureGeneric(LivingEntity entity) {
        float localPressure = 0.0f;
        int clearBlocks = 0;
        int yellowBlocks = 0;
        boolean immersedInDark = false;
        boolean touchingClear = false;
        boolean touchingYellow = false;
        boolean carryingDarkMatter = entity instanceof ServerPlayer p && isCarryingDarkMatter(p);

        BlockPos center = entity.blockPosition();
        net.minecraft.world.phys.Vec3 entityCenter = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
            BlockState blockState = entity.level().getBlockState(pos);
            FluidState fluidState = entity.level().getFluidState(pos);

            float sourceStrength = 0.0f;
            if (fluidState.getType().isSame(ModFluids.DARK_MATTER.get())
                    || fluidState.getType().isSame(ModFluids.FLOWING_DARK_MATTER.get())) {
                sourceStrength = 4.0f;
            } else if (blockState.is(ModBlocks.DARK_MATTER_BLOCK.get())) {
                sourceStrength = 3.0f;
            } else if (blockState.is(ModBlocks.INFECTION_GROWTH.get())) {
                sourceStrength = 2.0f;
            } else if (blockState.is(ModBlocks.CORRUPTED_SOIL.get())) {
                sourceStrength = 1.0f;
            } else if (blockState.is(ModBlocks.DARK_MATTER_ORE.get())
                    || blockState.is(ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get())) {
                sourceStrength = 0.5f;
            }

            if (sourceStrength > 0.0f) {
                net.minecraft.world.phys.Vec3 blockCenter = new net.minecraft.world.phys.Vec3(
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D
                );
                double distance = Math.sqrt(blockCenter.distanceToSqr(entityCenter));
                float attenuation = (float) (1.0D / (1.0D + (distance * 0.75D)));
                localPressure += sourceStrength * attenuation;
            }

            if (blockState.is(ModBlocks.CLEAR_MATTER_BLOCK.get())) {
                clearBlocks++;
            }

            if (isYellowMatterBlock(blockState)) {
                yellowBlocks++;
            }
        }

        int forwardPressure = 0;
        net.minecraft.world.phys.Vec3 lookVec = entity.getLookAngle();
        net.minecraft.world.phys.Vec3 startPos = entity.getEyePosition();

        for (int d = 1; d <= 20; d++) {
            BlockPos p = BlockPos.containing(startPos.add(lookVec.scale(d)));
            if (isInfectionBlock(entity.level().getBlockState(p))) {
                forwardPressure += Math.max(1, (22 - d) / 4);
            }
        }

        if (!entity.level().isClientSide && entity.tickCount % 20 == 0) {
            int ambient = countDarkMatterParticles(entity.level(), center, 16, 6) / 120;
            AMBIENT_CACHE.put(entity.getUUID(), ambient);
        }

        int ambientChunkExposure = AMBIENT_CACHE.getOrDefault(entity.getUUID(), 0);
        float chunkDensity = getChunkInfectionDensity(entity.level(), center);
        int chunkInfluence = Math.round(chunkDensity * 3.0f);
        int rawDarkPressure = Math.round(localPressure) + ambientChunkExposure + chunkInfluence + forwardPressure;

        FluidState feetFluid = entity.level().getFluidState(entity.blockPosition());
        FluidState headFluid = entity.level().getFluidState(entity.blockPosition().above());
        if (feetFluid.getType().isSame(ModFluids.DARK_MATTER.get()) || headFluid.getType().isSame(ModFluids.DARK_MATTER.get())) {
            immersedInDark = true;
            rawDarkPressure += 8;
        }

        if (isWaterOrLava(feetFluid) || isWaterOrLava(headFluid)) {
            rawDarkPressure = 0;
            immersedInDark = false;
        }

        if (carryingDarkMatter) {
            rawDarkPressure += 1;
        }

        BlockState feetBlock = entity.level().getBlockState(entity.blockPosition());
        BlockState headBlock = entity.level().getBlockState(entity.blockPosition().above());
        if (feetBlock.is(ModBlocks.CLEAR_MATTER_BLOCK.get())
                || headBlock.is(ModBlocks.CLEAR_MATTER_BLOCK.get())
        ) {
            touchingClear = true;
        }
        if (isYellowMatterBlock(feetBlock) || isYellowMatterBlock(headBlock)) {
            touchingYellow = true;
        }

        int armorPieces = entity instanceof ServerPlayer p ? getYellowArmorPieces(p) : 0;
        float armorProtectionRatio = armorPieces * 0.125F;
        int blockedExposure = Math.round(rawDarkPressure * armorProtectionRatio);

        int rawClearPressure = clearBlocks * 3 + (touchingClear ? 5 : 0);
        int rawYellowPressure = yellowBlocks * 3 + (touchingYellow ? 5 : 0);

        float clearProtectionRatio = Math.min(1.0F, (clearBlocks + yellowBlocks) * 0.20F);
        if (touchingClear) {
            clearProtectionRatio = Math.min(1.0F, clearProtectionRatio + 0.20F);
        }
        int clearRelief = Math.round(rawDarkPressure * clearProtectionRatio);

        return new ExposureData(
                rawDarkPressure,
                Math.max(0, rawDarkPressure - blockedExposure - clearRelief),
                blockedExposure,
                clearRelief,
                rawClearPressure,
                rawYellowPressure,
                immersedInDark,
                touchingClear,
                touchingYellow,
                carryingDarkMatter,
                armorPieces,
                Math.round((armorProtectionRatio + clearProtectionRatio) * 100.0F)
        );
    }

    private static boolean isInfectionBlock(BlockState state) {
        return state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                || state.is(ModBlocks.INFECTION_GROWTH.get())
                || state.is(ModBlocks.CORRUPTED_SOIL.get())
                || state.is(ModBlocks.DARK_MATTER_ORE.get())
                || state.is(ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get());
    }

    private static boolean isImmersedInDark(LivingEntity entity) {
        FluidState feetFluid = entity.level().getFluidState(entity.blockPosition());
        FluidState headFluid = entity.level().getFluidState(entity.blockPosition().above());
        return feetFluid.getType().isSame(ModFluids.DARK_MATTER.get()) || headFluid.getType().isSame(ModFluids.DARK_MATTER.get());
    }

    private static void applyExposure(ServerPlayer player, IInfectionData data, ExposureData exposure) {
        int delta = exposure.effectiveDarkPressure() - exposure.clearRelief();
        if (delta > 0) {
            data.addInfection(Math.min(8, delta));
        } else if (delta < 0) {
            data.reduceInfection(Math.min(12, -delta));
        }

        if ((exposure.touchingClear() || exposure.clearRelief() >= 12) && player.level().getGameTime() % 100L == 0L) {
            data.reducePermanentHealthPenalty(1);
        }
    }

    private static void applySelfDebuffs(ServerPlayer player, IInfectionData data, ExposureData exposure) {
        if (player.hasEffect(ModEffects.CLEAR_SHIELD.get())) return;

        int severity = Math.max(0, data.getInfection() - exposure.clearRelief());

        if (severity >= 5) player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 80, severity >= 50 ? 2 : 1, true, false, true));
        if (severity >= 10) player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, severity >= 60 ? 2 : 1, true, false, true));
        if (severity >= 15) player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, severity >= 70 ? 1 : 0, true, false, true));
        if (severity >= 20) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, severity >= 65 ? 1 : 0, true, false, true));
        if (severity >= 25 && !exposure.touchingClear()) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false, true));
        if (severity >= 35 && !exposure.touchingClear()) player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false, true));

        if (exposure.carryingDarkMatter()) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 2, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false, true));
        }

        if (exposure.touchingClear()) {
            player.removeEffect(MobEffects.BLINDNESS);
            if (severity < 30) {
                player.removeEffect(MobEffects.CONFUSION);
            }
        }
    }

    private static void applyMatterSynergy(ServerPlayer player, IInfectionData data, ExposureData exposure) {
        player.getCapability(br.com.murilo.liberthia.registry.ModCapabilities.MATTER_ENERGY).ifPresent(energy -> {
            int dark = energy.getDarkEnergy();
            int clear = energy.getClearEnergy();
            int yellow = energy.getYellowEnergy();
            long gameTime = player.level().getGameTime();
            boolean darkDominant = dark >= DNA_DOMINANCE_THRESHOLD && dark > clear && dark > yellow;
            boolean darkClearBalanced = Math.abs(dark - clear) <= DNA_BALANCE_TOLERANCE && dark >= 350 && clear >= 350;
            boolean clearYellowImmune = Math.abs(clear - yellow) <= 50 && clear >= 450 && yellow >= 450 && dark <= 100;

            if (dark > 0 && yellow > 0) {
                int repulsion = Math.min(dark, Math.max(2, yellow / 40));
                energy.setDarkEnergy(dark - repulsion);
            }

            if (dark > 0 && clear > 0) {
                data.addMutation("LUCID_CORRUPTION");
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, true));
                player.removeEffect(MobEffects.CONFUSION);

                if (gameTime % 20L == 0L) {
                    data.addInfection(1);
                }
            }

            if (clear > 0 && yellow > 0) {
                data.addMutation("TACTICAL_REASON");
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));

                if (gameTime % 120L == 0L && player.getRandom().nextFloat() < 0.25F) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, true, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, true));
                }
            }

            // Só fica imune com DNA 50/50 entre Clara e Amarela
            data.setImmune(clearYellowImmune);

            if (darkDominant) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1, true, false, true));

                if (gameTime % 40L == 0L) {
                    for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(4.5D))) {
                        if (nearby != player) {
                            nearby.hurt(player.damageSources().magic(), 2.0F);
                        }
                    }
                }
            } else if (darkClearBalanced) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 80, 1, true, false, true));

                if (gameTime % 120L == 0L) {
                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 0.6F, 0.7F);
                }
            } else if (clear > 0) {
                // Matéria Clara desfaz mutações da Escura, mas não impede receber infecção
                data.removeMutation("LUCID_CORRUPTION");
                data.removeMutation("HEAVY_STEPS");
                data.removeMutation("RADIO_EYES");
                player.removeEffect(MobEffects.CONFUSION);
                player.removeEffect(MobEffects.HUNGER);
                player.removeEffect(MobEffects.BLINDNESS);
            }
        });
    }

    private static void updateDnaProfile(ServerPlayer player, ExposureData exposure) {
        player.getCapability(br.com.murilo.liberthia.registry.ModCapabilities.MATTER_ENERGY).ifPresent(energy -> {
            int dark = energy.getDarkEnergy();
            int clear = energy.getClearEnergy();
            int yellow = energy.getYellowEnergy();

            int darkGain = exposure.effectiveDarkPressure() > 0 ? 2 : 0;
            if (exposure.immersedInDark()) {
                darkGain += 2;
            }
            int clearGain = exposure.clearPressure() > 0 ? 2 : 0;
            int yellowGain = exposure.yellowPressure() > 0 ? 2 : 0;

            dark = Math.min(energy.getMaxEnergy(), dark + darkGain - ((clearGain + yellowGain) > 0 ? 1 : 0));
            clear = Math.min(energy.getMaxEnergy(), clear + clearGain - (darkGain > 0 ? 1 : 0));
            yellow = Math.min(energy.getMaxEnergy(), yellow + yellowGain - (darkGain > 0 ? 1 : 0));

            // Matéria Clara desfaz progressivamente DNA escuro
            if (clearGain > 0 && dark > 0) {
                dark = Math.max(0, dark - 2);
            }

            dark = Math.max(0, dark);
            clear = Math.max(0, clear);
            yellow = Math.max(0, yellow);

            int[] values = new int[]{dark, clear, yellow};
            int minIdx = 0;
            if (values[1] < values[minIdx]) minIdx = 1;
            if (values[2] < values[minIdx]) minIdx = 2;
            values[minIdx] = Math.max(0, values[minIdx] - 4);

            energy.setDarkEnergy(values[0]);
            energy.setClearEnergy(values[1]);
            energy.setYellowEnergy(values[2]);
        });
    }

    private static void spreadNeutralMatters(ServerPlayer player, ExposureData exposure) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (player.tickCount % 30 != 0) {
            return;
        }

        float clearChance = Math.min(0.35F, exposure.clearPressure() / 80.0F);
        float yellowChance = Math.min(0.35F, exposure.yellowPressure() / 80.0F);
        if (clearChance <= 0 && yellowChance <= 0) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            BlockPos surface = findNearbySurface(serverLevel, player.blockPosition(), SURFACE_SPREAD_RADIUS);
            BlockState surfaceState = serverLevel.getBlockState(surface);
            if (!isNaturalSurface(surfaceState)) {
                continue;
            }

            if (yellowChance > clearChance && serverLevel.random.nextFloat() < yellowChance) {
                serverLevel.setBlockAndUpdate(surface, ModBlocks.YELLOW_MATTER_BLOCK.get().defaultBlockState());
                MatterHistoryManager.registerProtectionBlock(serverLevel, surface, ModBlocks.YELLOW_MATTER_BLOCK.get().defaultBlockState());
            } else if (serverLevel.random.nextFloat() < clearChance) {
                serverLevel.setBlockAndUpdate(surface, ModBlocks.CLEAR_MATTER_BLOCK.get().defaultBlockState());
                MatterHistoryManager.registerProtectionBlock(serverLevel, surface, ModBlocks.CLEAR_MATTER_BLOCK.get().defaultBlockState());
            }
        }
    }

    public static boolean isCarryingDarkMatter(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isDarkItem(stack)) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isDarkItem(stack)) return true;
        }
        return false;
    }

    private static boolean isDarkItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        return item == ModItems.DARK_MATTER_BUCKET.get()
                || item == ModItems.DARK_MATTER_BLOCK_ITEM.get()
                || item == ModItems.DARK_MATTER_SHARD.get()
                || item == ModItems.DARK_MATTER_ORE_ITEM.get()
                || item == ModItems.DEEPSLATE_DARK_MATTER_ORE_ITEM.get()
                || item == ModItems.INFECTION_GROWTH_ITEM.get();
    }

    private static int getYellowArmorPieces(ServerPlayer player) {
        int pieces = 0;
        for (ItemStack stack : player.getInventory().armor) {
            if (isYellowMatterArmor(stack)) pieces++;
        }
        return pieces;
    }

    private static boolean isYellowMatterArmor(ItemStack stack) {
        return !stack.isEmpty() && (
                stack.is(ModItems.YELLOW_MATTER_HELMET.get())
                        || stack.is(ModItems.YELLOW_MATTER_CHESTPLATE.get())
                        || stack.is(ModItems.YELLOW_MATTER_LEGGINGS.get())
                        || stack.is(ModItems.YELLOW_MATTER_BOOTS.get())
        );
    }

    private static void advanceInfectionStage(ServerLevel level, BlockPos groundPos, float soilChance, float growthChance, float fluidChance) {
        if (groundPos == null) {
            return;
        }

        if (isSpreadBlockedByProtectiveBlocks(level, groundPos)) {
            return;
        }

        BlockState groundState = level.getBlockState(groundPos);

        if (isNaturalSurface(groundState)) {
            if (level.random.nextFloat() < soilChance) {
                MatterHistoryManager.recordOriginalBlock(level, groundPos, groundState);
                level.setBlockAndUpdate(groundPos, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
            }
            return;
        }

        if (groundState.is(ModBlocks.CORRUPTED_SOIL.get()) || groundState.is(ModBlocks.DARK_MATTER_BLOCK.get())) {
            if (level.random.nextFloat() < growthChance) {
                tryGrowInfectionColumn(level, groundPos.above());
            }

            if (level.random.nextFloat() < fluidChance) {
                tryCondenseDarkFluid(level, groundPos);
            }
            return;
        }

        if (groundState.is(ModBlocks.INFECTION_GROWTH.get())) {
            BlockPos base = findGrowthBase(level, groundPos);

            if (level.random.nextFloat() < growthChance) {
                BlockPos top = getGrowthTop(level, base);
                tryGrowInfectionColumn(level, top.above());
            }

            if (level.random.nextFloat() < fluidChance) {
                tryCondenseDarkFluid(level, base);
            }
        }
    }

    private static boolean isNaturalSurface(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.SAND)
                || state.is(Blocks.STONE);
    }

    private static boolean tryGrowInfectionColumn(ServerLevel level, BlockPos pos) {
        if (isSpreadBlockedByProtectiveBlocks(level, pos)) {
            return false;
        }
        if (isHydroBlocked(level, pos)) {
            return false;
        }

        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        if (hasAdjacentGrowth(level, pos)) {
            return false;
        }

        BlockPos cursor = pos.below();
        int growthCount = 0;

        while (level.getBlockState(cursor).is(ModBlocks.INFECTION_GROWTH.get())) {
            growthCount++;
            cursor = cursor.below();
        }

        BlockState baseState = level.getBlockState(cursor);
        boolean validBase = baseState.is(ModBlocks.CORRUPTED_SOIL.get()) || baseState.is(ModBlocks.DARK_MATTER_BLOCK.get());

        if (!validBase || growthCount >= MAX_INFECTION_GROWTH_HEIGHT) {
            return false;
        }

        level.setBlockAndUpdate(pos, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
        return true;
    }

    private static boolean hasAdjacentGrowth(Level level, BlockPos pos) {
        return level.getBlockState(pos.north()).is(ModBlocks.INFECTION_GROWTH.get())
                || level.getBlockState(pos.south()).is(ModBlocks.INFECTION_GROWTH.get())
                || level.getBlockState(pos.east()).is(ModBlocks.INFECTION_GROWTH.get())
                || level.getBlockState(pos.west()).is(ModBlocks.INFECTION_GROWTH.get());
    }

    private static boolean tryCondenseDarkFluid(ServerLevel level, BlockPos base) {
        if (isSpreadBlockedByProtectiveBlocks(level, base)) {
            return false;
        }
        if (isHydroBlocked(level, base)) {
            return false;
        }

        BlockPos realBase = level.getBlockState(base).is(ModBlocks.INFECTION_GROWTH.get())
                ? findGrowthBase(level, base)
                : base;

        BlockState baseState = level.getBlockState(realBase);
        if (!baseState.is(ModBlocks.CORRUPTED_SOIL.get()) && !baseState.is(ModBlocks.DARK_MATTER_BLOCK.get())) {
            return false;
        }

        int growthHeight = countGrowthAboveBase(level, realBase);
        if (growthHeight < MAX_INFECTION_GROWTH_HEIGHT) {
            return false;
        }

        int localDensity = countDarkMatterFoci(level, realBase, 3);
        if (localDensity < 8) {
            return false;
        }

        BlockPos fluidPos = getGrowthTop(level, realBase).above();
        if (isSpreadBlockedByProtectiveBlocks(level, fluidPos)) {
            return false;
        }

        if (!level.getBlockState(fluidPos).isAir() || !level.getFluidState(fluidPos).isEmpty()) {
            return false;
        }

        level.setBlockAndUpdate(fluidPos, ModFluids.DARK_MATTER.get().defaultFluidState().createLegacyBlock());
        return true;
    }

    private static BlockPos findGrowthBase(Level level, BlockPos pos) {
        BlockPos cursor = pos.immutable();

        while (level.getBlockState(cursor).is(ModBlocks.INFECTION_GROWTH.get())) {
            cursor = cursor.below();
        }

        return cursor;
    }

    private static BlockPos getGrowthTop(Level level, BlockPos base) {
        BlockPos cursor = base.immutable();

        while (level.getBlockState(cursor.above()).is(ModBlocks.INFECTION_GROWTH.get())) {
            cursor = cursor.above();
        }

        return cursor;
    }

    private static int countGrowthAboveBase(Level level, BlockPos base) {
        int count = 0;
        BlockPos cursor = base.above();

        while (level.getBlockState(cursor).is(ModBlocks.INFECTION_GROWTH.get())) {
            count++;
            cursor = cursor.above();
        }

        return count;
    }

    private static BlockPos resolveGroundBlock(LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        BlockState state = entity.level().getBlockState(pos);

        if (state.isAir() || !state.isSolidRender(entity.level(), pos)) {
            return pos.below();
        }

        return pos;
    }

    private static BlockPos findNearbySurface(ServerLevel level, BlockPos origin, int radius) {
        int dx = level.random.nextInt((radius * 2) + 1) - radius;
        int dz = level.random.nextInt((radius * 2) + 1) - radius;

        BlockPos sample = origin.offset(dx, 0, dz);
        BlockPos surface = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                sample
        );

        return surface.below();
    }

    private static void processDarkFluidActivity(ServerLevel level, BlockPos center) {
        int fluidBlocks = 0;
        BlockPos selected = null;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-FLUID_SCAN_RADIUS, -FLUID_SCAN_VERTICAL, -FLUID_SCAN_RADIUS),
                center.offset(FLUID_SCAN_RADIUS, FLUID_SCAN_VERTICAL, FLUID_SCAN_RADIUS)
        )) {
            FluidState fluidState = level.getFluidState(pos);
            if (!fluidState.getType().isSame(ModFluids.DARK_MATTER.get())) {
                continue;
            }

            if (isSpreadBlockedByProtectiveBlocks(level, pos)) {
                continue;
            }

            fluidBlocks++;
            if (selected == null) {
                selected = pos.immutable();
            }

            if (level.random.nextFloat() < 0.25f) {
                level.sendParticles(
                        ParticleTypes.SQUID_INK,
                        pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D,
                        6,
                        0.30D, 0.15D, 0.30D,
                        0.01D
                );

                level.sendParticles(
                        ParticleTypes.SMOKE,
                        pos.getX() + 0.5D, pos.getY() + 0.95D, pos.getZ() + 0.5D,
                        4,
                        0.20D, 0.10D, 0.20D,
                        0.005D
                );
            }
        }

        if (fluidBlocks < BLACK_HOLE_MIN_FLUID_BLOCKS || selected == null) {
            return;
        }

        if (hasNearbyBlackHole(level, selected, 32.0D)) {
            return;
        }

        if (isSpreadBlockedByProtectiveBlocks(level, selected)) {
            return;
        }

        int localParticles = countDarkMatterParticles(level, selected, FLUID_SCAN_RADIUS, FLUID_SCAN_VERTICAL);
        if (localParticles >= BLACK_HOLE_PARTICLE_THRESHOLD) {
            BlockPos spawnPos = selected.above(2 + level.random.nextInt(2));
            if (level.getBlockState(spawnPos).isAir()
                    && level.getBlockState(spawnPos.above()).isAir()
                    && !isSpreadBlockedByProtectiveBlocks(level, spawnPos)) {
                spawnBlackHole(level, spawnPos, localParticles);
            }
        }
    }

    public static void evaluateDarkMatterRegion(ServerLevel level, BlockPos center) {
        processDarkFluidActivity(level, center);

        int particles = countDarkMatterParticles(level, center, FLUID_SCAN_RADIUS, FLUID_SCAN_VERTICAL);
        if (particles < BLACK_HOLE_PARTICLE_THRESHOLD) {
            return;
        }

        if (hasNearbyBlackHole(level, center, 48.0D) || isSpreadBlockedByProtectiveBlocks(level, center)) {
            return;
        }

        BlockPos surface = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                center
        );
        BlockPos spawnPos = surface.above(2);
        if (level.getBlockState(spawnPos).isAir() && level.getBlockState(spawnPos.above()).isAir()) {
            spawnBlackHole(level, spawnPos, particles);
        }
    }

    public static int countDarkMatterParticles(Level level, BlockPos center, int radius, int vertical) {
        int particles = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -vertical, -radius),
                center.offset(radius, vertical, radius)
        )) {
            BlockState state = level.getBlockState(pos);
            FluidState fluid = level.getFluidState(pos);

            if (state.is(ModBlocks.DARK_MATTER_BLOCK.get())) particles += 14;
            else if (state.is(ModBlocks.INFECTION_GROWTH.get())) particles += 11;
            else if (state.is(ModBlocks.CORRUPTED_SOIL.get())) particles += 7;
            else if (state.is(ModBlocks.DARK_MATTER_ORE.get()) || state.is(ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get())) particles += 9;

            if (fluid.getType().isSame(ModFluids.DARK_MATTER.get()) || fluid.getType().isSame(ModFluids.FLOWING_DARK_MATTER.get())) {
                particles += 25;
            }
        }
        return particles;
    }

    // Compatibilidade para chamadas antigas
    public static int countDarkMatterParticles(Level level, BlockPos center, int radius) {
        return countDarkMatterParticles(level, center, radius, 6);
    }

    public static int countDarkMatterParticles(Level level, BlockPos center) {
        return countDarkMatterParticles(level, center, 16, 6);
    }

    private static boolean hasNearbyBlackHole(ServerLevel level, BlockPos pos, double radius) {
        return !level.getEntitiesOfClass(
                br.com.murilo.liberthia.entity.BlackHoleEntity.class,
                new AABB(pos).inflate(radius)
        ).isEmpty();
    }

    private static boolean isSpreadBlockedByProtectiveBlocks(ServerLevel level, BlockPos targetPos) {
        if (MatterHistoryManager.hasProtectionInChunkRange(level, targetPos, 16)) {
            return true;
        }
        if (isHydroBlocked(level, targetPos)) {
            return true;
        }

        if (hasYellowMatterProtection(level, targetPos)) {
            return true;
        }

        return hasClearMatterProtection(level, targetPos);
    }

    private static boolean hasYellowMatterProtection(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            if (isYellowMatterBlock(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClearMatterProtection(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (isClearMatterBlock(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClearMatterBlock(BlockState state) {
        if (state.is(ModBlocks.CLEAR_MATTER_BLOCK.get())) {
            return true;
        }
        return hasRegistryPath(state, "clear_matter")
                || hasRegistryPath(state, "white_matter")
                || hasRegistryPath(state, "materia_branca");
    }

    private static boolean isYellowMatterBlock(BlockState state) {
        return hasRegistryPath(state, "yellow_matter")
                || hasRegistryPath(state, "yellowmatter")
                || hasRegistryPath(state, "materia_amarela");
    }

    private static boolean hasRegistryPath(BlockState state, String expectedFragment) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) {
            return false;
        }
        return key.getPath().contains(expectedFragment);
    }

    private static boolean isHydroBlocked(Level level, BlockPos pos) {
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            FluidState fluid = level.getFluidState(scan);
            if (isWaterOrLava(fluid)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWaterOrLava(FluidState fluid) {
        return fluid.is(net.minecraft.tags.FluidTags.WATER) || fluid.is(net.minecraft.tags.FluidTags.LAVA);
    }

    private static double findNearestDarkMatterDistance(Level level, BlockPos center, int radius) {
        double nearest = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            FluidState fluid = level.getFluidState(pos);
            if (isInfectionBlock(state)
                    || fluid.getType().isSame(ModFluids.DARK_MATTER.get())
                    || fluid.getType().isSame(ModFluids.FLOWING_DARK_MATTER.get())) {
                nearest = Math.min(nearest, Math.sqrt(center.distSqr(pos)));
            }
        }
        return nearest == Double.MAX_VALUE ? 999.0D : nearest;
    }

}
