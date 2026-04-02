package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LiberthiaMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.liberthia.main"))
                    .icon(() -> ModItems.DARK_MATTER_BUCKET.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // --- Blocos ---
                        output.accept(ModItems.DARK_MATTER_BLOCK_ITEM.get());
                        output.accept(ModItems.CORRUPTED_SOIL_ITEM.get());
                        output.accept(ModItems.CLEAR_MATTER_BLOCK_ITEM.get());
                        output.accept(ModItems.YELLOW_MATTER_BLOCK_ITEM.get());
                        output.accept(ModItems.DARK_MATTER_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_DARK_MATTER_ORE_ITEM.get());
                        output.accept(ModItems.WHITE_MATTER_ORE_ITEM.get());
                        output.accept(ModItems.INFECTION_GROWTH_ITEM.get());
                        output.accept(ModItems.PURIFICATION_BENCH_ITEM.get());
                        output.accept(ModItems.WHITE_MATTER_BOMB_ITEM.get());
                        output.accept(ModItems.PURITY_BEACON_ITEM.get());
                        // --- Workbenches ---
                        output.accept(ModItems.DARK_MATTER_FORGE_ITEM.get());
                        output.accept(ModItems.MATTER_INFUSER_ITEM.get());
                        output.accept(ModItems.RESEARCH_TABLE_ITEM.get());
                        output.accept(ModItems.CONTAINMENT_CHAMBER_ITEM.get());
                        output.accept(ModItems.MATTER_TRANSMUTER_ITEM.get());
                        // --- Materiais ---
                        output.accept(ModItems.DARK_MATTER_BUCKET.get());
                        output.accept(ModItems.DARK_MATTER_SHARD.get());
                        output.accept(ModItems.YELLOW_MATTER_INGOT.get());
                        output.accept(ModItems.HOLY_ESSENCE.get());
                        // --- Ferramentas ---
                        output.accept(ModItems.CLEANSING_GRENADE.get());
                        output.accept(ModItems.GEIGER_COUNTER.get());
                        output.accept(ModItems.CLEAR_MATTER_INJECTOR.get());
                        output.accept(ModItems.CLEAR_MATTER_PILL.get());
                        output.accept(ModItems.CLEAR_MATTER_SHIELD.get());
                        output.accept(ModItems.WHITE_LIGHT_WAND.get());
                        output.accept(ModItems.WHITE_MATTER_FINDER.get());
                        output.accept(ModItems.SAFE_SIPHON.get());
                        // --- Armadura Yellow ---
                        output.accept(ModItems.YELLOW_MATTER_HELMET.get());
                        output.accept(ModItems.YELLOW_MATTER_CHESTPLATE.get());
                        output.accept(ModItems.YELLOW_MATTER_LEGGINGS.get());
                        output.accept(ModItems.YELLOW_MATTER_BOOTS.get());
                        // --- Armadura Clear ---
                        output.accept(ModItems.CLEAR_MATTER_HELMET.get());
                        output.accept(ModItems.CLEAR_MATTER_CHESTPLATE.get());
                        output.accept(ModItems.CLEAR_MATTER_LEGGINGS.get());
                        output.accept(ModItems.CLEAR_MATTER_BOOTS.get());
                        // --- Dark Matter Tools ---
                        output.accept(ModItems.DARK_MATTER_SWORD.get());
                        output.accept(ModItems.DARK_MATTER_PICKAXE.get());
                        output.accept(ModItems.DARK_MATTER_AXE.get());
                        // --- New Materials ---
                        output.accept(ModItems.STABILIZED_DARK_MATTER.get());
                        output.accept(ModItems.VOID_CRYSTAL.get());
                        output.accept(ModItems.SINGULARITY_CORE.get());
                        output.accept(ModItems.MATTER_CORE.get());
                        output.accept(ModItems.PURIFIED_ESSENCE.get());
                        output.accept(ModItems.RESEARCH_NOTES.get());
                        // --- Lore ---
                        output.accept(ModItems.HOST_JOURNAL.get());
                        output.accept(ModItems.WORKER_BADGE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
