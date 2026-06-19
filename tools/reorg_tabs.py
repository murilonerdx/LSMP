"""r143: Reorg tabs — move items pos-PULSO da tab principal pra Magic/Horror."""
import re, sys

# HORROR items — Cosmic Horror, Loom Dimension, Liminal artifacts
HORROR = [
    # Cosmic Horror Items
    "WHISPERING_VEIL", "EYES_OF_ABYSS", "CURSED_CRADLE", "PENDULUM_OF_DREAD",
    "LANTERN_OF_FALSE_MEMORY", "TONGUE_OF_OLD_ONES", "HOURGLASS_OF_REGRESSION",
    "VOID_SEER_ORB",
    # Cosmic Horror triggers
    "FORBIDDEN_TOME", "TENDRIL_SIGIL", "DARK_MATTER_LASER",
    # Loom/Spirit Dimension ores + magic
    "RIFTITE_ORE_ITEM", "RIFTITE_SHARD", "UMBRAL_ORE_ITEM", "UMBRAL_SHARD",
    "VOIDITE_ORE_ITEM", "VOIDITE_SHARD", "LOOM_STONE_ITEM",
    "SPIRITUAL_LINK", "SPIRITUAL_CONNECTION",
    # Pale Watch Artifacts (r55)
    "CLONE_ARMY", "STAREDOWN_PENDANT", "PALE_BLINK_PENDANT",
    "PARALYZE_PENDANT", "SPIRIT_GUIDE",
    # 15 cryptic artifacts (r56)
    "PULLED_STRING", "QUIET_MARK", "LONELY_ECHO", "FOLDED_DISTANCE",
    "THROAT_SALT", "SOFT_WOUND", "LOOKING_GLASS", "HALF_STEP",
    "BENT_IRON", "PALE_COIN", "WET_BELL", "MARROW_WHISTLE",
    "LISTENING_GLASS", "SUNKEN_RING", "HAND_ON_GLASS",
    "EXODUS_BOOK",
    # 3 Liminal entry keys (r57)
    "DROWNED_COMPASS", "FOLDED_ADDRESS", "BARK_TOKEN",
    # 12 dimensional ore items (r56)
    "SOULITE_SHARD", "PALE_CRYSTAL_SHARD", "VEINSTONE_FRAGMENT",
    "HOLLOW_SILVER_NUGGET", "MOURNING_EMBER", "GHOST_QUARTZ_SHARD",
    "NULL_IRON_SHARD", "ABYSSIUM_DUST", "BLACK_STAR_CORE",
    "DISTORTION_CRYSTAL", "VOID_GOLD_NUGGET", "EYE_STONE_SHARD",
]

