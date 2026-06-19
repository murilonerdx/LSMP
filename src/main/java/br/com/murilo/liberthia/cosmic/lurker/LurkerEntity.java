package br.com.murilo.liberthia.cosmic.lurker;

import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * r173: <b>Lurker</b> — o "rosto no escuro". Aparece ~10 blocos do player
 * enquanto ele minera, no escuro, com uma das caras de terror. Não pode ser
 * atacado, não colide, e SOME sozinho depois de 2s (40t). Puro susto.
 */
public class LurkerEntity extends Entity {

    private static final EntityDataAccessor<Integer> FACE =
            SynchedEntityData.defineId(LurkerEntity.class, EntityDataSerializers.INT);

    public static final int MAX_LIFE = 40; // 2s
    private int life = 0;

    public LurkerEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public static LurkerEntity spawn(ServerLevel level, double x, double y, double z, int face) {
        LurkerEntity e = ModEntities.LURKER.get().create(level);
        if (e == null) return null;
        e.setFace(face);
        e.moveTo(x, y, z, 0, 0);
        level.addFreshEntity(e);
        return e;
    }

    @Override protected void defineSynchedData() { this.entityData.define(FACE, 0); }
    public int getFace() { return this.entityData.get(FACE); }
    public void setFace(int f) { this.entityData.set(FACE, f); }
    public int getLife() { return life; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        life++;
        if (life >= MAX_LIFE) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 10, 0.2, 0.4, 0.2, 0.01);
            }
            discard();
        }
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurt(DamageSource src, float amt) { return false; }
    @Override public boolean isInvulnerable() { return true; }

    @Override protected void readAdditionalSaveData(CompoundTag t) { life = t.getInt("Life"); setFace(t.getInt("Face")); }
    @Override protected void addAdditionalSaveData(CompoundTag t) { t.putInt("Life", life); t.putInt("Face", getFace()); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
