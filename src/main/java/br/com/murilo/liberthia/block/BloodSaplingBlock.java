package br.com.murilo.liberthia.block;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Blood Sapling — sapling que cresce em {@code blood_log}/{@code blood_leaves}
 * usando o configured feature {@code liberthia:blood_tree}.
 *
 * <p>Diferente do {@code SanguineSaplingBlock} (BushBlock cosmético), este
 * estende {@link SaplingBlock} de verdade — então usa {@link BloodTreeGrower}
 * pra invocar o tree feature do datapack durante tick aleatório ou bone meal.
 */
public class BloodSaplingBlock extends SaplingBlock {
    public BloodSaplingBlock(BlockBehaviour.Properties props) {
        super(new BloodTreeGrower(), props);
    }
}
