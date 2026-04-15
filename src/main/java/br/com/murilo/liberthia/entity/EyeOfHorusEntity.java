package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModCapabilities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Olho de Horus — entidade ambiental que observa jogadores em áreas de alta infecção.
 * Conforme o lore: os exploradores de Horus reportaram uma "presença" observando-os.
 * Não pode ser atacada ou morta. Flutua, segue players com olhar, e aplica debuffs
 * de medo quando olhada diretamente. Desaparece se a infecção da área diminuir.
 */
public class EyeOfHorusEntity extends Entity {

    private int lifetimeTicks = 0;
    private int maxLifetime = 600; // 30 seconds default
    private int gazeCheckCooldown = 0;
    private double hoverHeight;

    private static final String[] GAZE_KEYS = {
            "entity.liberthia.eye_of_horus.gaze1",
            "entity.liberthia.eye_of_horus.gaze2",
            "entity.liberthia.eye_of_horus.gaze3"
    };

    public EyeOfHorusEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.hoverHeight = 4.0 + level.random.nextDouble() * 4.0;
    }

    @Override
    protected void defineSynchedData() {
        // No synched data needed
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifetimeTicks = tag.getInt("LifetimeTicks");
        this.maxLifetime = tag.getInt("MaxLifetime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifetimeTicks", lifetimeTicks);
        tag.putInt("MaxLifetime", maxLifetime);
    }

    @Override
    public void tick() {
        super.tick();

        lifetimeTicks++;
        if (lifetimeTicks > maxLifetime) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        // Slow hover movement — drift slightly
        double bobOffset = Math.sin(tickCount * 0.05) * 0.02;
        setDeltaMovement(0, bobOffset, 0);
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

        if (level().isClientSide) {
            // Client-side particles — eerie purple/sculk
            if (tickCount % 3 == 0) {
                level().addParticle(ParticleTypes.REVERSE_PORTAL,
                        getX() + (random.nextDouble() - 0.5) * 1.5,
                        getY() + random.nextDouble() * 1.5,
                        getZ() + (random.nextDouble() - 0.5) * 1.5,
                        0, 0.02, 0);
                level().addParticle(ParticleTypes.ENCHANT,
                        getX() + (random.nextDouble() - 0.5),
                        getY() + 0.5,
                        getZ() + (random.nextDouble() - 0.5),
                        0, -0.05, 0);
            }
            return;
        }

        gazeCheckCooldown--;

        // Track nearest player — slowly rotate towards them
        List<Player> nearby = level().getEntitiesOfClass(
                Player.class, new AABB(blockPosition()).inflate(24.0));

        if (!nearby.isEmpty()) {
            Player closest = null;
            double minDist = Double.MAX_VALUE;
            for (Player p : nearby) {
                double d = distanceTo(p);
                if (d < minDist) {
                    minDist = d;
                    closest = p;
                }
            }

            if (closest != null) {
                // Face the player
                Vec3 dir = closest.getEyePosition().subtract(position()).normalize();
                this.setYRot((float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F);
                this.setXRot((float) -(Math.atan2(dir.y, dir.horizontalDistance()) * (180.0 / Math.PI)));

                // Check if player is looking back at the Eye
                if (gazeCheckCooldown <= 0 && minDist < 16.0) {
                    Vec3 playerLook = closest.getLookAngle();
                    Vec3 toEye = position().subtract(closest.getEyePosition()).normalize();
                    double dot = playerLook.dot(toEye);

                    if (dot > 0.92) { // Player is staring directly
                        gazeCheckCooldown = 100;
                        // Fear effect — the Eye notices you looking
                        closest.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false, true));
                        closest.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false, true));

                        String key = GAZE_KEYS[random.nextInt(GAZE_KEYS.length)];
                        closest.displayClientMessage(
                                Component.translatable(key)
                                        .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC),
                                false);

                        level().playSound(null, blockPosition(),
                                SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE,
                                0.6F, 0.4F);

                        // Add infection
                        closest.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                            data.addInfection(3);
                            data.setDirty(true);
                        });

                        // Teleport away when seen
                        if (level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                                    getX(), getY(), getZ(),
                                    30, 0.8, 0.8, 0.8, 0.1);
                        }
                        double newX = getX() + (random.nextDouble() - 0.5) * 20;
                        double newZ = getZ() + (random.nextDouble() - 0.5) * 20;
                        teleportTo(newX, getY(), newZ);
                    }
                }
            }
        }

        // Ambient sounds
        if (tickCount % 80 == 0) {
            level().playSound(null, blockPosition(),
                    SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
                    0.3F, 0.3F);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false; // Invulnerable
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    public void setMaxLifetime(int ticks) {
        this.maxLifetime = ticks;
    }
}
