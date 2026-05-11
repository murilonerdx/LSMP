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
    public boolean isRandomlyTicking(BlockState state) { return false; /* DISABLED */ }

    public static boolean isContained(Level level, BlockPos pos) {
        return BloodAltarBlock.countChalkSymbols(level, pos) >= 4;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        /* DISABLED — kill switch permanente */
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
