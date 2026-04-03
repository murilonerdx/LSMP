package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class WhiteMatterExplosionEntity extends Entity {

    private int fuse = 80;
    private boolean exploded = false;

    public WhiteMatterExplosionEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            // Emit white particles while fusing
            if (!exploded) {
                for (int i = 0; i < 3; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 1.5;
                    double offsetY = random.nextDouble() * 1.5;
                    double offsetZ = (random.nextDouble() - 0.5) * 1.5;
                    level().addParticle(ParticleTypes.END_ROD,
                            getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                            0.0, 0.05, 0.0);
                }
            }
            return;
        }

        fuse--;

        if (fuse <= 0 && !exploded) {
            exploded = true;
            detonate();
            this.discard();
        }
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos center = blockPosition();

        // Clear infection blocks in 8-block radius
        int clearRadius = 8;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-clearRadius, -clearRadius, -clearRadius),
                center.offset(clearRadius, clearRadius, clearRadius))) {
            if (pos.distSqr(center) > clearRadius * clearRadius) {
                continue;
            }
            BlockState state = serverLevel.getBlockState(pos);
            if (state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                    || state.is(ModBlocks.CORRUPTED_SOIL.get())
                    || state.is(ModBlocks.INFECTION_GROWTH.get())) {
                serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }

        // Apply CLEAR_SHIELD effect to all living entities in 16-block radius
        AABB effectArea = new AABB(center).inflate(16.0);
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, effectArea)) {
            entity.addEffect(new MobEffectInstance(ModEffects.CLEAR_SHIELD.get(), 1200));
        }

        // Spawn 100 END_ROD particles in burst
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                getX(), getY() + 0.5, getZ(),
                100, 3.0, 3.0, 3.0, 0.1);

        // Play CLEAR_HUM sound
        serverLevel.playSound(null, center, ModSounds.CLEAR_HUM.get(),
                SoundSource.BLOCKS, 2.0F, 0.8F);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.fuse = tag.getInt("Fuse");
        this.exploded = tag.getBoolean("Exploded");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Fuse", this.fuse);
        tag.putBoolean("Exploded", this.exploded);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
