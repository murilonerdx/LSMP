package br.com.murilo.liberthia.manual;

import br.com.murilo.liberthia.manual.ManualContent.Chapter;
import br.com.murilo.liberthia.manual.ManualContent.Page;

import java.util.List;

/**
 * English (en_us) version of the Codex. Loaded lazily by
 * {@link ManualContent#chaptersForLocale} when the client's locale is not PT.
 *
 * <p>This file mirrors the structure of {@link ManualContent#CHAPTERS} —
 * intro chapters covering the three matters + operational manuals for the
 * 7 main blocks + pipes/logistics + analyzer/cures + recent changelog.
 *
 * <p>Lore-heavy chapters from PT (codex of researchers, Horus expedition,
 * etc) are NOT replicated here — they're written in a flowing PT style that
 * doesn't translate cleanly. Players running non-PT clients still see the
 * full UI/gameplay docs which is what matters for usage.
 */
public final class ManualContentEn {

    private ManualContentEn() {}

    public static final List<Chapter> CHAPTERS = List.of(

            new Chapter("§5Welcome", List.of(
                    new Page("Liberthia",
                            "§dLiberthia§r§7 hosts the §5three matters§r§7 at once — "
                                    + "§5Dark§r§7, §fClear§r§7, and §eYellow§r§7.\n\n"
                                    + "§7Use the §dchapters§r§7 on the left to navigate. The §darrows§r§7 "
                                    + "at the bottom flip pages within a chapter.\n\n"
                                    + "§l§5Focus of this manual:§r§7 how to use each item/block. "
                                    + "For full lore, read the §dResearcher's Codex§r§7 in-game."),
                    new Page("Useful key — F8",
                            "§dF8§r§7 cycles the Matter HUD between the 4 corners of your screen.\n\n"
                                    + "§7Configurable at §lOptions → Controls → Liberthia§r§7."),
                    new Page("Admin commands",
                            "§7For testing with specific profiles:\n\n"
                                    + "§o/liberthia matter set <player> dark|white|yellow <0-100>§r\n\n"
                                    + "§o/liberthia matter add <player> dark|white|yellow <-100..100>§r\n\n"
                                    + "§o/liberthia matter get <player>§r\n\n"
                                    + "§o/liberthia matter clear <player>§r")
            )),

            new Chapter("§5The Three Matters", List.of(
                    new Page("Dark Matter",
                            "§5Most powerful of the three§r§7. Warps reality around it, creating hostile "
                                    + "environments where chaos rules.\n\n"
                                    + "§7At small scales — like in Liberthia — it infects hosts, turning them "
                                    + "into §opuppets§r§7 of an Entity that possibly controls it.\n\n"
                                    + "§dManipulated with precision§r§7, it generates life from nothing. "
                                    + "Researchers call it §oanti-creation§r§7."),
                    new Page("Clear Matter",
                            "§fThe purest one§r§7. Neutralizes contamination from the other two.\n\n"
                                    + "§7Found in cold biomes (deserts, tundra). Light, almost weightless.\n\n"
                                    + "§7Used in §dpurification§r§7 and as a §dcatalyst§r§7 in most refining "
                                    + "machines."),
                    new Page("Yellow Matter",
                            "§eThe rarest and most enigmatic§r§7. Solar/divine vibes.\n\n"
                                    + "§7Hard to obtain naturally — you usually need to §oexplode§r§7 dark "
                                    + "matter shards with TNT to convert them.\n\n"
                                    + "§7Carries §opersuasive§r§7 energy. Pure tools made of yellow matter "
                                    + "are §lextremely§r§7 efficient.")
            )),

            new Chapter("§dOperations Manual", List.of(
                    new Page("How to read this chapter",
                            "§7This chapter documents the §d7 main blocks§r§7 of Liberthia progression:\n\n"
                                    + "§a§l1.§r§7 §dPurification Bench§r§7 — cleans contamination\n"
                                    + "§a§l2.§r§7 §dDark Matter Forge§r§7 — refines DM with fuel\n"
                                    + "§a§l3.§r§7 §dMatter Infuser§r§7 — infuses matter into items\n"
                                    + "§a§l4.§r§7 §dResearch Table§r§7 — research and lore\n"
                                    + "§a§l5.§r§7 §dContainment Chamber§r§7 — contains creatures\n"
                                    + "§a§l6.§r§7 §dMatter Transmuter§r§7 — converts matter types\n"
                                    + "§a§l7.§r§7 §dDark Matter Alchemizer§r§7 — top-tier alchemy\n\n"
                                    + "§7Each page shows: §ohow to craft, ingredients, output, visual recipe§r§7."),

                    new Page("Purification Bench",
                            "§b§lBasic purification§r§7 — converts contaminated items into clean essence.\n\n"
                                    + "§a§lUsage:§r§7 right-click opens GUI:\n"
                                    + "§7• §oslot 0§r: contaminated item (e.g. dark_matter_shard)\n"
                                    + "§7• §oslot 1§r: catalyst (water_bottle)\n"
                                    + "§7• §oslot 2§r: output (purified_essence)\n\n"
                                    + "§e§lCraft:§r §7Purified Essence (3x top) + Quartz Block (2x sides) + "
                                    + "Glass (center) + Smooth Stone (3x base).\n\n"
                                    + "§c§lDrop:§r §7Break → drops the block itself.",
                            "liberthia:purification_bench"),

                    new Page("Dark Matter Forge",
                            "§5§lAdvanced forge§r§7 — combines two inputs with fuel to create new items.\n\n"
                                    + "§a§lUsage:§r§7 right-click opens GUI with 4 slots:\n"
                                    + "§7• §oslot 0§r: §dFuel§r (dark_matter_shard, dark_matter_bucket, dark_matter_block)\n"
                                    + "§7• §oslot 1§r: §dInput 1§r\n"
                                    + "§7• §oslot 2§r: §dInput 2§r\n"
                                    + "§7• §oslot 3§r: §dOutput§r\n\n"
                                    + "§e§lKnown recipes:§r\n"
                                    + "§7• DM Shard + Iron Ingot → Stabilized DM\n"
                                    + "§7• Stabilized DM + Void Crystal → Singularity Core\n"
                                    + "§7• DM Block + Purified Essence → 2x Purified Essence\n\n"
                                    + "§c§lDrop:§r §7Block self + items inside drop on the ground.",
                            "liberthia:dark_matter_forge"),

                    new Page("Matter Infuser",
                            "§3§lMatter infusion§r§7 — adds matter properties to existing items.\n\n"
                                    + "§a§lUsage:§r§7 GUI with 5 slots (DM, CM, YM, Catalyst, Output).\n\n"
                                    + "§e§lExample recipe:§r\n"
                                    + "§7• Dark Matter Block + Clear Matter Block + Purified Essence → "
                                    + "3x Purified Essence\n"
                                    + "§7• ... + Yellow Matter Ingot → 4x Clear Matter Pill\n\n"
                                    + "§b§lCraft:§r §7Iron Ingot (4x cross) + Glass (4x sides) + "
                                    + "Quartz Block (center).",
                            "liberthia:matter_infuser"),

                    new Page("Research Table",
                            "§e§lResearch desk§r§7 — produces §dwritten books§r§7 and unlocks recipes.\n\n"
                                    + "§a§lUsage:§r§7 GUI with 3 slots (Material, Paper, Output).\n\n"
                                    + "§e§lRecipes:§r\n"
                                    + "§7• §dDark Matter Shard§r + §oPaper§r → §dWritten Book§r (research notes)\n"
                                    + "§7• §dPurified Essence§r + §oBook§r → §dWritten Book§r (lore pages)\n"
                                    + "§7• §dClear Matter Block§r + §oPaper§r → §dClear Matter Pill§r\n\n"
                                    + "§b§lCraft:§r §7Books (2x top) + Iron Ingot (center) + Oak Planks (3x mid) + "
                                    + "Sticks (sides bottom).",
                            "liberthia:research_table"),

                    new Page("Containment Chamber",
                            "§b§lContainment chamber§r§7 — neutralizes hostile energy from matter blocks.\n\n"
                                    + "§a§lUsage:§r§7 GUI with 4 slots — 2 inputs + 1 catalyst + 1 output.\n\n"
                                    + "§e§lRecipes:§r\n"
                                    + "§7• §dDark Matter Block§r + §dDark Matter Shard§r → 4x §dDark Matter Shard§r\n"
                                    + "§7• §dYellow Matter Block§r + §dPurified Essence§r → §dSingularity Core§r\n\n"
                                    + "§b§lCraft:§r §7Obsidian (4x corners) + Iron Block (2x top/bottom mid) + "
                                    + "Glass (2x sides mid) + Dark Matter Shard (center).",
                            "liberthia:containment_chamber"),

                    new Page("Matter Transmuter",
                            "§6§lTransmutation§r§7 — converts one matter type into another using a catalyst.\n\n"
                                    + "§a§lUsage:§r§7 GUI with 3 slots (Input, Catalyst, Output).\n\n"
                                    + "§e§lRecipes:§r\n"
                                    + "§7• §dDark Matter Block§r + §dPurified Essence§r → §dClear Matter Block§r\n"
                                    + "§7• §dClear Matter Block§r + §dYellow Matter Ingot§r → §dYellow Matter Block§r\n\n"
                                    + "§b§lCraft:§r §7Recipe at data/liberthia/recipes/matter_transmuter.json.",
                            "liberthia:matter_transmuter"),

                    new Page("Dark Matter Alchemizer",
                            "§5§lTop-tier alchemy§r§7 — combines processes from other machines into one "
                                    + "unified flow. High blast resistance (1200F).\n\n"
                                    + "§a§lUsage:§r§7 Has complex GUI — opens full-screen with tabs.\n\n"
                                    + "§e§lFunctions:§r\n"
                                    + "§7• Combines §dForge§r + §dTransmuter§r§7 in one\n"
                                    + "§7• Supports §oupgrades§r§7 (Speed, Efficiency, Capacity)\n"
                                    + "§7• Multi-slot output\n\n"
                                    + "§c§lDrop:§r §7Block self. §lWarning:§r§7 strong explosions (TNT, creepers) "
                                    + "can still destroy — 1200F is high, not infinite.",
                            "liberthia:dark_matter_alchemizer")
            )),

            new Chapter("§dPipes and Logistics", List.of(
                    new Page("Overview",
                            "§7Liberthia's §dpipe system§r§7 moves items between inventories (chests, "
                                    + "machines, dispensers, etc) without needing a hopper.\n\n"
                                    + "§a§l3 main blocks:§r\n"
                                    + "§7• §dItem Pipe§r§7 — just transports (no source/sink)\n"
                                    + "§7• §dItem Extractor§r§7 — pulls items from adjacent inventory\n"
                                    + "§7• §dItem Inserter§r§7 — pushes items into adjacent inventory\n\n"
                                    + "§a§l3 upgrades:§r §7Speed (4 or 64 items/op), Efficiency (less cost), "
                                    + "Capacity (bigger filters)."),

                    new Page("How to connect",
                            "§a§l1.§r§7 Place §dItem Extractor§r§7 STUCK to a chest/inventory source.\n\n"
                                    + "§a§l2.§r§7 Extend the network with §dItem Pipes§r§7 (any path — they "
                                    + "auto-connect).\n\n"
                                    + "§a§l3.§r§7 Place §dItem Inserter§r§7 STUCK to the destination.\n\n"
                                    + "§a§l4.§r§7 §lShift+click§r§7 a pipe face to cycle mode:\n"
                                    + "§7•   §oDEFAULT§r — passes items but doesn't extract/insert directly\n"
                                    + "§7•   §oEXTRACT§r — pulls items from this face\n"
                                    + "§7•   §oINSERT§r — sends items to this face\n"
                                    + "§7•   §oDISABLED§r — face closed (no items pass)"),

                    new Page("v0.1.7 bug fix",
                            "§c§lBefore:§r§7 when pulling from a chest with multiple item types, the pipe "
                                    + "wasted its budget on slot 0 (1 type only). You had to wait for that "
                                    + "type to empty before reaching the others.\n\n"
                                    + "§a§lNow:§r§7 budget is PER SLOT. All item types move §lsimultaneously§r§7 "
                                    + "in the same tick.\n\n"
                                    + "§7Result: pipes are now §oN×§r§7 faster in chests with many different "
                                    + "types (where N = number of occupied slots)."),

                    new Page("Speed Upgrade",
                            "§c§lSpeed Upgrade§r§7 — install on a pipe face (right-click with upgrade in hand).\n\n"
                                    + "§a§lEffect:§r §71x = 4 items/op. §o2x = 16 items/op. 3x = 64 items/op§r§7.\n\n"
                                    + "§e§lCraft:§r §7Feather (center) + Redstone (4x cross) + Iron Ingot (4x corners).",
                            "liberthia:speed_upgrade"),

                    new Page("Efficiency Upgrade",
                            "§b§lEfficiency Upgrade§r§7 — reduces operation cooldown.\n\n"
                                    + "§a§lEffect:§r §71x = -25% cooldown. 2x = -50%. 3x = -75%.\n\n"
                                    + "§e§lCraft:§r §7Copper Ingot (center) + Redstone Torch (4x cross) + "
                                    + "Iron Ingot (4x corners).",
                            "liberthia:efficiency_upgrade"),

                    new Page("Capacity Upgrade",
                            "§6§lCapacity Upgrade§r§7 — increases pipe filter list size.\n\n"
                                    + "§a§lEffect:§r §71x = 8 slots. 2x = 18 slots. 3x = 36 slots.\n\n"
                                    + "§e§lCraft:§r §7Shulker Shell (center) + Leather (4x cross) + "
                                    + "Iron Ingot (4x corners).",
                            "liberthia:capacity_upgrade")
            )),

            new Chapter("§dAnalyzer and Cures", List.of(
                    new Page("Matter Analyzer — usage",
                            "§5§lMatter analyzer§r§7 — read-only \"computer\".\n\n"
                                    + "§a§lUsage:§r§7 GUI with 1 input slot. Place an item and shows:\n"
                                    + "§7• §dDM§r§7 / §fWM§r§7 / §eYM§r§7 current values\n"
                                    + "§7• Composed mutation type (DARK, WHITE, YELLOW, mixed)\n"
                                    + "§7• Total matter (sum)\n\n"
                                    + "§7Accepts §oSample Vials§r§7 filled (NBT read) or items from "
                                    + "§oMatterContentRegistry§r§7 (static)."),

                    new Page("Sample Vial workflow",
                            "§a§l1.§r§7 Craft §dSample Vial§r§7 (3x Glass Pane in V + Iron Nugget center).\n\n"
                                    + "§a§l2.§r§7 Right-click a block — collects sample (block NBT + DM aura "
                                    + "scan in radius 5 around).\n\n"
                                    + "§a§l3.§r§7 Place the filled vial in the §dMatter Analyzer§r§7.\n\n"
                                    + "§a§l4.§r§7 Read the values. Vial is §oreusable§r§7 — empty and reuse."),

                    new Page("Matter Cure",
                            "§b§lEmergency cure§r§7 — zeroes player's DM/WM/YM + grants Regen II + Resistance I "
                                    + "for 10s.\n\n"
                                    + "§a§lWhen to use:§r§7 when your Matter HUD is about to overflow or you "
                                    + "absorbed too much of a matter you didn't want.\n\n"
                                    + "§c§lLimitation:§r§7 if you have no matter to purify, it does NOT consume "
                                    + "(avoids waste).\n\n"
                                    + "§e§lCraft:§r §7Glowstone Dust (top) + 2x Purified Essence (sides) + "
                                    + "Clear Matter Pill (center) + Glass Bottle (bottom).",
                            "liberthia:matter_cure"),

                    new Page("Daily Pill",
                            "§e§lDaily pill§r§7 — routine maintenance.\n\n"
                                    + "§a§lEffect (1 in-game day = 20min real):§r\n"
                                    + "§7• §dResistance I§r§7 — reduces damage by 20%\n"
                                    + "§7• §dRegeneration I§r§7 — passive healing\n"
                                    + "§7• §dAbsorption I§r§7 (1min) — +2 hearts shield\n\n"
                                    + "§a§lDoes NOT zero matter§r§7 — just buffers exposure damage.\n\n"
                                    + "§e§lCraft:§r §74x Glow Berries + 2x Sugar + 2x Nether Wart + Clear Matter Pill "
                                    + "(center) → §o4 pills§r§7.",
                            "liberthia:daily_pill")
            )),

            new Chapter("§dv0.1.7 Changes", List.of(
                    new Page("What changed",
                            "§a§lNew features:§r\n"
                                    + "§7• §dDarkMatterShard§r§7 drops while mining (stone, deepslate, etc) "
                                    + "with up to 3% chance at bedrock.\n"
                                    + "§7• §dTNT + DarkMatterShard§r§7 → §dYellowMatterIngot§r§7 via explosion.\n"
                                    + "§7• §dSampleVial§r§7 is now craftable.\n"
                                    + "§7• Cures §dMatterCure§r§7 + §dDailyPill§r§7.\n"
                                    + "§7• Pipes now transfer all item types §osimultaneously§r.\n\n"
                                    + "§c§lRemoved items:§r\n"
                                    + "§7• HolyEssence → migrated to PurifiedEssence\n"
                                    + "§7• WhiteMatterFinder, SafeSiphon (cut from design)"),

                    new Page("DarkMatterShard mining",
                            "§7When you §lmine§r§7 stone/cobblestone/deepslate/tuff/netherrack/blackstone/basalt/"
                                    + "endstone/dripstone with §liron+ pickaxe§r§7, there's a chance to drop:\n\n"
                                    + "§a§lFormula:§r §70.5%% + 0.005%% × (64 - Y)\n"
                                    + "§7• At Y=64: §o0.5%%§r\n"
                                    + "§7• At Y=0: §o0.82%%§r\n"
                                    + "§7• At Y=-64: §o1.14%%§r\n"
                                    + "§7• Max cap: §o3%%§r\n\n"
                                    + "§7§c§lWooden/stone§r§7 pickaxes do NOT count (anti-farm)."),

                    new Page("TNT → YellowMatterIngot",
                            "§7Drop §dDarkMatterShards§r§7 on the ground, place TNT, ignite and §lboom§r§7:\n\n"
                                    + "§a§lConversion:§r §71 shard = 1 ingot. Full stack converted.\n\n"
                                    + "§7Conversion radius: §o4 blocks§r§7 (matches TNT blast).\n\n"
                                    + "§7Effects: §oparticles END_ROD§r§7 + §oAMETHYST_CHIME sound§r§7.\n\n"
                                    + "§c§lWarning:§r§7 shards can be destroyed if the explosion hits before "
                                    + "the conversion. Place inside a 1-block hole to protect them.")
            ))
    );
}
