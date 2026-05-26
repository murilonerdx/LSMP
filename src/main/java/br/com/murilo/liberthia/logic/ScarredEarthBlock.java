package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cicatriz no terreno — substitui solo corrompido quando infecção é limpa.
 * Bloco simples sem randomTick.
 *
 * <p>Emite aura fina de matéria escura nos arredores (resíduo da infecção).
 * Detectável pelo SampleVialItem.
 */
public class ScarredEarthBlock extends Block {
    public ScarredEarthBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Aura suave — scar tem só resíduo (visual cosmético; ignora DevMode)
        DarkMatterAura.emit(level, pos, random, 0.6f);
    }
}
