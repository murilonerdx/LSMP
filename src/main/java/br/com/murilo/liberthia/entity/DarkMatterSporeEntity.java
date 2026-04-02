package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DarkMatterSporeEntity extends Entity {

    private static final int DEFAULT_MAX_LIFE = 100;
    private static final int GROUND_SCAN_EXTRA_HEIGHT = 20;

    // Área total de solo infectado ao redor do ponto de impacto
    private static final int SOIL_INFECTION_RADIUS = 4;

    // A infecção vertical nunca passa de 3 blocos acima do chão
    private static final int MAX_GROWTH_HEIGHT = 3;

    // Precisa existir pelo menos 1 bloco vazio entre colunas de growth
    // Ex.: distância horizontal mínima = 2
    private static final int GROWTH_MIN_SPACING = 2;

    private int lifeTime = 0;
    private int maxLife = DEFAULT_MAX_LIFE;
    private Vec3 moveTarget;

    public DarkMatterSporeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setTarget(Vec3 target) {
        this.moveTarget = target;
        this.maxLife = (int) (position().distanceTo(target) * 2) + 20;
    }

    @Override
    public void tick() {
        super.tick();
        lifeTime++;

        if (level().isClientSide) {
            spawnParticles();
            return;
        }

        if (moveTowardsTargetAndSpread()) {
            return;
        }

        if (lifeTime > maxLife) {
            plantInfection();
            this.discard();
        }
    }

    private boolean moveTowardsTargetAndSpread() {
        if (moveTarget == null) {
            return false;
        }

        Vec3 currentPosition = position();

        if (currentPosition.distanceToSqr(moveTarget) < 4.0D) {
            plantInfection();
            this.discard();
            return true;
        }

        Vec3 dir = moveTarget.subtract(currentPosition).normalize().scale(0.8D);
        this.setPos(currentPosition.add(dir));
        return false;
    }

    private void plantInfection() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos center = blockPosition();
        List<BlockPos> infectedGroundPositions = new ArrayList<>();

        // 1) Infecta TODO o solo em uma área circular
        for (int dx = -SOIL_INFECTION_RADIUS; dx <= SOIL_INFECTION_RADIUS; dx++) {
            for (int dz = -SOIL_INFECTION_RADIUS; dz <= SOIL_INFECTION_RADIUS; dz++) {
                if ((dx * dx) + (dz * dz) > (SOIL_INFECTION_RADIUS * SOIL_INFECTION_RADIUS)) {
                    continue;
                }

                BlockPos columnBase = center.offset(dx, 0, dz);
                BlockPos groundPos = findGroundInColumn(serverLevel, columnBase);

                if (groundPos == null) {
                    continue;
                }

                serverLevel.setBlockAndUpdate(
                        groundPos,
                        ModBlocks.CORRUPTED_SOIL.get().defaultBlockState()
                );

                infectedGroundPositions.add(groundPos.immutable());
            }
        }

        // 2) Ordena para priorizar o centro da infecção
        infectedGroundPositions.sort(Comparator.comparingDouble(pos -> horizontalDistanceSq(pos, center)));

        // 3) Plante colunas de growth com espaçamento entre si
        List<BlockPos> growthBases = new ArrayList<>();

        for (BlockPos groundPos : infectedGroundPositions) {
            if (!canPlaceGrowthColumn(serverLevel, groundPos, growthBases)) {
                continue;
            }

            int growthHeight = calculateGrowthHeight(groundPos, center);

            if (growthHeight <= 0) {
                continue;
            }

            placeGrowthColumn(serverLevel, groundPos, growthHeight);
            growthBases.add(groundPos.immutable());
        }
    }

    private BlockPos findGroundInColumn(ServerLevel serverLevel, BlockPos columnBase) {
        int startY = Math.min(serverLevel.getMaxBuildHeight() - 1, columnBase.getY() + GROUND_SCAN_EXTRA_HEIGHT);
        int endY = serverLevel.getMinBuildHeight();

        for (int y = startY; y >= endY; y--) {
            BlockPos currentPos = new BlockPos(columnBase.getX(), y, columnBase.getZ());
            BlockState state = serverLevel.getBlockState(currentPos);

            if (isValidGround(serverLevel, currentPos, state)) {
                return currentPos;
            }
        }

        return null;
    }

    private boolean isValidGround(ServerLevel serverLevel, BlockPos pos, BlockState state) {
        if (!state.isSolidRender(serverLevel, pos)) {
            return false;
        }

        if (state.is(ModBlocks.CLEAR_MATTER_BLOCK.get())) {
            return false;
        }

        if (state.is(ModBlocks.INFECTION_GROWTH.get())) {
            return false;
        }

        return true;
    }

    private boolean canPlaceGrowthColumn(ServerLevel serverLevel, BlockPos groundPos, List<BlockPos> existingGrowthBases) {
        if (!serverLevel.getBlockState(groundPos).is(ModBlocks.CORRUPTED_SOIL.get())) {
            return false;
        }

        // Primeiro bloco acima do chão precisa estar livre
        if (!serverLevel.getBlockState(groundPos.above()).isAir()) {
            return false;
        }

        // Não deixa nascer growth colado em outro growth
        for (BlockPos existingBase : existingGrowthBases) {
            int dx = Math.abs(existingBase.getX() - groundPos.getX());
            int dz = Math.abs(existingBase.getZ() - groundPos.getZ());

            // Chebyshev < 2 => sem espaço vazio entre eles
            if (dx < GROWTH_MIN_SPACING && dz < GROWTH_MIN_SPACING) {
                return false;
            }
        }

        return true;
    }

    private int calculateGrowthHeight(BlockPos groundPos, BlockPos center) {
        double distanceSq = horizontalDistanceSq(groundPos, center);
        double radiusSq = SOIL_INFECTION_RADIUS * SOIL_INFECTION_RADIUS;

        if (distanceSq <= radiusSq * 0.20D) {
            return 3;
        }

        if (distanceSq <= radiusSq * 0.55D) {
            return 2;
        }

        return 1;
    }

    private double horizontalDistanceSq(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return (dx * dx) + (dz * dz);
    }

    private void placeGrowthColumn(ServerLevel serverLevel, BlockPos groundPos, int height) {
        BlockPos currentSupport = groundPos;

        for (int i = 1; i <= height && i <= MAX_GROWTH_HEIGHT; i++) {
            BlockPos placePos = groundPos.above(i);

            // Não sobrescreve bloco existente
            if (!serverLevel.getBlockState(placePos).isAir()) {
                break;
            }

            // Não deixa flutuar: precisa haver suporte válido logo abaixo
            BlockState supportState = serverLevel.getBlockState(currentSupport);
            boolean validSupport =
                    supportState.is(ModBlocks.CORRUPTED_SOIL.get()) ||
                            supportState.is(ModBlocks.INFECTION_GROWTH.get());

            if (!validSupport) {
                break;
            }

            serverLevel.setBlockAndUpdate(
                    placePos,
                    ModBlocks.INFECTION_GROWTH.get().defaultBlockState()
            );

            currentSupport = placePos;
        }
    }

    private void spawnParticles() {
        if (level().random.nextFloat() < 0.3f) {
            level().addParticle(ParticleTypes.SQUID_INK, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTime = tag.getInt("lifeTime");
        this.maxLife = tag.contains("maxLife") ? tag.getInt("maxLife") : DEFAULT_MAX_LIFE;

        if (tag.contains("targetX")) {
            this.moveTarget = new Vec3(
                    tag.getDouble("targetX"),
                    tag.getDouble("targetY"),
                    tag.getDouble("targetZ")
            );
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("lifeTime", lifeTime);
        tag.putInt("maxLife", maxLife);

        if (moveTarget != null) {
            tag.putDouble("targetX", moveTarget.x);
            tag.putDouble("targetY", moveTarget.y);
            tag.putDouble("targetZ", moveTarget.z);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}