package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

/**
 * Lightning Node — calls real {@link LightningBolt} on the closest non-kin
 * living entity within range. Range and rate scale with AGE 0..3.
 */
public class LightningNodeBlock extends Block {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public LightningNodeBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AGE);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) { return true; }

    @Override
    public void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moved) {
        super.onPlace(s, l, p, old, moved);
        if (!l.isClientSide) l.scheduleTick(p, this, 60);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        attack(level, pos, age);
        // Rate: 200 → 80 ticks (1/4× faster at age 3)
//        int rate = 200 - age * rand.nextInt(40);
        level.scheduleTick(pos, this, 60);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        if (age < 3 && rand.nextFloat() < 0.20F) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
        }
    }

    private void attack(ServerLevel level, BlockPos pos, int age) {
        if (FleshMotherBlock.isContained(level, pos)) return;
        double range = 6.0 + age * 2.0;
        AABB box = new AABB(pos).inflate(range);

        LivingEntity target = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(le)) continue;
            double d = le.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < bestDist) { bestDist = d; target = le; }
        }
        if (target == null) return;

        // Spawn real lightning bolt at target position
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(age < 2); // Real damage only at age >= 2
            level.addFreshEntity(bolt);
        }
        level.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                12 + age * 4, 0.3, 0.4, 0.3, 0.1);
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.4F, 1.4F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        for (int i = 0; i < 1 + age; i++) {
            double px = pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.6;
            double py = pos.getY() + 0.5 + (rand.nextDouble() - 0.5) * 0.6;
            double pz = pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, 0.05, 0);
        }
    }
}
