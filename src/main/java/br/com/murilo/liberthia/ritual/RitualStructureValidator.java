package br.com.murilo.liberthia.ritual;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class RitualStructureValidator {

    private RitualStructureValidator() {
    }

    public static Optional<Component> validate(Level level, BlockPos pedestalPos) {
        if (!level.getBlockState(pedestalPos.east()).is(ModBlocks.BONE_RUNE.get())) {
            return Optional.of(Component.literal("O símbolo de ossos precisa estar à direita do pedestal."));
        }

        if (!level.getBlockState(pedestalPos.west()).is(ModBlocks.GOLD_RUNE.get())) {
            return Optional.of(Component.literal("O símbolo de ouro precisa estar à esquerda do pedestal."));
        }

        if (!level.getBlockState(pedestalPos.north()).is(ModBlocks.DIAMOND_RUNE.get())) {
            return Optional.of(Component.literal("O símbolo de diamante precisa estar acima do pedestal."));
        }

        if (!level.getBlockState(pedestalPos.south()).is(ModBlocks.NETHERITE_RUNE.get())) {
            return Optional.of(Component.literal("O símbolo de netherite precisa estar abaixo do pedestal."));
        }

        if (!isCandle(level.getBlockState(pedestalPos.north().east()))) {
            return Optional.of(Component.literal("Falta uma vela no canto nordeste."));
        }

        if (!isCandle(level.getBlockState(pedestalPos.north().west()))) {
            return Optional.of(Component.literal("Falta uma vela no canto noroeste."));
        }

        if (!isCandle(level.getBlockState(pedestalPos.south().east()))) {
            return Optional.of(Component.literal("Falta uma vela no canto sudeste."));
        }

        if (!isCandle(level.getBlockState(pedestalPos.south().west()))) {
            return Optional.of(Component.literal("Falta uma vela no canto sudoeste."));
        }

        return Optional.empty();
    }

    private static boolean isCandle(BlockState state) {
        return state.is(Blocks.CANDLE)
                || state.is(Blocks.WHITE_CANDLE)
                || state.is(Blocks.BLACK_CANDLE)
                || state.is(Blocks.RED_CANDLE)
                || state.is(Blocks.PURPLE_CANDLE)
                || state.is(Blocks.BLUE_CANDLE)
                || state.is(Blocks.LIGHT_BLUE_CANDLE)
                || state.is(Blocks.CYAN_CANDLE)
                || state.is(Blocks.GREEN_CANDLE)
                || state.is(Blocks.LIME_CANDLE)
                || state.is(Blocks.YELLOW_CANDLE)
                || state.is(Blocks.ORANGE_CANDLE)
                || state.is(Blocks.BROWN_CANDLE)
                || state.is(Blocks.GRAY_CANDLE)
                || state.is(Blocks.LIGHT_GRAY_CANDLE)
                || state.is(Blocks.MAGENTA_CANDLE)
                || state.is(Blocks.PINK_CANDLE);
    }
}
