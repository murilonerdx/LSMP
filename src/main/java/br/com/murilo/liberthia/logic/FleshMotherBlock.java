package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flesh Mother — proliferation source block. Turns nearby blocks into flesh.
 * Contained by drawing 4+ chalk symbols within radius 4 (no ordering).
 */
public class FleshMotherBlock extends Block {
    public FleshMotherBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    public static boolean isContained(Level level, BlockPos pos) {
        return BloodAltarBlock.countChalkSymbols(level, pos) >= 4;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        // ----- Boss summon ritual (Fase 2) -----
        // 4 HeartOfFlesh blocks at (±3,0,0) and (0,0,±3), at night → spawn boss.
        if (checkSummonRitual(level, pos)) {
            performSummon(level, pos);
            return;
        }
        if (isContained(level, pos)) {
            // Contained: just emit idle particles, no spread
            level.sendParticles(ParticleTypes.ASH,
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    4, 0.4, 0.2, 0.4, 0.01);
            return;
        }
        // Altar dependency: stop proliferating if the altar is gone
        if (!BloodAltarBlock.hasActiveAltarNearby(level, pos, 20)) return;
        // Proliferate: try to convert 1-3 adjacent blocks (anti-float)
        int attempts = 5;
        for (int i = 0; i < attempts; i++) {
            int ox = rand.nextInt(5) - 2;
            int oy = rand.nextInt(3) - 1; // -1..+1
            int oz = rand.nextInt(5) - 2;
            BlockPos target = pos.offset(ox, oy, oz);
            BlockState ts = level.getBlockState(target);
            if (ts.isAir() || ts.is(Blocks.WATER) || ts.is(ModBlocks.FLESH_MOTHER.get())
                    || ts.is(ModBlocks.LIVING_FLESH.get()) || ts.is(ModBlocks.ATTACKING_FLESH.get())
                    || ts.is(ModBlocks.CHALK_SYMBOL.get()))
                continue;
            // Anti-float: need solid support below
            BlockPos belowTarget = target.below();
            if (!level.getBlockState(belowTarget).isFaceSturdy(level, belowTarget, Direction.UP))
                continue;
            // 20% chance to create attacking flesh, else living flesh
            Block fleshType = rand.nextInt(5) == 0 ? ModBlocks.ATTACKING_FLESH.get() : ModBlocks.LIVING_FLESH.get();
            level.setBlockAndUpdate(target, fleshType.defaultBlockState());
            level.sendParticles(BloodParticles.BLOOD,
                    target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.05);
            break;
        }
    }

    private static boolean checkSummonRitual(ServerLevel level, BlockPos pos) {
        if (level.isDay()) return false;
        Block heart = ModBlocks.HEART_OF_FLESH.get();
        return level.getBlockState(pos.offset(3, 0, 0)).is(heart)
                && level.getBlockState(pos.offset(-3, 0, 0)).is(heart)
                && level.getBlockState(pos.offset(0, 0, 3)).is(heart)
                && level.getBlockState(pos.offset(0, 0, -3)).is(heart);
    }

    private static void performSummon(ServerLevel level, BlockPos pos) {
        // Consume the 4 heart blocks
        level.setBlockAndUpdate(pos.offset(3, 0, 0), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.offset(-3, 0, 0), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.offset(0, 0, 3), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.offset(0, 0, -3), Blocks.AIR.defaultBlockState());
        // Replace the FleshMother block with air and spawn the boss on top.
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var boss = br.com.murilo.liberthia.registry.ModEntities.FLESH_MOTHER_BOSS.get().create(level);
        if (boss != null) {
            boss.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);
            level.addFreshEntity(boss);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    1, 0, 0, 0, 0);
            level.sendParticles(BloodParticles.BLOOD,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    120, 2.0, 2.0, 2.0, 0.5);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                    net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
            // Global announce
            for (var p : level.players()) {
                p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§4A Mãe ouviu. Ela sobe."), false);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(BloodParticles.BLOOD,
                    pos.getX() + rand.nextDouble(),
                    pos.getY() + 1.0 + rand.nextDouble() * 0.3,
                    pos.getZ() + rand.nextDouble(),
                    (rand.nextDouble() - 0.5) * 0.1, 0.05, (rand.nextDouble() - 0.5) * 0.1);
        }
        if (isContained(level, pos)) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.WHITE_ASH,
                        pos.getX() + rand.nextDouble(),
                        pos.getY() + 1.0,
                        pos.getZ() + rand.nextDouble(),
                        0, 0.02, 0);
            }
        }
    }
}
