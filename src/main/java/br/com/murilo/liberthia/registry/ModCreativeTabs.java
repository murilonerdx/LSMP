package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    // DeferredRegister.create(ResourceLocation, String) é o overload que NÃO
    // depende de SRG remap do MC vanilla. A própria Forge resolve o SRG do
    // ResourceKey internamente, no JAR dela — o bytecode do nosso mod só
    // contém uma chamada pra um método público da Forge.
    //
    // Histórico das tentativas anteriores (todas com SRG mismatch entre
    // Forge 47.4.18 compile e 47.4.0 runtime):
    //   1. ResourceKey.createRegistryKey(...) → NoSuchMethodError
    //   2. Registries.CREATIVE_MODE_TAB       → NoSuchFieldError
    //   3. ForgeRegistries.Keys.CREATIVE_MODE_TABS → nem existe
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(
                    new ResourceLocation("minecraft", "creative_mode_tab"),
                    LiberthiaMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.liberthia.main"))
                    .icon(() -> ModItems.DARK_MATTER_BUCKET.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // --- Blocos ---
                        output.accept(ModItems.DARK_MATTER_BLOCK_ITEM.get());
                        output.accept(ModItems.CORRUPTED_SOIL_ITEM.get());
                        output.accept(ModItems.CLEAR_MATTER_BLOCK_ITEM.get());
                        output.accept(ModItems.CLEAR_MATTER_BLOCK_ITEM.get());
                        output.accept(ModItems.YELLOW_MATTER_BLOCK_ITEM.get());
                        output.accept(ModItems.SWORD_BRUM.get());
                        output.accept(ModItems.DARK_MATTER_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_DARK_MATTER_ORE_ITEM.get());
                        output.accept(ModItems.WHITE_MATTER_ORE_ITEM.get());
                        output.accept(ModItems.INFECTION_GROWTH_ITEM.get());
                        output.accept(ModItems.PURIFICATION_BENCH_ITEM.get());
                        // REMOVIDO v0.1.13: WHITE_MATTER_BOMB_ITEM
                        output.accept(ModItems.PURITY_BEACON_ITEM.get());

                        // --- Workbenches ---
                        output.accept(ModItems.DARK_MATTER_FORGE_ITEM.get());
                        output.accept(ModItems.MATTER_INFUSER_ITEM.get());
                        output.accept(ModItems.RESEARCH_TABLE_ITEM.get());
                        output.accept(ModItems.CONTAINMENT_CHAMBER_ITEM.get());
                        output.accept(ModItems.MATTER_TRANSMUTER_ITEM.get());
                        output.accept(ModItems.DARK_MATTER_ALCHEMIZER_ITEM.get());
                        output.accept(ModItems.DARK_MATTER_GENERATOR_ITEM.get());
                        output.accept(ModItems.ENERGY_CABLE_ITEM.get());
                        output.accept(ModItems.ITEM_PIPE_ITEM.get());
                        output.accept(ModItems.ITEM_EXTRACTOR_ITEM.get());
                        output.accept(ModItems.ITEM_INSERTER_ITEM.get());
                        output.accept(ModItems.DIMENSIONAL_CHEST_ITEM.get());
                        output.accept(ModItems.MATTER_REFINER_ITEM.get());
                        output.accept(ModItems.SPEED_UPGRADE.get());
                        output.accept(ModItems.EFFICIENCY_UPGRADE.get());
                        output.accept(ModItems.CAPACITY_UPGRADE.get());
                        // --- Cadeia de Refinação (Turn 1-3) ---
                        output.accept(ModItems.DARK_MATTER_CHEST_ITEM.get());
                        output.accept(ModItems.FRAGMENTED_GENERATOR_ITEM.get());
                        output.accept(ModItems.LASER_EMITTER_ITEM.get());
                        output.accept(ModItems.CRYSTALLIZER_ITEM.get());
                        output.accept(ModItems.AUTO_FARMER_ITEM.get());
                        output.accept(ModItems.DIMENSIONAL_EXTRACTOR_ITEM.get());
                        output.accept(ModItems.INACTIVE_DARK_MATTER.get());
                        output.accept(ModItems.ACTIVE_DARK_MATTER.get());
                        output.accept(ModItems.DARK_MATTER_CATALYST.get());
                        output.accept(ModItems.DIMENSIONAL_COMPASS.get());
                        output.accept(ModItems.ENERGY_METER.get());
                        output.accept(ModItems.CONTAINMENT_GLOVE.get());
                        // --- Lab/Lore ---
                        output.accept(ModItems.MATTER_ANALYZER_ITEM.get());
                        output.accept(ModItems.LIBERTHIA_MANUAL.get());
                        // --- Baterias ---
                        output.accept(ModItems.BATTERY_BASIC_ITEM.get());
                        output.accept(ModItems.BATTERY_ADVANCED_ITEM.get());
                        output.accept(ModItems.BATTERY_QUANTUM_ITEM.get());
                        // REMOVIDO v0.1.13: LIBERTHIA_WRENCH
                        output.accept(ModItems.PYLON_REMOTE.get());
                        output.accept(ModItems.DARK_MATTER_CELL.get());
                        output.accept(ModItems.WIRELESS_CHARGER_ITEM.get());
                        output.accept(ModItems.SAMPLE_VIAL.get());
                        output.accept(ModItems.RESEARCHER_CODEX.get());
                        output.accept(ModItems.HORUS_EYE_SHARD.get());
                        output.accept(ModItems.EQUILIBRIUM_CRYSTAL.get());
                        output.accept(ModItems.PLAYER_LOCK.get());
                        // --- Materiais ---
                        output.accept(ModItems.DARK_MATTER_BUCKET.get());
                        output.accept(ModItems.DARK_MATTER_SHARD.get());
                        output.accept(ModItems.YELLOW_MATTER_INGOT.get());
                        // v0.1.52: ingots de DM e CM (matching o YM)
                        output.accept(ModItems.DARK_MATTER_INGOT.get());
                        output.accept(ModItems.CLEAR_MATTER_INGOT.get());
                        // v0.1.22: Selo de Passagem — protege contra mobs/blocos hostis de sangue
                        output.accept(ModItems.PASSAGE_SIGIL.get());
                        // v0.1.22: Coroa do Boss — item OP toggleável (HP 1800, aura, gaze, reflect)
                        output.accept(ModItems.BOSS_CROWN.get());
                        // --- Ferramentas ---
                        // REMOVIDO v0.1.13: CLEANSING_GRENADE
                        output.accept(ModItems.GEIGER_COUNTER.get());
                        output.accept(ModItems.CLEAR_MATTER_INJECTOR.get());
                        output.accept(ModItems.CLEAR_MATTER_PILL.get());
                        // v0.1.49: pílulas dedicadas por matter (purgam só o tipo)
                        output.accept(ModItems.DARK_MATTER_PILL.get());
                        output.accept(ModItems.YELLOW_MATTER_PILL.get());
                        output.accept(ModItems.MATTER_CURE.get());
                        output.accept(ModItems.DAILY_PILL.get());
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
                        // REMOVIDO v0.1.13: GLITCH_BLOCK_ITEM
                        output.accept(ModItems.WORMHOLE_BLOCK_ITEM.get());

                        // --- Infected Variant Blocks (v0.1.13) ---
                        output.accept(ModItems.DM_INFECTED_DIRT_ITEM.get());
                        output.accept(ModItems.DM_INFECTED_SAND_ITEM.get());
                        output.accept(ModItems.DM_INFECTED_STONE_ITEM.get());
                        output.accept(ModItems.DM_INFECTED_GRASS_ITEM.get());
                        output.accept(ModItems.WM_BLEACHED_DIRT_ITEM.get());
                        output.accept(ModItems.WM_BLEACHED_SAND_ITEM.get());
                        output.accept(ModItems.WM_BLEACHED_STONE_ITEM.get());
                        output.accept(ModItems.WM_BLEACHED_GRASS_ITEM.get());
                        output.accept(ModItems.YM_UNSTABLE_DIRT_ITEM.get());
                        output.accept(ModItems.YM_UNSTABLE_SAND_ITEM.get());
                        output.accept(ModItems.YM_UNSTABLE_STONE_ITEM.get());
                        output.accept(ModItems.YM_UNSTABLE_GRASS_ITEM.get());

                        // --- Pendant cosmético (v0.1.13) ---
                        output.accept(ModItems.WHITE_MATTER_PENDANT.get());
                        // --- Refined Containment Artifacts (v0.1.30) ---
                        output.accept(ModItems.REFINED_CONTAINMENT_PENDANT.get());
                        output.accept(ModItems.REFINED_CONTAINMENT_GLOVE.get());
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
                        output.accept(ModItems.BLOOD_MAGE_EGG.get());
                        output.accept(ModItems.BLOOD_HOUND_EGG.get());
                        // --- Seringa (T5b) ---
                        output.accept(ModItems.BLOOD_SYRINGE.get());
                        // --- T6: EvilCraft ports ---
                        output.accept(ModItems.BLOOD_TELEPORT_PEARL.get());
                        output.accept(ModItems.TAINTED_APPLE.get());
                        output.accept(ModItems.PURGING_PENDANT.get());
                        // --- T7: attacking blocks ---
                        output.accept(ModItems.WITHERING_EYE_ITEM.get());
                        output.accept(ModItems.VENOM_GEYSER_ITEM.get());
                        output.accept(ModItems.LIGHTNING_COIL_ITEM.get());
                        // --- T8: vanilla-effect throwables ---
                        output.accept(ModItems.VEILING_ORB.get());
                        output.accept(ModItems.MIND_SPLINTER_DART.get());
                        // --- T9: extra attacking blocks ---
                        output.accept(ModItems.THORN_BRIAR_ITEM.get());
                        output.accept(ModItems.LIGHTNING_NODE_ITEM.get());
                        output.accept(ModItems.SCREAMING_SOUL_ITEM.get());
                        output.accept(ModItems.MAGNETIC_PYLON_ITEM.get());
                        // --- T9: throwables + utility ---
                        output.accept(ModItems.LIGHTNING_GRENADE.get());
                        output.accept(ModItems.BURNING_GEM.get());
                        output.accept(ModItems.FROST_FLASK.get());
                        output.accept(ModItems.EYE_OF_DECAY.get());
                        output.accept(ModItems.WITHERED_TOTEM.get());
                        // --- Op tools ---
                        output.accept(ModItems.GROWTH_ROD.get());
                        output.accept(ModItems.SHRINK_ROD.get());
                        output.accept(ModItems.COMMAND_TABLET.get());
                        output.accept(ModItems.COMMAND_PYLON_ITEM.get());
                        output.accept(ModItems.SCRIPT_TABLET.get());
                        // --- Sanguine Ward (anti Blood Infection) ---
                        output.accept(ModItems.SANGUINE_WARD_HELMET.get());
                        output.accept(ModItems.SANGUINE_WARD_CHESTPLATE.get());
                        output.accept(ModItems.SANGUINE_WARD_LEGGINGS.get());
                        output.accept(ModItems.SANGUINE_WARD_BOOTS.get());
                        output.accept(ModItems.SANGUINE_WARD_SWORD.get());
                        output.accept(ModItems.SANGUINE_WARD_PICKAXE.get());
                        output.accept(ModItems.BLOOD_WARD_CHARM.get());
                        output.accept(ModItems.TAINTED_ESSENCE.get());
                        output.accept(ModItems.CLEANSING_SALT.get());
                        output.accept(ModItems.PURIFYING_FLASK.get());
                        // --- Ritual blocks ---
                        output.accept(ModItems.BLOOD_SACRIFICIAL_BOWL_ITEM.get());
                        output.accept(ModItems.GOLDEN_BLOOD_BOWL_ITEM.get());
                        output.accept(ModItems.CRYSTALLIZED_BLOOD_SOUL.get());
                        // --- Blood staves + attacking blood blocks ---
                        output.accept(ModItems.THORN_STAFF.get());
                        output.accept(ModItems.LIGHTNING_STAFF.get());
                        output.accept(ModItems.SOUL_SCREAM_SWORD.get());
                        output.accept(ModItems.MAGNETIC_WAND.get());
                        output.accept(ModItems.HEMORRHAGE_SPIKE_ITEM.get());
                        output.accept(ModItems.SANGUINE_SNARE_ITEM.get());
                        output.accept(ModItems.VEIL_OF_MADNESS_ITEM.get());
                        output.accept(ModItems.PHANTOM_PORTAL_ITEM.get());
                        // --- Occultism ports ---
                        output.accept(ModItems.BLOOD_CHALK.get());
                        output.accept(ModItems.BLOOD_TORCH_ITEM.get());
                        // --- Sanguine Wood set ---
                        output.accept(ModItems.SANGUINE_LOG_ITEM.get());
                        output.accept(ModItems.SANGUINE_WOOD_ITEM.get());
                        output.accept(ModItems.STRIPPED_SANGUINE_LOG_ITEM.get());
                        output.accept(ModItems.STRIPPED_SANGUINE_WOOD_ITEM.get());
                        output.accept(ModItems.SANGUINE_PLANKS_ITEM.get());
                        output.accept(ModItems.SANGUINE_LEAVES_ITEM.get());
                        output.accept(ModItems.SANGUINE_SAPLING_ITEM.get());
                        output.accept(ModItems.SANGUINE_STAIRS_ITEM.get());
                        output.accept(ModItems.SANGUINE_SLAB_ITEM.get());
                        output.accept(ModItems.SANGUINE_FENCE_ITEM.get());
                        output.accept(ModItems.SANGUINE_FENCE_GATE_ITEM.get());
                        output.accept(ModItems.SANGUINE_BUTTON_ITEM.get());
                        output.accept(ModItems.SANGUINE_PRESSURE_PLATE_ITEM.get());
                        output.accept(ModItems.SANGUINE_DOOR_ITEM.get());
                        output.accept(ModItems.SANGUINE_TRAPDOOR_ITEM.get());
                        // --- Blood Tree set ---
                        output.accept(ModItems.BLOOD_LOG_ITEM.get());
                        output.accept(ModItems.STRIPPED_BLOOD_LOG_ITEM.get());
                        output.accept(ModItems.BLOOD_PLANKS_ITEM.get());
                        output.accept(ModItems.BLOOD_LEAVES_ITEM.get());
                        output.accept(ModItems.BLOOD_SAPLING_ITEM.get());
                        output.accept(ModItems.BLOOD_STAIRS_ITEM.get());
                        output.accept(ModItems.BLOOD_SLAB_ITEM.get());
                        output.accept(ModItems.BLOOD_FENCE_ITEM.get());
                        output.accept(ModItems.BLOOD_FENCE_GATE_ITEM.get());
                        output.accept(ModItems.BLOOD_DOOR_ITEM.get());
                        output.accept(ModItems.BLOOD_TRAPDOOR_ITEM.get());
                        // --- Possessed spawn eggs ---
                        output.accept(ModItems.POSSESSED_ZOMBIE_EGG.get());
                        output.accept(ModItems.POSSESSED_SKELETON_EGG.get());
                        // --- Boss artifacts ---
                        output.accept(ModItems.CURSED_IDOL.get());
                        output.accept(ModItems.VEILED_LANTERN.get());
                        output.accept(ModItems.PULSING_HEART.get());
                        // --- New monster spawn eggs ---
                        output.accept(ModItems.BLOOD_WARDEN_EGG.get());
                        output.accept(ModItems.WEAVING_SHADE_EGG.get());
                        output.accept(ModItems.DISARMER_EGG.get());
                        // Pipe utilities
                        output.accept(ModItems.PIPE_FILTER.get());
                        output.accept(ModItems.PIPE_FILTER_NOT.get());
                        // Decoração — pelúcia
                        output.accept(ModBlocks.HOST_PLUSH.get());
                        // Matter Extraction Network — extractor central + tanks + 3 pipes
                        output.accept(ModBlocks.MATTER_EXTRACTOR.get());
                        output.accept(ModBlocks.MATTER_TANK.get());
                        output.accept(ModBlocks.MATTER_PIPE_DARK.get());
                        output.accept(ModBlocks.MATTER_PIPE_CLEAR.get());
                        output.accept(ModBlocks.MATTER_PIPE_YELLOW.get());

                        // Matter Purifier + ingots purificados + dark matter armor
                        output.accept(ModBlocks.MATTER_PURIFIER.get());
                        // v0.1.44: Matter Pill Brewer — alquimia de pílulas
                        output.accept(ModBlocks.MATTER_PILL_BREWER.get());
                        output.accept(ModItems.PURIFIED_DARK_MATTER_INGOT.get());
                        output.accept(ModItems.PURIFIED_CLEAR_MATTER_INGOT.get());
                        output.accept(ModItems.PURIFIED_YELLOW_MATTER_INGOT.get());
                        output.accept(ModItems.DARK_MATTER_HELMET.get());
                        output.accept(ModItems.DARK_MATTER_CHESTPLATE.get());
                        output.accept(ModItems.DARK_MATTER_LEGGINGS.get());
                        output.accept(ModItems.DARK_MATTER_BOOTS.get());

                        output.accept(ModItems.BONE_SEAL.get());
                        output.accept(ModItems.GOLD_SEAL.get());
                        output.accept(ModItems.DIAMOND_SEAL.get());
                        output.accept(ModItems.NETHERITE_SEAL.get());

                        output.accept(ModItems.IMAGE_FRAME_BOOK.get());
                        output.accept(ModItems.IMAGE_FRAME_BOOK_BUILDER.get());
                        output.accept(ModItems.RED_KIRIKO_BOOK.get());

                        // --- v1: Matter Pendants ---
                        output.accept(ModItems.DARK_MATTER_PENDANT.get());
                        output.accept(ModItems.CLEAR_MATTER_PENDANT.get());
                        output.accept(ModItems.YELLOW_MATTER_PENDANT.get());
                        // --- v1: Astaron Relics ---
                        output.accept(ModItems.RELIQUIA_PROTECAO_ASTARON.get());
                        output.accept(ModItems.ASTARON_ACCESS_KEY.get());
                        output.accept(ModItems.PES_QUEIMANTES_ASTARON.get());
                        output.accept(ModItems.FLAME_KEY.get());
                        // --- v1: Mercurial Boots ---
                        output.accept(ModItems.BOTAS_MERCURIAIS.get());
                        // --- v1: Vision Swap Lens ---
                        output.accept(ModItems.VISION_SWAP_LENS.get());
                        // --- v1: Possession Amulet (raro/admin) ---
                        output.accept(ModItems.POSSESSION_AMULET.get());
                        // --- v0.1.22 r22: Mind Ward (antídoto ao Amulet) ---
                        output.accept(ModItems.MIND_WARD.get());
                        // --- v0.1.22 r23: COSMIC HORROR (5 itens de loucura) ---
                        output.accept(ModItems.MADNESS_AURA.get());
                        output.accept(ModItems.MADDENING_GAZE.get());
                        output.accept(ModItems.MASS_POSSESSION_CROWN.get());
                        output.accept(ModItems.MIRROR_OF_INSANITY.get());
                        output.accept(ModItems.SOUL_CLONER.get());
                        // --- v0.1.22 r24: SPIRIT WORLD ---
                        output.accept(ModItems.SOUL_SEVER.get());
                        output.accept(ModItems.SPIRIT_ALTAR_ITEM.get());
                        output.accept(ModItems.SPIRIT_STONE_ITEM.get());
                        // --- v0.1.22 r27: DIMENSIONAL ANTENNA ---
                        output.accept(ModItems.DIMENSIONAL_ANTENNA_ITEM.get());
                        // --- v0.1.22 r28: QUANTUM TERMINAL ---
                        output.accept(ModItems.QUANTUM_TERMINAL_ITEM.get());
                        // --- v0.1.35: Pulso (sonic boom artifact) ---
                        output.accept(ModItems.PULSO.get());
                        // --- v0.1.22 r29: SPIRIT WORLD BLOCKS (12) ---
                        output.accept(ModItems.SPIRIT_GRASS_BLOCK_ITEM.get());
                        output.accept(ModItems.SPIRIT_DIRT_ITEM.get());
                        output.accept(ModItems.ETHEREAL_STONE_ITEM.get());
                        output.accept(ModItems.ETHEREAL_STONE_BRICKS_ITEM.get());
                        output.accept(ModItems.SOUL_BRICK_ITEM.get());
                        output.accept(ModItems.HALO_MARBLE_ITEM.get());
                        output.accept(ModItems.HALO_MARBLE_BRICKS_ITEM.get());
                        output.accept(ModItems.DREAM_GLASS_ITEM.get());
                        output.accept(ModItems.WHISPERWOOD_LOG_ITEM.get());
                        output.accept(ModItems.WHISPERWOOD_PLANKS_ITEM.get());
                        output.accept(ModItems.WHISPERWOOD_LEAVES_ITEM.get());
                        output.accept(ModItems.ASTRAL_LANTERN_ITEM.get());
                        output.accept(ModItems.CRYSTAL_SPIRIT_ORE_ITEM.get());
                        output.accept(ModItems.SANCTUM_WARD_ITEM.get());
                        output.accept(ModItems.HOLY_CENSER_ITEM.get());
                        // --- v0.1.22 r29: COSMIC HORROR ITEMS (8) ---
                        output.accept(ModItems.WHISPERING_VEIL.get());
                        output.accept(ModItems.EYES_OF_ABYSS.get());
                        output.accept(ModItems.CURSED_CRADLE.get());
                        output.accept(ModItems.PENDULUM_OF_DREAD.get());
                        output.accept(ModItems.LANTERN_OF_FALSE_MEMORY.get());
                        output.accept(ModItems.TONGUE_OF_OLD_ONES.get());
                        output.accept(ModItems.HOURGLASS_OF_REGRESSION.get());
                        output.accept(ModItems.VOID_SEER_ORB.get());
                        // --- v0.1.22 r29: ANGEL / SPIRIT MAGIC (12) ---
                        output.accept(ModItems.HALO_OF_LIGHT.get());
                        output.accept(ModItems.WINGS_OF_ASCENSION.get());
                        output.accept(ModItems.ANGEL_TEAR_AMULET.get());
                        output.accept(ModItems.SPIRIT_ANCHOR.get());
                        output.accept(ModItems.CHOIR_BELL.get());
                        output.accept(ModItems.DIVINE_SMITE_ROD.get());
                        output.accept(ModItems.SOUL_MIRROR.get());
                        output.accept(ModItems.SPIRIT_COMPASS.get());
                        output.accept(ModItems.HOLY_WATER_BUCKET.get());
                        output.accept(ModItems.ANGEL_WING_FEATHER.get());
                        output.accept(ModItems.SERAPH_BLADE.get());
                        output.accept(ModItems.PRAYER_BOOK.get());
                        // --- v0.1.22 r32: OCCULT RITUAL SYSTEM ---
                        // Chalks
                        output.accept(ModItems.CHALK_WHITE.get());
                        output.accept(ModItems.CHALK_GOLDEN.get());
                        output.accept(ModItems.CHALK_PURPLE.get());
                        output.accept(ModItems.CHALK_RED.get());
                        output.accept(ModItems.CHALK_BLACK.get());
                        // Candles
                        output.accept(ModItems.CANDLE_WHITE_OCCULT_ITEM.get());
                        output.accept(ModItems.CANDLE_GOLDEN_OCCULT_ITEM.get());
                        output.accept(ModItems.CANDLE_PURPLE_OCCULT_ITEM.get());
                        output.accept(ModItems.CANDLE_RED_OCCULT_ITEM.get());
                        output.accept(ModItems.CANDLE_BLACK_OCCULT_ITEM.get());
                        // Tools
                        output.accept(ModItems.LIGHTER.get());
                        output.accept(ModItems.RITUAL_DAGGER.get());
                        output.accept(ModItems.RITUAL_CHALICE.get());
                        // Blocks
                        output.accept(ModItems.RITUAL_CIRCLE_ITEM.get());
                        output.accept(ModItems.SPIRIT_MINER_ITEM.get());
                        // Sigils (10)
                        output.accept(ModItems.SIGIL_FOLIOT.get());
                        output.accept(ModItems.SIGIL_DJINNI.get());
                        output.accept(ModItems.SIGIL_AFRIT.get());
                        output.accept(ModItems.SIGIL_BAEL.get());
                        output.accept(ModItems.SIGIL_LUCIFER.get());
                        output.accept(ModItems.SIGIL_SANDALPHON.get());
                        output.accept(ModItems.SIGIL_METATRON.get());
                        output.accept(ModItems.SIGIL_NECRO.get());
                        output.accept(ModItems.SIGIL_BANISHING.get());
                        output.accept(ModItems.SIGIL_DIMENSIONAL.get());
                        // Bound crystals
                        output.accept(ModItems.BOUND_FOLIOT_CRYSTAL.get());
                        output.accept(ModItems.BOUND_DJINNI_CRYSTAL.get());
                        output.accept(ModItems.BOUND_AFRIT_CRYSTAL.get());
                        // --- v0.1.22 r33: LOOM DIMENSION ---
                        output.accept(ModItems.DARK_MATTER_LASER.get());
                        output.accept(ModItems.LOOM_WATCHER_EGG.get());
                        output.accept(ModItems.LOOM_PERIPHERAL_EGG.get());
                        output.accept(ModItems.LOOM_SCREAMER_EGG.get());
                        output.accept(ModItems.SKY_TEAR_HORN.get());
                        // r35: COSMIC HORROR SYSTEM trigger
                        output.accept(ModItems.FORBIDDEN_TOME.get());
                        // r36: Grimório de feitiços aprendidos
                        output.accept(ModItems.GRIMOIRE.get());
                        // r40: Creative Grimoire — todos feitiços + mana infinita
                        output.accept(ModItems.CREATIVE_GRIMOIRE.get());
                        // r42: Spell Crafting Table portátil
                        output.accept(ModItems.SPELL_CRAFTING_TABLE.get());
                        // r45: Cosmic Horror Expansion items
                        output.accept(ModItems.CURSED_EFFIGY.get());
                        output.accept(ModItems.WATCHER_MARK.get());
                        output.accept(ModItems.PHANTOM_CALLER.get());
                        output.accept(ModItems.VULTO_LENS.get());
                        output.accept(ModItems.INSANITY_CROWN.get());
                        output.accept(ModItems.TENDRIL_SIGIL.get());
                        output.accept(ModItems.VOICE_CURSE_AMULET.get());
                        output.accept(ModItems.SILENT_WITNESS_CLOAK.get());
                        // r46: Eldritch Artifacts (10 items cosmic horror profundo)
                        output.accept(ModItems.WATCHING_EYE.get());
                        output.accept(ModItems.BLACK_SIGNAL_RADIO.get());
                        output.accept(ModItems.HOLLOW_MASK.get());
                        output.accept(ModItems.FLESH_LANTERN.get());
                        output.accept(ModItems.FALSE_TOTEM.get());
                        output.accept(ModItems.INFECTION_NEEDLE.get());
                        output.accept(ModItems.BOOK_IMPOSSIBLE.get());
                        output.accept(ModItems.MIMIC_HEART.get());
                        output.accept(ModItems.RED_TAPE.get());
                        output.accept(ModItems.NULL_BELL.get());
                        // r47: Living Server Artifacts (4 originais)
                        output.accept(ModItems.GEOMETRY_KEY.get());
                        output.accept(ModItems.LOW_SIGNAL.get());
                        output.accept(ModItems.PALE_THREAD.get());
                        output.accept(ModItems.MIRROR_FRUIT.get());
                        // r48: Reflection Seed (admin artifact)
                        output.accept(ModItems.REFLECTION_SEED.get());
                        // r50: Caretaker Console (GUI tool)
                        output.accept(ModItems.CARETAKER_CONSOLE.get());
                        // r51: 10 Admin Cosmic Artifacts
                        output.accept(ModItems.BLACK_VEIL.get());
                        output.accept(ModItems.TENDRIL_CROWN.get());
                        output.accept(ModItems.FALSE_SUN.get());
                        output.accept(ModItems.MIRROR_PULSE.get());
                        output.accept(ModItems.SILENT_BELL.get());
                        output.accept(ModItems.OPEN_EYE.get());
                        output.accept(ModItems.THREAD_OF_DISTANCE.get());
                        output.accept(ModItems.FLESH_SIGNAL.get());
                        output.accept(ModItems.DEEP_WATER.get());
                        output.accept(ModItems.AUDIENCE_MARK.get());
                        // r54: items que foram esquecidos do tab
                        output.accept(ModItems.MAGIC_BOOK.get());
                        output.accept(ModItems.RIFTITE_ORE_ITEM.get());
                        output.accept(ModItems.RIFTITE_SHARD.get());
                        output.accept(ModItems.UMBRAL_ORE_ITEM.get());
                        output.accept(ModItems.UMBRAL_SHARD.get());
                        output.accept(ModItems.VOIDITE_ORE_ITEM.get());
                        output.accept(ModItems.VOIDITE_SHARD.get());
                        output.accept(ModItems.LOOM_STONE_ITEM.get());
                        output.accept(ModItems.SPIRITUAL_LINK.get());
                        output.accept(ModItems.SPIRITUAL_CONNECTION.get());
                        // r55: PALE WATCH ARTIFACTS
                        output.accept(ModItems.CLONE_ARMY.get());
                        output.accept(ModItems.STAREDOWN_PENDANT.get());
                        output.accept(ModItems.PALE_BLINK_PENDANT.get());
                        output.accept(ModItems.PARALYZE_PENDANT.get());
                        output.accept(ModItems.SPIRIT_GUIDE.get());
                        output.accept(ModItems.PALE_IRON_INGOT.get());
                        output.accept(ModItems.SOULSTEEL_INGOT.get());
                        output.accept(ModItems.RIFT_CRYSTAL.get());

                        // r56: 15 cryptic cosmic artifacts
                        output.accept(ModItems.PULLED_STRING.get());
                        output.accept(ModItems.QUIET_MARK.get());
                        output.accept(ModItems.LONELY_ECHO.get());
                        output.accept(ModItems.FOLDED_DISTANCE.get());
                        output.accept(ModItems.THROAT_SALT.get());
                        output.accept(ModItems.SOFT_WOUND.get());
                        output.accept(ModItems.LOOKING_GLASS.get());
                        output.accept(ModItems.HALF_STEP.get());
                        output.accept(ModItems.BENT_IRON.get());
                        output.accept(ModItems.PALE_COIN.get());
                        output.accept(ModItems.WET_BELL.get());
                        output.accept(ModItems.MARROW_WHISTLE.get());
                        output.accept(ModItems.LISTENING_GLASS.get());
                        output.accept(ModItems.SUNKEN_RING.get());
                        output.accept(ModItems.HAND_ON_GLASS.get());

                        // r56: EXODUS BOOK
                        output.accept(ModItems.EXODUS_BOOK.get());

                        // r61: OBSERVATION CASTING ITEMS
                        output.accept(ModItems.SPELLSWORD.get());
                        output.accept(ModItems.SOURCE_GEM.get());

                        // r62: Observation Tome + Source Jar
                        output.accept(ModItems.OBSERVATION_TOME.get());
                        output.accept(ModItems.SOURCE_JAR_ITEM.get());

                        // r64: Grimório de Observação
                        output.accept(ModItems.GRIMOIRE_OF_OBSERVATION.get());

                        // r68: Spell Parchment + 23 Glyph items
                        output.accept(ModItems.SPELL_PARCHMENT.get());
                        // r69: Scribes Table block (mesa de craft)
                        output.accept(ModItems.SCRIBES_TABLE_ITEM.get());
                        // Methods (azuis)
                        output.accept(ModItems.GLYPH_DIRECT_GAZE.get());
                        output.accept(ModItems.GLYPH_WATCH_PERIPHERAL.get());
                        output.accept(ModItems.GLYPH_WATCH_MEMORY.get());
                        output.accept(ModItems.GLYPH_WATCH_SILENCE.get());
                        output.accept(ModItems.GLYPH_WATCH_REFLECTION.get());
                        output.accept(ModItems.GLYPH_METHOD_TOUCH.get());
                        output.accept(ModItems.GLYPH_METHOD_SELF.get());
                        // Manifestations (roxas)
                        output.accept(ModItems.GLYPH_TENDRIL.get());
                        output.accept(ModItems.GLYPH_MANIFEST_SILENCE.get());
                        output.accept(ModItems.GLYPH_MANIFEST_MIRROR.get());
                        output.accept(ModItems.GLYPH_MANIFEST_WHISPER.get());
                        output.accept(ModItems.GLYPH_MANIFEST_DECAY.get());
                        output.accept(ModItems.GLYPH_MANIFEST_GLIMPSE.get());
                        output.accept(ModItems.GLYPH_EFFECT_IGNITE.get());
                        output.accept(ModItems.GLYPH_EFFECT_HARM.get());
                        output.accept(ModItems.GLYPH_EFFECT_HEAL.get());
                        output.accept(ModItems.GLYPH_EFFECT_FREEZE.get());
                        output.accept(ModItems.GLYPH_EFFECT_LAUNCH.get());
                        output.accept(ModItems.GLYPH_EFFECT_SLOWFALL.get());
                        // Distortions (amarelos)
                        output.accept(ModItems.GLYPH_AMPLIFY.get());
                        output.accept(ModItems.GLYPH_LINGER.get());
                        output.accept(ModItems.GLYPH_ECHO.get());
                        output.accept(ModItems.GLYPH_SECRET.get());
                        // r70: 12 novos glyphs portados AN
                        output.accept(ModItems.GLYPH_METHOD_LASER.get());
                        output.accept(ModItems.GLYPH_METHOD_BURST.get());
                        output.accept(ModItems.GLYPH_METHOD_ORBIT.get());
                        output.accept(ModItems.GLYPH_METHOD_WALL.get());
                        output.accept(ModItems.GLYPH_METHOD_CHAIN.get());
                        output.accept(ModItems.GLYPH_EFFECT_LIGHTNING.get());
                        output.accept(ModItems.GLYPH_EFFECT_GRAVITY.get());
                        output.accept(ModItems.GLYPH_EFFECT_BLIND.get());
                        output.accept(ModItems.GLYPH_EFFECT_LEVITATE.get());
                        output.accept(ModItems.GLYPH_EFFECT_KNOCKBACK.get());
                        output.accept(ModItems.GLYPH_EFFECT_EXPLOSION.get());
                        output.accept(ModItems.GLYPH_EFFECT_FANGS.get());

                        // r71: 25 elemental glyphs (5 per class)
                        // FIRE
                        output.accept(ModItems.GLYPH_FIREBALL.get());
                        output.accept(ModItems.GLYPH_INFERNO.get());
                        output.accept(ModItems.GLYPH_CLEANSING_FLAME.get());
                        output.accept(ModItems.GLYPH_SOLAR_PULSE.get());
                        output.accept(ModItems.GLYPH_BURNING_AURA.get());
                        // WATER
                        output.accept(ModItems.GLYPH_BUBBLE_SHIELD.get());
                        output.accept(ModItems.GLYPH_TIDAL_WAVE.get());
                        output.accept(ModItems.GLYPH_FROST_LANCE.get());
                        output.accept(ModItems.GLYPH_MIST_VEIL.get());
                        output.accept(ModItems.GLYPH_HEALING_RAIN.get());
                        // EARTH
                        output.accept(ModItems.GLYPH_STONE_SPIKES.get());
                        output.accept(ModItems.GLYPH_QUAKE_STEP.get());
                        output.accept(ModItems.GLYPH_VEIN_SIGHT.get());
                        output.accept(ModItems.GLYPH_EARTHEN_WALL.get());
                        output.accept(ModItems.GLYPH_ROOTS.get());
                        // AIR
                        output.accept(ModItems.GLYPH_GUST.get());
                        output.accept(ModItems.GLYPH_TORNADO.get());
                        output.accept(ModItems.GLYPH_SKY_STEP.get());
                        output.accept(ModItems.GLYPH_VELOCITY.get());
                        output.accept(ModItems.GLYPH_WIND_CUTTER.get());
                        // COSMIC
                        output.accept(ModItems.GLYPH_VOID_PULL.get());
                        output.accept(ModItems.GLYPH_DREAD_STARE.get());
                        output.accept(ModItems.GLYPH_MIND_SPIKE.get());
                        output.accept(ModItems.GLYPH_REALITY_TEAR.get());
                        output.accept(ModItems.GLYPH_SINGULARITY.get());
                        // r71: Source upgrade items
                        output.accept(ModItems.SOURCE_CRYSTAL.get());
                        output.accept(ModItems.SOURCE_CATALYST.get());
                        output.accept(ModItems.SOURCE_LENS.get());
                        output.accept(ModItems.SOUL_FRAGMENT.get());

                        // r72: 10 utility glyphs + 3 augments
                        output.accept(ModItems.GLYPH_PLACE_BLOCK.get());
                        output.accept(ModItems.GLYPH_BREAK_BLOCK.get());
                        output.accept(ModItems.GLYPH_CONJURE_WATER.get());
                        output.accept(ModItems.GLYPH_LIGHT.get());
                        output.accept(ModItems.GLYPH_SNARE.get());
                        output.accept(ModItems.GLYPH_HEX.get());
                        output.accept(ModItems.GLYPH_PICKUP.get());
                        output.accept(ModItems.GLYPH_PIERCE.get());
                        output.accept(ModItems.GLYPH_SPLIT.get());
                        output.accept(ModItems.GLYPH_AOE.get());

                        // r72: Chalk + Rune block
                        output.accept(ModItems.OBSERVATION_CHALK.get());
                        output.accept(ModItems.RUNE_BLOCK_ITEM.get());
                        // r73: Imbuement Table block
                        output.accept(ModItems.IMBUEMENT_TABLE_ITEM.get());
                        // r77: Spell Binding Pedestal
                        output.accept(ModItems.SPELL_BINDING_PEDESTAL_ITEM.get());

                        // r74: 3 Grimoire tiers
                        output.accept(ModItems.GRIMOIRE_APPRENTICE.get());
                        output.accept(ModItems.GRIMOIRE_MASTER.get());
                        output.accept(ModItems.GRIMOIRE_ARCHMAGE.get());
                        // r74: Spirit World ores + Mana berries + relay
                        output.accept(ModItems.SOURCESTONE_ORE_ITEM.get());
                        output.accept(ModItems.SPIRIT_GEM_ORE_ITEM.get());
                        output.accept(ModItems.MANA_BERRY_BUSH_ITEM.get());
                        output.accept(ModItems.MANA_BERRY.get());
                        output.accept(ModItems.SOURCE_RELAY_ITEM.get());
                        // r74: Bookwyrm familiar spawn egg
                        output.accept(ModItems.BOOKWYRM_SPAWN_EGG.get());

                        // r72: 8 Prebuilt Spell Tomes
                        output.accept(ModItems.TOME_PYROMANCER.get());
                        output.accept(ModItems.TOME_FROSTBINDER.get());
                        output.accept(ModItems.TOME_SKYWALKER.get());
                        output.accept(ModItems.TOME_WEBWEAVER.get());
                        output.accept(ModItems.TOME_DEATH_BEAM.get());
                        output.accept(ModItems.TOME_HEALING_LIGHT.get());
                        output.accept(ModItems.TOME_DASH.get());
                        output.accept(ModItems.TOME_SINGULARITY.get());

                        // r57: 3 LIMINAL DIMENSION ENTRY KEYS
                        output.accept(ModItems.DROWNED_COMPASS.get());
                        output.accept(ModItems.FOLDED_ADDRESS.get());
                        output.accept(ModItems.BARK_TOKEN.get());

                        // r56: 12 new dimensional ore items
                        output.accept(ModItems.SOULITE_SHARD.get());
                        output.accept(ModItems.PALE_CRYSTAL_SHARD.get());
                        output.accept(ModItems.VEINSTONE_FRAGMENT.get());
                        output.accept(ModItems.HOLLOW_SILVER_NUGGET.get());
                        output.accept(ModItems.MOURNING_EMBER.get());
                        output.accept(ModItems.GHOST_QUARTZ_SHARD.get());
                        output.accept(ModItems.NULL_IRON_SHARD.get());
                        output.accept(ModItems.ABYSSIUM_DUST.get());
                        output.accept(ModItems.BLACK_STAR_CORE.get());
                        output.accept(ModItems.DISTORTION_CRYSTAL.get());
                        output.accept(ModItems.VOID_GOLD_NUGGET.get());
                        output.accept(ModItems.EYE_STONE_SHARD.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
