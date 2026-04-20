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
                        // --- Clear Matter Tools ---
                        output.accept(ModItems.CLEAR_MATTER_SWORD.get());
                        output.accept(ModItems.CLEAR_MATTER_PICKAXE.get());
                        output.accept(ModItems.CLEAR_MATTER_AXE.get());
                        // --- Yellow Matter Tools ---
                        output.accept(ModItems.YELLOW_MATTER_SWORD.get());
                        output.accept(ModItems.YELLOW_MATTER_PICKAXE.get());
                        output.accept(ModItems.YELLOW_MATTER_AXE.get());
                        output.accept(ModItems.YELLOW_MATTER_SHIELD.get());
                        // --- Containment Suit ---
                        output.accept(ModItems.CONTAINMENT_SUIT_HELMET.get());
                        output.accept(ModItems.CONTAINMENT_SUIT_CHESTPLATE.get());
                        output.accept(ModItems.CONTAINMENT_SUIT_LEGGINGS.get());
                        output.accept(ModItems.CONTAINMENT_SUIT_BOOTS.get());
                        // --- New Infection Blocks ---
                        output.accept(ModItems.CORRUPTED_STONE_ITEM.get());
                        output.accept(ModItems.INFECTION_VEIN_ITEM.get());
                        output.accept(ModItems.SPORE_BLOOM_ITEM.get());
                        output.accept(ModItems.CORRUPTED_LOG_ITEM.get());
                        output.accept(ModItems.WHITE_MATTER_TNT_ITEM.get());
                        output.accept(ModItems.GLITCH_BLOCK_ITEM.get());
                        output.accept(ModItems.WORMHOLE_BLOCK_ITEM.get());
                        // --- Buckets ---
                        output.accept(ModItems.DARK_MATTER_BUCKET.get());
                        output.accept(ModItems.CLEAR_MATTER_BUCKET.get());
                        output.accept(ModItems.YELLOW_MATTER_BUCKET.get());
                        // --- Medical ---
                        output.accept(ModItems.WHITE_MATTER_SYRINGE.get());
                        // --- Special Items ---
                        output.accept(ModItems.PROTECTION_RUBY.get());
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
//                        output.accept(ModItems.FIELD_JOURNAL.get());
                        output.accept(ModItems.EYE_OF_HORUS.get());
                        output.accept(ModItems.EQUILIBRIUM_FRAGMENT.get());
                        output.accept(ModItems.EXPEDITION_TRACKER.get());
                        output.accept(ModItems.MATTER_AMPOULE.get());
                        // --- New Infection Blocks ---
                        output.accept(ModItems.SCARRED_EARTH_ITEM.get());
                        output.accept(ModItems.SCARRED_STONE_ITEM.get());
                        output.accept(ModItems.QUARANTINE_WARD_ITEM.get());
                        output.accept(ModItems.UNSTABLE_MATTER_ITEM.get());
                        output.accept(ModItems.INFECTION_HEART_ITEM.get());
                        // --- Worker Admin Tools ---
                        output.accept(ModItems.WORKER_TELEPORTER.get());
                        output.accept(ModItems.WORKER_LIGHTNING.get());
                        output.accept(ModItems.WORKER_INVENTORY_VIEWER.get());
                        output.accept(ModItems.WORKER_VOICE_BOX.get());
                        output.accept(ModItems.WORKER_CLONE.get());
                        output.accept(ModItems.ADMIN_TOOL.get());
                        // --- Control & Prison Items ---
                        output.accept(ModItems.GRAVITY_TRAP.get());
                        output.accept(ModItems.REVELATION_LENS.get());
                        output.accept(ModItems.GRAVITY_ANCHOR.get());
                        output.accept(ModItems.FREEZE_STAFF.get());
                        output.accept(ModItems.MARKING_STICK.get());
                        output.accept(ModItems.EXECUTION_STICK.get());
                        output.accept(ModItems.SUMMON_STAFF.get());
                        // --- Order Weapons ---
                        output.accept(ModItems.HOLY_BLADE.get());
                        output.accept(ModItems.HOLY_HAMMER.get());
                        // --- Blood Fountain ---
                        output.accept(ModItems.BLOOD_FOUNTAIN_ITEM.get());
                        // --- Blood Ritual / Proliferation ---
                        output.accept(ModItems.CHALK.get());
                        output.accept(ModItems.CHALK_SYMBOL_ITEM.get());
                        output.accept(ModItems.BLOOD_CURE_PILL.get());
                        output.accept(ModItems.BLOOD_ALTAR_ITEM.get());
                        output.accept(ModItems.LIVING_FLESH_ITEM.get());
                        output.accept(ModItems.FLESH_MOTHER_ITEM.get());
                        output.accept(ModItems.ATTACKING_FLESH_ITEM.get());
                        output.accept(ModItems.BLOOD_INFECTION_BLOCK_ITEM.get());
                        output.accept(ModItems.BLOOD_INFESTATION_BLOCK_ITEM.get());
                        output.accept(ModItems.BLOOD_VOLCANO_ITEM.get());
                        output.accept(ModItems.BLOOD_SPIKE_ITEM.get());
                        output.accept(ModItems.BLOOD_BUCKET.get());
                        // --- Blood terrain variants ---
                        output.accept(ModItems.BLOOD_DIRT_ITEM.get());
                        output.accept(ModItems.BLOOD_SAND_ITEM.get());
                        output.accept(ModItems.BLOOD_STONE_ITEM.get());
                        output.accept(ModItems.BLOOD_COAL_ORE_ITEM.get());
                        output.accept(ModItems.BLOOD_IRON_ORE_ITEM.get());
                        output.accept(ModItems.BLOOD_GOLD_ORE_ITEM.get());
                        output.accept(ModItems.BLOOD_DIAMOND_ORE_ITEM.get());
                        output.accept(ModItems.BLOOD_REDSTONE_ORE_ITEM.get());
                        output.accept(ModItems.BLOOD_LAPIS_ORE_ITEM.get());
                        output.accept(ModItems.BLOOD_EMERALD_ORE_ITEM.get());
                        // --- Blood Armor ---
                        output.accept(ModItems.BLOOD_HELMET.get());
                        output.accept(ModItems.BLOOD_CHESTPLATE.get());
                        output.accept(ModItems.BLOOD_LEGGINGS.get());
                        output.accept(ModItems.BLOOD_BOOTS.get());
                        // --- Order Armor / Spells ---
                        output.accept(ModItems.ORDER_HELMET.get());
                        output.accept(ModItems.ORDER_CHESTPLATE.get());
                        output.accept(ModItems.ORDER_LEGGINGS.get());
                        output.accept(ModItems.ORDER_BOOTS.get());
                        output.accept(ModItems.HOLY_SMITE_STAFF.get());
                        output.accept(ModItems.SANCTIFY_ORB.get());
                        output.accept(ModItems.BLOOD_SCYTHE.get());
                        // --- A Mãe (Fase 2) ---
                        output.accept(ModItems.HEART_OF_FLESH_BLOCK_ITEM.get());
                        output.accept(ModItems.HEART_OF_FLESH_ITEM.get());
                        output.accept(ModItems.HEART_OF_THE_MOTHER.get());
                        output.accept(ModItems.SANGUINE_CORE.get());
                        output.accept(ModItems.SANGUINE_ESSENCE.get());
                        output.accept(ModItems.FLESH_MOTHER_BOSS_EGG.get());
                        // --- Ordem × Sangue (Fase 5) ---
                        output.accept(ModItems.ORDER_SHRINE_ITEM.get());
                        output.accept(ModItems.DESECRATED_HOLY_RELIC.get());
                        output.accept(ModItems.ORDER_PALADIN_EGG.get());
                        // --- Armas & Magia (Fase 4) ---
                        output.accept(ModItems.HEMOMANCER_STAFF.get());
                        output.accept(ModItems.BLOOD_BOW.get());
                        output.accept(ModItems.BLOOD_RITUAL_DAGGER.get());
                        output.accept(ModItems.BLOOD_PACT_AMULET.get());
                        // --- Alquimia (Fase 3) ---
                        output.accept(ModItems.BLOOD_CAULDRON_ITEM.get());
                        output.accept(ModItems.BLOOD_VIAL.get());
                        output.accept(ModItems.BLOOD_VIAL_FILLED.get());
                        output.accept(ModItems.CONGEALED_BLOOD.get());
                        output.accept(ModItems.FLESH_THREAD.get());
                        // --- Culto do Sangue (Fase 1) ---
                        output.accept(ModItems.BLOODY_RAG.get());
                        output.accept(ModItems.RUSTED_DAGGER.get());
                        output.accept(ModItems.PRIEST_SIGIL.get());
                        output.accept(ModItems.TOME_OF_THE_MOTHER.get());
                        output.accept(ModItems.TOME_OF_THE_PILGRIM.get());
                        // --- Spawn Eggs ---
                        output.accept(ModItems.FLESH_CRAWLER_EGG.get());
                        output.accept(ModItems.GORE_WORM_EGG.get());
                        output.accept(ModItems.BLOOD_CULTIST_EGG.get());
                        output.accept(ModItems.BLOOD_PRIEST_EGG.get());
                        output.accept(ModItems.WOUNDED_PILGRIM_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
