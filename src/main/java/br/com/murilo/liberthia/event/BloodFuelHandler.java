package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * BloodFuelHandler — torna os blocos da árvore de sangue combustíveis na
 * furnace. Valores espelhando madeira vanilla (oak = 300 pra log, 200 pra
 * planks/fence/slab/stairs).
 *
 * <p>Usando {@link FurnaceFuelBurnTimeEvent} em vez de override no BlockItem
 * porque assim funciona pra todas as variantes (slab/stairs/etc) com um único
 * listener — limpo, central, sem precisar tocar nos register sites.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BloodFuelHandler {
    private BloodFuelHandler() {}

    @SubscribeEvent
    public static void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        Item item = stack.getItem();

        // Logs e wood-equivalents — 300 ticks (15 s, igual oak_log).
        if (item == ModBlocks.BLOOD_LOG.get().asItem()
                || item == ModBlocks.STRIPPED_BLOOD_LOG.get().asItem()) {
            event.setBurnTime(300);
            return;
        }
        // Planks-derived (slab dá 150 vanilla, mas pra consistência usamos 200).
        if (item == ModBlocks.BLOOD_PLANKS.get().asItem()
                || item == ModBlocks.BLOOD_STAIRS.get().asItem()
                || item == ModBlocks.BLOOD_FENCE.get().asItem()
                || item == ModBlocks.BLOOD_FENCE_GATE.get().asItem()
                || item == ModBlocks.BLOOD_TRAPDOOR.get().asItem()) {
            event.setBurnTime(200);
            return;
        }
        // Slab vanilla dá 150 ticks (metade duma planks).
        if (item == ModBlocks.BLOOD_SLAB.get().asItem()) {
            event.setBurnTime(150);
            return;
        }
        // Sapling — 100 ticks (5 s), igual oak_sapling.
        if (item == ModBlocks.BLOOD_SAPLING.get().asItem()) {
            event.setBurnTime(100);
            return;
        }
        // Door vanilla = 200 ticks.
        if (item == ModBlocks.BLOOD_DOOR.get().asItem()) {
            event.setBurnTime(200);
        }
    }
}
