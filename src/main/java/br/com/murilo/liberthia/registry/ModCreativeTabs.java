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
                        // r179: Fenda Dimensional + 4 blocos de irregularidade
                        output.accept(ModItems.RIFT_OPENER.get());
                        output.accept(ModItems.RIFT_RESIDUE_ITEM.get());
                        output.accept(ModItems.WARPED_SPACE_ITEM.get());
                        output.accept(ModItems.VOID_SCAR_ITEM.get());
                        output.accept(ModItems.DIMENSIONAL_FLUX_ITEM.get());
                        output.accept(ModItems.LUCID_TONIC.get()); // r179: tônico de lucidez
                        // r180: novos mobs + artefatos divinos
                        output.accept(ModItems.O_OBSERVADO_SPAWN_EGG.get());
                        output.accept(ModItems.SILENCE_SHEPHERD_SPAWN_EGG.get());
                        output.accept(ModItems.ASCENSION_SEAL.get());
                        output.accept(ModItems.ETERNITY_CROWN.get());
                        output.accept(ModItems.END_ARCHIVIST_SPAWN_EGG.get());
                        output.accept(ModItems.BLIND_AMALGAM_SPAWN_EGG.get());
                        output.accept(ModItems.PARASITIC_EYE_SPAWN_EGG.get());
                        output.accept(ModItems.CINDER_PARASITE_SPAWN_EGG.get());
                        output.accept(ModItems.CONSTELLATION_CORE.get());
                        output.accept(ModItems.BLIND_ASTRONOMER_SPAWN_EGG.get());
                        output.accept(ModItems.HIVE_HEART.get());
                        output.accept(ModItems.HIVE_QUEEN_SPAWN_EGG.get());
                        // r194 — equipamentos dos chefes
                        output.accept(ModItems.ASTRAL_HELMET.get());
                        output.accept(ModItems.ASTRAL_CHESTPLATE.get());
                        output.accept(ModItems.ASTRAL_LEGGINGS.get());
                        output.accept(ModItems.ASTRAL_BOOTS.get());
                        output.accept(ModItems.PARASITIC_HELMET.get());
                        output.accept(ModItems.PARASITIC_CHESTPLATE.get());
                        output.accept(ModItems.PARASITIC_LEGGINGS.get());
                        output.accept(ModItems.PARASITIC_BOOTS.get());
                        output.accept(ModItems.GRAVITY_STAFF.get());
                        output.accept(ModItems.DIMENSIONAL_TELESCOPE.get());
                        output.accept(ModItems.PARASITIC_PICKAXE.get());
                        output.accept(ModItems.PARASITIC_SWORD.get());
                        output.accept(ModItems.PARASITIC_AXE.get());
                        output.accept(ModItems.LIVING_BACKPACK.get());
                        // r195 — 10 relíquias de Astaron (loot-only; aqui pra testar/ver)
                        output.accept(ModItems.ASTARON_EYE_RELIC.get());
                        output.accept(ModItems.ASTARON_MIND_AMULET.get());
                        output.accept(ModItems.ASTARON_HUNTER_GAUNTLET.get());
                        output.accept(ModItems.ASTARON_WARDEN_SASH.get());
                        output.accept(ModItems.ASTARON_VOID_TREADS.get());
                        output.accept(ModItems.ASTARON_MIRROR_RING.get());
                        output.accept(ModItems.ASTARON_FACELESS_CROWN.get());
                        output.accept(ModItems.ASTARON_ESSENCE_RELIC.get());
                        output.accept(ModItems.ASTARON_VOID_CORE.get());
                        output.accept(ModItems.ASTARON_STAR_PENDANT.get());
                        output.accept(ModItems.GAZE_LEECH_SPAWN_EGG.get());
                        output.accept(ModItems.BLIND_WEAVER_SPAWN_EGG.get());
                        output.accept(ModItems.MAW_CRAWLER_SPAWN_EGG.get());
                        output.accept(ModItems.WHISPER_MITE_SPAWN_EGG.get());
                        output.accept(ModItems.DREAD_ORB_SPAWN_EGG.get());
                        output.accept(ModItems.FLESH_WATCHER_SPAWN_EGG.get());
                        output.accept(ModItems.VOID_TICK_SPAWN_EGG.get());
                        output.accept(ModItems.GLOOM_MOTH_SPAWN_EGG.get());
                        output.accept(ModItems.ROT_EYE_SPAWN_EGG.get());
                        output.accept(ModItems.SCREAM_LARVA_SPAWN_EGG.get());
                        output.accept(ModItems.MIRROR_SPAWN_SPAWN_EGG.get());
                        output.accept(ModItems.PARASITE_HOST_SPAWN_EGG.get());
                        output.accept(ModItems.COLOSSAL_EYE_SPAWN_EGG.get());
                        output.accept(ModItems.COBAIA_SPAWN_EGG.get());
                        output.accept(ModItems.MATTER_SAMPLE.get());
                        output.accept(ModItems.COBAIA_ANALYZER_ITEM.get());
                        output.accept(ModItems.MIND_ANCHOR.get());
                        output.accept(ModItems.ENTROPY_ENGINE_ITEM.get());
                        output.accept(ModItems.REALITY_ANCHOR_ITEM.get());
                        output.accept(ModItems.STABILIZER_BEAM_ITEM.get());
                        output.accept(ModItems.FOCUS_CRYSTAL.get());
                        output.accept(ModItems.SANITY_REACTOR_ITEM.get());
                        output.accept(ModItems.RIFT_SIPHON_ITEM.get());
                        output.accept(ModItems.MATTER_PISTOL.get());
                        output.accept(ModItems.NULL_SEAL.get());
                        output.accept(ModItems.SPELLBREAKER.get());
                        output.accept(ModItems.MAGIC_SEAL.get());
                        output.accept(ModItems.ANTIMAGIC_SWORD.get());
                        output.accept(ModItems.WARD_PILLAR_ITEM.get());
                        output.accept(ModItems.VARATHA_SPEAR.get());
                        output.accept(ModItems.CODEX_COSMIC.get());
                        output.accept(ModItems.CODEX_MATTER.get());
                        output.accept(ModItems.CODEX_MAGIC.get());
                        output.accept(ModItems.DIMENSIONAL_SERUM.get());
                        output.accept(ModItems.ASTARON_SCALE.get());
                        output.accept(ModItems.RIFT_CUTTER_T1.get());
                        output.accept(ModItems.RIFT_CUTTER_T2.get());
                        output.accept(ModItems.RIFT_CUTTER_T3.get());
                        output.accept(ModBlocks.RIFT_FORGE.get());
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
                        output.accept(ModItems.MATTER_TESTER_ITEM.get());
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
                        output.accept(ModItems.DARK_MATTER_SYRINGE.get());
                        output.accept(ModItems.YELLOW_MATTER_SYRINGE.get());
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
                        output.accept(ModItems.CHALK.get());
                        output.accept(ModItems.CHALK_SYMBOL_ITEM.get());
                        // --- Order Armor / Spells ---
                        output.accept(ModItems.ORDER_HELMET.get());
                        output.accept(ModItems.ORDER_CHESTPLATE.get());
                        output.accept(ModItems.ORDER_LEGGINGS.get());
                        output.accept(ModItems.ORDER_BOOTS.get());
                        output.accept(ModItems.HOLY_SMITE_STAFF.get());
                        output.accept(ModItems.SANCTIFY_ORB.get());
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
                        output.accept(ModItems.THORN_STAFF.get());
                        output.accept(ModItems.LIGHTNING_STAFF.get());
                        output.accept(ModItems.SOUL_SCREAM_SWORD.get());
                        output.accept(ModItems.MAGNETIC_WAND.get());
                        output.accept(ModItems.VEIL_OF_MADNESS_ITEM.get());
                        output.accept(ModItems.PHANTOM_PORTAL_ITEM.get());
                        // --- Possessed spawn eggs ---
                        output.accept(ModItems.POSSESSED_ZOMBIE_EGG.get());
                        output.accept(ModItems.POSSESSED_SKELETON_EGG.get());
                        // --- Boss artifacts ---
                        output.accept(ModItems.CURSED_IDOL.get());
                        output.accept(ModItems.MIST_CENSER.get());
                        output.accept(ModItems.VEILED_LANTERN.get());
                        output.accept(ModItems.PULSING_HEART.get());
                        // --- New monster spawn eggs ---
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
                        output.accept(ModItems.ANTIMAGIC_HELMET.get());
                        output.accept(ModItems.ANTIMAGIC_CHESTPLATE.get());
                        output.accept(ModItems.ANTIMAGIC_LEGGINGS.get());
                        output.accept(ModItems.ANTIMAGIC_BOOTS.get());
                        output.accept(ModItems.ANTIMAGIC_SHIELD.get());

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
                        // --- v1: Possession Amulet (raro/admin) ---
                        // --- v0.1.22 r22: Mind Ward (antídoto ao Amulet) ---
                        // --- v0.1.22 r23: COSMIC HORROR (5 itens de loucura) ---
                        // --- v0.1.22 r24: SPIRIT WORLD ---
                        // --- v0.1.22 r27: DIMENSIONAL ANTENNA ---
                        output.accept(ModItems.DIMENSIONAL_ANTENNA_ITEM.get());
                        // --- v0.1.22 r28: QUANTUM TERMINAL ---
                        output.accept(ModItems.QUANTUM_TERMINAL_ITEM.get());
                        // --- v0.1.35: Pulso (sonic boom artifact) ---
                        output.accept(ModItems.PULSO.get());
                        // --- v0.1.22 r29: SPIRIT WORLD BLOCKS (12) ---
                        // --- v0.1.22 r29: COSMIC HORROR ITEMS (8) ---
                        // --- v0.1.22 r29: ANGEL / SPIRIT MAGIC (12) ---
                        // --- v0.1.22 r32: OCCULT RITUAL SYSTEM ---
                        // Chalks
                        // Candles
                        // Tools
                        // Blocks
                        // Sigils (10)
                        // Bound crystals
                        // --- v0.1.22 r33: LOOM DIMENSION ---
                        // r35: COSMIC HORROR SYSTEM trigger
                        // r36: Grimório antigo — DESCONTINUADO em r140 (ainda funciona pra saves antigos)
                        // Substituído pelo GRIMOIRE_OF_OBSERVATION + GRIMOIRE_BOOK.
                        // Pra dar use /give @s liberthia:grimoire
                        // output.accept(ModItems.GRIMOIRE.get());
                        // r40: Creative Grimoire — todos feitiços + mana infinita (ADMIN)
                        // r42: Spell Crafting Table portátil
                        // r45: Cosmic Horror Expansion items
                        // r46: Eldritch Artifacts (10 items cosmic horror profundo)
                        // r47: Living Server Artifacts (4 originais)
                        // r48: Reflection Seed (admin artifact)
                        // r50: Caretaker Console (GUI tool)
                        // r51: 10 Admin Cosmic Artifacts
                        // r54: items que foram esquecidos do tab
                        // r55: PALE WATCH ARTIFACTS

                        // r56: 15 cryptic cosmic artifacts

                        // r56: EXODUS BOOK

                        // r61: OBSERVATION CASTING ITEMS

                        // r62: Observation Tome + Source Jar

                        // r64: Grimório de Observação

                        // r68: Spell Parchment + 23 Glyph items
                        // r69: Scribes Table block (mesa de craft)
                        // Methods (azuis)
                        // Manifestations (roxas)
                        // Distortions (amarelos)
                        // r70: 12 novos glyphs portados AN

                        // r71: 25 elemental glyphs (5 per class)
                        // FIRE
                        // WATER
                        // EARTH
                        // AIR
                        // COSMIC
                        // r71: Source upgrade items

                        // r72: 10 utility glyphs + 3 augments

                        // r72: Chalk + Rune block
                        // r73: Imbuement Table block
                        // r77: Spell Binding Pedestal

                        // r140: Consolidado em 2 grimórios principais.
                        // Apprentice/Master/Archmage continuam funcionais (saves antigos)
                        // mas escondidos da tab — usuário pediu pra usar só 2 livros:
                        // 1. GRIMOIRE_OF_OBSERVATION → criar/cast feitiços custom
                        // 2. GRIMOIRE_BOOK → composição via Spell Weaver
                        // Pra dar tiers no creative use comando /give @s liberthia:grimoire_apprentice
                        // output.accept(ModItems.GRIMOIRE_APPRENTICE.get());
                        // output.accept(ModItems.GRIMOIRE_MASTER.get());
                        // output.accept(ModItems.GRIMOIRE_ARCHMAGE.get());
                        // r74: Spirit World ores + Mana berries + relay
                        // r74: Bookwyrm familiar spawn egg

                        // r72: 8 Prebuilt Spell Tomes

                        // r57: 3 LIMINAL DIMENSION ENTRY KEYS

                        // r56: 12 new dimensional ore items
                    })
                    .build());

    // ════════════════════════════════════════════════════════════════════════
    // r110: MAGIC ARSENAL TAB — todos os items das phases r82-r109
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<CreativeModeTab> MAGIC_ARSENAL = CREATIVE_MODE_TABS.register("magic_arsenal",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Liberthia: Magia"))
                    // r142: STAFF_FIRE não tem texture — usa GRIMOIRE_OF_OBSERVATION (foil + book look)
                    .icon(() -> ModItems.GRIMOIRE_OF_OBSERVATION.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        // r164: Sourcelinks removidos.
                        // ─── r85: Scrying ───
                        out.accept(ModItems.SCRYING_LENS.get());
                        out.accept(ModItems.SCRYER_OCULUS_ITEM.get());
                        // r165: Ritual Brazier + Ritual Tablets removidos — ritual system deletado.
                        // ─── r88: Automation ───
                        out.accept(ModItems.DOMINION_WAND.get());
                        out.accept(ModItems.SPELL_PRISM_ITEM.get());
                        out.accept(ModItems.SPELL_TURRET_ITEM.get());
                        out.accept(ModItems.SPELL_SENSOR_ITEM.get());
                        // ─── r89: Atmospheric Blocks ───
                        out.accept(ModItems.MIRROR_WEAVE_ITEM.get());
                        out.accept(ModItems.SKY_WEAVE_ITEM.get());
                        out.accept(ModItems.GHOST_WEAVE_ITEM.get());
                        out.accept(ModItems.FALSE_WEAVE_ITEM.get());
                        out.accept(ModItems.MAGIC_FIRE_FIRE_ITEM.get());
                        out.accept(ModItems.MAGIC_FIRE_ICE_ITEM.get());
                        out.accept(ModItems.MAGIC_FIRE_LIGHTNING_ITEM.get());
                        out.accept(ModItems.MAGIC_FIRE_ELDRITCH_ITEM.get());
                        out.accept(ModItems.MAGIC_FIRE_HOLY_ITEM.get());
                        out.accept(ModItems.MAGIC_FIRE_NATURE_ITEM.get());
                        out.accept(ModItems.MAGELIGHT_TORCH_ITEM.get());
                        // ─── r90: Perk Threads ───
                        out.accept(ModItems.PERK_JUMP.get());
                        out.accept(ModItems.PERK_STEP_HEIGHT.get());
                        out.accept(ModItems.PERK_REPAIRING.get());
                        out.accept(ModItems.PERK_MAGIC_RESIST.get());
                        out.accept(ModItems.PERK_TOUGHNESS.get());
                        out.accept(ModItems.PERK_FEATHER.get());
                        out.accept(ModItems.PERK_GLIDING.get());
                        out.accept(ModItems.PERK_MAGIC_CAPACITY.get());
                        out.accept(ModItems.PERK_SPELL_DAMAGE.get());
                        out.accept(ModItems.PERK_SATURATION.get());
                        out.accept(ModItems.PERK_KNOCKBACK_RESIST.get());
                        out.accept(ModItems.PERK_POTION_DURATION.get());
                        out.accept(ModItems.PERK_LOOTING.get());
                        out.accept(ModItems.PERK_BONDED.get());
                        // ─── r91: Casting Depth ───
                        out.accept(ModItems.COUNTERSPELL.get());
                        // ─── r92: Storage + Travel ───
                        out.accept(ModItems.MOB_JAR_ITEM.get());
                        out.accept(ModItems.POTION_JAR_ITEM.get());
                        out.accept(ModItems.VOID_JAR.get());
                        out.accept(ModItems.WARP_SCROLL.get());
                        out.accept(ModItems.STABLE_WARP_SCROLL.get());
                        // ─── r93: Scrolls + Affinity Rings ───
                        out.accept(ModItems.SCROLL_FIRE.get());
                        out.accept(ModItems.SCROLL_ICE.get());
                        out.accept(ModItems.SCROLL_LIGHTNING.get());
                        out.accept(ModItems.SCROLL_ELDRITCH.get());
                        out.accept(ModItems.SCROLL_HOLY.get());
                        out.accept(ModItems.SCROLL_NATURE.get());
                        out.accept(ModItems.RING_FIREWARP.get());
                        out.accept(ModItems.RING_LURKER.get());
                        out.accept(ModItems.RING_TELEPORT.get());
                        out.accept(ModItems.RING_CAPACITY.get());
                        out.accept(ModItems.ARCANE_SALVAGE.get());
                        // ─── r164: Magic Accessories (rings / gloves / belts × mana/sanity/power) ───
                        out.accept(ModItems.RING_MANA_FLOW.get());
                        out.accept(ModItems.RING_LUCIDITY.get());
                        out.accept(ModItems.RING_ARCANE_POWER.get());
                        out.accept(ModItems.GLOVE_MANA_CHANNELER.get());
                        out.accept(ModItems.GLOVE_LUCID_VEIL.get());
                        out.accept(ModItems.GLOVE_BATTLEMAGE.get());
                        out.accept(ModItems.BELT_MANA_RESERVOIR.get());
                        out.accept(ModItems.BELT_SANCTUM_SASH.get());
                        out.accept(ModItems.BELT_ARCHMAGE.get());
                        // ─── r94: Staffs + Orbs + Focus ───
                        out.accept(ModItems.STAFF_FIRE.get());
                        out.accept(ModItems.STAFF_ICE.get());
                        out.accept(ModItems.STAFF_LIGHTNING.get());
                        out.accept(ModItems.STAFF_ELDRITCH.get());
                        out.accept(ModItems.STAFF_HOLY.get());
                        out.accept(ModItems.STAFF_NATURE.get());
                        out.accept(ModItems.ORB_MANA_BOOST.get());
                        out.accept(ModItems.ORB_COOLDOWN.get());
                        out.accept(ModItems.ORB_SPELL_DAMAGE.get());
                        out.accept(ModItems.ORB_HEALTH.get());
                        out.accept(ModItems.ORB_SOURCE_REGEN.get());
                        out.accept(ModItems.FOCUS_FIRE.get());
                        out.accept(ModItems.FOCUS_ICE.get());
                        out.accept(ModItems.FOCUS_LIGHTNING.get());
                        out.accept(ModItems.FOCUS_ELDRITCH.get());
                        out.accept(ModItems.FOCUS_HOLY.get());
                        out.accept(ModItems.FOCUS_NATURE.get());
                        // ─── r95: Familiar spawn eggs ───
                        out.accept(ModItems.WISP_PICKER_EGG.get());
                        out.accept(ModItems.GROVE_SPRITE_EGG.get());
                        out.accept(ModItems.SOUL_REAPER_EGG.get());
                        out.accept(ModItems.WHELP_EGG.get());
                        out.accept(ModItems.CARBUNCLE_EGG.get());
                        out.accept(ModItems.AMETHYST_GOLEM_EGG.get());
                        // ─── r96: Iconic spells ───
                        out.accept(ModItems.SPECTRAL_HAMMER.get());
                        out.accept(ModItems.LIGHTNING_LANCE.get());
                        out.accept(ModItems.DEVOUR_JAW.get());
                        // ─── r97: Utility ───
                        out.accept(ModItems.REPOSITORY_ITEM.get());
                        out.accept(ModItems.JAR_OF_LIGHT.get());
                        out.accept(ModItems.ENCHANTERS_GAUNTLET.get());
                        // ─── r98: Boss + Minions eggs ───
                        out.accept(ModItems.ABYSSAL_LICH_EGG.get());
                        out.accept(ModItems.LICH_STALKER_EGG.get());
                        out.accept(ModItems.LICH_HUNTER_EGG.get());
                        // ─── r99: Walls ───
                        out.accept(ModItems.FIRE_WALL_ITEM.get());
                        out.accept(ModItems.ICE_WALL_ITEM.get());
                        out.accept(ModItems.LIGHTNING_WALL_ITEM.get());
                        out.accept(ModItems.HOLY_WALL_ITEM.get());
                        // ─── r100: Auto-blocks (r164: Whirlwind+AutoMiner removidos) ───
                        out.accept(ModItems.MAGE_CAULDRON_ITEM.get());
                        // ─── r101: Wizard Chestplates ───
                        out.accept(ModItems.WIZARD_CHEST_FIRE.get());
                        out.accept(ModItems.WIZARD_CHEST_ICE.get());
                        out.accept(ModItems.WIZARD_CHEST_LIGHTNING.get());
                        out.accept(ModItems.WIZARD_CHEST_ELDRITCH.get());
                        out.accept(ModItems.WIZARD_CHEST_HOLY.get());
                        out.accept(ModItems.WIZARD_CHEST_NATURE.get());
                        // ─── r103: Crops ───
                        out.accept(ModItems.MAGE_BLOOM_SEED.get());
                        out.accept(ModItems.MAGE_BLOOM_FIBER.get());
                        out.accept(ModItems.SOURCE_BERRY.get());
                        // ─── r104: Spell Bow ───
                        out.accept(ModItems.SPELL_BOW.get());
                        // ─── Arco da Caçadora ───
                        out.accept(ModItems.HUNTRESS_BOW.get());
                        // ─── Tambor da Lua do Medo ───
                        out.accept(ModItems.FEAR_MOON_DRUM.get());
                        // ─── Punhos Celestiais ───
                        out.accept(ModItems.CELESTIAL_FISTS.get());
                        // ─── r105: Scribe tables ───
                        out.accept(ModItems.INSCRIPTION_TABLE_ITEM.get());
                        out.accept(ModItems.SCROLL_FORGE_ITEM.get());
                        // ─── r106: New Wizards eggs ───
                        out.accept(ModItems.PYROMANCER_EGG.get());
                        out.accept(ModItems.CRYOMANCER_EGG.get());
                        out.accept(ModItems.ELECTROMANCER_EGG.get());
                        out.accept(ModItems.NECROMANCER_EGG.get());
                        out.accept(ModItems.ELDRITCH_CULTIST_EGG.get());
                        out.accept(ModItems.APOTHECARIST_EGG.get());
                        out.accept(ModItems.KEEPER_EGG.get());
                        out.accept(ModItems.ARCHEVOKER_EGG.get());
                        // ─── r107: Wizard Armor Pieces ───
                        out.accept(ModItems.WIZARD_HELM_FIRE.get());
                        out.accept(ModItems.WIZARD_LEGS_FIRE.get());
                        out.accept(ModItems.WIZARD_BOOTS_FIRE.get());
                        out.accept(ModItems.WIZARD_HELM_ICE.get());
                        out.accept(ModItems.WIZARD_LEGS_ICE.get());
                        out.accept(ModItems.WIZARD_BOOTS_ICE.get());
                        out.accept(ModItems.WIZARD_HELM_LIGHTNING.get());
                        out.accept(ModItems.WIZARD_LEGS_LIGHTNING.get());
                        out.accept(ModItems.WIZARD_BOOTS_LIGHTNING.get());
                        out.accept(ModItems.WIZARD_HELM_ELDRITCH.get());
                        out.accept(ModItems.WIZARD_LEGS_ELDRITCH.get());
                        out.accept(ModItems.WIZARD_BOOTS_ELDRITCH.get());
                        out.accept(ModItems.WIZARD_HELM_HOLY.get());
                        out.accept(ModItems.WIZARD_LEGS_HOLY.get());
                        out.accept(ModItems.WIZARD_BOOTS_HOLY.get());
                        out.accept(ModItems.WIZARD_HELM_NATURE.get());
                        out.accept(ModItems.WIZARD_LEGS_NATURE.get());
                        out.accept(ModItems.WIZARD_BOOTS_NATURE.get());
                        // ─── r108: Iconic spells ───
                        out.accept(ModItems.ICE_TOMB_SPELL.get());
                        out.accept(ModItems.ASCENSION_SPELL.get());
                        out.accept(ModItems.HEAT_SURGE_SPELL.get());
                        // ─── r111-r112: Universal Spell Library (50 spells) ───
                        // Fire
                        out.accept(ModItems.SPELL_FIREBALL.get());
                        out.accept(ModItems.SPELL_BURNING_DASH.get());
                        out.accept(ModItems.SPELL_INFERNO.get());
                        out.accept(ModItems.SPELL_MAGMA_BOMB.get());
                        out.accept(ModItems.SPELL_SUN_BEAM.get());
                        out.accept(ModItems.SPELL_PHOENIX_REBORN.get());
                        out.accept(ModItems.SPELL_CAUTERIZE.get());
                        out.accept(ModItems.SPELL_HEAT_WAVE.get());
                        // Ice
                        out.accept(ModItems.SPELL_FROSTBOLT.get());
                        out.accept(ModItems.SPELL_ICE_SPIKE.get());
                        out.accept(ModItems.SPELL_FROST_NOVA.get());
                        out.accept(ModItems.SPELL_GLACIAL_STORM.get());
                        out.accept(ModItems.SPELL_FROST_STEP.get());
                        out.accept(ModItems.SPELL_FROZEN_GROUND.get());
                        out.accept(ModItems.SPELL_RAY_OF_FROST.get());
                        out.accept(ModItems.SPELL_ICE_LANCE.get());
                        // Lightning
                        out.accept(ModItems.SPELL_LIGHTNING_BOLT.get());
                        out.accept(ModItems.SPELL_SPARK_BURST.get());
                        out.accept(ModItems.SPELL_CHAIN_LIGHTNING.get());
                        out.accept(ModItems.SPELL_SHOCK.get());
                        out.accept(ModItems.SPELL_THUNDER_STEP.get());
                        out.accept(ModItems.SPELL_STORM_CLOUD.get());
                        out.accept(ModItems.SPELL_STATIC_FIELD.get());
                        out.accept(ModItems.SPELL_LIGHTNING_LANCE.get());
                        // Eldritch
                        out.accept(ModItems.SPELL_VOID_TENTACLE.get());
                        out.accept(ModItems.SPELL_MIND_SPIKE.get());
                        out.accept(ModItems.SPELL_ELDRITCH_BLAST.get());
                        out.accept(ModItems.SPELL_SOUL_TEAR.get());
                        out.accept(ModItems.SPELL_MADNESS_WAVE.get());
                        out.accept(ModItems.SPELL_COSMIC_VOID.get());
                        // Holy
                        out.accept(ModItems.SPELL_GREATER_HEAL.get());
                        out.accept(ModItems.SPELL_SMITE.get());
                        out.accept(ModItems.SPELL_DIVINE_LIGHT.get());
                        out.accept(ModItems.SPELL_SUN_STRIKE.get());
                        out.accept(ModItems.SPELL_HOLY_LANCE.get());
                        out.accept(ModItems.SPELL_HEALING_AURA.get());
                        out.accept(ModItems.SPELL_SACRED_GROUND.get());
                        out.accept(ModItems.SPELL_JUDGMENT.get());
                        // Nature
                        out.accept(ModItems.SPELL_VINE_TANGLE.get());
                        out.accept(ModItems.SPELL_EARTH_WALL.get());
                        out.accept(ModItems.SPELL_STONE_SHARD.get());
                        out.accept(ModItems.SPELL_WISPS_HEAL.get());
                        out.accept(ModItems.SPELL_ROOTS.get());
                        out.accept(ModItems.SPELL_BRAMBLE_STORM.get());
                        // Evocation
                        out.accept(ModItems.SPELL_MAGIC_MISSILE.get());
                        out.accept(ModItems.SPELL_BONE_SPEAR.get());
                        out.accept(ModItems.SPELL_MAGIC_SHIELD.get());
                        out.accept(ModItems.SPELL_SUMMON_VEX.get());
                        // ─── r116: Spirit Reagents (drops do Spirit World pra craft glyphs) ───
                        out.accept(ModItems.WISP_ESSENCE.get());
                        out.accept(ModItems.ASTRAL_DUST.get());
                        out.accept(ModItems.WHISPERWOOD_RESIN.get());
                        out.accept(ModItems.MEMORY_SHARD.get());
                        out.accept(ModItems.ECTOPLASM_STRAND.get());
                        out.accept(ModItems.PHANTOM_INK.get());
                        out.accept(ModItems.VEIL_FRAGMENT.get());
                        // ─── r117: Spirit Robes armor (4 peças) + Conduit + Transmuter ───
                        out.accept(ModItems.SPIRIT_ROBES_HELM.get());
                        out.accept(ModItems.SPIRIT_ROBES_CHEST.get());
                        out.accept(ModItems.SPIRIT_ROBES_LEGS.get());
                        out.accept(ModItems.SPIRIT_ROBES_BOOTS.get());
                        out.accept(ModItems.SPIRIT_CONDUIT_ITEM.get());
                        out.accept(ModItems.SOURCE_TRANSMUTER_ITEM.get());
                        // ─── r118: Glyph Inscriber (GUI bonita) ───
                        out.accept(ModItems.GLYPH_INSCRIBER_ITEM.get());
                        // ─── r119: Spell Weaver + Grimoire + Modifiers + Apolão spells ───
                        out.accept(ModItems.SPELL_WEAVER_ITEM.get());
                        out.accept(ModItems.GRIMOIRE_BOOK.get());
                        // ─── r155 Phase 2: Arcane Workbench ───
                        out.accept(ModItems.ARCANE_WORKBENCH_ITEM.get());
                        // ─── r162: Class Pedestal ───
                        out.accept(ModItems.CLASS_PEDESTAL_ITEM.get());
                        // ─── r159: Componentes do Arcane Workbench ───
                        out.accept(ModItems.MAGIC_TABLET.get());
                        out.accept(ModItems.ARCANE_ORB.get());
                        out.accept(ModItems.SCHOOL_RUNE_FIRE.get());
                        out.accept(ModItems.SCHOOL_RUNE_ICE.get());
                        out.accept(ModItems.SCHOOL_RUNE_LIGHTNING.get());
                        out.accept(ModItems.SCHOOL_RUNE_BLOOD.get());
                        out.accept(ModItems.SCHOOL_RUNE_ELDRITCH.get());
                        out.accept(ModItems.SCHOOL_RUNE_HOLY.get());
                        out.accept(ModItems.SCHOOL_RUNE_NATURE.get());
                        // Mobility spells
                        out.accept(ModItems.SPELL_WINGS_OF_SOURCE.get());
                        out.accept(ModItems.SPELL_PHASE_DASH.get());
                        out.accept(ModItems.SPELL_WIND_STEP.get());
                        out.accept(ModItems.SPELL_LEVITATE_SELF.get());
                        out.accept(ModItems.SPELL_BLINK.get());
                        // Apolão tier
                        out.accept(ModItems.SPELL_SOLAR_APOCALYPSE.get());
                        out.accept(ModItems.SPELL_ELDRITCH_METEOR.get());
                        out.accept(ModItems.SPELL_DIVINE_JUDGMENT_APOLAO.get());
                        out.accept(ModItems.SPELL_APOCALYPSE.get());
                        // r120 VAZIO
                        out.accept(ModItems.SPELL_VOID.get());
                        out.accept(ModItems.SPELL_VOID_LASER.get());
                        out.accept(ModItems.VOID_REAGENT.get());
                        // r172 ACESSÍVEIS (15) — mobilidade/utilidade custo baixo
                        out.accept(ModItems.SPELL_AIR_DASH.get());
                        out.accept(ModItems.SPELL_FEATHER_GRACE.get());
                        out.accept(ModItems.SPELL_GUST_LEAP.get());
                        out.accept(ModItems.SPELL_SHADOW_STEP.get());
                        out.accept(ModItems.SPELL_SWIFT_CURRENT.get());
                        out.accept(ModItems.SPELL_STONE_SKIN.get());
                        out.accept(ModItems.SPELL_EMBER_SPARK.get());
                        out.accept(ModItems.SPELL_FROST_TOUCH.get());
                        out.accept(ModItems.SPELL_STATIC_JOLT.get());
                        out.accept(ModItems.SPELL_MINOR_HEAL.get());
                        out.accept(ModItems.SPELL_THORN_WHIP.get());
                        out.accept(ModItems.SPELL_GLIDE.get());
                        out.accept(ModItems.SPELL_BLINK_SHORT.get());
                        out.accept(ModItems.SPELL_WARMTH.get());
                        out.accept(ModItems.SPELL_NIMBLE_REFLEXES.get());
                        // r172 scrolls de combate (8)
                        out.accept(ModItems.SPELL_ARC_BOLT.get());
                        out.accept(ModItems.SPELL_CINDER_BURST.get());
                        out.accept(ModItems.SPELL_FROST_SHARD.get());
                        out.accept(ModItems.SPELL_THUNDER_CLAP.get());
                        out.accept(ModItems.SPELL_VENOM_SPIT.get());
                        out.accept(ModItems.SPELL_BLOOD_LASH.get());
                        out.accept(ModItems.SPELL_MINOR_SMITE.get());
                        out.accept(ModItems.SPELL_VOID_GRIP.get());
                        // r172 orbes arcanos (5)
                        out.accept(ModItems.ORB_HEALING.get());
                        out.accept(ModItems.ORB_WARDING.get());
                        out.accept(ModItems.ORB_SWIFTNESS.get());
                        out.accept(ModItems.ORB_INSIGHT.get());
                        out.accept(ModItems.ORB_MIGHT.get());
                        // r172 focus extras (5)
                        out.accept(ModItems.FOCUS_ARCANE.get());
                        out.accept(ModItems.FOCUS_VOID.get());
                        out.accept(ModItems.FOCUS_STORM.get());
                        out.accept(ModItems.FOCUS_VERDANT.get());
                        out.accept(ModItems.FOCUS_RADIANT.get());
                        // Modifier glyphs
                        out.accept(ModItems.MOD_AMPLIFY.get());
                        out.accept(ModItems.MOD_AOE.get());
                        out.accept(ModItems.MOD_PIERCE.get());
                        out.accept(ModItems.MOD_MULTISHOT.get());
                        out.accept(ModItems.MOD_CHAIN.get());
                        out.accept(ModItems.MOD_IGNITE.get());
                        out.accept(ModItems.MOD_FREEZE.get());
                        out.accept(ModItems.MOD_KNOCKBACK.get());
                        out.accept(ModItems.MOD_LIFESTEAL.get());
                        out.accept(ModItems.MOD_RANGE.get());
                        out.accept(ModItems.MOD_SUSTAIN.get());
                        out.accept(ModItems.MOD_PENETRATE.get());
                        out.accept(ModItems.MOD_APOLAO.get());
                        // r173: 10 novas Runas (modifiers)
                        out.accept(ModItems.MOD_POISON.get());
                        out.accept(ModItems.MOD_WITHER.get());
                        out.accept(ModItems.MOD_SLOW.get());
                        out.accept(ModItems.MOD_WEAKEN.get());
                        out.accept(ModItems.MOD_BLIND.get());
                        out.accept(ModItems.MOD_GRAVITY.get());
                        out.accept(ModItems.MOD_LEVITATE.get());
                        out.accept(ModItems.MOD_WARD.get());
                        out.accept(ModItems.MOD_HASTEN.get());
                        out.accept(ModItems.MOD_SMITE.get());

                        // ════════════════════════════════════════════════════════
                        // r142: Items mágicos antes da tab principal (vision swap pra baixo)
                        // ════════════════════════════════════════════════════════
                        // Vision Swap (visão = magia)
                        out.accept(ModItems.VISION_SWAP_LENS.get());
                        // Spirit altar / spirit dimension
                        out.accept(ModItems.SPIRIT_ALTAR_ITEM.get());
                        out.accept(ModItems.SPIRIT_STONE_ITEM.get());
                        // Spirit World worldgen blocks
                        out.accept(ModItems.SPIRIT_GRASS_BLOCK_ITEM.get());
                        out.accept(ModItems.SPIRIT_DIRT_ITEM.get());
                        out.accept(ModItems.ETHEREAL_STONE_ITEM.get());
                        out.accept(ModItems.ETHEREAL_STONE_BRICKS_ITEM.get());
                        out.accept(ModItems.SOUL_BRICK_ITEM.get());
                        out.accept(ModItems.HALO_MARBLE_ITEM.get());
                        out.accept(ModItems.HALO_MARBLE_BRICKS_ITEM.get());
                        out.accept(ModItems.DREAM_GLASS_ITEM.get());
                        out.accept(ModItems.WHISPERWOOD_LOG_ITEM.get());
                        out.accept(ModItems.WHISPERWOOD_PLANKS_ITEM.get());
                        out.accept(ModItems.WHISPERWOOD_LEAVES_ITEM.get());
                        out.accept(ModItems.ASTRAL_LANTERN_ITEM.get());
                        out.accept(ModItems.CRYSTAL_SPIRIT_ORE_ITEM.get());
                        out.accept(ModItems.SANCTUM_WARD_ITEM.get());
                        out.accept(ModItems.HOLY_CENSER_ITEM.get());
                        // Angel / Spirit magic items
                        out.accept(ModItems.HALO_OF_LIGHT.get());
                        out.accept(ModItems.WINGS_OF_ASCENSION.get());
                        out.accept(ModItems.ANGEL_TEAR_AMULET.get());
                        out.accept(ModItems.SPIRIT_ANCHOR.get());
                        out.accept(ModItems.CHOIR_BELL.get());
                        out.accept(ModItems.DIVINE_SMITE_ROD.get());
                        out.accept(ModItems.SOUL_MIRROR.get());
                        out.accept(ModItems.SPIRIT_COMPASS.get());
                        out.accept(ModItems.HOLY_WATER_BUCKET.get());
                        out.accept(ModItems.ANGEL_WING_FEATHER.get());
                        out.accept(ModItems.SERAPH_BLADE.get());
                        out.accept(ModItems.PRAYER_BOOK.get());

                        // r143: moved from main tab
                        out.accept(ModItems.BOOKWYRM_SPAWN_EGG.get());
                        out.accept(ModItems.VISITANTE_SPAWN_EGG.get());
                        out.accept(ModItems.MULHER_HORIZONTE_SPAWN_EGG.get());
                        out.accept(ModItems.CACADOR_SPAWN_EGG.get());
                        out.accept(ModItems.ESPREITADOR_SPAWN_EGG.get());
                        out.accept(ModItems.LURKER_SPAWN_EGG.get());
                        out.accept(ModItems.EMF_METER.get());
                        out.accept(ModItems.DIMENSIONAL_IRREGULARITY.get());
                        out.accept(ModItems.CAMERA.get());
                        out.accept(ModItems.PHOTOGRAPH.get());
                        out.accept(ModItems.ABYSSAL_CORE.get());
                        out.accept(br.com.murilo.liberthia.item.AbyssalCoreItem.crystallized(ModItems.ABYSSAL_CORE.get()));
                        out.accept(ModItems.VOID_EYE.get());
                        out.accept(ModItems.BLACK_MIRROR.get());
                        out.accept(ModItems.SANITY_CANDLE.get());
                        out.accept(ModItems.BROKEN_RADIO.get());
                        out.accept(ModItems.CURSED_DOLL.get());
                        out.accept(ModItems.WINDOW_WATCHER_EGG.get());
                        out.accept(ModItems.MAPA_INVERTIDO.get());
                        out.accept(ModItems.WALKIE_TALKIE.get());
                        out.accept(ModItems.CHICOTE_CELESTIAL.get());
                        out.accept(ModItems.COMPUTADOR.get());
                        out.accept(ModItems.PRINTER_ITEM.get());
                        out.accept(ModItems.HARD_DRIVE.get());
                        out.accept(ModItems.THREAD_LOOM_ITEM.get());
                        out.accept(ModItems.CUSTOM_THREAD.get());
                        out.accept(ModItems.ORB_INFUSER_ITEM.get());
                        out.accept(ModItems.CUSTOM_ORB.get());
                        out.accept(ModItems.BOUND_AFRIT_CRYSTAL.get());
                        out.accept(ModItems.BOUND_DJINNI_CRYSTAL.get());
                        out.accept(ModItems.BOUND_FOLIOT_CRYSTAL.get());
                        out.accept(ModItems.CANDLE_BLACK_OCCULT_ITEM.get());
                        out.accept(ModItems.CANDLE_GOLDEN_OCCULT_ITEM.get());
                        out.accept(ModItems.CANDLE_PURPLE_OCCULT_ITEM.get());
                        out.accept(ModItems.CANDLE_RED_OCCULT_ITEM.get());
                        out.accept(ModItems.CANDLE_WHITE_OCCULT_ITEM.get());
                        out.accept(ModItems.CHALK_BLACK.get());
                        out.accept(ModItems.CHALK_GOLDEN.get());
                        out.accept(ModItems.CHALK_PURPLE.get());
                        out.accept(ModItems.CHALK_RED.get());
                        out.accept(ModItems.CHALK_WHITE.get());
                        out.accept(ModItems.CREATIVE_GRIMOIRE.get());
                        out.accept(ModItems.GLYPH_AMPLIFY.get());
                        out.accept(ModItems.GLYPH_AOE.get());
                        out.accept(ModItems.GLYPH_BREAK_BLOCK.get());
                        out.accept(ModItems.GLYPH_BUBBLE_SHIELD.get());
                        out.accept(ModItems.GLYPH_BURNING_AURA.get());
                        out.accept(ModItems.GLYPH_CLEANSING_FLAME.get());
                        out.accept(ModItems.GLYPH_CONJURE_WATER.get());
                        out.accept(ModItems.GLYPH_DIRECT_GAZE.get());
                        out.accept(ModItems.GLYPH_DREAD_STARE.get());
                        out.accept(ModItems.GLYPH_EARTHEN_WALL.get());
                        out.accept(ModItems.GLYPH_ECHO.get());
                        out.accept(ModItems.GLYPH_EFFECT_BLIND.get());
                        out.accept(ModItems.GLYPH_EFFECT_EXPLOSION.get());
                        out.accept(ModItems.GLYPH_EFFECT_FANGS.get());
                        // r172 glifos novos
                        out.accept(ModItems.GLYPH_VAMPIRIC_GAZE.get());
                        out.accept(ModItems.GLYPH_GRAVITY_PULL.get());
                        out.accept(ModItems.GLYPH_REPULSE.get());
                        out.accept(ModItems.GLYPH_PETRIFY.get());
                        out.accept(ModItems.GLYPH_SOOTHE.get());
                        out.accept(ModItems.GLYPH_SCORCH.get());
                        out.accept(ModItems.GLYPH_UPDRAFT.get());
                        out.accept(ModItems.GLYPH_ENFEEBLE.get());
                        out.accept(ModItems.GLYPH_EFFECT_FREEZE.get());
                        out.accept(ModItems.GLYPH_EFFECT_GRAVITY.get());
                        out.accept(ModItems.GLYPH_EFFECT_HARM.get());
                        out.accept(ModItems.GLYPH_EFFECT_HEAL.get());
                        out.accept(ModItems.GLYPH_EFFECT_IGNITE.get());
                        out.accept(ModItems.GLYPH_EFFECT_KNOCKBACK.get());
                        out.accept(ModItems.GLYPH_EFFECT_LAUNCH.get());
                        out.accept(ModItems.GLYPH_EFFECT_LEVITATE.get());
                        out.accept(ModItems.GLYPH_EFFECT_LIGHTNING.get());
                        out.accept(ModItems.GLYPH_EFFECT_SLOWFALL.get());
                        out.accept(ModItems.GLYPH_FIREBALL.get());
                        out.accept(ModItems.GLYPH_FROST_LANCE.get());
                        out.accept(ModItems.GLYPH_GUST.get());
                        out.accept(ModItems.GLYPH_HEALING_RAIN.get());
                        out.accept(ModItems.GLYPH_HEX.get());
                        out.accept(ModItems.GLYPH_INFERNO.get());
                        out.accept(ModItems.GLYPH_LIGHT.get());
                        out.accept(ModItems.GLYPH_LINGER.get());
                        out.accept(ModItems.GLYPH_MANIFEST_DECAY.get());
                        out.accept(ModItems.GLYPH_MANIFEST_GLIMPSE.get());
                        out.accept(ModItems.GLYPH_MANIFEST_MIRROR.get());
                        out.accept(ModItems.GLYPH_MANIFEST_SILENCE.get());
                        out.accept(ModItems.GLYPH_MANIFEST_WHISPER.get());
                        out.accept(ModItems.GLYPH_METHOD_BURST.get());
                        out.accept(ModItems.GLYPH_METHOD_CHAIN.get());
                        out.accept(ModItems.GLYPH_METHOD_LASER.get());
                        out.accept(ModItems.GLYPH_METHOD_ORBIT.get());
                        out.accept(ModItems.GLYPH_METHOD_SELF.get());
                        out.accept(ModItems.GLYPH_METHOD_TOUCH.get());
                        out.accept(ModItems.GLYPH_METHOD_WALL.get());
                        out.accept(ModItems.GLYPH_MIND_SPIKE.get());
                        out.accept(ModItems.GLYPH_MIST_VEIL.get());
                        out.accept(ModItems.GLYPH_PICKUP.get());
                        out.accept(ModItems.GLYPH_PIERCE.get());
                        out.accept(ModItems.GLYPH_PLACE_BLOCK.get());
                        out.accept(ModItems.GLYPH_QUAKE_STEP.get());
                        out.accept(ModItems.GLYPH_REALITY_TEAR.get());
                        out.accept(ModItems.GLYPH_ROOTS.get());
                        out.accept(ModItems.GLYPH_SECRET.get());
                        out.accept(ModItems.GLYPH_SINGULARITY.get());
                        out.accept(ModItems.GLYPH_SKY_STEP.get());
                        out.accept(ModItems.GLYPH_SNARE.get());
                        out.accept(ModItems.GLYPH_SOLAR_PULSE.get());
                        out.accept(ModItems.GLYPH_SPLIT.get());
                        out.accept(ModItems.GLYPH_STONE_SPIKES.get());
                        out.accept(ModItems.GLYPH_TENDRIL.get());
                        out.accept(ModItems.GLYPH_TIDAL_WAVE.get());
                        out.accept(ModItems.GLYPH_TORNADO.get());
                        out.accept(ModItems.GLYPH_VEIN_SIGHT.get());
                        out.accept(ModItems.GLYPH_VELOCITY.get());
                        out.accept(ModItems.GLYPH_VOID_PULL.get());
                        out.accept(ModItems.GLYPH_WATCH_MEMORY.get());
                        out.accept(ModItems.GLYPH_WATCH_PERIPHERAL.get());
                        out.accept(ModItems.GLYPH_WATCH_REFLECTION.get());
                        out.accept(ModItems.GLYPH_WATCH_SILENCE.get());
                        out.accept(ModItems.GLYPH_WIND_CUTTER.get());
                        out.accept(ModItems.GRIMOIRE_OF_OBSERVATION.get());
                        out.accept(ModItems.IMBUEMENT_TABLE_ITEM.get());
                        out.accept(ModItems.LIGHTER.get());
                        out.accept(ModItems.MAGIC_BOOK.get());
                        out.accept(ModItems.MANA_BERRY.get());
                        out.accept(ModItems.MANA_BERRY_BUSH_ITEM.get());
                        out.accept(ModItems.OBSERVATION_CHALK.get());
                        out.accept(ModItems.OBSERVATION_TOME.get());
                        out.accept(ModItems.PALE_IRON_INGOT.get());
                        out.accept(ModItems.RIFT_CRYSTAL.get());
                        out.accept(ModItems.RITUAL_CHALICE.get());
                        out.accept(ModItems.RITUAL_CIRCLE_ITEM.get());
                        out.accept(ModItems.RITUAL_DAGGER.get());
                        out.accept(ModItems.RUNE_BLOCK_ITEM.get());
                        out.accept(ModItems.SCRIBES_TABLE_ITEM.get());
                        out.accept(ModItems.SIGIL_AFRIT.get());
                        out.accept(ModItems.SIGIL_BAEL.get());
                        out.accept(ModItems.SIGIL_BANISHING.get());
                        out.accept(ModItems.SIGIL_DIMENSIONAL.get());
                        out.accept(ModItems.SIGIL_DJINNI.get());
                        out.accept(ModItems.SIGIL_FOLIOT.get());
                        out.accept(ModItems.SIGIL_LUCIFER.get());
                        out.accept(ModItems.SIGIL_METATRON.get());
                        out.accept(ModItems.SIGIL_NECRO.get());
                        out.accept(ModItems.SIGIL_SANDALPHON.get());
                        out.accept(ModItems.SOULSTEEL_INGOT.get());
                        out.accept(ModItems.SOUL_FRAGMENT.get());
                        out.accept(ModItems.SOURCESTONE_ORE_ITEM.get());
                        out.accept(ModItems.SOURCE_CATALYST.get());
                        out.accept(ModItems.SOURCE_CRYSTAL.get());
                        out.accept(ModItems.SOURCE_GEM.get());
                        out.accept(ModItems.SOURCE_JAR_ITEM.get());
                        out.accept(ModItems.SOURCE_LENS.get());
                        out.accept(ModItems.SOURCE_RELAY_ITEM.get());
                        out.accept(ModItems.SPELLSWORD.get());
                        out.accept(ModItems.SPELL_BINDING_PEDESTAL_ITEM.get());
                        out.accept(ModItems.SPELL_CRAFTING_TABLE.get());
                        out.accept(ModItems.SPELL_PARCHMENT.get());
                        out.accept(ModItems.SPIRIT_GEM_ORE_ITEM.get());
                        out.accept(ModItems.SPIRIT_MINER_ITEM.get());
                        out.accept(ModItems.TOME_DASH.get());
                        out.accept(ModItems.TOME_DEATH_BEAM.get());
                        out.accept(ModItems.TOME_FROSTBINDER.get());
                        out.accept(ModItems.TOME_HEALING_LIGHT.get());
                        out.accept(ModItems.TOME_PYROMANCER.get());
                        out.accept(ModItems.TOME_SINGULARITY.get());
                        out.accept(ModItems.TOME_SKYWALKER.get());
                        out.accept(ModItems.TOME_WEBWEAVER.get());

                        // r153: TODOS os factory spells (carregados dos JSONs em data/liberthia/spells/)
                        for (String spellId : br.com.murilo.liberthia.magic.factory.SpellRecipeRegistry.ids()) {
                            try {
                                out.accept(br.com.murilo.liberthia.magic.factory.DynamicSpellItem
                                        .stackFor(ModItems.FACTORY_SPELL_SCROLL.get(), spellId));
                            } catch (Throwable ignored) {}
                        }
                        // r168: TODOS os spells do SpellLibrary (in-code, ~150 incluindo os 50 r168)
                        for (String spellId : br.com.murilo.liberthia.magic.spell.SpellLibrary.ALL.keySet()) {
                            try {
                                out.accept(br.com.murilo.liberthia.magic.factory.DynamicSpellItem
                                        .stackFor(ModItems.FACTORY_SPELL_SCROLL.get(), spellId));
                            } catch (Throwable ignored) {}
                        }
                    })
                    .build());

    // ════════════════════════════════════════════════════════════════════════
    // r110: HORROR FRAMEWORK TAB — items r81 (4 cosmic horror items + 4 entities)
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<CreativeModeTab> HORROR_FRAMEWORK = CREATIVE_MODE_TABS.register("horror_framework",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Liberthia: Horror"))
                    .icon(() -> ModItems.FRACTURED_SCRIPTURE.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        // ─── r81: Cosmic Horror Items ───
                        out.accept(ModItems.MIRROR_MASK.get());
                        out.accept(ModItems.FRACTURED_SCRIPTURE.get());
                        out.accept(ModItems.HALO_OF_ABADDON.get());
                        out.accept(ModItems.VEINBOUND_CHESTPLATE.get());
                        // ─── r81: Cosmic Horror Entity eggs ───
                        out.accept(ModItems.EMPTY_MAN_EGG.get());
                        out.accept(ModItems.OBSERVER_EGG.get());
                        out.accept(ModItems.ABSENCE_EGG.get());
                        out.accept(ModItems.REMEMBERED_EGG.get());

                        // ════════════════════════════════════════════════════════
                        // r141: 10 Eldritch Artifacts admin (r51)
                        // ════════════════════════════════════════════════════════
                        out.accept(ModItems.BLACK_VEIL.get());
                        out.accept(ModItems.TENDRIL_CROWN.get());
                        out.accept(ModItems.FALSE_SUN.get());
                        out.accept(ModItems.MIRROR_PULSE.get());
                        out.accept(ModItems.SILENT_BELL.get());
                        out.accept(ModItems.OPEN_EYE.get());
                        out.accept(ModItems.THREAD_OF_DISTANCE.get());
                        out.accept(ModItems.FLESH_SIGNAL.get());
                        out.accept(ModItems.DEEP_WATER.get());
                        out.accept(ModItems.AUDIENCE_MARK.get());

                        // ════════════════════════════════════════════════════════
                        // r141: Caretaker / Observatory (r48-r50)
                        // ════════════════════════════════════════════════════════
                        out.accept(ModItems.CARETAKER_CONSOLE.get());
                        out.accept(ModItems.REFLECTION_SEED.get());

                        // ════════════════════════════════════════════════════════
                        // r141: Liminal Dimensions artifacts (r56-r58)
                        // ════════════════════════════════════════════════════════
                        out.accept(ModItems.MIRROR_FRUIT.get());
                        out.accept(ModItems.PALE_THREAD.get());
                        out.accept(ModItems.LOW_SIGNAL.get());
                        out.accept(ModItems.GEOMETRY_KEY.get());
                        out.accept(ModItems.NULL_BELL.get());
                        out.accept(ModItems.RED_TAPE.get());
                        out.accept(ModItems.MIMIC_HEART.get());
                        out.accept(ModItems.BOOK_IMPOSSIBLE.get());
                        out.accept(ModItems.INFECTION_NEEDLE.get());
                        out.accept(ModItems.FALSE_TOTEM.get());
                        out.accept(ModItems.FLESH_LANTERN.get());
                        out.accept(ModItems.HOLLOW_MASK.get());
                        out.accept(ModItems.BLACK_SIGNAL_RADIO.get());
                        out.accept(ModItems.WATCHING_EYE.get());
                        out.accept(ModItems.SILENT_WITNESS_CLOAK.get());

                        // ════════════════════════════════════════════════════════
                        // r141: Insanity / Cosmic Horror items r45-r46
                        // ════════════════════════════════════════════════════════
                        out.accept(ModItems.VOICE_CURSE_AMULET.get());
                        out.accept(ModItems.INSANITY_CROWN.get());
                        out.accept(ModItems.VULTO_LENS.get());
                        out.accept(ModItems.PHANTOM_CALLER.get());
                        out.accept(ModItems.WATCHER_MARK.get());
                        out.accept(ModItems.CURSED_EFFIGY.get());
                        out.accept(ModItems.SKY_TEAR_HORN.get());

                        // ════════════════════════════════════════════════════════
                        // r141: Loom mobs spawn eggs (Spirit World horror)
                        // ════════════════════════════════════════════════════════
                        out.accept(ModItems.LOOM_WATCHER_EGG.get());
                        out.accept(ModItems.LOOM_PERIPHERAL_EGG.get());
                        out.accept(ModItems.LOOM_SCREAMER_EGG.get());

                        // ════════════════════════════════════════════════════════
                        // r142: Possession / Madness / Soul items
                        // ════════════════════════════════════════════════════════
                        out.accept(ModItems.POSSESSION_AMULET.get());
                        out.accept(ModItems.SOUL_SEVER.get());
                        out.accept(ModItems.SOUL_CLONER.get());
                        out.accept(ModItems.MIRROR_OF_INSANITY.get());
                        out.accept(ModItems.MASS_POSSESSION_CROWN.get());
                        out.accept(ModItems.MADDENING_GAZE.get());
                        out.accept(ModItems.MADNESS_AURA.get());
                        out.accept(ModItems.MIND_WARD.get());

                        // r143: moved from main tab
                        out.accept(ModItems.ABYSSIUM_DUST.get());
                        out.accept(ModItems.BARK_TOKEN.get());
                        out.accept(ModItems.BENT_IRON.get());
                        out.accept(ModItems.BLACK_STAR_CORE.get());
                        out.accept(ModItems.CLONE_ARMY.get());
                        out.accept(ModItems.CURSED_CRADLE.get());
                        out.accept(ModItems.DARK_MATTER_LASER.get());
                        out.accept(ModItems.DISTORTION_CRYSTAL.get());
                        out.accept(ModItems.DROWNED_COMPASS.get());
                        out.accept(ModItems.EXODUS_BOOK.get());
                        out.accept(ModItems.EYES_OF_ABYSS.get());
                        out.accept(ModItems.EYE_STONE_SHARD.get());
                        out.accept(ModItems.FOLDED_ADDRESS.get());
                        out.accept(ModItems.FOLDED_DISTANCE.get());
                        out.accept(ModItems.FORBIDDEN_TOME.get());
                        out.accept(ModItems.GHOST_QUARTZ_SHARD.get());
                        out.accept(ModItems.HALF_STEP.get());
                        out.accept(ModItems.HAND_ON_GLASS.get());
                        out.accept(ModItems.HOLLOW_SILVER_NUGGET.get());
                        out.accept(ModItems.HOURGLASS_OF_REGRESSION.get());
                        out.accept(ModItems.LANTERN_OF_FALSE_MEMORY.get());
                        out.accept(ModItems.LISTENING_GLASS.get());
                        out.accept(ModItems.LONELY_ECHO.get());
                        out.accept(ModItems.LOOKING_GLASS.get());
                        out.accept(ModItems.LOOM_STONE_ITEM.get());
                        out.accept(ModItems.MARROW_WHISTLE.get());
                        out.accept(ModItems.MOURNING_EMBER.get());
                        out.accept(ModItems.NULL_IRON_SHARD.get());
                        out.accept(ModItems.PALE_BLINK_PENDANT.get());
                        out.accept(ModItems.PALE_COIN.get());
                        out.accept(ModItems.PALE_CRYSTAL_SHARD.get());
                        out.accept(ModItems.PARALYZE_PENDANT.get());
                        out.accept(ModItems.PENDULUM_OF_DREAD.get());
                        out.accept(ModItems.PULLED_STRING.get());
                        out.accept(ModItems.QUIET_MARK.get());
                        out.accept(ModItems.RIFTITE_ORE_ITEM.get());
                        out.accept(ModItems.RIFTITE_SHARD.get());
                        out.accept(ModItems.SOFT_WOUND.get());
                        out.accept(ModItems.SOULITE_SHARD.get());
                        out.accept(ModItems.SPIRITUAL_CONNECTION.get());
                        out.accept(ModItems.SPIRITUAL_LINK.get());
                        out.accept(ModItems.SPIRIT_GUIDE.get());
                        out.accept(ModItems.STAREDOWN_PENDANT.get());
                        out.accept(ModItems.SUNKEN_RING.get());
                        out.accept(ModItems.TENDRIL_SIGIL.get());
                        out.accept(ModItems.THROAT_SALT.get());
                        out.accept(ModItems.TONGUE_OF_OLD_ONES.get());
                        out.accept(ModItems.UMBRAL_ORE_ITEM.get());
                        out.accept(ModItems.UMBRAL_SHARD.get());
                        out.accept(ModItems.VEINSTONE_FRAGMENT.get());
                        out.accept(ModItems.VOIDITE_ORE_ITEM.get());
                        out.accept(ModItems.VOIDITE_SHARD.get());
                        out.accept(ModItems.VOID_GOLD_NUGGET.get());
                        out.accept(ModItems.VOID_SEER_ORB.get());
                        out.accept(ModItems.WET_BELL.get());
                        out.accept(ModItems.WHISPERING_VEIL.get());
                    })
                    .build());


    // ════════════════════════════════════════════════════════════════════════
    // r144: ABBADON TAB — Sangue, Carne, Culto, Sangue×Ordem (lugar de aniquilação)
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<CreativeModeTab> ABBADON = CREATIVE_MODE_TABS.register("abbadon",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Liberthia: Abbadon"))
                    .icon(() -> ModItems.HEART_OF_THE_MOTHER.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        // ─── Blood Fountain / Cure / Altar / Cauldron ───
                        out.accept(ModItems.BLOOD_FOUNTAIN_ITEM.get());
                        out.accept(ModItems.BLOOD_CURE_PILL.get());
                        out.accept(ModItems.BLOOD_ALTAR_ITEM.get());
                        out.accept(ModItems.BLOOD_CAULDRON_ITEM.get());
                        // ─── Flesh / Mother (Fase 2) ───
                        out.accept(ModItems.LIVING_FLESH_ITEM.get());
                        out.accept(ModItems.FLESH_MOTHER_ITEM.get());
                        out.accept(ModItems.ATTACKING_FLESH_ITEM.get());
                        out.accept(ModItems.HEART_OF_FLESH_BLOCK_ITEM.get());
                        out.accept(ModItems.HEART_OF_FLESH_ITEM.get());
                        out.accept(ModItems.HEART_OF_THE_MOTHER.get());
                        out.accept(ModItems.FLESH_MOTHER_BOSS_EGG.get());
                        out.accept(ModItems.FLESH_THREAD.get());
                        out.accept(ModItems.FLESH_CRAWLER_EGG.get());
                        // ─── Blood Infection / Volcano / Spike ───
                        out.accept(ModItems.BLOOD_INFECTION_BLOCK_ITEM.get());
                        out.accept(ModItems.BLOOD_INFESTATION_BLOCK_ITEM.get());
                        out.accept(ModItems.BLOOD_VOLCANO_ITEM.get());
                        out.accept(ModItems.BLOOD_SPIKE_ITEM.get());
                        out.accept(ModItems.BLOOD_BUCKET.get());
                        // ─── Blood Terrain variants ───
                        out.accept(ModItems.BLOOD_DIRT_ITEM.get());
                        out.accept(ModItems.BLOOD_SAND_ITEM.get());
                        out.accept(ModItems.BLOOD_STONE_ITEM.get());
                        out.accept(ModItems.BLOOD_COAL_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_IRON_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_GOLD_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_DIAMOND_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_REDSTONE_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_LAPIS_ORE_ITEM.get());
                        out.accept(ModItems.BLOOD_EMERALD_ORE_ITEM.get());
                        // ─── Blood Armor ───
                        out.accept(ModItems.BLOOD_HELMET.get());
                        out.accept(ModItems.BLOOD_CHESTPLATE.get());
                        out.accept(ModItems.BLOOD_LEGGINGS.get());
                        out.accept(ModItems.BLOOD_BOOTS.get());
                        // ─── Blood Scythe + Sanguine Core/Essence ───
                        out.accept(ModItems.BLOOD_SCYTHE.get());
                        out.accept(ModItems.SANGUINE_CORE.get());
                        out.accept(ModItems.SANGUINE_ESSENCE.get());
                        // ─── Order x Sangue (Fase 5) ───
                        out.accept(ModItems.ORDER_SHRINE_ITEM.get());
                        out.accept(ModItems.DESECRATED_HOLY_RELIC.get());
                        out.accept(ModItems.ORDER_PALADIN_EGG.get());
                        // ─── Weapons / Magic (Fase 4) ───
                        out.accept(ModItems.HEMOMANCER_STAFF.get());
                        out.accept(ModItems.BLOOD_RITUAL_DAGGER.get());
                        out.accept(ModItems.BLOOD_PACT_AMULET.get());
                        // ─── Alquimia de Sangue (Fase 3) ───
                        out.accept(ModItems.BLOOD_VIAL.get());
                        out.accept(ModItems.BLOOD_VIAL_FILLED.get());
                        out.accept(ModItems.CONGEALED_BLOOD.get());
                        // ─── Culto do Sangue (Fase 1) ───
                        out.accept(ModItems.BLOODY_RAG.get());
                        out.accept(ModItems.RUSTED_DAGGER.get());
                        out.accept(ModItems.PRIEST_SIGIL.get());
                        out.accept(ModItems.TOME_OF_THE_MOTHER.get());
                        out.accept(ModItems.TOME_OF_THE_PILGRIM.get());
                        // ─── Spawn Eggs ───
                        out.accept(ModItems.GORE_WORM_EGG.get());
                        out.accept(ModItems.BLOOD_CULTIST_EGG.get());
                        out.accept(ModItems.BLOOD_PRIEST_EGG.get());
                        out.accept(ModItems.WOUNDED_PILGRIM_EGG.get());
                        out.accept(ModItems.BLOOD_MAGE_EGG.get());
                        out.accept(ModItems.BLOOD_HOUND_EGG.get());
                        out.accept(ModItems.BLOOD_WARDEN_EGG.get());
                        // ─── Seringa + Tainted ───
                        out.accept(ModItems.BLOOD_SYRINGE.get());
                        out.accept(ModItems.BLOOD_TELEPORT_PEARL.get());
                        out.accept(ModItems.TAINTED_APPLE.get());
                        // ─── Sanguine Ward (anti Blood Infection) ───
                        out.accept(ModItems.SANGUINE_WARD_HELMET.get());
                        out.accept(ModItems.SANGUINE_WARD_CHESTPLATE.get());
                        out.accept(ModItems.SANGUINE_WARD_LEGGINGS.get());
                        out.accept(ModItems.SANGUINE_WARD_BOOTS.get());
                        out.accept(ModItems.SANGUINE_WARD_SWORD.get());
                        out.accept(ModItems.SANGUINE_WARD_PICKAXE.get());
                        out.accept(ModItems.BLOOD_WARD_CHARM.get());
                        out.accept(ModItems.TAINTED_ESSENCE.get());
                        out.accept(ModItems.CLEANSING_SALT.get());
                        out.accept(ModItems.PURIFYING_FLASK.get());
                        // ─── Ritual blocks ───
                        out.accept(ModItems.BLOOD_SACRIFICIAL_BOWL_ITEM.get());
                        out.accept(ModItems.GOLDEN_BLOOD_BOWL_ITEM.get());
                        out.accept(ModItems.CRYSTALLIZED_BLOOD_SOUL.get());
                        // ─── Blood Attacking blocks ───
                        out.accept(ModItems.HEMORRHAGE_SPIKE_ITEM.get());
                        out.accept(ModItems.SANGUINE_SNARE_ITEM.get());
                        // ─── Occultism blood ───
                        out.accept(ModItems.BLOOD_CHALK.get());
                        out.accept(ModItems.BLOOD_TORCH_ITEM.get());
                        // ─── Sanguine Wood set ───
                        out.accept(ModItems.SANGUINE_LOG_ITEM.get());
                        out.accept(ModItems.SANGUINE_WOOD_ITEM.get());
                        out.accept(ModItems.STRIPPED_SANGUINE_LOG_ITEM.get());
                        out.accept(ModItems.STRIPPED_SANGUINE_WOOD_ITEM.get());
                        out.accept(ModItems.SANGUINE_PLANKS_ITEM.get());
                        out.accept(ModItems.SANGUINE_LEAVES_ITEM.get());
                        out.accept(ModItems.SANGUINE_SAPLING_ITEM.get());
                        out.accept(ModItems.SANGUINE_STAIRS_ITEM.get());
                        out.accept(ModItems.SANGUINE_SLAB_ITEM.get());
                        out.accept(ModItems.SANGUINE_FENCE_ITEM.get());
                        out.accept(ModItems.SANGUINE_FENCE_GATE_ITEM.get());
                        out.accept(ModItems.SANGUINE_BUTTON_ITEM.get());
                        out.accept(ModItems.SANGUINE_PRESSURE_PLATE_ITEM.get());
                        out.accept(ModItems.SANGUINE_DOOR_ITEM.get());
                        out.accept(ModItems.SANGUINE_TRAPDOOR_ITEM.get());
                        // ─── Blood Tree set ───
                        out.accept(ModItems.BLOOD_LOG_ITEM.get());
                        out.accept(ModItems.STRIPPED_BLOOD_LOG_ITEM.get());
                        out.accept(ModItems.BLOOD_PLANKS_ITEM.get());
                        out.accept(ModItems.BLOOD_LEAVES_ITEM.get());
                        out.accept(ModItems.BLOOD_SAPLING_ITEM.get());
                        out.accept(ModItems.BLOOD_STAIRS_ITEM.get());
                        out.accept(ModItems.BLOOD_SLAB_ITEM.get());
                        out.accept(ModItems.BLOOD_FENCE_ITEM.get());
                        out.accept(ModItems.BLOOD_FENCE_GATE_ITEM.get());
                        out.accept(ModItems.BLOOD_DOOR_ITEM.get());
                        out.accept(ModItems.BLOOD_TRAPDOOR_ITEM.get());
                        // ─── Magic-school Blood ───
                        out.accept(ModItems.MAGIC_FIRE_BLOOD_ITEM.get());
                        out.accept(ModItems.PERK_VAMPIRIC.get());
                        out.accept(ModItems.SCROLL_BLOOD.get());
                        out.accept(ModItems.RING_BLOODBORN.get());
                        out.accept(ModItems.STAFF_BLOOD.get());
                        out.accept(ModItems.FOCUS_BLOOD.get());
                        out.accept(ModItems.WIZARD_CHEST_BLOOD.get());
                        out.accept(ModItems.WIZARD_HELM_BLOOD.get());
                        out.accept(ModItems.WIZARD_LEGS_BLOOD.get());
                        out.accept(ModItems.WIZARD_BOOTS_BLOOD.get());
                        // ─── Spells de sangue (Iron's Spells) ───
                        out.accept(ModItems.SPELL_BLOOD_STEP.get());
                        out.accept(ModItems.SPELL_LIFEDRAIN.get());
                        out.accept(ModItems.SPELL_HEARTSTOP.get());
                        out.accept(ModItems.SPELL_BLOOD_SPEAR.get());
                        out.accept(ModItems.SPELL_SANGUINE_BIND.get());
                        out.accept(ModItems.SPELL_CRIMSON_MIST.get());
                        out.accept(ModItems.SPELL_VAMPIRIC_TOUCH.get());
                        out.accept(ModItems.SPELL_BLOOD_PACT.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    // r180c: Liberthia Tech — Powah/Mekanism-style energia, máquinas, ferramentas, materiais.
    public static final RegistryObject<CreativeModeTab> TECH = CREATIVE_MODE_TABS.register("tech",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.liberthia.tech"))
                    .icon(() -> ModTech.STEEL_INGOT.get().getDefaultInstance())
                    .withTabsBefore(new ResourceLocation(LiberthiaMod.MODID, "main"))
                    .displayItems((parameters, output) -> {
                        // ─── Lote 1: materiais base ───
                        output.accept(ModTech.STEEL_INGOT.get());
                        output.accept(ModTech.STEEL_PLATE.get());
                        output.accept(ModTech.STEEL_BLOCK_ITEM.get());
                        output.accept(ModTech.ENERGIZED_STEEL.get());
                        output.accept(ModTech.CONTROL_CIRCUIT.get());
                        // ─── Lote 2: dusts + componentes ───
                        output.accept(ModTech.IRON_DUST.get());
                        output.accept(ModTech.GOLD_DUST.get());
                        output.accept(ModTech.COPPER_DUST.get());
                        output.accept(ModTech.STEEL_DUST.get());
                        output.accept(ModTech.COPPER_COIL.get());
                        // ─── Lote 3: energia ───
                        output.accept(ModTech.ENERGY_CELL_BASIC_ITEM.get());
                        output.accept(ModTech.ENERGY_CELL_ADVANCED_ITEM.get());
                        output.accept(ModTech.ENERGY_CELL_ULTIMATE_ITEM.get());
                        output.accept(ModTech.SOLAR_PANEL_ITEM.get());
                        output.accept(ModTech.THERMO_GENERATOR_ITEM.get());
                        // ─── Lote 4: máquinas ───
                        output.accept(ModTech.CRUSHER_ITEM.get());
                        output.accept(ModTech.ENERGIZED_SMELTER_ITEM.get());
                        output.accept(ModTech.COMPRESSOR_ITEM.get());
                        output.accept(ModTech.ALLOY_SMELTER_ITEM.get());
                        output.accept(ModTech.SAWMILL_ITEM.get());
                        // ─── Lote 5: ferramentas energizadas ───
                        output.accept(ModTech.ENERGIZED_PICKAXE.get());
                        output.accept(ModTech.ENERGIZED_AXE.get());
                        output.accept(ModTech.ENERGIZED_SHOVEL.get());
                        output.accept(ModTech.ENERGIZED_SWORD.get());
                        output.accept(ModTech.ENERGIZED_DRILL.get());
                        output.accept(ModTech.PAXEL.get());
                        // ─── Lote 6: power armor ───
                        output.accept(ModTech.POWER_HELMET.get());
                        output.accept(ModTech.POWER_CHESTPLATE.get());
                        output.accept(ModTech.POWER_LEGGINGS.get());
                        output.accept(ModTech.POWER_BOOTS.get());
                        // ─── Lote 7: utilidade ───
                        output.accept(ModTech.BATTERY.get());
                        output.accept(ModTech.CHARGER_ITEM.get());
                        output.accept(ModTech.TECH_WRENCH.get());
                        output.accept(ModTech.ITEM_MAGNET.get());
                        output.accept(ModTech.JETPACK.get());
                        // ─── Lote 8: componentes + geradores + portable charger ───
                        output.accept(ModTech.REINFORCED_PLATE.get());
                        output.accept(ModTech.ENERGIZED_CIRCUIT.get());
                        output.accept(ModTech.PORTABLE_CHARGER.get());
                        output.accept(ModTech.FURNATOR_ITEM.get());
                        output.accept(ModTech.MAGMATOR_ITEM.get());
                        // ─── Integração: Reator de Matéria Escura ───
                        output.accept(ModTech.MATTER_REACTOR_ITEM.get());
                        // ─── Motores de entropia (espalham + revert) ───
                        output.accept(ModTech.CRYSTALIZED_DARK_MATTER_ITEM.get());
                        output.accept(ModTech.ENTROPY_CORE_ITEM.get());
                        output.accept(ModTech.BLACK_MATTER_ENGINE_ITEM.get());
                        // ─── Onda 1: máquinas arcanas + intermediários (cadeia multi-etapa) ───
                        output.accept(ModTech.METAL_PRESS.get());
                        output.accept(ModTech.ARCANE_INFUSER.get());
                        output.accept(ModTech.TECH_ASSEMBLER.get());
                        output.accept(ModTech.MANA_CONDENSER.get());
                        output.accept(ModTech.CRYSTAL_SMELTER.get());
                        // r188 — 16 máquinas novas + 8 intermediários
                        output.accept(ModTech.QUANTUM_PULVERIZER.get());
                        output.accept(ModTech.CRYSTALLIZATION_CHAMBER.get());
                        output.accept(ModTech.COIL_WINDER.get());
                        output.accept(ModTech.MATTER_CONDENSER_T.get());
                        output.accept(ModTech.NANO_ASSEMBLER.get());
                        output.accept(ModTech.ENERGY_DISTILLER.get());
                        output.accept(ModTech.FLUX_FORGE.get());
                        output.accept(ModTech.MANA_CRYSTALLIZER.get());
                        output.accept(ModTech.ARCANE_CIRCUIT_PRINTER.get());
                        output.accept(ModTech.DARK_ALLOY_SMELTER.get());
                        output.accept(ModTech.PHOTON_INFUSER.get());
                        output.accept(ModTech.MATTER_REPLICATOR.get());
                        output.accept(ModTech.CRYSTAL_GROWER.get());
                        output.accept(ModTech.ESSENCE_COMPRESSOR.get());
                        output.accept(ModTech.RUNE_ETCHER.get());
                        output.accept(ModTech.SINGULARITY_PRESS.get());
                        // r189 — +18 máquinas
                        output.accept(ModTech.ARCANE_COLLECTOR.get());
                        output.accept(ModTech.MANA_REACTOR.get());
                        output.accept(ModTech.CRYSTAL_RESONATOR.get());
                        output.accept(ModTech.ENDER_CONDENSER.get());
                        output.accept(ModTech.SOUL_EXTRACTOR.get());
                        output.accept(ModTech.BLAZE_REACTOR.get());
                        output.accept(ModTech.MATTER_FABRICATOR.get());
                        output.accept(ModTech.CIRCUIT_ASSEMBLER.get());
                        output.accept(ModTech.PLATE_PRESS.get());
                        output.accept(ModTech.WIRE_DRAWER.get());
                        output.accept(ModTech.GEM_POLISHER.get());
                        output.accept(ModTech.INGOT_FORMER.get());
                        output.accept(ModTech.ARCANE_SYNTHESIZER.get());
                        output.accept(ModTech.FLUX_DYNAMO.get());
                        output.accept(ModTech.SHARD_SPLITTER.get());
                        output.accept(ModTech.COSMIC_DISTILLER.get());
                        output.accept(ModTech.RUNE_INSCRIBER.get());
                        output.accept(ModTech.SINGULARITY_CORE_FORGE.get());
                        output.accept(ModTech.QUANTUM_DUST.get());
                        output.accept(ModTech.PHOTON_CRYSTAL.get());
                        output.accept(ModTech.RESONANCE_COIL.get());
                        output.accept(ModTech.DARK_MATTER_CELL_CORE.get());
                        output.accept(ModTech.ASSEMBLED_MATRIX.get());
                        output.accept(ModTech.ENERGY_ESSENCE.get());
                        output.accept(ModTech.MANA_CRYSTAL.get());
                        output.accept(ModTech.VOID_ALLOY_INGOT.get());
                        output.accept(ModBlocks.DARK_INFECTED_GRASS.get());
                        output.accept(ModBlocks.DARK_INFECTED_DIRT.get());
                        output.accept(ModBlocks.DARK_INFECTED_SAND.get());
                        output.accept(ModBlocks.DARK_INFECTED_STONE.get());
                        output.accept(ModTech.HARDENED_PLATE.get());
                        output.accept(ModTech.WARDED_PLATE.get());
                        output.accept(ModTech.MANA_CAPACITOR.get());
                        output.accept(ModTech.ARCANE_ALLOY.get());
                        output.accept(ModTech.WARDED_MODULE.get());
                        // ─── Onda 2 ───
                        output.accept(ModTech.FLIGHT_BEACON.get());
                        output.accept(ModTech.BEACON_FLIGHT_UPGRADE.get());
                    })
                    .build());

    // ════════════ Tecno-Arcano (anti-magia) ════════════
    public static final RegistryObject<CreativeModeTab> ANTIMAGIC = CREATIVE_MODE_TABS.register("antimagic",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.liberthia.antimagic"))
                    .icon(() -> ModTech.WARDED_HELMET.get().getDefaultInstance())
                    .withTabsBefore(new ResourceLocation(LiberthiaMod.MODID, "tech"))
                    .displayItems((params, out) -> {
                        // componentes tier (um puxa o outro)
                        out.accept(ModTech.WARDED_CORE.get());
                        out.accept(ModTech.ARCANE_DISRUPTOR.get());
                        out.accept(ModTech.NULL_MATRIX.get());
                        // Armadura Bastião + Escudo Dissonante
                        out.accept(ModTech.WARDED_HELMET.get());
                        out.accept(ModTech.WARDED_CHESTPLATE.get());
                        out.accept(ModTech.WARDED_LEGGINGS.get());
                        out.accept(ModTech.WARDED_BOOTS.get());
                        out.accept(ModTech.WARDED_SHIELD.get());
                        // Selos
                        out.accept(ModTech.MANA_SUPPRESSOR_ITEM.get());
                        out.accept(ModTech.ARCANE_SENTINEL_ITEM.get());
                        out.accept(ModTech.ARCANE_TURRET.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
