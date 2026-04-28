package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
 * Venom Geyser — vomits a tall parabolic arc of toxic spore particles onto
 * the closest living target. Each hit applies stacking POISON + SLOWNESS.
 * AGE 0–3: more spitballs per shot, bigger range, bigger debuffs.
 */
public class VenomGeyserBlock extends Block {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public VenomGeyserBlock(Properties props) {
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
        if (!level.isClientSide) level.scheduleTick(pos, this, 25);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        attack(level, pos, age);
        int rate = 25 - age * 5;
        level.scheduleTick(pos, this, rate + rand.nextInt(8));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        if (age < 3 && rand.nextFloat() < 0.18F) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    20, 0.4, 0.4, 0.4, 0.05);
        }
    }

    private void attack(ServerLevel level, BlockPos pos, int age) {
        if (FleshMotherBlock.isContained(level, pos)) return;
        double range = 4.5 + age * 1.5;
        AABB box = new AABB(pos).inflate(range);
        Vec3 origin = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        int volleys = 1 + age;

        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(le)) continue;

            Vec3 target = new Vec3(le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ());
            Vec3 delta = target.subtract(origin);

            // Parabolic spit arc — peaks higher at higher AGE.
            // Cap volleys at 2 to avoid 4×16 particle spam at age 3.
            int cappedVolleys = Math.min(2, volleys);
            for (int v = 0; v < cappedVolleys; v++) {
                double jitter = (level.random.nextDouble() - 0.5) * 0.3;
                int steps = 8; // was 16
                for (int s = 1; s <= steps; s++) {
                    double t = s / (double) steps;
                    double arc = -1.6 * (t - 0.5) * (t - 0.5) + (0.4 + age * 0.1);
                    double x = origin.x + (delta.x + jitter) * t;
                    double y = origin.y + delta.y * t + arc;
                    double z = origin.z + (delta.z + jitter) * t;
                    level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 1, 0.04, 0.04, 0.04, 0.0);
                    if (s % 3 == 0) {
                        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 1, 0.07, 0.07, 0.07, 0.01);
                    }
                }
            }

            // Splatter at impact (single combined burst)
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    le.getX(), le.getY() + 0.3, le.getZ(),
                    8 + age * 2, 0.4, 0.5, 0.4, 0.05);

            float dmg = 1.0F + age * 0.5F;
            le.hurt(le.damageSources().magic(), dmg);
            le.addEffect(new MobEffectInstance(MobEffects.POISON, 120 + age * 60, Math.min(3, age)));
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100 + age * 40, 1 + age / 2));
            le.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200 + age * 60, 1));
            if (age >= 2) {
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            }

            if (age >= 4) {
                le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            }
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.7F, 0.5F + age * 0.1F);
            break;
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        for (int i = 0; i < 2 + age; i++) {
            double px = pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.4;
            double py = pos.getY() + 1.0 + rand.nextDouble() * 0.3;
            double pz = pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.4;
            level.addParticle(ParticleTypes.SCULK_SOUL, px, py, pz,
                    (rand.nextDouble() - 0.5) * 0.05, 0.06, (rand.nextDouble() - 0.5) * 0.05);
        }
    }
}
