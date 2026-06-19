package br.com.murilo.liberthia.dimension;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * r179: <b>Fenda Dimensional</b> — um rasgo animado no ar (sprite billboard, ver
 * {@code DimensionalRiftRenderer}) que mostra o "outro lado" (vazio estrelado).
 *
 * <p>Persiste indefinidamente (salva com o chunk) até ser removida por comando
 * ({@code /liberthia rift clear}). Quem <b>atravessa</b> a fenda é levado pro
 * <b>Mundo Espiritual</b> ({@link SpiritDimension#enterSpiritWorld}).
 */
public class DimensionalRiftEntity extends Entity {

    /** 0→1 progresso de abertura (sincronizado pro cliente p/ animar o rasgo). */
    private static final EntityDataAccessor<Float> OPEN =
            SynchedEntityData.defineId(DimensionalRiftEntity.class, EntityDataSerializers.FLOAT);

    private static final int OPEN_TICKS = 24;
    private int age = 0;
    private int teleportCooldown = 0;

    public DimensionalRiftEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OPEN, 0.0F);
    }

    public float getOpen() { return this.entityData.get(OPEN); }
    public float getSpin(float partial) { return (age + partial) * 1.5F; }
    public int getAge() { return age; }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (level().isClientSide) {
            // brilho ambiente no cliente (borda do rasgo)
            if (age % 2 == 0) {
                double a = age * 0.3 + (random.nextDouble() * Math.PI * 2);
                level().addParticle(ParticleTypes.REVERSE_PORTAL,
                        getX() + Math.cos(a) * 0.5, getY() + 1.0 + (random.nextDouble() - 0.5) * 1.6,
                        getZ() + Math.sin(a) * 0.2, 0, 0.01, 0);
            }
            return;
        }

        // Abertura (0→1 em OPEN_TICKS), depois fica aberta pra sempre
        float open = Math.min(1.0F, age / (float) OPEN_TICKS);
        if (Math.abs(getOpen() - open) > 0.001F) this.entityData.set(OPEN, open);
        if (teleportCooldown > 0) teleportCooldown--;

        if (level() instanceof ServerLevel sl) {
            // r185: a fenda fecha sozinha após 10 minutos (12000 ticks)
            if (age >= 12000) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0, getZ(), 30, 0.4, 0.9, 0.3, 0.05);
                sl.playSound(null, blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.AMBIENT, 0.6F, 0.5F);
                discard();
                return;
            }
            if (age % 3 == 0) {
                sl.sendParticles(ParticleTypes.PORTAL,
                        getX(), getY() + 1.0, getZ(), 3, 0.25, 0.8, 0.1, 0.4);
            }
            if (age % 70 == 0) {
                sl.playSound(null, blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.5F, 0.7F);
            }
            if (age == 2) {
                sl.playSound(null, blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.AMBIENT, 0.7F, 0.6F);
            }
        }

        // Atravessar a fenda → BIDIRECIONAL (overworld ↔ Mundo Espiritual)
        if (open >= 1.0F && teleportCooldown == 0) {
            AABB box = getBoundingBox().inflate(0.3, 0.6, 0.3);
            List<Player> players = level().getEntitiesOfClass(Player.class, box);
            for (Player p : players) {
                if (!(p instanceof ServerPlayer sp) || sp.isSpectator()) continue;
                // r180b (report #73): player recém-teleportado fica imune; enquanto
                // estiver PARADO em cima do rift, renova a imunidade → sem ping-pong.
                if (SpiritDimension.isRiftImmune(sp)) {
                    SpiritDimension.grantRiftImmunity(sp, 60);
                    continue;
                }
                boolean ok;
                if (SpiritDimension.isInSpiritWorld(sp)) {
                    // atravessar no Spirit World = JANELA DE VOLTA pro mundo real
                    ok = SpiritDimension.returnToBody(sp);
                } else {
                    ok = SpiritDimension.enterSpiritWorld(sp);
                    // cria a fenda de retorno no Spirit World (a "janela de volta" pedida)
                    if (ok) {
                        ServerLevel spirit = sp.server.getLevel(SpiritDimension.SPIRIT_WORLD);
                        if (spirit != null) ensureReturnRift(spirit, sp.getX(), sp.getY(), sp.getZ(), sp.getYRot());
                    }
                }
                if (ok) {
                    teleportCooldown = 40;
                    if (level() instanceof ServerLevel sl)
                        sl.playSound(null, blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6F, 0.9F);
                }
            }
        }
    }

    /**
     * r180b (report #73): garante uma fenda de retorno no Spirit World ~2.5b à
     * frente do ponto de chegada, pra o player ter como voltar (e não nascer
     * dentro dela). Idempotente — não duplica se já existe uma por perto.
     */
    private void ensureReturnRift(ServerLevel spirit, double x, double y, double z, float yaw) {
        double rad = Math.toRadians(yaw);
        double rx = x - Math.sin(rad) * 2.5;
        double rz = z + Math.cos(rad) * 2.5;
        AABB near = new AABB(rx - 2, y - 2, rz - 2, rx + 2, y + 2, rz + 2);
        if (!spirit.getEntitiesOfClass(DimensionalRiftEntity.class, near).isEmpty()) return;
        DimensionalRiftEntity rift = new DimensionalRiftEntity(this.getType(), spirit);
        rift.setPos(rx, y, rz);
        rift.age = OPEN_TICKS; // nasce já aberta
        spirit.addFreshEntity(rift);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
        if (age < OPEN_TICKS) age = OPEN_TICKS; // recarrega já aberta
        teleportCooldown = tag.getInt("Cooldown");
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putInt("Cooldown", teleportCooldown);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double dist) { return dist < 16384.0; }
}
