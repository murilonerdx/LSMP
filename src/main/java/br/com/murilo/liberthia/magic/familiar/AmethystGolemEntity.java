package br.com.murilo.liberthia.magic.familiar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.24 r109: <b>Amethyst Golem</b> — familiar AN style. Walks around
 * Budding Amethyst, harvests Amethyst Clusters quando maduros, dropa crystals
 * no chão. Não há "linked" — passivo no biome.
 */
public class AmethystGolemEntity extends PathfinderMob {

    public AmethystGolemEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel sl)) return;

        // Sparkle particles
        if (tickCount % 12 == 0) {
            sl.sendParticles(ParticleTypes.GLOW,
                    getX(), getY() + 0.8, getZ(),
                    1, 0.3, 0.5, 0.3, 0.02);
        }

        // A cada 80t, scan AMETHYST_CLUSTER próximo + harvest
        if (tickCount % 80 == 0) {
            tryHarvestAmethyst(sl);
        }
    }

    private void tryHarvestAmethyst(ServerLevel sl) {
        BlockPos here = blockPosition();
        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = here.offset(dx, dy, dz);
                    BlockState s = sl.getBlockState(p);
                    if (s.is(Blocks.AMETHYST_CLUSTER)) {
                        // Drop amethyst shards e remove o cluster
                        spawnAtLocation(new net.minecraft.world.item.ItemStack(
                                net.minecraft.world.item.Items.AMETHYST_SHARD,
                                2 + random.nextInt(2)));
                        sl.removeBlock(p, false);
                        sl.sendParticles(ParticleTypes.SCRAPE,
                                p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                                10, 0.3, 0.3, 0.3, 0.05);
                        return;
                    }
                }
            }
        }
    }
}
