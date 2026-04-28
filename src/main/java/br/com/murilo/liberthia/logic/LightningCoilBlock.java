package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Lightning Coil — emits a zigzag chain of electric sparks at the closest
 * target. Applies Glowing + Mining Fatigue + a brief paralysis (Slowness 6).
 * Drains 1 XP level on hit when AGE >= 2 (for players).
 */
public class LightningCoilBlock extends Block {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public LightningCoilBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AGE); }

    @Override
    public boolean isRandomlyTicking(BlockState state) { return true; }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide) level.scheduleTick(pos, this, 20);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        attack(level, pos, age, rand);
        int rate = 20 - age * 4;
        level.scheduleTick(pos, this, rate + rand.nextInt(5));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        if (age < 3 && rand.nextFloat() < 0.20F) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
        }
    }

    private void attack(ServerLevel level, BlockPos pos, int age, RandomSource rand) {
        if (FleshMotherBlock.isContained(level, pos)) return;
        double range = 6.0 + age * 1.0;
        AABB box = new AABB(pos).inflate(range);
        Vec3 origin = new Vec3(pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5);

        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(le)) continue;

            Vec3 target = new Vec3(le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ());
            Vec3 delta = target.subtract(origin);
            double len = delta.length();
            if (len < 0.001) continue;
            Vec3 dir = delta.scale(1.0 / len);

            // Zigzag — perpendicular jitter alternating sign each segment.
            // Reduced from 12+age*3 segments × 3 micro-steps × 2 particle types
            // (~72-126 sendParticles calls per attack) to 8+age segments with no
            // micro-steps. Drops cost ~10×.
            int segs = 8 + age;
            Vec3 perp1 = dir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 perp2 = dir.cross(perp1).normalize();
            for (int s = 1; s <= segs; s++) {
                double t = s / (double) segs;
                Vec3 mid = origin.add(delta.scale(t));
                double jitter = (rand.nextDouble() - 0.5) * (0.6 + age * 0.1);
                double jitter2 = (rand.nextDouble() - 0.5) * (0.6 + age * 0.1);
                Vec3 zig = mid.add(perp1.scale(jitter)).add(perp2.scale(jitter2));
                level.sendParticles(ParticleTypes.END_ROD, zig.x, zig.y, zig.z, 1, 0, 0, 0, 0);
                if ((s & 1) == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, zig.x, zig.y, zig.z, 1, 0.05, 0.05, 0.05, 0.05);
                }
            }
            // Impact crit burst
            level.sendParticles(ParticleTypes.CRIT, target.x, target.y, target.z,
                    6 + age * 2, 0.4, 0.4, 0.4, 0.4);
            level.sendParticles(ParticleTypes.FLASH, target.x, target.y + 0.5, target.z,
                    1, 0.0, 0.0, 0.0, 0.0);

            float dmg = 1.5F + age * 0.5F;
            le.hurt(le.damageSources().lightningBolt(), dmg);
            le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200 + age * 80, 0));
            le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120 + age * 30, Math.min(3, age + 1)));
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5)); // paralysis flash

            if (age >= 2 && le instanceof ServerPlayer sp) {
                sp.giveExperienceLevels(-1);
            }

            level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS,
                    0.4F, 1.6F + age * 0.1F);
            break;
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        for (int i = 0; i < 2 + age; i++) {
            double px = pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.5;
            double py = pos.getY() + 0.5 + (rand.nextDouble() - 0.5) * 0.5;
            double pz = pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz,
                    (rand.nextDouble() - 0.5) * 0.2, (rand.nextDouble() - 0.5) * 0.2, (rand.nextDouble() - 0.5) * 0.2);
        }
    }
}
