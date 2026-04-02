package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModFluids;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class BlackHoleEntity extends Entity {

    private static final EntityDataAccessor<Float> GROWTH_PROGRESS =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> NEUTRAL_MODE =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> COLLAPSE_PRIMED =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> MANIFESTED =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int BASE_CHARGE_CAP = 2000;
    private static final int OVERCHARGE_START = 2400;
    private static final float MAX_SCALE = 12.0F;

    private static final int MIN_CONTAMINATION_TO_FORM = 700;
    private static final int MIN_AIR_SPORE_DENSITY_TO_FORM = 32;
    private static final double AIR_SPORE_SCAN_RADIUS = 28.0D;
    private static final int FORMATION_REQUIRED_PROGRESS = 120;
    private static final int FORMATION_TIMEOUT_TICKS = 200;

    private static final int MIN_TRIGGER_PERCENT = 45;
    private static final int MAX_TRIGGER_PERCENT = 95;
    private static final int COLLAPSE_WARNING_TICKS = 80;
    private static final int AUTONOMOUS_COLLAPSE_WARNING_TICKS = 46;

    private static final int AUTONOMOUS_COLLAPSE_CONTAMINATION = 2600;
    private static final int AUTONOMOUS_COLLAPSE_LOCAL_MASS = 180;
    private static final float AUTONOMOUS_COLLAPSE_CHARGE_FACTOR = 1.15F;
    private static final int AUTONOMOUS_ACCELERATION_CONTAMINATION = 3200;

    private static final int SURFACE_CORRUPTION_DEPTH = 14;
    private static final int SURFACE_CORRUPTION_HEIGHT = 4;

    private static final double PLAYER_ARM_RADIUS = 18.0D;
    private static final double PLAYER_ESCAPE_RADIUS = 28.0D;
    private static final double PROXIMITY_CATASTROPHIC_RADIUS = 2.4D;

    private static final double DEFAULT_EXPLOSION_SIZE_MULTIPLIER = 0.55D;
    private static final double DEFAULT_PLAYER_DEATH_EXPLOSION_MULTIPLIER = 3.40D;
    private static final double DEFAULT_PROXIMITY_EXPLOSION_MULTIPLIER = 2.60D;

    private static final double MIN_CRATER_RADIUS = 7.0D;
    private static final double MAX_NORMAL_CRATER_RADIUS = 34.0D;
    private static final double MAX_FORCED_CRATER_RADIUS = 72.0D;

    private static final int NORMAL_BURST_INTERVAL = 10;
    private static final int NEUTRAL_BURST_INTERVAL = 4;
    private static final int GROUND_SCAN_DEPTH = 14;

    private int age = 0;
    private float stability = 1.0F;
    private boolean exploded = false;

    private int contamination = 0;
    private float collapseCharge = 0.0F;
    private int localDarkMatterMass = 0;

    private int triggerPercent = -1;
    private float triggerCharge = -1.0F;

    private boolean neutralMode = false;
    private boolean collapsePrimed = false;
    private boolean autonomousCollapsePrimed = false;
    private int collapseCountdown = -1;

    private boolean manifested = false;
    private int formationProgress = 0;
    private int airSporeDensity = 0;

    private boolean emergencyPlayerDeathDetonation = false;
    private boolean emergencyProximityDetonation = false;
    private double emergencyExplosionMultiplier = 1.0D;

    private double explosionSizeMultiplier = DEFAULT_EXPLOSION_SIZE_MULTIPLIER;
    private double playerDeathExplosionMultiplier = DEFAULT_PLAYER_DEATH_EXPLOSION_MULTIPLIER;
    private double proximityExplosionMultiplier = DEFAULT_PROXIMITY_EXPLOSION_MULTIPLIER;

    private float lastDimensionScale = 1.0F;

    private final Set<Long> forcedChunks = new HashSet<>();
    private int lastForcedChunkX = Integer.MIN_VALUE;
    private int lastForcedChunkZ = Integer.MIN_VALUE;
    private int lastForcedChunkRadius = -1;

    public BlackHoleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean canManifestAt(ServerLevel level, BlockPos pos, int contaminationIndex) {
        return contaminationIndex >= MIN_CONTAMINATION_TO_FORM
                && sampleNearbyAirSporeDensity(level, pos) >= MIN_AIR_SPORE_DENSITY_TO_FORM;
    }

    private static int sampleNearbyAirSporeDensity(ServerLevel level, BlockPos pos) {
        int density = 0;
        Vec3 center = Vec3.atCenterOf(pos);
        AABB scanBox = new AABB(pos).inflate(AIR_SPORE_SCAN_RADIUS);

        for (DarkMatterSporeEntity spore : level.getEntitiesOfClass(DarkMatterSporeEntity.class, scanBox)) {
            double dist = Math.sqrt(spore.position().distanceToSqr(center));
            double falloff = Math.max(0.15D, 1.0D - (dist / AIR_SPORE_SCAN_RADIUS));
            density += Math.max(1, Mth.floor(6.0D * falloff));
        }

        return density;
    }

    public void setNeutralMode(boolean neutralMode) {
        this.neutralMode = neutralMode;
        this.entityData.set(NEUTRAL_MODE, neutralMode);

        if (neutralMode) {
            setCollapsePrimed(false);
            collapseCountdown = -1;
            autonomousCollapsePrimed = false;
            emergencyPlayerDeathDetonation = false;
            emergencyProximityDetonation = false;
            emergencyExplosionMultiplier = 1.0D;
        }
    }

    public void setExplosionSizeMultiplier(double multiplier) {
        this.explosionSizeMultiplier = Mth.clamp(multiplier, 0.15D, 4.0D);
    }

    public void setPlayerDeathExplosionMultiplier(double multiplier) {
        this.playerDeathExplosionMultiplier = Mth.clamp(multiplier, 1.0D, 8.0D);
    }

    public void setProximityExplosionMultiplier(double multiplier) {
        this.proximityExplosionMultiplier = Mth.clamp(multiplier, 1.0D, 8.0D);
    }

    public void setTriggerPercent(int percent) {
        this.triggerPercent = Mth.clamp(percent, MIN_TRIGGER_PERCENT, 100);
        this.triggerCharge = (BASE_CHARGE_CAP * this.triggerPercent) / 100.0F;
    }

    public boolean isManifested() {
        return manifested;
    }

    public int getAirSporeDensity() {
        return airSporeDensity;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            updateDimensionsIfNeeded();
            spawnParticles();
            return;
        }

        age++;
        ensureTriggerInitialized();

        if (stability <= 0.0F) {
            releaseForcedChunks();
            this.discard();
            return;
        }

        if (age % 4 == 0) {
            refreshAirSporeDensity();
        }

        if (age % 10 == 0) {
            refreshLocalDarkMatterMass();
            if (manifested && !neutralMode) {
                absorbLocalDarkMatterMass();
            }
        }

        if (!manifested) {
            tickFormationPhase();
            this.entityData.set(GROWTH_PROGRESS, getVisualProgress());
            this.entityData.set(NEUTRAL_MODE, neutralMode);
            this.entityData.set(COLLAPSE_PRIMED, false);
            this.entityData.set(MANIFESTED, false);
            updateDimensionsIfNeeded();
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            ensureForcedChunks(serverLevel);
        }

        if (age % 2 == 0) {
            absorbAmbientEnergy();
        }

        if (!neutralMode && contamination > OVERCHARGE_START && age % 5 == 0) {
            accelerateOvercharge();
        }

        this.entityData.set(GROWTH_PROGRESS, getVisualProgress());
        this.entityData.set(NEUTRAL_MODE, neutralMode);
        this.entityData.set(MANIFESTED, true);
        updateDimensionsIfNeeded();

        pullEntities();

        if (!neutralMode && (emergencyPlayerDeathDetonation || emergencyProximityDetonation) && !exploded) {
            catastrophicExplosion();
            return;
        }

        if (!neutralMode && updateCollapsePrimingAndMaybeExplode()) {
            return;
        }

        if (!neutralMode) {
            consumeBlocks();
        }

        if (age % 2 == 0) {
            infectArea();
        }

        int burstInterval = neutralMode ? NEUTRAL_BURST_INTERVAL : NORMAL_BURST_INTERVAL;
        if (age % burstInterval == 0) {
            launchInfectionBurst();
        }

        if (age % 5 == 0) {
            spawnInfectionStorm();
        }

        if (age % 16 == 0) {
            level().playSound(
                    null,
                    blockPosition(),
                    ModSounds.DARK_PULSE.get(),
                    SoundSource.BLOCKS,
                    neutralMode ? 1.2F : 1.8F + (getGrowthProgress() * 2.8F),
                    neutralMode ? 0.95F : 0.8F - (getGrowthProgress() * 0.45F)
            );
        }

        if (!neutralMode && collapsePrimed && age % 8 == 0) {
            level().playSound(
                    null,
                    blockPosition(),
                    ModSounds.CLEAR_HUM.get(),
                    SoundSource.BLOCKS,
                    0.8F + (getGrowthProgress() * 1.2F),
                    0.55F
            );
        }
    }

    private void tickFormationPhase() {
        if (age % 2 == 0) {
            absorbAmbientEnergy();
        }

        boolean enoughContamination = contamination >= MIN_CONTAMINATION_TO_FORM;
        boolean enoughAirSpores = airSporeDensity >= MIN_AIR_SPORE_DENSITY_TO_FORM;

        if (enoughContamination && enoughAirSpores) {
            int gain = 3 + Math.min(6, airSporeDensity / 14);
            formationProgress = Math.min(FORMATION_REQUIRED_PROGRESS, formationProgress + gain);
        } else {
            int decay = enoughContamination ? 2 : 5;
            formationProgress = Math.max(0, formationProgress - decay);
        }

        if (age % 5 == 0) {
            spawnInfectionStorm();
        }

        if (age % 8 == 0) {
            launchInfectionBurst();
        }

        if (formationProgress >= FORMATION_REQUIRED_PROGRESS) {
            manifested = true;
            this.entityData.set(MANIFESTED, true);
            collapseCharge = Math.max(collapseCharge, Math.min(triggerCharge * 0.65F, contamination));
            return;
        }

        if (age >= FORMATION_TIMEOUT_TICKS && formationProgress <= 0) {
            this.discard();
        }
    }

    private void refreshAirSporeDensity() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            airSporeDensity = 0;
            return;
        }

        airSporeDensity = sampleNearbyAirSporeDensity(serverLevel, blockPosition());
    }

    private void refreshLocalDarkMatterMass() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            localDarkMatterMass = 0;
            return;
        }

        int radius = manifested ? 12 + Mth.floor(getGrowthProgress() * 8.0F) : 8;
        int vertical = manifested ? 8 : 6;
        MutableBlockPos mutable = new MutableBlockPos();
        int mass = 0;
        BlockPos center = blockPosition();

        for (int x = -radius; x <= radius; x += 2) {
            for (int z = -radius; z <= radius; z += 2) {
                for (int y = -vertical; y <= vertical; y += 2) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                    BlockState state = serverLevel.getBlockState(mutable);
                    FluidState fluidState = serverLevel.getFluidState(mutable);

                    if (state.is(ModBlocks.DARK_MATTER_BLOCK.get())) {
                        mass += 6;
                    } else if (state.is(ModBlocks.CORRUPTED_SOIL.get())) {
                        mass += 2;
                    } else if (state.is(ModBlocks.INFECTION_GROWTH.get())) {
                        mass += 3;
                    }

                    if (fluidState.getType().isSame(ModFluids.DARK_MATTER.get())) {
                        mass += 5;
                    }
                }
            }
        }

        mass += Math.max(0, airSporeDensity / 2);
        localDarkMatterMass = mass;
    }

    private void absorbLocalDarkMatterMass() {
        if (localDarkMatterMass <= 0) {
            return;
        }

        int bonus = Math.max(0, (localDarkMatterMass - 48) / 8);
        if (bonus > 0) {
            addContamination(Math.min(90, bonus));
        }
    }

    private void ensureTriggerInitialized() {
        if (triggerPercent >= 0 && triggerCharge >= 0.0F) {
            return;
        }

        triggerPercent = level().random.nextInt(MAX_TRIGGER_PERCENT - MIN_TRIGGER_PERCENT + 1) + MIN_TRIGGER_PERCENT;
        triggerCharge = (BASE_CHARGE_CAP * triggerPercent) / 100.0F;
    }

    private boolean isAutonomousCollapseReady() {
        return !neutralMode
                && manifested
                && (
                contamination >= AUTONOMOUS_COLLAPSE_CONTAMINATION
                        || localDarkMatterMass >= AUTONOMOUS_COLLAPSE_LOCAL_MASS
                        || collapseCharge >= Math.max(triggerCharge, BASE_CHARGE_CAP * AUTONOMOUS_COLLAPSE_CHARGE_FACTOR)
        );
    }

    private boolean updateCollapsePrimingAndMaybeExplode() {
        if (neutralMode || exploded || !manifested) {
            setCollapsePrimed(false);
            collapseCountdown = -1;
            autonomousCollapsePrimed = false;
            return false;
        }

        boolean playerThresholdReached = collapseCharge >= triggerCharge;
        boolean autonomousReady = isAutonomousCollapseReady();

        if (!playerThresholdReached && !autonomousReady) {
            setCollapsePrimed(false);
            collapseCountdown = -1;
            autonomousCollapsePrimed = false;
            return false;
        }

        boolean armingPlayerNearby = hasUnsafePlayersWithin(PLAYER_ARM_RADIUS);
        boolean noUnsafePlayersNearby = !hasUnsafePlayersWithin(PLAYER_ESCAPE_RADIUS);

        if (!collapsePrimed) {
            if (autonomousReady) {
                autonomousCollapsePrimed = true;
                setCollapsePrimed(true);
                collapseCountdown = AUTONOMOUS_COLLAPSE_WARNING_TICKS;
            } else if (armingPlayerNearby) {
                autonomousCollapsePrimed = false;
                setCollapsePrimed(true);
                collapseCountdown = COLLAPSE_WARNING_TICKS;
            }
            return false;
        }

        if (!autonomousCollapsePrimed && noUnsafePlayersNearby) {
            setCollapsePrimed(false);
            collapseCountdown = -1;
            collapseCharge = Math.max(triggerCharge * 0.82F, collapseCharge - 30.0F);
            return false;
        }

        if (autonomousCollapsePrimed && !autonomousReady && !playerThresholdReached) {
            setCollapsePrimed(false);
            collapseCountdown = -1;
            autonomousCollapsePrimed = false;
            return false;
        }

        if (collapseCountdown > 0) {
            int countdownStep = 1;

            if (autonomousCollapsePrimed) {
                countdownStep = contamination >= AUTONOMOUS_ACCELERATION_CONTAMINATION ? 3 : 2;
                if (localDarkMatterMass >= AUTONOMOUS_COLLAPSE_LOCAL_MASS + 80) {
                    countdownStep++;
                }
            }

            collapseCountdown -= countdownStep;
        }

        if (collapseCountdown <= 0) {
            catastrophicExplosion();
            return true;
        }

        return false;
    }

    private void setCollapsePrimed(boolean primed) {
        this.collapsePrimed = primed;
        this.entityData.set(COLLAPSE_PRIMED, primed);
    }

    private boolean hasUnsafePlayersWithin(double radius) {
        Vec3 center = new Vec3(getX(), getY() + (getBbHeight() * 0.5D), getZ());
        double radiusSq = radius * radius;

        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius + 2.0D))) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            Vec3 targetCenter = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
            if (targetCenter.distanceToSqr(center) <= radiusSq) {
                return true;
            }
        }

        return false;
    }

    private void absorbAmbientEnergy() {
        int sampleRadius = manifested
                ? 3 + Mth.floor(getGrowthProgress() * 6.0F)
                : 2 + Math.min(6, formationProgress / 20);

        int samples = manifested
                ? 8 + Mth.floor(getGrowthProgress() * 20.0F)
                : 6 + Math.min(12, formationProgress / 10);

        int gained = 0;
        BlockPos center = blockPosition();

        for (int i = 0; i < samples; i++) {
            BlockPos target = center.offset(
                    level().random.nextInt(sampleRadius * 2 + 1) - sampleRadius,
                    level().random.nextInt(sampleRadius * 2 + 1) - sampleRadius,
                    level().random.nextInt(sampleRadius * 2 + 1) - sampleRadius
            );

            BlockState state = level().getBlockState(target);

            if (!state.isAir()) {
                gained += state.getFluidState().isEmpty() ? 1 : 2;
            } else if (!level().getFluidState(target).isEmpty()) {
                gained += 2;
            }
        }

        if (!manifested && airSporeDensity > 0) {
            gained += Math.min(10, airSporeDensity / 8);
        }

        addContamination(gained);
    }

    private void addContamination(int amount) {
        if (amount <= 0) {
            return;
        }

        contamination += amount;
        collapseCharge += amount;

        if (manifested && !neutralMode && contamination > OVERCHARGE_START) {
            float overflow = contamination - OVERCHARGE_START;
            collapseCharge += 12.0F + (overflow * 0.035F);
        }

        collapseCharge = Math.min(collapseCharge, BASE_CHARGE_CAP * 6.0F);
    }

    private void accelerateOvercharge() {
        float overflow = contamination - OVERCHARGE_START;
        float bonus = 5.0F + (overflow * 0.04F) + (getGrowthProgress() * 12.0F);
        collapseCharge = Math.min(collapseCharge + bonus, BASE_CHARGE_CAP * 6.0F);
    }

    private float getGrowthProgress() {
        return Mth.clamp(collapseCharge / (float) BASE_CHARGE_CAP, 0.0F, 1.0F);
    }

    private float getVisualProgress() {
        if (manifested) {
            return getGrowthProgress();
        }

        return Mth.clamp((formationProgress / (float) FORMATION_REQUIRED_PROGRESS) * 0.35F, 0.0F, 0.35F);
    }

    private float getCurrentScale() {
        return 1.0F + (this.entityData.get(GROWTH_PROGRESS) * (MAX_SCALE - 1.0F));
    }

    private int getInfectionRadius() {
        int overflowBonus = Math.min(18, contamination / 180);
        return 8 + Mth.floor(getGrowthProgress() * 24.0F) + overflowBonus;
    }

    private double getPullRadius() {
        return neutralMode
                ? 8.0D + (getGrowthProgress() * 18.0D)
                : 12.0D + (getGrowthProgress() * 40.0D);
    }

    private double getEventHorizonRadius() {
        return neutralMode
                ? 0.0D
                : 2.2D + (getGrowthProgress() * 7.0D);
    }

    private int getConsumeRadius() {
        return 2 + Mth.floor(getGrowthProgress() * 8.0F);
    }

    private int getAbsorptionLimitPerCycle() {
        return 3 + Mth.floor(getGrowthProgress() * 10.0F);
    }

    private int getConsumeAttemptsPerCycle() {
        return 12 + Mth.floor(getGrowthProgress() * 80.0F);
    }

    private void updateDimensionsIfNeeded() {
        float currentScale = getCurrentScale();
        if (Math.abs(currentScale - lastDimensionScale) >= 0.30F) {
            lastDimensionScale = currentScale;
            refreshDimensions();
        }
    }

    private void ensureForcedChunks(ServerLevel serverLevel) {
        int centerChunkX = blockPosition().getX() >> 4;
        int centerChunkZ = blockPosition().getZ() >> 4;
        int blockRadius = Math.max(getInfectionRadius() + 24, 32);
        int chunkRadius = Mth.clamp(Mth.ceil(blockRadius / 16.0F), 1, 6);

        if (centerChunkX == lastForcedChunkX
                && centerChunkZ == lastForcedChunkZ
                && chunkRadius == lastForcedChunkRadius
                && !forcedChunks.isEmpty()) {
            return;
        }

        releaseForcedChunks();

        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                serverLevel.setChunkForced(cx, cz, true);
                forcedChunks.add(ChunkPos.asLong(cx, cz));
            }
        }

        lastForcedChunkX = centerChunkX;
        lastForcedChunkZ = centerChunkZ;
        lastForcedChunkRadius = chunkRadius;
    }

    private void ensureForcedAreaForRadius(ServerLevel serverLevel, double radiusInBlocks) {
        int centerChunkX = blockPosition().getX() >> 4;
        int centerChunkZ = blockPosition().getZ() >> 4;
        int chunkRadius = Mth.clamp(Mth.ceil((float) radiusInBlocks / 16.0F) + 1, 1, 10);

        if (centerChunkX == lastForcedChunkX
                && centerChunkZ == lastForcedChunkZ
                && chunkRadius <= lastForcedChunkRadius
                && !forcedChunks.isEmpty()) {
            return;
        }

        releaseForcedChunks();

        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                serverLevel.setChunkForced(cx, cz, true);
                forcedChunks.add(ChunkPos.asLong(cx, cz));
            }
        }

        lastForcedChunkX = centerChunkX;
        lastForcedChunkZ = centerChunkZ;
        lastForcedChunkRadius = chunkRadius;
    }

    private void releaseForcedChunks() {
        if (!(level() instanceof ServerLevel serverLevel) || forcedChunks.isEmpty()) {
            return;
        }

        for (long chunkKey : forcedChunks) {
            ChunkPos pos = new ChunkPos(chunkKey);
            serverLevel.setChunkForced(pos.x, pos.z, false);
        }

        forcedChunks.clear();
        lastForcedChunkX = Integer.MIN_VALUE;
        lastForcedChunkZ = Integer.MIN_VALUE;
        lastForcedChunkRadius = -1;
    }

    private void pullEntities() {
        if (!manifested) {
            return;
        }

        double radius = getPullRadius();
        double eventHorizon = getEventHorizonRadius();
        float growth = getGrowthProgress();
        Vec3 center = new Vec3(getX(), getY() + (getBbHeight() * 0.5D), getZ());

        boolean playerKilledThisTick = false;
        boolean proximityDetonationThisTick = false;

        for (Entity entity : level().getEntitiesOfClass(Entity.class, getBoundingBox().inflate(radius))) {
            if (entity == this) {
                continue;
            }

            Vec3 targetCenter = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            Vec3 delta = center.subtract(targetCenter);
            double distSq = delta.lengthSqr();

            if (distSq < 0.0001D) {
                continue;
            }

            double dist = Math.sqrt(distSq);
            Vec3 direction = delta.scale(1.0D / dist);

            double maxForce = neutralMode
                    ? 0.06D + (growth * 0.12D)
                    : 0.12D + (growth * 0.30D);

            double baseForce = neutralMode
                    ? (0.25D + (growth * 1.10D)) / Math.max(1.0D, distSq / 3.0D)
                    : (0.60D + (growth * 2.30D)) / Math.max(1.0D, distSq / 3.0D);

            double force = Math.min(maxForce, baseForce);

            if (entity instanceof FallingBlockEntity) {
                force *= neutralMode ? 1.5D : 2.8D;
            }

            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(force)));
            entity.hurtMarked = true;

            if (!neutralMode && entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
                if (dist <= PROXIMITY_CATASTROPHIC_RADIUS) {
                    proximityDetonationThisTick = true;
                }
            }

            if (!neutralMode && dist <= eventHorizon) {
                if (entity instanceof LivingEntity living) {
                    float damage = 10.0F + (growth * 26.0F);
                    boolean wasAlive = living.isAlive();

                    living.hurt(damageSources().magic(), damage);
                    addContamination(10);

                    if (wasAlive && !living.isAlive() && entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
                        playerKilledThisTick = true;
                    }
                } else if (entity instanceof FallingBlockEntity) {
                    entity.discard();
                    addContamination(8);

                    level().playSound(
                            null,
                            blockPosition(),
                            ModSounds.CLEAR_HUM.get(),
                            SoundSource.BLOCKS,
                            0.6F,
                            0.45F
                    );
                } else if (!(entity instanceof BlackHoleEntity)) {
                    entity.discard();
                    addContamination(5);
                }
            }
        }

        if (!neutralMode) {
            if (playerKilledThisTick) {
                armEmergencyPlayerDeathExplosion();
            } else if (proximityDetonationThisTick) {
                armEmergencyProximityExplosion();
            }
        }
    }

    private void armEmergencyPlayerDeathExplosion() {
        emergencyPlayerDeathDetonation = true;
        emergencyProximityDetonation = false;
        emergencyExplosionMultiplier = playerDeathExplosionMultiplier;
        collapseCharge = Math.max(collapseCharge, triggerCharge);
    }

    private void armEmergencyProximityExplosion() {
        emergencyProximityDetonation = true;
        emergencyPlayerDeathDetonation = false;
        emergencyExplosionMultiplier = proximityExplosionMultiplier;
        collapseCharge = Math.max(collapseCharge, triggerCharge);
    }

    private void consumeBlocks() {
        if (level().isClientSide || neutralMode || !manifested) {
            return;
        }

        int radius = getConsumeRadius();
        int attempts = getConsumeAttemptsPerCycle();
        int absorbLimit = getAbsorptionLimitPerCycle();
        BlockPos center = blockPosition();

        int absorbedThisCycle = 0;

        for (int i = 0; i < attempts && absorbedThisCycle < absorbLimit; i++) {
            int x = center.getX() + level().random.nextInt(radius * 2 + 1) - radius;
            int y = center.getY() + level().random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + level().random.nextInt(radius * 2 + 1) - radius;

            BlockPos target = new BlockPos(x, y, z);

            if (target.distSqr(center) > (double) (radius * radius)) {
                continue;
            }

            BlockState state = level().getBlockState(target);
            if (!canConsume(state, target)) {
                continue;
            }

            FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level(), target, state);
            fallingBlock.setNoGravity(true);
            level().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());

            absorbedThisCycle++;
            addContamination(8);
        }
    }

    private boolean canConsume(BlockState state, BlockPos pos) {
        return !state.isAir()
                && state.getDestroySpeed(level(), pos) >= 0.0F
                && !state.is(Blocks.BEDROCK)
                && !state.is(Blocks.END_PORTAL_FRAME)
                && !state.is(Blocks.END_PORTAL)
                && !state.is(Blocks.END_GATEWAY)
                && !state.is(Blocks.NETHER_PORTAL)
                && !state.is(Blocks.BARRIER)
                && !state.is(Blocks.COMMAND_BLOCK)
                && !state.is(Blocks.CHAIN_COMMAND_BLOCK)
                && !state.is(Blocks.REPEATING_COMMAND_BLOCK)
                && !state.is(Blocks.STRUCTURE_BLOCK)
                && !state.is(Blocks.STRUCTURE_VOID)
                && !state.is(Blocks.JIGSAW)
                && !state.is(ModBlocks.CLEAR_MATTER_BLOCK.get());
    }

    private void infectArea() {
        if (!(level() instanceof ServerLevel serverLevel) || !manifested) {
            return;
        }

        float growth = getGrowthProgress();
        int radius = getInfectionRadius();
        int attempts = neutralMode
                ? 26 + Mth.floor(growth * 140.0F)
                : 20 + Mth.floor(growth * 90.0F);

        BlockPos center = blockPosition();

        for (int i = 0; i < attempts; i++) {
            int x = center.getX() + level().random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + level().random.nextInt(radius * 2 + 1) - radius;

            infectSurfaceColumnDeep(serverLevel, new BlockPos(x, center.getY(), z), !neutralMode);

            if (!neutralMode && growth >= 0.65F && level().random.nextFloat() < 0.28F) {
                infectSurfaceColumnDeep(serverLevel, new BlockPos(x + level().random.nextInt(3) - 1, center.getY(), z + level().random.nextInt(3) - 1), true);
            }
        }
    }

    private void infectSurfaceColumnDeep(ServerLevel serverLevel, BlockPos columnBase, boolean aggressive) {
        BlockPos surfacePos = findGroundInColumn(serverLevel, columnBase);
        if (surfacePos == null) {
            return;
        }

        BlockState surfaceState = serverLevel.getBlockState(surfacePos);
        if (surfaceState.is(ModBlocks.CLEAR_MATTER_BLOCK.get()) || surfaceState.is(Blocks.BEDROCK)) {
            return;
        }

        int minY = Math.max(serverLevel.getMinBuildHeight() + 1, surfacePos.getY() - SURFACE_CORRUPTION_DEPTH);
        int maxY = Math.min(serverLevel.getMaxBuildHeight() - 1, surfacePos.getY() + SURFACE_CORRUPTION_HEIGHT);

        MutableBlockPos mutable = new MutableBlockPos();
        boolean infectedAnything = false;

        for (int y = surfacePos.getY(); y >= minY; y--) {
            mutable.set(surfacePos.getX(), y, surfacePos.getZ());
            if (corruptBelowSurfaceBlock(serverLevel, mutable, y == surfacePos.getY(), aggressive)) {
                infectedAnything = true;
            }
        }

        for (int y = surfacePos.getY() + 1; y <= maxY; y++) {
            mutable.set(surfacePos.getX(), y, surfacePos.getZ());
            if (corruptAboveSurfaceBlock(serverLevel, mutable, aggressive)) {
                infectedAnything = true;
            }
        }

        if (infectedAnything) {
            sproutSurfaceGrowth(serverLevel, surfacePos);
        }
    }

    private boolean corruptBelowSurfaceBlock(ServerLevel serverLevel, BlockPos pos, boolean surfaceLayer, boolean aggressive) {
        BlockState state = serverLevel.getBlockState(pos);

        if (state.is(ModBlocks.CLEAR_MATTER_BLOCK.get()) || state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)) {
            return false;
        }

        if (state.is(ModBlocks.CORRUPTED_SOIL.get())
                || state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                || state.is(ModBlocks.INFECTION_GROWTH.get())) {
            return false;
        }

        if (!serverLevel.getFluidState(pos).isEmpty()) {
            serverLevel.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
            addContamination(4);
            return true;
        }

        if (state.isAir()) {
            if (aggressive) {
                serverLevel.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
                addContamination(2);
                return true;
            }
            return false;
        }

        if (surfaceLayer && isSurfaceSoilLike(state)) {
            serverLevel.setBlockAndUpdate(pos, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
            addContamination(3);
            return true;
        }

        if (isDeepCorruptibleBlock(serverLevel, pos, state)) {
            serverLevel.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
            addContamination(4);
            return true;
        }

        return false;
    }

    private boolean corruptAboveSurfaceBlock(ServerLevel serverLevel, BlockPos pos, boolean aggressive) {
        BlockState state = serverLevel.getBlockState(pos);

        if (state.is(ModBlocks.CLEAR_MATTER_BLOCK.get()) || state.is(Blocks.BEDROCK)) {
            return false;
        }

        if (state.is(ModBlocks.CORRUPTED_SOIL.get())
                || state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                || state.is(ModBlocks.INFECTION_GROWTH.get())) {
            return false;
        }

        if (!serverLevel.getFluidState(pos).isEmpty()) {
            serverLevel.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
            addContamination(3);
            return true;
        }

        if (state.isAir()) {
            return false;
        }

        if (state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES)
                || hasRegistryPath(state, "grass")
                || hasRegistryPath(state, "fern")
                || hasRegistryPath(state, "vine")
                || hasRegistryPath(state, "flower")
                || hasRegistryPath(state, "plant")
                || hasRegistryPath(state, "crop")
                || aggressive) {
            if (canConsume(state, pos)) {
                serverLevel.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
                addContamination(3);
                return true;
            }
        }

        return false;
    }

    private void sproutSurfaceGrowth(ServerLevel serverLevel, BlockPos surfacePos) {
        BlockState baseState = serverLevel.getBlockState(surfacePos);
        if (!baseState.is(ModBlocks.CORRUPTED_SOIL.get()) && !baseState.is(ModBlocks.DARK_MATTER_BLOCK.get())) {
            return;
        }

        BlockPos first = surfacePos.above();
        if (!serverLevel.getBlockState(first).isAir() || !serverLevel.getFluidState(first).isEmpty()) {
            return;
        }

        if (hasNearbyInfectionGrowth(serverLevel, first)) {
            return;
        }

        serverLevel.setBlockAndUpdate(first, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
        addContamination(1);

        BlockPos second = first.above();
        if (serverLevel.getBlockState(second).isAir()
                && serverLevel.getFluidState(second).isEmpty()
                && serverLevel.random.nextFloat() < 0.35F) {
            serverLevel.setBlockAndUpdate(second, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
            addContamination(1);
        }

        BlockPos third = second.above();
        if (serverLevel.getBlockState(third).isAir()
                && serverLevel.getFluidState(third).isEmpty()
                && serverLevel.random.nextFloat() < 0.18F) {
            serverLevel.setBlockAndUpdate(third, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
            addContamination(1);
        }
    }

    private boolean isSurfaceSoilLike(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY);
    }

    private boolean isDeepCorruptibleBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.is(ModBlocks.CLEAR_MATTER_BLOCK.get())
                || state.is(ModBlocks.CORRUPTED_SOIL.get())
                || state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                || state.is(ModBlocks.INFECTION_GROWTH.get())) {
            return false;
        }

        if (!canConsume(state, pos)) {
            return false;
        }

        if (state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.END_STONE)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || hasRegistryPath(state, "_ore")) {
            return true;
        }

        return state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    private boolean hasRegistryPath(BlockState state, String expectedFragment) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) {
            return false;
        }
        return key.getPath().contains(expectedFragment);
    }

    private void launchInfectionBurst() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float growth = manifested ? getGrowthProgress() : Math.max(0.15F, getVisualProgress());

        int visualParticleCount = neutralMode
                ? 1400 + Mth.floor(growth * 1800.0F)
                : 420 + Mth.floor(growth * 700.0F);

        if (!manifested) {
            visualParticleCount = 160 + Mth.floor((airSporeDensity / 4.0F) * 10.0F);
        }

        int infectionLandingCount = neutralMode
                ? 180 + Mth.floor(growth * 220.0F)
                : 70 + Mth.floor(growth * 120.0F);

        if (!manifested) {
            infectionLandingCount = 24 + Math.min(80, airSporeDensity);
        }

        double spread = neutralMode
                ? 2.0D + (growth * 4.0D)
                : 1.2D + (growth * 2.0D);

        if (!manifested) {
            spread = 1.0D + Math.min(3.0D, airSporeDensity / 20.0D);
        }

        double maxDistance = neutralMode
                ? 20.0D + (growth * 34.0D)
                : 10.0D + (growth * 18.0D);

        if (!manifested) {
            maxDistance = 6.0D + Math.min(14.0D, airSporeDensity / 3.0D);
        }

        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                getX(),
                getY() + 0.6D,
                getZ(),
                visualParticleCount,
                spread,
                spread,
                spread,
                0.26D
        );

        serverLevel.sendParticles(
                ParticleTypes.PORTAL,
                getX(),
                getY() + 0.6D,
                getZ(),
                Math.max(1, visualParticleCount / 3),
                spread * 0.8D,
                spread * 0.8D,
                spread * 0.8D,
                0.12D
        );

        serverLevel.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                getX(),
                getY() + 0.6D,
                getZ(),
                Math.max(1, visualParticleCount / 4),
                spread * 0.7D,
                spread * 0.7D,
                spread * 0.7D,
                0.08D
        );

        for (int i = 0; i < infectionLandingCount; i++) {
            double angle = serverLevel.random.nextDouble() * (Math.PI * 2.0D);
            double distance = 4.0D + (serverLevel.random.nextDouble() * maxDistance);

            int targetX = Mth.floor(getX() + Math.cos(angle) * distance);
            int targetZ = Mth.floor(getZ() + Math.sin(angle) * distance);

            infectLandingPoint(serverLevel, new BlockPos(targetX, blockPosition().getY(), targetZ));
        }
    }

    private void infectLandingPoint(ServerLevel serverLevel, BlockPos columnBase) {
        infectSurfaceColumnDeep(serverLevel, columnBase, !neutralMode);
    }

    private BlockPos findGroundInColumn(ServerLevel serverLevel, BlockPos columnBase) {
        BlockPos surface = serverLevel.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING,
                new BlockPos(columnBase.getX(), 0, columnBase.getZ())
        );

        MutableBlockPos mutable = new MutableBlockPos(surface.getX(), surface.getY(), surface.getZ());

        if (serverLevel.getBlockState(mutable).isAir()) {
            mutable.move(0, -1, 0);
        }

        int minY = Math.max(serverLevel.getMinBuildHeight() + 1, mutable.getY() - GROUND_SCAN_DEPTH);

        while (mutable.getY() >= minY) {
            BlockState state = serverLevel.getBlockState(mutable);

            if (state.isSolidRender(serverLevel, mutable)
                    && !state.is(ModBlocks.CLEAR_MATTER_BLOCK.get())
                    && !state.is(Blocks.BEDROCK)) {
                return mutable.immutable();
            }

            mutable.move(0, -1, 0);
        }

        return null;
    }

    private boolean hasNearbyInfectionGrowth(ServerLevel serverLevel, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                if (serverLevel.getBlockState(pos.offset(dx, 0, dz)).is(ModBlocks.INFECTION_GROWTH.get())) {
                    return true;
                }
            }
        }

        return false;
    }

    public void stabilize(float amount) {
        this.stability -= amount;
        this.collapseCharge = Math.max(0.0F, this.collapseCharge - (amount * 40.0F));
        this.contamination = Math.max(0, this.contamination - Mth.floor(amount * 12.0F));

        if (!isAutonomousCollapseReady() && this.collapseCharge < this.triggerCharge) {
            autonomousCollapsePrimed = false;
            setCollapsePrimed(false);
            collapseCountdown = -1;
        }

        this.entityData.set(GROWTH_PROGRESS, getVisualProgress());
        updateDimensionsIfNeeded();
    }

    private void catastrophicExplosion() {
        if (exploded || level().isClientSide || neutralMode || !manifested) {
            return;
        }

        exploded = true;

        if (!(level() instanceof ServerLevel serverLevel)) {
            this.discard();
            return;
        }

        double baseCraterRadius = (4.5D + (Math.sqrt(Math.max(1.0D, collapseCharge)) * 0.30D)) * explosionSizeMultiplier;
        double maxAllowedRadius = MAX_NORMAL_CRATER_RADIUS;

        if (emergencyPlayerDeathDetonation || emergencyProximityDetonation) {
            baseCraterRadius *= emergencyExplosionMultiplier;
            maxAllowedRadius = MAX_FORCED_CRATER_RADIUS;
        }

        if (autonomousCollapsePrimed) {
            baseCraterRadius *= 1.15D;
        }

        double chaosFactor = 0.92D + (serverLevel.random.nextDouble() * 0.16D);
        double craterRadius = Mth.clamp(baseCraterRadius * chaosFactor, MIN_CRATER_RADIUS, maxAllowedRadius);

        double innerCoreRadius = craterRadius * (emergencyPlayerDeathDetonation || emergencyProximityDetonation ? 0.36D : 0.28D);
        int maxDepth = Mth.floor(10.0D + (craterRadius * 0.80D));

        ensureForcedAreaForRadius(serverLevel, craterRadius + 24.0D);

        float vanillaBlastPower = Mth.clamp(6.0F + (float) (craterRadius * 0.22D), 6.0F, 32.0F);

        BlockPos center = blockPosition();

        serverLevel.playSound(
                null,
                center,
                ModSounds.DARK_PULSE.get(),
                SoundSource.BLOCKS,
                6.0F,
                0.22F
        );

        serverLevel.explode(
                this,
                center.getX() + 0.5D,
                center.getY() + 0.5D,
                center.getZ() + 0.5D,
                vanillaBlastPower,
                true,
                Level.ExplosionInteraction.BLOCK
        );

        carveCircularCrater(serverLevel, center, craterRadius, innerCoreRadius, maxDepth);
        corruptCraterRing(serverLevel, center, craterRadius);
        consumeBlastVictims(serverLevel, center, craterRadius + 12.0D);

        releaseForcedChunks();
        this.discard();
    }

    private void carveCircularCrater(ServerLevel serverLevel, BlockPos center, double radius, double innerCoreRadius, int maxDepth) {
        int minY = serverLevel.getMinBuildHeight() + 1;
        int maxY = serverLevel.getMaxBuildHeight() - 1;
        MutableBlockPos mutable = new MutableBlockPos();

        for (int x = Mth.floor(center.getX() - radius); x <= Mth.floor(center.getX() + radius); x++) {
            for (int z = Mth.floor(center.getZ() - radius); z <= Mth.floor(center.getZ() + radius); z++) {
                double dx = (x + 0.5D) - (center.getX() + 0.5D);
                double dz = (z + 0.5D) - (center.getZ() + 0.5D);
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > radius) {
                    continue;
                }

                double normalized = 1.0D - (dist / radius);
                double bowl = normalized * normalized;
                int rimCut = 4 + Mth.floor(normalized * 7.0D);

                BlockPos surface = serverLevel.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING,
                        new BlockPos(x, 0, z)
                );

                int topY = Math.min(maxY, surface.getY() + rimCut);

                int localDepth = Math.max(
                        4,
                        Mth.floor((maxDepth * bowl) + (radius * 0.10D))
                );

                if (dist <= innerCoreRadius) {
                    double coreFactor = 1.0D - (dist / innerCoreRadius);
                    localDepth += Mth.floor(8.0D + (coreFactor * 18.0D));
                }

                int bottomY = Math.max(minY, surface.getY() - localDepth);

                for (int y = topY; y >= bottomY; y--) {
                    mutable.set(x, y, z);
                    BlockState state = serverLevel.getBlockState(mutable);

                    if (canConsume(state, mutable)) {
                        serverLevel.setBlock(mutable, Blocks.AIR.defaultBlockState(), 3);
                    }
                }

                if (dist <= innerCoreRadius) {
                    int shaftBottom = Math.max(minY, bottomY - 7);

                    for (int y = bottomY; y >= shaftBottom; y--) {
                        mutable.set(x, y, z);
                        BlockState state = serverLevel.getBlockState(mutable);

                        if (canConsume(state, mutable)) {
                            serverLevel.setBlock(mutable, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }

                    mutable.set(x, shaftBottom, z);
                    if (serverLevel.getBlockState(mutable).isAir()) {
                        serverLevel.setBlock(mutable, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void corruptCraterRing(ServerLevel serverLevel, BlockPos center, double radius) {
        int outerRadius = Mth.floor(radius + 10.0D);

        for (int i = 0; i < outerRadius * 16; i++) {
            int x = center.getX() + serverLevel.random.nextInt(outerRadius * 2 + 1) - outerRadius;
            int z = center.getZ() + serverLevel.random.nextInt(outerRadius * 2 + 1) - outerRadius;

            double dx = (x + 0.5D) - (center.getX() + 0.5D);
            double dz = (z + 0.5D) - (center.getZ() + 0.5D);
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist < radius * 0.65D || dist > outerRadius) {
                continue;
            }

            infectSurfaceColumnDeep(serverLevel, new BlockPos(x, center.getY(), z), true);
        }
    }

    private void consumeBlastVictims(ServerLevel serverLevel, BlockPos center, double radius) {
        for (Entity entity : serverLevel.getEntitiesOfClass(Entity.class, getBoundingBox().inflate(radius))) {
            if (entity == this) {
                continue;
            }

            double distSq = entity.position().distanceToSqr(
                    center.getX() + 0.5D,
                    center.getY() + 0.5D,
                    center.getZ() + 0.5D
            );

            if (distSq <= radius * radius) {
                if (entity instanceof LivingEntity living) {
                    float damage = 22.0F + (float) (radius * 0.75D);
                    living.hurt(damageSources().magic(), damage);
                } else {
                    entity.discard();
                }
            }
        }
    }

    private void spawnInfectionStorm() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float growth = manifested ? getGrowthProgress() : Math.max(0.15F, getVisualProgress());
        int count;

        if (!manifested) {
            count = 40 + Math.min(240, airSporeDensity * 3);
        } else {
            count = neutralMode
                    ? 160 + Mth.floor(growth * 260.0F)
                    : 40 + Mth.floor(growth * 140.0F);
        }

        double spread;
        if (!manifested) {
            spread = 2.0D + Math.min(8.0D, airSporeDensity / 6.0D);
        } else {
            spread = neutralMode
                    ? 5.0D + (growth * 18.0D)
                    : 2.0D + (growth * 10.0D);
        }

        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                getX(),
                getY() + 0.4D,
                getZ(),
                count,
                spread,
                spread,
                spread,
                !manifested ? 0.10D : (neutralMode ? 0.18D : 0.03D)
        );

        serverLevel.sendParticles(
                ParticleTypes.PORTAL,
                getX(),
                getY() + 0.4D,
                getZ(),
                Math.max(1, count / 2),
                spread * 0.8D,
                spread * 0.8D,
                spread * 0.8D,
                !manifested ? 0.08D : (neutralMode ? 0.14D : 0.10D)
        );

        serverLevel.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                getX(),
                getY() + 0.4D,
                getZ(),
                Math.max(1, count / 3),
                spread * 0.7D,
                spread * 0.7D,
                spread * 0.7D,
                !manifested ? 0.04D : (neutralMode ? 0.06D : 0.03D)
        );
    }

    private void spawnParticles() {
        if (!level().isClientSide) {
            return;
        }

        float growth = this.entityData.get(GROWTH_PROGRESS);
        boolean neutralVisual = this.entityData.get(NEUTRAL_MODE);
        boolean primedVisual = this.entityData.get(COLLAPSE_PRIMED);
        boolean manifestedVisual = this.entityData.get(MANIFESTED);

        double radius = manifestedVisual ? 2.0D + (growth * 12.0D) : 1.2D + (growth * 8.0D);
        int count = neutralVisual
                ? 100 + Mth.floor(growth * 260.0F)
                : 30 + Mth.floor(growth * 150.0F);

        if (!manifestedVisual) {
            count = 40 + Mth.floor(growth * 180.0F);
        }

        for (int i = 0; i < count; i++) {
            double theta = level().random.nextDouble() * 2.0D * Math.PI;
            double phi = Math.acos(2.0D * level().random.nextDouble() - 1.0D);

            double x = getX() + radius * Math.sin(phi) * Math.cos(theta);
            double y = getY() + radius * Math.sin(phi) * Math.sin(theta);
            double z = getZ() + radius * Math.cos(phi);

            double inwardBase = neutralVisual ? 0.04D : 0.08D;
            double inwardScale = neutralVisual ? 0.05D : 0.10D;

            if (!manifestedVisual) {
                inwardBase = 0.03D;
                inwardScale = 0.04D;
            }

            double vx = (getX() - x) * (inwardBase + growth * inwardScale);
            double vy = (getY() - y) * (inwardBase + growth * inwardScale);
            double vz = (getZ() - z) * (inwardBase + growth * inwardScale);

            level().addParticle(ParticleTypes.SQUID_INK, x, y, z, vx, vy, vz);

            if (i % 2 == 0) {
                level().addParticle(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
            }

            if (i % 4 == 0) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, vx, vy, vz);
            }

            if (neutralVisual && manifestedVisual && i % 3 == 0) {
                double outX = (level().random.nextDouble() - 0.5D) * (0.30D + growth * 0.60D);
                double outY = (level().random.nextDouble() - 0.2D) * (0.18D + growth * 0.25D);
                double outZ = (level().random.nextDouble() - 0.5D) * (0.30D + growth * 0.60D);

                level().addParticle(
                        ParticleTypes.SQUID_INK,
                        getX(),
                        getY() + 0.4D,
                        getZ(),
                        outX,
                        outY,
                        outZ
                );
            }

            if (primedVisual && manifestedVisual && i % 5 == 0) {
                level().addParticle(
                        ParticleTypes.SMOKE,
                        getX() + (level().random.nextDouble() - 0.5D) * (1.0D + growth * 3.0D),
                        getY() + (level().random.nextDouble() - 0.5D) * (1.0D + growth * 3.0D),
                        getZ() + (level().random.nextDouble() - 0.5D) * (1.0D + growth * 3.0D),
                        0.0D,
                        0.02D,
                        0.0D
                );
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseForcedChunks();
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(GROWTH_PROGRESS, 0.0F);
        this.entityData.define(NEUTRAL_MODE, false);
        this.entityData.define(COLLAPSE_PRIMED, false);
        this.entityData.define(MANIFESTED, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("age");
        stability = tag.getFloat("stability");
        exploded = tag.getBoolean("exploded");

        contamination = tag.getInt("contamination");
        collapseCharge = tag.getFloat("collapseCharge");
        localDarkMatterMass = tag.getInt("localDarkMatterMass");

        triggerPercent = tag.getInt("triggerPercent");
        triggerCharge = tag.getFloat("triggerCharge");

        neutralMode = tag.getBoolean("neutralMode");
        collapsePrimed = tag.getBoolean("collapsePrimed");
        autonomousCollapsePrimed = tag.getBoolean("autonomousCollapsePrimed");
        collapseCountdown = tag.getInt("collapseCountdown");

        manifested = tag.getBoolean("manifested");
        formationProgress = tag.getInt("formationProgress");
        airSporeDensity = tag.getInt("airSporeDensity");

        emergencyPlayerDeathDetonation = tag.getBoolean("emergencyPlayerDeathDetonation");
        emergencyProximityDetonation = tag.getBoolean("emergencyProximityDetonation");
        emergencyExplosionMultiplier = tag.getDouble("emergencyExplosionMultiplier");

        explosionSizeMultiplier = tag.contains("explosionSizeMultiplier")
                ? tag.getDouble("explosionSizeMultiplier")
                : DEFAULT_EXPLOSION_SIZE_MULTIPLIER;

        playerDeathExplosionMultiplier = tag.contains("playerDeathExplosionMultiplier")
                ? tag.getDouble("playerDeathExplosionMultiplier")
                : DEFAULT_PLAYER_DEATH_EXPLOSION_MULTIPLIER;

        proximityExplosionMultiplier = tag.contains("proximityExplosionMultiplier")
                ? tag.getDouble("proximityExplosionMultiplier")
                : DEFAULT_PROXIMITY_EXPLOSION_MULTIPLIER;

        if (triggerPercent < 0 || triggerCharge < 0.0F) {
            triggerPercent = -1;
            triggerCharge = -1.0F;
        }

        this.entityData.set(GROWTH_PROGRESS, getVisualProgress());
        this.entityData.set(NEUTRAL_MODE, neutralMode);
        this.entityData.set(COLLAPSE_PRIMED, collapsePrimed);
        this.entityData.set(MANIFESTED, manifested);
        updateDimensionsIfNeeded();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", age);
        tag.putFloat("stability", stability);
        tag.putBoolean("exploded", exploded);

        tag.putInt("contamination", contamination);
        tag.putFloat("collapseCharge", collapseCharge);
        tag.putInt("localDarkMatterMass", localDarkMatterMass);

        tag.putInt("triggerPercent", triggerPercent);
        tag.putFloat("triggerCharge", triggerCharge);

        tag.putBoolean("neutralMode", neutralMode);
        tag.putBoolean("collapsePrimed", collapsePrimed);
        tag.putBoolean("autonomousCollapsePrimed", autonomousCollapsePrimed);
        tag.putInt("collapseCountdown", collapseCountdown);

        tag.putBoolean("manifested", manifested);
        tag.putInt("formationProgress", formationProgress);
        tag.putInt("airSporeDensity", airSporeDensity);

        tag.putBoolean("emergencyPlayerDeathDetonation", emergencyPlayerDeathDetonation);
        tag.putBoolean("emergencyProximityDetonation", emergencyProximityDetonation);
        tag.putDouble("emergencyExplosionMultiplier", emergencyExplosionMultiplier);

        tag.putDouble("explosionSizeMultiplier", explosionSizeMultiplier);
        tag.putDouble("playerDeathExplosionMultiplier", playerDeathExplosionMultiplier);
        tag.putDouble("proximityExplosionMultiplier", proximityExplosionMultiplier);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float scale = 1.0F + (this.entityData.get(GROWTH_PROGRESS) * (MAX_SCALE - 1.0F));
        return EntityDimensions.scalable(scale, scale);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}