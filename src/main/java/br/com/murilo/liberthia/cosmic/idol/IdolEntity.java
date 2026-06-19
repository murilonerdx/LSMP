package br.com.murilo.liberthia.cosmic.idol;

import br.com.murilo.liberthia.cosmic.scare.ScareS2CPacket;
import br.com.murilo.liberthia.cosmic.scare.ScareType;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * r178: <b>O Ídolo</b> — stalker PERSISTENTE de "slow burn" (inspirado em The Idol /
 * Father Fester). Server-side com efeitos client. Não é "in your face": observa de
 * LONGE, se aproxima devagar ao longo do encontro, adapta ao movimento do player,
 * congela quando encarado (anjo chorão) e some quando você o encara de perto (climax
 * com flash + grito). Persistente — gerenciado pelo {@link IdolManager}.
 */
public class IdolEntity extends Monster {

    /** Encontro máximo (20 min) — depois some sozinho e o ciclo recomeça via manager. */
    public static final int MAX_HAUNT = 24000;
    private static final double FAR = 48.0, NEAR = 6.0;

    private UUID targetPlayer;
    private boolean aggressive = false;  // agressivo = mata no contato; psicológico = só assombra
    private float closeness = 0F;   // 0 = longe (FAR), 1 = perto (NEAR)
    private int lifeTicks = 0;
    private int repositionCd = 40;
    private int revealCd = 0;       // anti-deslize do teleporte
    private int soundCd = 100;
    private int sanityCd = 0;

    public IdolEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override protected void registerGoals() { this.goalSelector.addGoal(0, new FloatGoal(this)); }

    public void bind(UUID player) { this.targetPlayer = player; setPersistenceRequired(); }
    public void setAggressive(boolean a) { this.aggressive = a; }

