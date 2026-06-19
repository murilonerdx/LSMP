package br.com.murilo.liberthia.block;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

/**
 * Tree grower que aponta pro datapack {@code data/liberthia/worldgen/configured_feature/blood_tree.json}.
 *
 * <p>Usado pelo {@link BloodSaplingBlock} pra crescer árvore quando o sapling
 * recebe random tick com luz suficiente, ou quando o jogador usa bone meal.
 */
public class BloodTreeGrower extends AbstractTreeGrower {

    public static final ResourceKey<ConfiguredFeature<?, ?>> BLOOD_TREE_KEY =
            ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                    new ResourceLocation("liberthia", "blood_tree"));

    @Nullable
    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean largeHive) {
        return BLOOD_TREE_KEY;
    }
}
