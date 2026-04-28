package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Phantom Portal — pad that teleports any non-{@link BloodKin} entity stepping
 * onto it.
 *
 * <p><b>Pairing</b>: if there's another {@code phantom_portal} block within 32
 * blocks (radius), the entity is teleported there (skips the source block to
 * avoid loops). If no pair exists, falls back to a random offset within 16
 * blocks. Two players can build linked portals by stacking them or just
 * placing pairs in two locations.
 *
 * <p>Walkable but with a 4 px tall hitbox so it can be stacked or placed in
 * tight spaces. Cooldown of 60 ticks per entity (NBT-tracked) prevents
 * back-and-forth loops.
 */
public class PhantomPortalBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);
    private static final String NBT_LAST_TP = "liberthia_phantom_portal_last_tp";
    private static final int TP_COOLDOWN = 60;
    private static final int PAIR_SCAN_RADIUS = 32;

    public PhantomPortalBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof LivingEntity le)) return;
        if (BloodKin.is(le)) return;
        if (le.isPassenger() || le.isVehicle()) return;

        CompoundTag data = le.getPersistentData();
        long now = le.tickCount;
        long last = data.getLong(NBT_LAST_TP);
        if (now - last < TP_COOLDOWN) return;
        data.putLong(NBT_LAST_TP, now);

        ServerLevel sl = (ServerLevel) level;

        // 1) Try to find a paired phantom_portal nearby (skipping self).
        BlockPos pair = findPairedPortal(sl, pos);
        BlockPos target;
        boolean isPair = false;

        if (pair != null) {
            target = pair.above();   // emerge 1 block above the destination pad
            isPair = true;
        } else {
            target = randomFallback(sl, pos);
        }
        if (target == null) target = pos.offset(8, 1, 8);

        // Pre-teleport burst.
        sl.sendParticles(ParticleTypes.PORTAL,
                le.getX(), le.getY() + 0.8, le.getZ(),
                30, 0.4, 1.0, 0.4, 0.5);

        le.teleportTo(target.getX() + 0.5, target.getY() + 0.1, target.getZ() + 0.5);

        // Post-teleport effects.
        sl.sendParticles(ParticleTypes.PORTAL,
                target.getX() + 0.5, target.getY() + 0.8, target.getZ() + 0.5,
                30, 0.4, 1.0, 0.4, 0.5);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                target.getX() + 0.5, target.getY() + 1.5, target.getZ() + 0.5,
                14, 0.3, 0.4, 0.3, 0.05);
        sl.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS, 0.7F, isPair ? 1.6F : 1.4F);

        if (!isPair) {
            // Random fallback feels more disorienting → stronger debuffs.
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1, false, true, true));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true, true));
        } else {
            // Paired teleport is "clean" — only mild disorientation.
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, true, true));
        }
    }

    /** Scans a sphere of radius {@link #PAIR_SCAN_RADIUS} blocks for another portal. */
    private BlockPos findPairedPortal(ServerLevel level, BlockPos self) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = PAIR_SCAN_RADIUS;
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    cur.set(self.getX() + dx, self.getY() + dy, self.getZ() + dz);
                    if (!level.isLoaded(cur)) continue;
                    if (level.getBlockState(cur).is(ModBlocks.PHANTOM_PORTAL.get())) {
                        double d = self.distSqr(cur);
                        if (d < bestDist) {
                            bestDist = d;
                            best = cur.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Random open spot within 16 blocks horizontally as fallback. */
    private BlockPos randomFallback(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 8; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 6 + level.random.nextDouble() * 10;
            int dx = (int) (Math.cos(a) * r);
            int dz = (int) (Math.sin(a) * r);
            BlockPos candidate = pos.offset(dx, 0, dz);
            for (int dy = -3; dy <= 6; dy++) {
                BlockPos c = candidate.offset(0, dy, 0);
                if (level.getBlockState(c).isAir()
                        && level.getBlockState(c.above()).isAir()
                        && !level.getBlockState(c.below()).isAir()) {
                    return c;
                }
            }
        }
        return null;
    }
}