# MAGIC items — Observation Casting, Rituals, Sigils, Glyphs
MAGIC = [
    # Occult Ritual System
    "CHALK_WHITE", "CHALK_GOLDEN", "CHALK_PURPLE", "CHALK_RED", "CHALK_BLACK",
    "CANDLE_WHITE_OCCULT_ITEM", "CANDLE_GOLDEN_OCCULT_ITEM",
    "CANDLE_PURPLE_OCCULT_ITEM", "CANDLE_RED_OCCULT_ITEM", "CANDLE_BLACK_OCCULT_ITEM",
    "LIGHTER", "RITUAL_DAGGER", "RITUAL_CHALICE",
    "RITUAL_CIRCLE_ITEM", "SPIRIT_MINER_ITEM",
    # 10 Sigils
    "SIGIL_FOLIOT", "SIGIL_DJINNI", "SIGIL_AFRIT", "SIGIL_BAEL",
    "SIGIL_LUCIFER", "SIGIL_SANDALPHON", "SIGIL_METATRON",
    "SIGIL_NECRO", "SIGIL_BANISHING", "SIGIL_DIMENSIONAL",
    # 3 Bound crystals
    "BOUND_FOLIOT_CRYSTAL", "BOUND_DJINNI_CRYSTAL", "BOUND_AFRIT_CRYSTAL",
    # Magic books + crafting
    "CREATIVE_GRIMOIRE", "SPELL_CRAFTING_TABLE", "MAGIC_BOOK",
    # Magic materials
    "PALE_IRON_INGOT", "SOULSTEEL_INGOT", "RIFT_CRYSTAL",
    # Observation Casting core
    "SPELLSWORD", "SOURCE_GEM",
    "OBSERVATION_TOME", "SOURCE_JAR_ITEM",
    "SPELL_PARCHMENT", "SCRIBES_TABLE_ITEM",
    # 73 Glyphs
    "GLYPH_DIRECT_GAZE", "GLYPH_WATCH_PERIPHERAL", "GLYPH_WATCH_MEMORY",
    "GLYPH_WATCH_SILENCE", "GLYPH_WATCH_REFLECTION",
    "GLYPH_METHOD_TOUCH", "GLYPH_METHOD_SELF",
    "GLYPH_TENDRIL", "GLYPH_MANIFEST_SILENCE", "GLYPH_MANIFEST_MIRROR",
    "GLYPH_MANIFEST_WHISPER", "GLYPH_MANIFEST_DECAY", "GLYPH_MANIFEST_GLIMPSE",
    "GLYPH_EFFECT_IGNITE", "GLYPH_EFFECT_HARM", "GLYPH_EFFECT_HEAL",
    "GLYPH_EFFECT_FREEZE", "GLYPH_EFFECT_LAUNCH", "GLYPH_EFFECT_SLOWFALL",
    "GLYPH_AMPLIFY", "GLYPH_LINGER", "GLYPH_ECHO", "GLYPH_SECRET",
    "GLYPH_METHOD_LASER", "GLYPH_METHOD_BURST", "GLYPH_METHOD_ORBIT",
    "GLYPH_METHOD_WALL", "GLYPH_METHOD_CHAIN",
    "GLYPH_EFFECT_LIGHTNING", "GLYPH_EFFECT_GRAVITY", "GLYPH_EFFECT_BLIND",
    "GLYPH_EFFECT_LEVITATE", "GLYPH_EFFECT_KNOCKBACK",
    "GLYPH_EFFECT_EXPLOSION", "GLYPH_EFFECT_FANGS",
    "GLYPH_FIREBALL", "GLYPH_INFERNO", "GLYPH_CLEANSING_FLAME",
    "GLYPH_SOLAR_PULSE", "GLYPH_BURNING_AURA",
    "GLYPH_BUBBLE_SHIELD", "GLYPH_TIDAL_WAVE", "GLYPH_FROST_LANCE",
    "GLYPH_MIST_VEIL", "GLYPH_HEALING_RAIN",
    "GLYPH_STONE_SPIKES", "GLYPH_QUAKE_STEP", "GLYPH_VEIN_SIGHT",
    "GLYPH_EARTHEN_WALL", "GLYPH_ROOTS",
    "GLYPH_GUST", "GLYPH_TORNADO", "GLYPH_SKY_STEP",
    "GLYPH_VELOCITY", "GLYPH_WIND_CUTTER",
    "GLYPH_VOID_PULL", "GLYPH_DREAD_STARE", "GLYPH_MIND_SPIKE",
    "GLYPH_REALITY_TEAR", "GLYPH_SINGULARITY",
    "GLYPH_PLACE_BLOCK", "GLYPH_BREAK_BLOCK", "GLYPH_CONJURE_WATER",
    "GLYPH_LIGHT", "GLYPH_SNARE", "GLYPH_HEX", "GLYPH_PICKUP",
    "GLYPH_PIERCE", "GLYPH_SPLIT", "GLYPH_AOE",
    # Source items
    "SOURCE_CRYSTAL", "SOURCE_CATALYST", "SOURCE_LENS", "SOUL_FRAGMENT",
    # More magic tools
    "OBSERVATION_CHALK", "RUNE_BLOCK_ITEM",
    "IMBUEMENT_TABLE_ITEM", "SPELL_BINDING_PEDESTAL_ITEM",
    "GRIMOIRE_OF_OBSERVATION",
    # Spirit World magic ores
    "SOURCESTONE_ORE_ITEM", "SPIRIT_GEM_ORE_ITEM",
    "MANA_BERRY_BUSH_ITEM", "MANA_BERRY", "SOURCE_RELAY_ITEM",
    "BOOKWYRM_SPAWN_EGG",
    # 8 Prebuilt Spell Tomes
    "TOME_PYROMANCER", "TOME_FROSTBINDER", "TOME_SKYWALKER",
    "TOME_WEBWEAVER", "TOME_DEATH_BEAM", "TOME_HEALING_LIGHT",
    "TOME_DASH", "TOME_SINGULARITY",
]

