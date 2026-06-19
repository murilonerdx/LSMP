package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.config.WorldChangesDisabled;
import br.com.murilo.liberthia.logic.entropy.EntropyTracker;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * r186 — <b>Buraco Negro de Matéria Escura</b> (vivo). Surge quando há MUITOS esporos perto de um
 * Motor de Matéria Escura. Esfera de partículas pulsante (fibonacci) que SUGA os players, dá uma
 * grande REPULSÃO, ataca com partículas pretas (Cegueira + Fome), SEGUE o player mais próximo,
 * infecta o chão e EXPLODE no contato (100% de matéria escura em todos por perto). Some por um
 * tempo (janela p/ quebrar o motor) e reaparece. Quebrar o motor o mata e reverte tudo.
 */
public class DarkMatterBlackHoleEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(DarkMatterBlackHoleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(DarkMatterBlackHoleEntity.class, EntityDataSerializers.INT);

    private static final int PULL_DURATION = 100;     // 5s sugando
    private static final int REPULSE_DURATION = 40;   // 2s de repulsão
    private static final int ACTIVE_DURATION = 600;   // 30s ativo total
    private static final int DORMANT_DURATION = 400;  // 20s dormente (janela p/ quebrar o motor)
    private static final double PULL_RADIUS = 20.0;

    private int age = 0;
    private int phase = 0;        // 0=pull, 1=repulse, 2=dormant
    private int phaseTimer = 0;
    private int dormantTimer = 0;
    private boolean repulsed = false;
    private String engineId = "";
    private int engineX, engineY, engineZ;

    public DarkMatterBlackHoleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public String getEngineId() { return engineId; }

    @Override protected void defineSynchedData() {
        this.entityData.define(DATA_AGE, 0);
        this.entityData.define(DATA_PHASE, 0);
    }

    public int getClientAge() { return entityData.get(DATA_AGE); }
    public int getClientPhase() { return entityData.get(DATA_PHASE); }

    public static DarkMatterBlackHoleEntity spawn(ServerLevel sl, BlockPos above, String engineId, BlockPos enginePos) {
        DarkMatterBlackHoleEntity e = new DarkMatterBlackHoleEntity(ModEntities.DARK_MATTER_BLACK_HOLE.get(), sl);
        e.moveTo(above.getX() + 0.5, above.getY() + 0.5, above.getZ() + 0.5, 0, 0);
        e.engineId = engineId;
        e.engineX = enginePos.getX(); e.engineY = enginePos.getY(); e.engineZ = enginePos.getZ();
        sl.addFreshEntity(e);
        sl.playSound(null, e.blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.2F, 0.5F);
        return e;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;          // visuais vêm de sendParticles (server)
        if (!(level() instanceof ServerLevel sl)) return;
        age++;
        entityData.set(DATA_AGE, age);
        phaseTimer++;

        switch (phase) {
            case 0 -> tickPull(sl);
            case 1 -> tickRepulse(sl);
            default -> tickDormant(sl);
        }
        entityData.set(DATA_PHASE, phase);

        if (phase != 2) {
            if (age % 10 == 0) followNearestPlayer(sl);
            if (age % 8 == 0) infectGroundBelow(sl);
            if (age % 20 == 0) attackNearbyPlayers(sl);
            if (age % 4 == 0) emitParticleSphere(sl);
        }
    }

    private void tickPull(ServerLevel sl) {
        AABB box = getBoundingBox().inflate(PULL_RADIUS);
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            Vec3 delta = position().add(0, getBbHeight() * 0.5, 0)
                    .subtract(p.position().add(0, p.getBbHeight() * 0.5, 0));
            double dist = delta.length();
            if (dist < 1.5) { doContactExplode(sl); return; }
            Vec3 dir = delta.normalize();
            double force = Math.min(0.18, 1.8 / Math.max(1.0, dist * dist / 4.0));
            p.setDeltaMovement(p.getDeltaMovement().add(dir.scale(force)));
            p.hurtMarked = true;
        }
        if (phaseTimer >= PULL_DURATION) { phase = 1; phaseTimer = 0; repulsed = false; }
    }

    private void tickRepulse(ServerLevel sl) {
        if (!repulsed) {
            repulsed = true;
            AABB box = getBoundingBox().inflate(PULL_RADIUS);
            for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
                if (p.isCreative() || p.isSpectator()) continue;
                Vec3 away = p.position().subtract(position());
                if (away.lengthSqr() < 1.0e-3) away = new Vec3(1, 0.4, 0);
                p.setDeltaMovement(away.normalize().scale(2.8).add(0, 0.5, 0));
                p.hurtMarked = true;
            }
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 0.6F);
        }
        if (phaseTimer >= REPULSE_DURATION) {
            phaseTimer = 0;
            if (age < ACTIVE_DURATION) { phase = 0; }
            else { phase = 2; dormantTimer = DORMANT_DURATION; }
        }
    }

    private void tickDormant(ServerLevel sl) {
        // segurança: se o motor sumiu, morre
        if (!sl.getBlockState(new BlockPos(engineX, engineY, engineZ)).is(ModTech.BLACK_MATTER_ENGINE.get())) {
            discard(); return;
        }
        if (--dormantTimer <= 0) {
            int topY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, engineX, engineZ);
            moveTo(engineX + 0.5, topY + 6, engineZ + 0.5, 0, 0);
            age = 0; phase = 0; phaseTimer = 0; repulsed = false;
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.2F, 0.5F);
        }
    }

    private void followNearestPlayer(ServerLevel sl) {
        Player t = sl.getNearestPlayer(this, 40.0);
        if (t == null) return;
        Vec3 dir = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ());
        if (dir.lengthSqr() < 1.0e-3) return;
        dir = dir.normalize();
        moveTo(getX() + dir.x * 1.5, getY(), getZ() + dir.z * 1.5, getYRot(), getXRot());
    }

    private void infectGroundBelow(ServerLevel sl) {
        if (WorldChangesDisabled.ACTIVE) return;
        for (int i = 0; i < 3; i++) {
            int x = Mth(sl, 2), z = Mth(sl, 2);
            int gx = (int) Math.floor(getX()) + x, gz = (int) Math.floor(getZ()) + z;
            int topY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz) - 1;
            BlockPos pos = new BlockPos(gx, topY, gz);
            if (!sl.hasChunkAt(pos)) continue;
            var cur = sl.getBlockState(pos);
            if (cur.isAir() || cur.getDestroySpeed(sl, pos) < 0) continue;
            if (cur.is(ModBlocks.DARK_MATTER_BLOCK.get())) continue;
            EntropyTracker.record(sl, engineId, pos, cur);
            sl.setBlock(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private int Mth(ServerLevel sl, int r) { return sl.random.nextInt(r * 2 + 1) - r; }

    private void attackNearbyPlayers(ServerLevel sl) {
        AABB box = getBoundingBox().inflate(8.0);
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0, false, true));
            p.hurt(p.damageSources().magic(), 2.0F);
            sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.0, p.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
        }
    }

    private void doContactExplode(ServerLevel sl) {
        AABB box = getBoundingBox().inflate(12.0);
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.getCapability(ModCapabilities.INFECTION).ifPresent(d -> { d.addInfection(500); d.setDirty(true); });
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, true));
            p.hurt(p.damageSources().magic(), 6.0F);
        }
        sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.0, getZ(), 200, 3.0, 3.0, 3.0, 0.1);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 80, 2.0, 2.0, 2.0, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.6F, 0.4F);
        discard();
    }

    private void emitParticleSphere(ServerLevel sl) {
        double R = 2.5 + 0.5 * Math.sin(age * 0.15);
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        int N = 40;
        boolean repulse = phase == 1;
        for (int i = 0; i < N; i++) {
            double y = 1.0 - (i / (double) (N - 1)) * 2.0;
            double rSlice = Math.sqrt(Math.max(0, 1.0 - y * y));
            double theta = goldenAngle * i;
            double px = getX() + R * rSlice * Math.cos(theta);
            double py = getY() + getBbHeight() * 0.5 + R * y;
            double pz = getZ() + R * rSlice * Math.sin(theta);
            sl.sendParticles(ParticleTypes.SQUID_INK, px, py, pz, 1, 0.02, 0.02, 0.02, 0.0);
            if (repulse && i % 3 == 0)
                sl.sendParticles(ParticleTypes.PORTAL, px, py, pz, 1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("bhAge"); phase = tag.getInt("bhPhase");
        phaseTimer = tag.getInt("bhPhaseTimer"); dormantTimer = tag.getInt("bhDormantTimer");
        engineId = tag.getString("engineId");
        engineX = tag.getInt("engineX"); engineY = tag.getInt("engineY"); engineZ = tag.getInt("engineZ");
        entityData.set(DATA_PHASE, phase); entityData.set(DATA_AGE, age);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("bhAge", age); tag.putInt("bhPhase", phase);
        tag.putInt("bhPhaseTimer", phaseTimer); tag.putInt("bhDormantTimer", dormantTimer);
        tag.putString("engineId", engineId);
        tag.putInt("engineX", engineX); tag.putInt("engineY", engineY); tag.putInt("engineZ", engineZ);
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 16384.0; }
}
