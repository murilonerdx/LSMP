package br.com.murilo.liberthia.magic.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * v0.1.24 r103: <b>Magebloom Crop</b> — planta que drop "fiber" usável em
 * robes / curio recipes.
 *
 * <p>Standard 4-stage CropBlock. Seed = mage_bloom_seed item, drop = mage_bloom_fiber.
 */
public class MageBloomCrop extends CropBlock {

    public MageBloomCrop(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected net.minecraft.world.level.ItemLike getBaseSeedId() {
        return br.com.murilo.liberthia.registry.ModItems.MAGE_BLOOM_SEED.get();
    }
}
