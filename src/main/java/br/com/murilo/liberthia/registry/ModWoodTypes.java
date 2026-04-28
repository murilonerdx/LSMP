package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * Registers the {@code sanguine} wood + block set types so vanilla classes
 * (DoorBlock, FenceGateBlock, PressurePlateBlock) accept them.
 */
public final class ModWoodTypes {
    public static final BlockSetType SANGUINE_SET =
            BlockSetType.register(new BlockSetType(LiberthiaMod.MODID + ":sanguine"));

    public static final WoodType SANGUINE =
            WoodType.register(new WoodType(LiberthiaMod.MODID + ":sanguine", SANGUINE_SET));

    private ModWoodTypes() {}
    public static void bootstrap() { /* class load triggers static init */ }
}
