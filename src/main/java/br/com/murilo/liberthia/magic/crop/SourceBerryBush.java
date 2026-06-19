package br.com.murilo.liberthia.magic.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.24 r103: <b>Source Berry Bush</b> — variante de sweet berry. Berries
 * dropam quando o player coleta. Não machuca o player ao caminhar.
 *
 * <p>Quando o player come a berry, restaura source NBT pra futuro Source cap
 * system (placeholder: +20 NBT liberthia.source_bonus).
 */
public class SourceBerryBush extends SweetBerryBushBlock {

    public SourceBerryBush(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // No prickly damage — source berries são "amigáveis"
    }
}
