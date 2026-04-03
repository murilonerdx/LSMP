package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.entity.DarkMatterSporeEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SporeBloomBlock extends Block {

    public SporeBloomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Check if base block is still valid
        if (!isValidBase(level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }

        // If player within 8 blocks, launch a spore at them
        AABB searchArea = new AABB(pos).inflate(8.0D);
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, searchArea,
                player -> !player.isSpectator() && !player.isCreative());

        if (!nearbyPlayers.isEmpty()) {
            Player target = nearbyPlayers.get(random.nextInt(nearbyPlayers.size()));
            launchSpore(level, pos, target);
        }
    }

    private void launchSpore(ServerLevel level, BlockPos pos, Player target) {
        DarkMatterSporeEntity spore = new DarkMatterSporeEntity(
                ModEntities.DARK_MATTER_SPORE.get(), level);
        spore.setPos(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        spore.setTarget(target.position());
        level.addFreshEntity(spore);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide() && !isValidBase(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    private boolean isValidBase(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlocks.CORRUPTED_SOIL.get())
                || below.is(ModBlocks.DARK_MATTER_BLOCK.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Constant purple particle emission
        for (int i = 0; i < 2; i++) {
            level.addParticle(
                    ParticleTypes.PORTAL,
                    pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                    pos.getY() + 0.3D + random.nextDouble() * 0.5D,
                    pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                    (random.nextDouble() - 0.5D) * 0.1D,
                    random.nextDouble() * 0.1D,
                    (random.nextDouble() - 0.5D) * 0.1D
            );
        }
        if (random.nextFloat() < 0.4f) {
            level.addParticle(
                    ParticleTypes.SQUID_INK,
                    pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                    0.0D, 0.03D, 0.0D
            );
        }
    }
}
