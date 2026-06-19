package br.com.murilo.liberthia.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * r185 — <b>Portal de Fenda</b> (verde) aberto pela Adaga Corta-Fendas. Billboard animado
 * (renderer próprio). Quem atravessa é teleportado para as coords/dimensão gravadas na adaga.
 * Transiente: some sozinho após ~30s. Carrega o alvo em NBT (sobrevive a um reload curto).
 */
public class RiftPortalEntity extends Entity {
    private static final EntityDataAccessor<Float> OPEN =
            SynchedEntityData.defineId(RiftPortalEntity.class, EntityDataSerializers.FLOAT);
    private static final int OPEN_TICKS = 16;
    private static final int LIFETIME = 600; // 30s

    private int age = 0;
    private int teleportCooldown = 0;
    private String targetDim = "minecraft:overworld";
    private double tx, ty, tz;
    private boolean isReturn = false; // r196: portal de volta não gera outro portal de volta

    public RiftPortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public void setTarget(String dim, double x, double y, double z) {
        this.targetDim = dim; this.tx = x; this.ty = y; this.tz = z;
    }

    @Override protected void defineSynchedData() { this.entityData.define(OPEN, 0.0F); }
    public float getOpen() { return this.entityData.get(OPEN); }
    public int getAge() { return age; }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (level().isClientSide) {
            if (age % 2 == 0) {
                double a = age * 0.3 + (random.nextDouble() * Math.PI * 2);
                level().addParticle(ParticleTypes.HAPPY_VILLAGER,
                        getX() + Math.cos(a) * 0.5, getY() + 1.0 + (random.nextDouble() - 0.5) * 1.4,
                        getZ() + Math.sin(a) * 0.2, 0, 0.01, 0);
            }
            return;
        }

        float open = Math.min(1.0F, age / (float) OPEN_TICKS);
        if (Math.abs(getOpen() - open) > 0.001F) this.entityData.set(OPEN, open);
        if (teleportCooldown > 0) teleportCooldown--;

        if (level() instanceof ServerLevel sl) {
            if (age % 3 == 0)
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0, getZ(), 3, 0.25, 0.8, 0.1, 0.02);
            if (age % 70 == 0)
                sl.playSound(null, blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.4F, 1.4F);

            if (open >= 1.0F && teleportCooldown == 0) {
                AABB box = getBoundingBox().inflate(0.4, 0.8, 0.4);
                List<ServerPlayer> players = sl.getEntitiesOfClass(ServerPlayer.class, box);
                ResourceLocation rl = ResourceLocation.tryParse(targetDim);
                ServerLevel target = rl == null ? null : sl.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, rl));
                if (target != null && !players.isEmpty()) {
                    // r196: captura a ORIGEM (este portal) p/ abrir uma fenda de VOLTA no destino
                    String originDim = sl.dimension().location().toString();
                    double ox = getX(), oy = getY(), oz = getZ();
                    boolean anyTeleported = false;
                    for (ServerPlayer p : players) {
                        if (p.isSpectator()) continue;
                        p.teleportTo(target, tx, ty, tz, EnumSet.noneOf(RelativeMovement.class), p.getYRot(), p.getXRot());
                        br.com.murilo.liberthia.dimension.SpiritDimension.grantRiftImmunity(p, 100); // evita ping-pong com fendas
                        target.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6F, 1.2F);
                        // r197: rasgar o tecido entre mundos pode transmitir uma doença dimensional (~20%)
                        if (p.getRandom().nextInt(5) == 0)
                            br.com.murilo.liberthia.event.DimensionalDiseaseHandler.contractRandom(p);
                        anyTeleported = true;
                    }
                    if (anyTeleported) {
                        teleportCooldown = 30;
                        // r196: portal de VOLTA pro mundo de origem, ao lado do ponto de chegada (sem ser de volta-do-volta)
                        if (!isReturn) {
                            RiftPortalEntity back = br.com.murilo.liberthia.registry.ModEntities.RIFT_PORTAL.get().create(target);
                            if (back != null) {
                                back.setPos(tx + 2.5, ty, tz);
                                back.setTarget(originDim, ox, oy, oz);
                                back.isReturn = true;
                                target.addFreshEntity(back);
                            }
                        }
                    }
                }
            }
            if (age >= LIFETIME) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0, getZ(), 20, 0.4, 0.8, 0.4, 0.05);
                discard();
            }
        }
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
        teleportCooldown = tag.getInt("TpCooldown");
        targetDim = tag.contains("TDim") && !tag.getString("TDim").isEmpty() ? tag.getString("TDim") : "minecraft:overworld";
        tx = tag.getDouble("TX"); ty = tag.getDouble("TY"); tz = tag.getDouble("TZ");
        isReturn = tag.getBoolean("IsReturn");
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putInt("TpCooldown", teleportCooldown);
        tag.putString("TDim", targetDim);
        tag.putDouble("TX", tx); tag.putDouble("TY", ty); tag.putDouble("TZ", tz);
        tag.putBoolean("IsReturn", isReturn);
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double dist) { return dist < 16384.0; }
}
