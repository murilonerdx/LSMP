package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Consciência Sombria — entidade consciente nascida da fusão Dark+Clear.
 * Conforme o lore: a mistura das duas matérias cria seres com motivações próprias.
 * Comportamento: persegue jogadores, aplica debuffs psíquicos, teleporta-se,
 * e sussurra mensagens perturbadoras.
 */
public class DarkConsciousnessEntity extends Monster {

    private int whisperCooldown = 0;
    private int teleportCooldown = 0;
    private int auraCooldown = 0;

    private static final String[] WHISPER_KEYS = {
            "entity.liberthia.dark_consciousness.whisper1",
            "entity.liberthia.dark_consciousness.whisper2",
            "entity.liberthia.dark_consciousness.whisper3",
            "entity.liberthia.dark_consciousness.whisper4",
            "entity.liberthia.dark_consciousness.whisper5"
    };

    public DarkConsciousnessEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean result = super.doHurtTarget(target);
        if (result && target instanceof LivingEntity living) {
            // Psychic attack — confusion + blindness
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            // Also infect
            living.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                data.addInfection(8);
                data.setDirty(true);
            });
        }
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (level().isClientSide) return;

        whisperCooldown--;
        teleportCooldown--;
        auraCooldown--;

        List<Player> nearbyPlayers = level().getEntitiesOfClass(
                Player.class, new AABB(blockPosition()).inflate(16.0));

        // Whisper disturbing messages to nearby players
        if (whisperCooldown <= 0 && !nearbyPlayers.isEmpty()) {
            whisperCooldown = 100 + random.nextInt(200); // 5-15 seconds
            String key = WHISPER_KEYS[random.nextInt(WHISPER_KEYS.length)];
            for (Player player : nearbyPlayers) {
                player.displayClientMessage(
                        Component.translatable(key)
                                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.ITALIC),
                        false);
            }
        }

        // Teleport towards target when far away
        if (teleportCooldown <= 0 && getTarget() instanceof Player target) {
            double dist = distanceTo(target);
            if (dist > 8.0 && dist < 32.0) {
                teleportCooldown = 60 + random.nextInt(80);
                Vec3 targetPos = target.position();
                Vec3 dir = targetPos.subtract(position()).normalize();
                double tx = targetPos.x - dir.x * 3;
                double tz = targetPos.z - dir.z * 3;
                if (randomTeleport(tx, targetPos.y, tz, true)) {
                    if (level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                                getX(), getY() + 1.0, getZ(),
                                20, 0.5, 0.8, 0.5, 0.05);
                    }
                    level().playSound(null, blockPosition(),
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE,
                            0.8F, 0.5F);
                }
            }
        }

        // Psychic aura — applies weakness and slowness to all nearby players
        if (auraCooldown <= 0) {
            auraCooldown = 40;
            for (Player player : nearbyPlayers) {
                if (player.distanceTo(this) <= 8.0) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
                    // Passive infection drain
                    player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                        data.addInfection(1);
                        data.setDirty(true);
                    });
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // Drops stabilized dark matter and void crystal
        this.spawnAtLocation(new ItemStack(ModItems.STABILIZED_DARK_MATTER.get(), 1 + random.nextInt(2)));
        if (random.nextFloat() < 0.4f) {
            this.spawnAtLocation(new ItemStack(ModItems.VOID_CRYSTAL.get(), 1));
        }
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 64.0;
    }
}
