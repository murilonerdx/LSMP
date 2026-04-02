package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

public class WhiteMatterBombItem extends Item {
    public WhiteMatterBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockPos pos = player.blockPosition();
            
            // Purge chunk
            level.playSound(null, pos, ModSounds.CLEAR_HUM.get(), SoundSource.PLAYERS, 2.0F, 0.5F);
            purgeAndBeautify(serverLevel, pos);
            
            // Set time and weather
            serverLevel.setDayTime(6000L); // Noon
            serverLevel.setWeatherParameters(0, 0, false, false);
            
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void purgeAndBeautify(ServerLevel level, BlockPos center) {
        LevelChunk chunk = level.getChunkAt(center);
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(minX + x, y, minZ + z);
                    processBlock(level, pos);
                }
            }
        }
    }

    private void processBlock(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.DARK_MATTER_BLOCK.get()) ||
            level.getBlockState(pos).is(ModBlocks.CORRUPTED_SOIL.get()) ||
            level.getBlockState(pos).is(ModBlocks.INFECTION_GROWTH.get())) {
            
            level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
            
            // Add flowers and beauty
            if (level.getBlockState(pos.above()).isAir()) {
                float roll = level.getRandom().nextFloat();
                if (roll < 0.05f) level.setBlockAndUpdate(pos.above(), Blocks.POPPY.defaultBlockState());
                else if (roll < 0.1f) level.setBlockAndUpdate(pos.above(), Blocks.DANDELION.defaultBlockState());
                else if (roll < 0.15f) level.setBlockAndUpdate(pos.above(), Blocks.AZURE_BLUET.defaultBlockState());
                else if (roll < 0.2f) level.setBlockAndUpdate(pos.above(), Blocks.OXEYE_DAISY.defaultBlockState());
            }
        }
    }
}
