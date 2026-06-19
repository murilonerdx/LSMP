package br.com.murilo.liberthia.loom.entity;

import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * v0.1.22 r34: Screaming Teleporter — FIX: agora usa Heightmap.MOTION_BLOCKING
 * pra achar ground real no floating islands. Não cai mais infinito.
 */
public class ScreamerTeleporterEntity extends Monster {

    private int teleportCooldown = 60;

    public ScreamerTeleporterEntity(EntityType<? extends ScreamerTeleporterEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player target = level().getNearestPlayer(this, 32.0);
        if (target == null) return;

        teleportCooldown--;
        if (teleportCooldown <= 0) {
            attemptTeleportNear(target);
            teleportCooldown = 60 + level().random.nextInt(60);
        }
    }

    /**
     * r34 FIX: usa Heightmap.MOTION_BLOCKING pra achar topo de bloco SÓLIDO
     * na coluna. Antes search dy=-3..+3 falhava em floating islands.
     */
    private void attemptTeleportNear(Player target) {
        ServerLevel sl = (ServerLevel) level();
        for (int i = 0; i < 12; i++) {
            double dist = 6 + sl.random.nextDouble() * 8;
            double angle = sl.random.nextDouble() * Math.PI * 2;
            int x = (int) (target.getX() + Math.cos(angle) * dist);
            int z = (int) (target.getZ() + Math.sin(angle) * dist);
            // r34: Heightmap acha o Y do topo da coluna (ground real)
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            // Sanity: y deve ser dentro do mundo + ter ar acima
            if (y < sl.getMinBuildHeight() || y >= sl.getMaxBuildHeight() - 2) continue;
            BlockPos groundPos = new BlockPos(x, y, z);
            // Verifica que tem espaço suficiente
            if (!sl.getBlockState(groundPos).isAir()) continue;
            if (!sl.getBlockState(groundPos.above()).isAir()) continue;

            this.moveTo(x + 0.5, y, z + 0.5, sl.random.nextFloat() * 360, 0);
            // r34: usa SCREAMER_TELEPORT custom sound
            sl.playSound(null, x, y, z, ModSounds.SCREAMER_TELEPORT.get(),
                    SoundSource.HOSTILE, 2.0F, 1.0F);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    x + 0.5, y + 1, z + 0.5, 30, 0.5, 0.5, 0.5, 0.1);
            sl.sendParticles(ParticleTypes.PORTAL,
                    x + 0.5, y + 1, z + 0.5, 50, 0.4, 0.8, 0.4, 0.3);
            return;
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof net.minecraft.world.entity.LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            le.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1));
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            level().playSound(null, this.blockPosition(), ModSounds.SCREAMER_SCREAM.get(),
                    SoundSource.HOSTILE, 2.0F, 1.0F);
            ((ServerLevel) level()).sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    le.getX(), le.getY() + 1, le.getZ(), 5, 0.3, 0.3, 0.3, 0);
        }
        return ok;
    }
}