    @Override public void checkDespawn() { if (targetPlayer == null) super.checkDespawn(); }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean fireImmune() { return true; }
    @Override public boolean canBeAffected(MobEffectInstance e) { return false; }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource src, float amt) {
        // não morre normal — some se atacado (deixa pra reaparecer depois)
        if (!level().isClientSide) vanish();
        return false;
    }

    private Player resolveTarget() {
        if (targetPlayer == null) return level().getNearestPlayer(this, 96.0);
        Player p = level().getPlayerByUUID(targetPlayer);
        return (p != null && p.isAlive() && !p.isSpectator()) ? p : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        lifeTicks++;
        if (lifeTicks >= MAX_HAUNT) { vanish(); return; }

        Player t = resolveTarget();
        if (t == null) { if (targetPlayer == null) discard(); return; }

        if (revealCd > 0 && --revealCd == 0) setInvisible(false);

        double dist = t.distanceTo(this);
        Vec3 toMe = position().add(0, getBbHeight() * 0.6, 0).subtract(t.getEyePosition());
        double dot = toMe.lengthSqr() < 1.0e-4 ? 0 : toMe.normalize().dot(t.getLookAngle());
        boolean watched = dot > 0.93 && hasLineOfSight(t);

        faceWithTwitch(t);

        // AGRESSIVO: chegou no contato → mata.
        if (aggressive && dist < 3.0) {
            if (t instanceof ServerPlayer sp) {
                ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.FLASH, 30, getRandom().nextInt(11), ""));
            }
            t.hurt(damageSources().magic(), 1000F);
            vanish();
            return;
        }

        // CLIMAX (psicológico): encarado de perto → flash + grito + some
        if (watched && dist < 16) { confront(t); return; }

        // observado de longe → CONGELA (não se aproxima enquanto você olha)
        if (!watched && --repositionCd <= 0) {
            repositionCd = 60 + getRandom().nextInt(90);              // 3–7,5s
            closeness = Math.min(1F, closeness + (aggressive ? 0.1F : 0.05F)); // agressivo aproxima 2×
            reposition(t);
        }
        ambient(t, dist);
    }

    private double targetDistance() { return FAR - closeness * (FAR - NEAR); }

    /** Reposiciona num anel à distância-alvo, de preferência atrás/no campo periférico. */
    private void reposition(Player t) {
        if (!(level() instanceof ServerLevel sl)) return;
        double want = targetDistance();
        Vec3 look = t.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0e-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        BlockPos best = null;
        for (int i = 0; i < 24; i++) {
            double ang = sl.random.nextDouble() * Math.PI * 2;
            Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
            // prefere ficar atrás/lado do player (não na cara) enquanto longe
            if (closeness < 0.7F && dir.dot(flat) > 0.25) continue;
            int x = (int) Math.floor(t.getX() + dir.x * want);
            int z = (int) Math.floor(t.getZ() + dir.z * want);
            BlockPos stand = findStand(sl, x, t.getBlockY(), z);
            if (stand != null) { best = stand; break; }
        }
        if (best == null) return;
        if (level() instanceof ServerLevel s2) s2.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 3, .2, .3, .2, .01);
        moveTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5, getYRot(), 0);
        setInvisible(true);
        revealCd = 8; // termina o lerp escondido (não dá pra ver o trajeto)
    }

    private void confront(Player t) {
        if (t instanceof ServerPlayer sp) {
            ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.FLASH, 22, getRandom().nextInt(11), ""));
            SpiritDimension.addSanity(sp, -12);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
        }
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 40, .4, .8, .4, .06);
            sl.playSound(null, blockPosition(), ModSounds.SCREAMER_SCREAM.get(), SoundSource.HOSTILE, 1.1F, 0.7F);
        }
        vanish();
    }

    private void ambient(Player t, double dist) {
        // som distante ocasional
        if (--soundCd <= 0) {
            soundCd = 160 + getRandom().nextInt(200);
            if (level() instanceof ServerLevel sl) {
                sl.playSound(null, blockPosition(), ModSounds.PERIPHERAL_WHISPER.get(),
                        SoundSource.HOSTILE, 0.5F, 0.4F + getRandom().nextFloat() * 0.2F);
            }
        }
        // dreno de sanidade quando perto e com linha de visão
        if (dist < 28 && hasLineOfSight(t) && --sanityCd <= 0 && t instanceof ServerPlayer sp) {
            sanityCd = 100; // a cada 5s
            SpiritDimension.addSanity(sp, -1);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
        }
    }

    /** Encara o player com a cabeça tremendo (estática). */
    private void faceWithTwitch(Player t) {
        double dx = t.getX() - getX(), dz = t.getZ() - getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        setYRot(yaw); setYBodyRot(yaw);
        if (getRandom().nextFloat() < 0.4F) {
            setYHeadRot(yaw + (getRandom().nextFloat() - 0.5F) * 70F);
            setXRot((getRandom().nextFloat() - 0.5F) * 45F);
        } else {
            setYHeadRot(yaw); setXRot(0F);
        }
    }

    private BlockPos findStand(ServerLevel sl, int x, int y0, int z) {
        for (int dy = 4; dy >= -5; dy--) {
            BlockPos feet = new BlockPos(x, y0 + dy, z);
            BlockState floor = sl.getBlockState(feet.below());
            if (floor.blocksMotion() && sl.getBlockState(feet).isAir()
                    && sl.getBlockState(feet.above()).isAir()) {
                return feet;
            }
        }
        return null;
    }

    private void vanish() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 24, .4, .6, .4, .05);
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 1, getZ(), 16, .4, .6, .4, .02);
        }
        if (targetPlayer != null) IdolManager.onIdolGone(targetPlayer);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putFloat("Closeness", closeness);
        tag.putBoolean("Aggro", aggressive);
        if (targetPlayer != null) tag.putUUID("Target", targetPlayer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        closeness = tag.getFloat("Closeness");
        aggressive = tag.getBoolean("Aggro");
        if (tag.hasUUID("Target")) targetPlayer = tag.getUUID("Target");
    }
}
