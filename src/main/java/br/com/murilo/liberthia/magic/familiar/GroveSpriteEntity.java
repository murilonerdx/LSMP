package br.com.murilo.liberthia.magic.familiar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;

/**
 * v0.1.24 r95: <b>Grove Sprite</b> — familiar Sylph-like que cresce plants
 * num raio + gera Source pra Mycelial Sourcelinks próximos.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>A cada 60 ticks, tenta bonemeal numa plant random num raio de 6</li>
 *   <li>Quando sucessful, adiciona +5 source no Sourcelink mais próximo</li>
 *   <li>Wander pacífico, não ataca, regen lento</li>
 * </ul>
 */
public class GroveSpriteEntity extends PathfinderMob {

    public GroveSpriteEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel sl)) return;

        // Particles ambient (verdes)
        if (tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 0.5, getZ(),
                    1, 0.3, 0.3, 0.3, 0.01);
        }

        // Bonemeal random plant
        if (tickCount % 60 == 0) {
            tryGrowRandomPlant(sl);
        }

        // Regen lento
        if (tickCount % 100 == 0 && getHealth() < getMaxHealth()) {
            heal(1F);
        }
    }

    private void tryGrowRandomPlant(ServerLevel sl) {
        int radius = 6;
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = random.nextInt(radius * 2) - radius;
            int dy = random.nextInt(3) - 1;
            int dz = random.nextInt(radius * 2) - radius;
            BlockPos p = blockPosition().offset(dx, dy, dz);
            var s = sl.getBlockState(p);
            if (s.getBlock() instanceof BonemealableBlock bb) {
                if (bb.isValidBonemealTarget(sl, p, s, false)
                        && bb.isBonemealSuccess(sl, sl.random, p, s)) {
                    bb.performBonemeal(sl, sl.random, p, s);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5,
                            5, 0.3, 0.3, 0.3, 0.05);

                    // Adiciona +5 source no Mycelial Sourcelink mais próximo (raio 16)
                    pushSourceToNearestSourcelink(sl, 5);
                    return;
                }
            }
        }
    }

    /** r164: sourcelinks removidos — Grove Sprite agora só faz partícula sem alvo. */
    private void pushSourceToNearestSourcelink(ServerLevel sl, int amount) {
        // no-op (mantida assinatura pra não quebrar callers)
    }
}
