package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Eye of Decay — right-click on dirt/grass to corrupt a 5×5 patch (creates
 * corrupted_soil + occasional infection_growth on top). Cooldown 60s.
 */
public class EyeOfDecayItem extends Item {
    public EyeOfDecayItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ServerLevel sl = (ServerLevel) level;
        BlockPos origin = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();

        int converted = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos p = origin.offset(dx, 0, dz);
                BlockState s = sl.getBlockState(p);
                if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.PODZOL)
                        || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.MYCELIUM)) {
                    sl.setBlockAndUpdate(p, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
                    if (sl.getBlockState(p.above()).isAir() && sl.random.nextFloat() < 0.30F) {
                        sl.setBlockAndUpdate(p.above(),
                                ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
                    }
                    sl.sendParticles(ParticleTypes.SCULK_SOUL,
                            p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5,
                            6, 0.4, 0.3, 0.4, 0.02);
                    converted++;
                }
            }
        }

        if (converted > 0) {
            sl.playSound(null, origin, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 0.8F, 0.7F);
            player.getCooldowns().addCooldown(this, 1200); // 60s
            if (!player.isCreative()) stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(ctx.getHand()));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
