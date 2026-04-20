package br.com.murilo.liberthia.init;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.TeleportExecutorStickItem;
import br.com.murilo.liberthia.item.TeleportMarkerStickItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TeleportToolsItems {
    public static Item TELEPORT_MARKER_STICK;
    public static Item TELEPORT_EXECUTOR_STICK;

    private TeleportToolsItems() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, helper -> {
            TELEPORT_MARKER_STICK = new TeleportMarkerStickItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
            TELEPORT_EXECUTOR_STICK = new TeleportExecutorStickItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

            helper.register(new ResourceLocation(LiberthiaMod.MODID, "teleport_marker_stick"), TELEPORT_MARKER_STICK);
            helper.register(new ResourceLocation(LiberthiaMod.MODID, "teleport_executor_stick"), TELEPORT_EXECUTOR_STICK);
        });
    }

    @SubscribeEvent
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            if (TELEPORT_MARKER_STICK != null) {
                event.accept(TELEPORT_MARKER_STICK);
            }
            if (TELEPORT_EXECUTOR_STICK != null) {
                event.accept(TELEPORT_EXECUTOR_STICK);
            }
        }
    }
}
