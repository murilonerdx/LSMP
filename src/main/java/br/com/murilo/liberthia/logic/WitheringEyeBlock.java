package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModCapabilities;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Withering Eye — fires a straight black-soul beam at the closest non-kin
 * living entity. Range, damage and rate scale with the {@link #AGE} property
 * which auto-evolves on random ticks (0 → 3).
 *
 * Permanent effect: every hit has a 5% chance of incrementing the player's
 * {@code permanentHealthPenalty} by 1 (cap 10) — survives death naturally.
 */

public class WitheringEyeBlock extends Block {
    Logger logger = LoggerFactory.getLogger(WitheringEyeBlock.class);

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public WitheringEyeBlock(Properties props) {
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide) level.scheduleTick(pos, this, 40);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        attack(level, pos, age);
        int rate = 18 - age * 6;

        logger.info("AGE: {} || rand {}", age, rand.toString());

        level.scheduleTick(pos, this, rate + rand.nextInt(6));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);

        if (age < 3 && rand.nextFloat() < 0.25F) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
        }
    }

    /**
     *
     * @param level
     * @param pos
     * @param age
     */
    private void attack(ServerLevel level, BlockPos pos, int age) {
        if (FleshMotherBlock.isContained(level, pos)) return;
        double range = 5.0 + age * 1.5;
        AABB box = new AABB(pos).inflate(range);
        Vec3 origin = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        LivingEntity target = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(le)) continue;
            double d = le.distanceToSqr(origin);
            if (d < bestDist) { bestDist = d; target = le; }
        }
        if (target == null) return;

        // Straight beam: dense SQUID_INK + sparse SOUL_FIRE_FLAME.
        Vec3 dst = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        Vec3 delta = dst.subtract(origin);
        int steps = 10 + age * 2; // was 18+age*4
        for (int s = 1; s <= steps; s++) {
            double t = s / (double) steps;
            double x = origin.x + delta.x * t;
            double y = origin.y + delta.y * t;
            double z = origin.z + delta.z * t;
            if ((s & 1) == 0) {
                level.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 1, 0.04, 0.04, 0.04, 0.0);
            }
            if (s % 5 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
        // Muzzle ring + impact burst (single combined call each)
        level.sendParticles(ParticleTypes.SOUL, origin.x, origin.y, origin.z,
                4 + age, 0.25, 0.25, 0.25, 0.05);
        level.sendParticles(ParticleTypes.SMOKE, dst.x, dst.y, dst.z,
                5, 0.4, 0.4, 0.4, 0.02);

        // Effects
        float dmg = 2.5F + age * 0.75F;
        target.hurt(target.damageSources().wither(), dmg);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100 + age * 20, Math.min(2, age)));
//        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80 + age * 30, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));

        if (target instanceof ServerPlayer sp && level.random.nextFloat() < 0.05F + age * 0.02F) {
            sp.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                data.setPermanentHealthPenalty(Math.min(10, data.getPermanentHealthPenalty() + 1));
                data.setDirty(true);
            });
        }

        level.playSound(null, pos, SoundEvents.WITHER_SHOOT, SoundSource.BLOCKS, 0.5F, 1.6F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        int age = state.getValue(AGE);
        for (int i = 0; i < 2 + age; i++) {
            double px = pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.6;
            double py = pos.getY() + 0.5 + (rand.nextDouble() - 0.5) * 0.6;
            double pz = pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0, 0.01, 0);
        }
    }
}
