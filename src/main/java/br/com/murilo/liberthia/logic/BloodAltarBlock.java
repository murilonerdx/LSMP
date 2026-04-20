package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.entity.BloodOrbEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blood Altar — mother of blood infection.
 * Spreads infection only onto blocks that have a solid neighbor (anti-floating),
 * converts terrain to flesh variants, spawns a BloodOrb when not contained.
 * Contained by drawing ≥4 CHALK_SYMBOL blocks within radius 4.
 */
public class BloodAltarBlock extends Block {
    public BloodAltarBlock(Properties props) { super(props); }

    @Override public boolean isRandomlyTicking(BlockState s) { return true; }

    public static int countChalkSymbols(Level level, BlockPos center) {
        int count = 0;
        int r = 4;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(ModBlocks.CHALK_SYMBOL.get())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public boolean isContained(Level level, BlockPos pos) {
        if (countChalkSymbols(level, pos) < 4) return false;
        // A live Blood Priest within channel radius invalidates containment.
        java.util.List<br.com.murilo.liberthia.entity.BloodPriestEntity> priests = level.getEntitiesOfClass(
                br.com.murilo.liberthia.entity.BloodPriestEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(
                        br.com.murilo.liberthia.entity.BloodPriestEntity.CHANNEL_RADIUS));
        for (var p : priests) {
            if (p.isAlive()) return false;
        }
        return true;
    }

    /**
     * True iff an active (non-contained) Blood Altar exists within the given
     * radius. Used by flesh/infection blocks so that breaking the altar stops
     * all spreading automatically.
     */
    public static boolean hasActiveAltarNearby(Level level, BlockPos pos, int radius) {
        // Global admin halt short-circuits all spread logic for flesh/infection blocks.
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return false;
        int rx = radius;
        int ry = 6;
        for (int dx = -rx; dx <= rx; dx += 2) {
            for (int dz = -rx; dz <= rx; dz += 2) {
                for (int dy = -ry; dy <= ry; dy += 2) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof BloodAltarBlock altar) {
                        if (!altar.isContained(level, p)) return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 5);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rng) {
        // Fast scheduled tick drives aggressive spread independent of randomTickSpeed.
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) {
            level.scheduleTick(pos, this, 40);
            return;
        }
        if (!isContained(level, pos)) {
            for (int i = 0; i < 20; i++) {
                spreadInfection(level, pos, rng);
            }
            // Occasionally spawn a worm near the altar so entities actually show up.
            if (rng.nextFloat() < 0.25F) {
                trySpawnWormAtBlood(level, pos, rng);
            }
        }
        // reschedule quickly — altar is *very* active
        level.scheduleTick(pos, this, 8 + rng.nextInt(8));
    }

    /** Spawns a flesh-crawler or blood-worm at a nearby flesh/blood block. */
    private void trySpawnWormAtBlood(ServerLevel level, BlockPos pos, RandomSource rng) {
        // Cap total worms in area
        int cap = 6;
        int count = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Silverfish.class,
                new net.minecraft.world.phys.AABB(pos).inflate(14.0)).size();
        if (count >= cap) return;

        for (int tries = 0; tries < 8; tries++) {
            int ox = rng.nextInt(13) - 6;
            int oz = rng.nextInt(13) - 6;
            BlockPos ground = pos.offset(ox, 0, oz);
            // Scan up/down for a blood-ish block with air above
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos g = ground.offset(0, dy, 0);
                BlockState s = level.getBlockState(g);
                boolean bloody = s.is(ModBlocks.LIVING_FLESH.get())
                        || s.is(ModBlocks.ATTACKING_FLESH.get())
                        || s.is(ModBlocks.FLESH_MOTHER.get())
                        || s.is(ModBlocks.BLOOD_INFECTION_BLOCK.get())
                        || s.is(ModBlocks.BLOOD_INFESTATION_BLOCK.get())
                        || s.is(ModBlocks.BLOOD_FLUID_BLOCK.get());
                if (!bloody) continue;
                BlockPos air = g.above();
                if (!level.getBlockState(air).isAir()) continue;
                var type = rng.nextInt(3) == 0 ? ModEntities.GORE_WORM.get()
                        : (rng.nextInt(2) == 0 ? ModEntities.BLOOD_WORM.get() : ModEntities.FLESH_CRAWLER.get());
                var worm = type.create(level);
                if (worm != null) {
                    worm.moveTo(air.getX() + 0.5, air.getY(), air.getZ() + 0.5,
                            rng.nextFloat() * 360F, 0F);
                    level.addFreshEntity(worm);
                    level.sendParticles(BloodParticles.BLOOD,
                            air.getX() + 0.5, air.getY() + 0.2, air.getZ() + 0.5,
                            14, 0.3, 0.2, 0.3, 0.1);
                }
                return;
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        // Heavy rising blood fountain column (constant)
        for (int i = 0; i < 8; i++) {
            double dx = rng.nextDouble();
            double dz = rng.nextDouble();
            level.addParticle(BloodParticles.BLOOD,
                    pos.getX() + dx, pos.getY() + 1.05 + rng.nextDouble() * 0.4, pos.getZ() + dz,
                    (rng.nextDouble() - 0.5) * 0.2, 0.25 + rng.nextDouble() * 0.35,
                    (rng.nextDouble() - 0.5) * 0.2);
        }
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + 0.3 + rng.nextDouble() * 0.4,
                    pos.getY() + 1.0, pos.getZ() + 0.3 + rng.nextDouble() * 0.4,
                    (rng.nextDouble() - 0.5) * 0.15, 0.2, (rng.nextDouble() - 0.5) * 0.15);
        }
        if (isContained(level, pos)) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.WHITE_ASH,
                        pos.getX() + rng.nextDouble(),
                        pos.getY() + 1.05,
                        pos.getZ() + rng.nextDouble(),
                        0, 0.02, 0);
            }
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rng) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        if (isContained(level, pos)) return;

        // Aggressive spread
        for (int i = 0; i < 12; i++) {
            spreadInfection(level, pos, rng);
        }

        // Spawn Blood Orb (cooldown via entity presence)
        java.util.List<BloodOrbEntity> existing = level.getEntitiesOfClass(BloodOrbEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(10.0));
        if (existing.isEmpty() && rng.nextFloat() < 0.25F) {
            BloodOrbEntity orb = ModEntities.BLOOD_ORB.get().create(level);
            if (orb != null) {
                orb.moveTo(pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, 0, 0);
                level.addFreshEntity(orb);
                level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 0.6F, 1.4F);
                level.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 2.0,
                        pos.getZ() + 0.5, 1, 0, 0, 0, 0);
                level.sendParticles(BloodParticles.BLOOD, pos.getX() + 0.5,
                        pos.getY() + 2.0, pos.getZ() + 0.5, 60, 0.8, 0.8, 0.8, 0.2);
            }
        }
    }

    /**
     * Replaces an existing solid terrain block (grass/dirt/stone/log/leaves/etc.)
     * near the altar with a flesh variant. Never places into the air or on floating
     * positions — the target must already be a non-air, replaceable terrain block.
     */
    private void spreadInfection(ServerLevel level, BlockPos pos, RandomSource rng) {
        // Narrower Y range to prevent diagonal flight; wider XZ for reach
        int ox = rng.nextInt(9) - 4;
        int oy = rng.nextInt(3) - 1; // -1..+1 only
        int oz = rng.nextInt(9) - 4;
        if (ox == 0 && oy == 0 && oz == 0) return;

        BlockPos target = pos.offset(ox, oy, oz);
        BlockState ts = level.getBlockState(target);

        // Skip already-corrupted blocks and non-replacements
        if (ts.isAir()) return;
        if (ts.is(ModBlocks.BLOOD_INFECTION_BLOCK.get())
                || ts.is(ModBlocks.BLOOD_INFESTATION_BLOCK.get())
                || ts.is(ModBlocks.LIVING_FLESH.get())
                || ts.is(ModBlocks.ATTACKING_FLESH.get())
                || ts.is(ModBlocks.FLESH_MOTHER.get())
                || ts.is(ModBlocks.BLOOD_ALTAR.get())
                || ts.is(ModBlocks.BLOOD_SPIKE.get())
                || ts.is(ModBlocks.CHALK_SYMBOL.get())) {
            return;
        }

        Block choice = null;

        // Grass / dirt → blood_dirt / infection (with random spike)
        if (ts.is(Blocks.GRASS_BLOCK) || ts.is(Blocks.DIRT) || ts.is(Blocks.PODZOL)
                || ts.is(Blocks.MYCELIUM) || ts.is(Blocks.COARSE_DIRT) || ts.is(Blocks.ROOTED_DIRT)
                || ts.is(Blocks.FARMLAND) || ts.is(Blocks.DIRT_PATH)) {
            float r = rng.nextFloat();
            if (r < 0.08F) choice = ModBlocks.BLOOD_SPIKE.get();
            else if (r < 0.25F) choice = ModBlocks.BLOOD_INFESTATION_BLOCK.get();
            else if (r < 0.55F) choice = ModBlocks.BLOOD_DIRT.get();
            else choice = ModBlocks.BLOOD_INFECTION_BLOCK.get();
        }
        // Sand / gravel → blood_sand
        else if (ts.is(Blocks.SAND) || ts.is(Blocks.RED_SAND) || ts.is(Blocks.GRAVEL)) {
            float r = rng.nextFloat();
            if (r < 0.08F) choice = ModBlocks.BLOOD_SPIKE.get();
            else if (r < 0.20F) choice = ModBlocks.BLOOD_INFECTION_BLOCK.get();
            else choice = ModBlocks.BLOOD_SAND.get();
        }
        // Stone-like → blood_stone / living flesh / attacking flesh
        else if (ts.is(Blocks.STONE) || ts.is(Blocks.COBBLESTONE) || ts.is(Blocks.DEEPSLATE)
                || ts.is(Blocks.SANDSTONE) || ts.is(Blocks.ANDESITE) || ts.is(Blocks.GRANITE)
                || ts.is(Blocks.DIORITE) || ts.is(Blocks.TUFF)) {
            float r = rng.nextFloat();
            if (r < 0.15F) choice = ModBlocks.ATTACKING_FLESH.get();
            else if (r < 0.30F) choice = ModBlocks.BLOOD_INFESTATION_BLOCK.get();
            else if (r < 0.55F) choice = ModBlocks.LIVING_FLESH.get();
            else choice = ModBlocks.BLOOD_STONE.get();
        }
        // Vanilla ores → blood ores
        else if (ts.is(Blocks.COAL_ORE) || ts.is(Blocks.DEEPSLATE_COAL_ORE)) {
            choice = ModBlocks.BLOOD_COAL_ORE.get();
        } else if (ts.is(Blocks.IRON_ORE) || ts.is(Blocks.DEEPSLATE_IRON_ORE)) {
            choice = ModBlocks.BLOOD_IRON_ORE.get();
        } else if (ts.is(Blocks.GOLD_ORE) || ts.is(Blocks.DEEPSLATE_GOLD_ORE) || ts.is(Blocks.NETHER_GOLD_ORE)) {
            choice = ModBlocks.BLOOD_GOLD_ORE.get();
        } else if (ts.is(Blocks.DIAMOND_ORE) || ts.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
            choice = ModBlocks.BLOOD_DIAMOND_ORE.get();
        } else if (ts.is(Blocks.REDSTONE_ORE) || ts.is(Blocks.DEEPSLATE_REDSTONE_ORE)) {
            choice = ModBlocks.BLOOD_REDSTONE_ORE.get();
        } else if (ts.is(Blocks.LAPIS_ORE) || ts.is(Blocks.DEEPSLATE_LAPIS_ORE)) {
            choice = ModBlocks.BLOOD_LAPIS_ORE.get();
        } else if (ts.is(Blocks.EMERALD_ORE) || ts.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
            choice = ModBlocks.BLOOD_EMERALD_ORE.get();
        }
        // Logs → flesh mother (rare propagator)
        else if (ts.is(BlockTags.LOGS)) {
            if (rng.nextFloat() < 0.15F) choice = ModBlocks.FLESH_MOTHER.get();
            else choice = ModBlocks.LIVING_FLESH.get();
        }
        // Leaves / plants / water → blood fluid — OK to place because we're substituting
        // a non-solid block; needs a support beneath to prevent floating.
        else if (ts.is(BlockTags.LEAVES) || ts.is(BlockTags.FLOWERS)
                || ts.is(BlockTags.REPLACEABLE_BY_TREES)
                || ts.is(Blocks.WATER) || ts.is(Blocks.TALL_GRASS)
                || ts.is(Blocks.GRASS) || ts.is(Blocks.FERN) || ts.is(Blocks.LARGE_FERN)
                || ts.is(Blocks.SEAGRASS) || ts.is(Blocks.KELP) || ts.is(Blocks.KELP_PLANT)) {
            // Require something solid beneath so blood_fluid doesn't float
            BlockState below = level.getBlockState(target.below());
            if (!below.isFaceSturdy(level, target.below(), Direction.UP) && !below.getFluidState().isEmpty()) {
                return;
            }
            choice = ModBlocks.BLOOD_FLUID_BLOCK.get();
        }

        if (choice == null) return;

        // Global anti-float check for SOLID flesh variants: must have a solid block
        // below OR be substituting an already-solid block (non-replaceable terrain).
        if (choice != ModBlocks.BLOOD_FLUID_BLOCK.get()) {
            boolean wasSolidTerrain = !ts.canBeReplaced() && !ts.getFluidState().isEmpty() ? false : !ts.canBeReplaced();
            BlockState below = level.getBlockState(target.below());
            boolean hasSupport = below.isFaceSturdy(level, target.below(), Direction.UP);
            if (!wasSolidTerrain && !hasSupport) {
                return;
            }
        }

        level.setBlockAndUpdate(target, choice.defaultBlockState());
        level.sendParticles(BloodParticles.BLOOD,
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                10, 0.3, 0.3, 0.3, 0.1);

        // Small chance to drip blood on top — only if on solid flesh
        BlockPos above = target.above();
        if (rng.nextFloat() < 0.12F && level.getBlockState(above).isAir()
                && choice != ModBlocks.BLOOD_FLUID_BLOCK.get()) {
            level.setBlockAndUpdate(above, ModBlocks.BLOOD_FLUID_BLOCK.get().defaultBlockState());
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (entity instanceof Player p && !p.isCreative() && !level.isClientSide) {
            p.hurt(p.damageSources().magic(), 2.0F);
        }
    }
}
