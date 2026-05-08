package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * Registers the {@code sanguine} wood + block set types so vanilla classes
 * (DoorBlock, FenceGateBlock, PressurePlateBlock) accept them.
 *
 * <p>Usa reflection pra lidar com diferentes versões do Forge que podem ter
 * ou não o método estático {@code register(...)}. Se {@code register} existir,
 * adiciona ao mapa global do Mojang (necessário pra persistência por nome em
 * algumas versões); senão, só guardamos a instância.
 */
public final class ModWoodTypes {
    public static final BlockSetType SANGUINE_SET = createSet(LiberthiaMod.MODID + ":sanguine");
    public static final WoodType SANGUINE = createWood(LiberthiaMod.MODID + ":sanguine", SANGUINE_SET);

    private ModWoodTypes() {}
    public static void bootstrap() { /* class load triggers static init */ }

    private static BlockSetType createSet(String name) {
        BlockSetType instance = new BlockSetType(name);
        try {
            // Forge/MC versions where register() exists — usa reflection pra evitar
            // NoSuchMethodError em versões mais antigas.
            java.lang.reflect.Method m = BlockSetType.class.getMethod("register", BlockSetType.class);
            m.invoke(null, instance);
        } catch (NoSuchMethodException ignored) {
            // Versão antiga: construtor já basta pra DoorBlock/etc.
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("BlockSetType.register falhou pra {}: {}", name, e.getMessage());
        }
        return instance;
    }

    private static WoodType createWood(String name, BlockSetType set) {
        WoodType instance = new WoodType(name, set);
        try {
            java.lang.reflect.Method m = WoodType.class.getMethod("register", WoodType.class);
            m.invoke(null, instance);
        } catch (NoSuchMethodException ignored) {
            // OK — algumas versões não têm register público
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("WoodType.register falhou pra {}: {}", name, e.getMessage());
        }
        return instance;
    }
}
