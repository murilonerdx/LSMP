package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.effect.BloodInfectionApplier;
import br.com.murilo.liberthia.logic.BloodParticles;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Blood Orb — floating particle-only entity spawned by BloodAltar.
 * Hovers over anchor block, shoots blood particles at nearby players,
 * applies Blood Infection effect + direct damage on close contact.
 * No model — only client renders it via particle trail (no renderer needed).
 */
public class BloodOrbEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(BloodOrbEntity.class, EntityDataSerializers.FLOAT);

    private int anchorX, anchorY, anchorZ;
    private int attackCooldown = 0;
    private int lifetime = 20 * 60 * 10; // 10 min

    public BloodOrbEntity(EntityType<? extends BloodOrbEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setAnchor(double x, double y, double z) {
        this.anchorX = (int) x;
        this.anchorY = (int) y;
        this.anchorZ = (int) z;
        this.setPos(x, y + 2.2, z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SCALE, 1.0F);
    }

    public float getScale() { return this.entityData.get(DATA_SCALE); }
    public void setScale(float s) { this.entityData.set(DATA_SCALE, s); }

    @Override
    public void tick() {
        super.tick();

        // Hover animation: sinusoidal vertical bob + rotation
        double bob = Math.sin(this.tickCount * 0.08) * 0.25;
        this.setPos(anchorX + 0.5, anchorY + 2.2 + bob, anchorZ + 0.5);

        if (this.level().isClientSide) {
            renderParticles();
            return;
        }

        // Decrement lifetime
        if (--lifetime <= 0) {
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(BloodParticles.BLOOD, getX(), getY(), getZ(),
                        40, 0.5, 0.5, 0.5, 0.3);
            }
            this.discard();
            return;
        }

        // Attack nearby players
        if (--attackCooldown <= 0) {
            attackCooldown = 30;
            ServerLevel sl = (ServerLevel) this.level();
            AABB area = new AABB(anchorX - 8, anchorY - 4, anchorZ - 8, anchorX + 9, anchorY + 8, anchorZ + 9);
            for (Player p : sl.getEntitiesOfClass(Player.class, area, pl -> !pl.isCreative() && !pl.isSpectator())) {
                double d = p.distanceToSqr(getX(), getY(), getZ());
                if (d > 64) continue;

                // Shoot particle trail from orb to player
                shootBloodTrail(sl, getX(), getY(), getZ(), p.getX(), p.getY() + 1.0, p.getZ());

                // Apply blood infection
                int amp = d < 9 ? 2 : d < 25 ? 1 : 0;
                p.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 20 * 20, amp, false, true, true));

                // Close range direct damage
                if (d < 9) {
                    p.hurt(p.damageSources().magic(), 2.0F);
                }
            }
        }

        // Ambient spawn particles
        if (this.tickCount % 3 == 0 && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(BloodParticles.BLOOD, getX(), getY(), getZ(),
                    6, 0.4, 0.4, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(),
                    1, 0.2, 0.2, 0.2, 0);
        }

        // Periodic growl
        if (this.tickCount % 80 == 0 && this.level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 0.8F, 0.3F);
        }
    }

    private void shootBloodTrail(ServerLevel sl, double fx, double fy, double fz, double tx, double ty, double tz) {
        Vec3 dir = new Vec3(tx - fx, ty - fy, tz - fz);
        double len = dir.length();
        if (len < 0.001) return;
        dir = dir.normalize();
        int steps = (int) Math.min(40, Math.max(3, len * 3));
        for (int i = 0; i < steps; i++) {
            double t = (i + 1) / (double) steps;
            double px = fx + dir.x * len * t;
            double py = fy + dir.y * len * t;
            double pz = fz + dir.z * len * t;
            sl.sendParticles(BloodParticles.BLOOD, px, py, pz, 1, 0.05, 0.05, 0.05, 0.0);
        }
        sl.sendParticles(BloodParticles.BLOOD, tx, ty, tz, 12, 0.3, 0.3, 0.3, 0.08);
    }

    private void renderParticles() {
        // Swirling blood core — spawn short-range particles around the orb
        for (int i = 0; i < 3; i++) {
            double ang = this.tickCount * 0.2 + i * 2.094;
            double r = 0.7 + 0.2 * Math.sin(this.tickCount * 0.1);
            double px = getX() + Math.cos(ang) * r;
            double pz = getZ() + Math.sin(ang) * r;
            double py = getY() + Math.sin(ang * 0.5) * 0.3;
            this.level().addParticle(BloodParticles.BLOOD, px, py, pz, 0, 0, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        return false; // invulnerable — must destroy altar to stop
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) { return dist < 64 * 64; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        anchorX = tag.getInt("aX");
        anchorY = tag.getInt("aY");
        anchorZ = tag.getInt("aZ");
        lifetime = tag.getInt("life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("aX", anchorX);
        tag.putInt("aY", anchorY);
        tag.putInt("aZ", anchorZ);
        tag.putInt("life", lifetime);
    }
}