path = "src/main/java/br/com/murilo/liberthia/registry/ModCreativeTabs.java"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()
    lines = content.split("\n")

# Find tab boundaries
main_tab_start = main_tab_end = None
magic_start = magic_end = None
horror_start = horror_end = None

for i, line in enumerate(lines):
    if "MAGIC_ARSENAL = CREATIVE_MODE_TABS" in line:
        magic_start = i
    elif "HORROR_FRAMEWORK = CREATIVE_MODE_TABS" in line:
        horror_start = i
        if magic_start is not None and magic_end is None:
            magic_end = i
    elif "Component.literal(\"Liberthia\")" in line:
        # Main tab marker
        for j in range(i-5, i+5):
            if "CREATIVE_MODE_TABS.register" in lines[j]:
                main_tab_start = j
                break

# Find end of main tab (next "CREATIVE_MODE_TABS.register" after it)
for i in range(main_tab_start or 0, magic_start or len(lines)):
    if i > (main_tab_start or 0) and "CREATIVE_MODE_TABS.register" in lines[i]:
        main_tab_end = i
        break
if main_tab_end is None:
    main_tab_end = magic_start

# End of horror = "private ModCreativeTabs"
for i in range(horror_start or 0, len(lines)):
    if i > (horror_start or 0) and "private ModCreativeTabs()" in lines[i]:
        horror_end = i
        break

print("Main tab: " + str(main_tab_start) + "-" + str(main_tab_end))
print("Magic tab: " + str(magic_start) + "-" + str(magic_end))
print("Horror tab: " + str(horror_start) + "-" + str(horror_end))

# Build regex to match output.accept(ModItems.X.get()); lines
def make_pattern(items):
    return re.compile(r'^\s*(?:output|out)\.accept\(ModItems\.(' + "|".join(items) + r')\.get\(\)\);\s*$')

horror_pat = make_pattern(HORROR)
magic_pat = make_pattern(MAGIC)

# Extract items found in main tab
horror_found = set()
magic_found = set()
new_lines = []

for i, line in enumerate(lines):
    in_main = main_tab_start is not None and main_tab_start <= i <= main_tab_end
    if in_main:
        hm = horror_pat.match(line)
        mm = magic_pat.match(line)
        if hm:
            horror_found.add(hm.group(1))
            continue  # remove from main
        if mm:
            magic_found.add(mm.group(1))
            continue  # remove from main
    new_lines.append(line)

print(f"\nFound {len(horror_found)} horror items to move")
print(f"Found {len(magic_found)} magic items to move")

# Now we need to find where to insert in Magic and Horror tabs
# Insert before the closing })`.build());` of each tab
new_content = "\n".join(new_lines)

# Re-parse to find updated line numbers
lines = new_content.split("\n")
magic_end_line = horror_end_line = None
for i, line in enumerate(lines):
    if "MAGIC_ARSENAL = CREATIVE_MODE_TABS" in line:
        magic_start = i
    elif "HORROR_FRAMEWORK = CREATIVE_MODE_TABS" in line:
        horror_start = i
        magic_end = i
# Find ".build());" line for each tab
def find_build_end(start):
    for j in range(start, len(lines)):
        if "})" in lines[j] and ".build())" in lines[j+1] if j+1 < len(lines) else False:
            return j
        if lines[j].strip() == "})":
            # Check next line for .build())
            if j+1 < len(lines) and ".build())" in lines[j+1]:
                return j
    return None

magic_close = find_build_end(magic_start)
horror_close = find_build_end(horror_start)
print(f"Magic close: {magic_close}, Horror close: {horror_close}")

# Build insertion strings — use sorted lists
horror_insert = "\n".join(
    [f"                        out.accept(ModItems.{n}.get()); // r143 moved from main"
     for n in sorted(horror_found)]
)
magic_insert = "\n".join(
    [f"                        out.accept(ModItems.{n}.get()); // r143 moved from main"
     for n in sorted(magic_found)]
)

# Insert into tabs — horror first (later line) so magic indices stay valid
if horror_close is not None:
    lines.insert(horror_close, horror_insert)
    # adjust magic_close if it's after... actually horror is always after magic so no impact
if magic_close is not None:
    lines.insert(magic_close, magic_insert)

with open(path, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

print(f"\nMoved {len(horror_found)} to Horror, {len(magic_found)} to Magic")
print("Done!")
