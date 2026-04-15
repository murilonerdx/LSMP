package br.com.murilo.liberthia.entry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LiberthiaMod.MODID);

    public static final RegistryObject<Item> ADMIN_TOOL = ITEMS.register(
            "admin_tool",
            () -> new AdminToolItem(new Item.Properties())
    );

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
