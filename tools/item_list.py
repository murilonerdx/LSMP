"""
Lista de items vanilla + Liberthia pra autocompletar no editor de receitas.
Subset dos mais comuns. User pode digitar qualquer ID custom.
"""

VANILLA_ITEMS = [
    # Building blocks
    "minecraft:stone", "minecraft:cobblestone", "minecraft:dirt", "minecraft:grass_block",
    "minecraft:sand", "minecraft:gravel", "minecraft:bedrock", "minecraft:obsidian",
    "minecraft:netherrack", "minecraft:end_stone", "minecraft:deepslate", "minecraft:tuff",
    "minecraft:granite", "minecraft:diorite", "minecraft:andesite", "minecraft:basalt",
    "minecraft:smooth_stone", "minecraft:bricks", "minecraft:nether_bricks",
    "minecraft:quartz_block", "minecraft:purpur_block", "minecraft:prismarine",
    "minecraft:terracotta", "minecraft:glass", "minecraft:ice", "minecraft:packed_ice",

    # Logs/Wood
    "minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log",
    "minecraft:jungle_log", "minecraft:acacia_log", "minecraft:dark_oak_log",
    "minecraft:cherry_log", "minecraft:mangrove_log",
    "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:stick",

    # Ores & ingots
    "minecraft:coal", "minecraft:charcoal", "minecraft:coal_block",
    "minecraft:iron_ore", "minecraft:iron_ingot", "minecraft:iron_block", "minecraft:iron_nugget",
    "minecraft:gold_ore", "minecraft:gold_ingot", "minecraft:gold_block", "minecraft:gold_nugget",
    "minecraft:copper_ore", "minecraft:copper_ingot", "minecraft:copper_block",
    "minecraft:diamond_ore", "minecraft:diamond", "minecraft:diamond_block",
    "minecraft:emerald_ore", "minecraft:emerald", "minecraft:emerald_block",
    "minecraft:lapis_ore", "minecraft:lapis_lazuli", "minecraft:lapis_block",
    "minecraft:redstone_ore", "minecraft:redstone", "minecraft:redstone_block",
    "minecraft:netherite_ingot", "minecraft:netherite_scrap", "minecraft:netherite_block",
    "minecraft:ancient_debris", "minecraft:nether_quartz_ore", "minecraft:quartz",
    "minecraft:amethyst_shard", "minecraft:amethyst_block", "minecraft:budding_amethyst",
    "minecraft:raw_iron", "minecraft:raw_gold", "minecraft:raw_copper",

    # Special items
    "minecraft:nether_star", "minecraft:dragon_egg", "minecraft:dragon_breath",
    "minecraft:elytra", "minecraft:totem_of_undying", "minecraft:enchanted_book",
    "minecraft:experience_bottle", "minecraft:end_crystal", "minecraft:beacon",
    "minecraft:conduit", "minecraft:heart_of_the_sea", "minecraft:nautilus_shell",
    "minecraft:trident", "minecraft:turtle_helmet", "minecraft:scute",
    "minecraft:phantom_membrane", "minecraft:ghast_tear", "minecraft:blaze_powder",
    "minecraft:blaze_rod", "minecraft:magma_cream", "minecraft:slime_ball",
    "minecraft:ender_pearl", "minecraft:ender_eye", "minecraft:popped_chorus_fruit",
    "minecraft:chorus_fruit", "minecraft:wither_skeleton_skull", "minecraft:skeleton_skull",

    # Tools/Weapons
    "minecraft:diamond_sword", "minecraft:iron_sword", "minecraft:netherite_sword",
    "minecraft:diamond_pickaxe", "minecraft:iron_pickaxe", "minecraft:netherite_pickaxe",
    "minecraft:diamond_axe", "minecraft:iron_axe", "minecraft:netherite_axe",
    "minecraft:bow", "minecraft:crossbow", "minecraft:shield",
    "minecraft:fishing_rod", "minecraft:flint_and_steel", "minecraft:shears",

    # Food
    "minecraft:wheat", "minecraft:bread", "minecraft:apple", "minecraft:golden_apple",
    "minecraft:enchanted_golden_apple", "minecraft:carrot", "minecraft:potato",
    "minecraft:beetroot", "minecraft:sugar_cane", "minecraft:cocoa_beans",
    "minecraft:beef", "minecraft:cooked_beef", "minecraft:porkchop", "minecraft:cooked_porkchop",
    "minecraft:chicken", "minecraft:cooked_chicken", "minecraft:mutton", "minecraft:cooked_mutton",
    "minecraft:salmon", "minecraft:cod", "minecraft:tropical_fish", "minecraft:pufferfish",
    "minecraft:cake", "minecraft:cookie", "minecraft:pumpkin_pie",
    "minecraft:honey_bottle", "minecraft:honeycomb", "minecraft:milk_bucket",

    # Misc
    "minecraft:bucket", "minecraft:water_bucket", "minecraft:lava_bucket", "minecraft:powder_snow_bucket",
    "minecraft:flint", "minecraft:gunpowder", "minecraft:bone", "minecraft:bone_meal",
    "minecraft:string", "minecraft:feather", "minecraft:leather", "minecraft:rabbit_hide",
    "minecraft:paper", "minecraft:book", "minecraft:writable_book", "minecraft:written_book",
    "minecraft:ink_sac", "minecraft:glow_ink_sac", "minecraft:spider_eye", "minecraft:fermented_spider_eye",
    "minecraft:rotten_flesh", "minecraft:wither_rose", "minecraft:torch", "minecraft:soul_torch",
    "minecraft:lantern", "minecraft:soul_lantern", "minecraft:campfire", "minecraft:soul_campfire",
    "minecraft:tnt", "minecraft:trapped_chest", "minecraft:chest", "minecraft:ender_chest",
    "minecraft:hopper", "minecraft:dropper", "minecraft:dispenser", "minecraft:observer",
    "minecraft:piston", "minecraft:sticky_piston", "minecraft:lever", "minecraft:button",
    "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:cartography_table",
    "minecraft:smithing_table", "minecraft:fletching_table", "minecraft:loom",
    "minecraft:grindstone", "minecraft:stonecutter", "minecraft:lectern", "minecraft:bell",
    "minecraft:anvil", "minecraft:enchanting_table", "minecraft:brewing_stand",
    "minecraft:cauldron", "minecraft:composter", "minecraft:barrel",

    # Potions / Misc consumables
    "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion",
    "minecraft:glass_bottle", "minecraft:arrow", "minecraft:tipped_arrow", "minecraft:spectral_arrow",
    "minecraft:firework_rocket", "minecraft:fire_charge",

    # Wool
    "minecraft:white_wool", "minecraft:black_wool", "minecraft:red_wool",
    "minecraft:blue_wool", "minecraft:green_wool", "minecraft:yellow_wool",
]

LIBERTHIA_ITEMS = [
    # Matter
    "liberthia:dark_matter_block", "liberthia:dark_matter_ore", "liberthia:dark_matter_shard",
    "liberthia:dark_matter_bucket", "liberthia:inactive_dark_matter", "liberthia:active_dark_matter",
    "liberthia:stabilized_dark_matter", "liberthia:dark_matter_catalyst", "liberthia:dark_matter_cell",
    "liberthia:unstable_matter",
    "liberthia:clear_matter_block", "liberthia:white_matter_ore", "liberthia:clear_matter_bucket",
    "liberthia:yellow_matter_block", "liberthia:yellow_matter_ingot", "liberthia:yellow_matter_bucket",
    "liberthia:matter_core", "liberthia:matter_ampoule",

    # Upgrades
    "liberthia:speed_upgrade", "liberthia:efficiency_upgrade", "liberthia:capacity_upgrade",

    # Tools
    "liberthia:liberthia_wrench", "liberthia:energy_meter", "liberthia:dimensional_compass",
    "liberthia:sample_vial", "liberthia:containment_glove",

    # Lore
    "liberthia:eye_of_horus", "liberthia:horus_eye_shard", "liberthia:equilibrium_crystal",
    "liberthia:equilibrium_fragment", "liberthia:singularity_core", "liberthia:void_crystal",
    "liberthia:burning_gem", "liberthia:protection_ruby",

    # Holy
    "liberthia:holy_essence", "liberthia:purified_essence", "liberthia:holy_blade",

    # Blood
    "liberthia:sanguine_core", "liberthia:sanguine_essence", "liberthia:living_flesh",
    "liberthia:heart_of_flesh", "liberthia:blood_bucket",
]

ALL_ITEMS = VANILLA_ITEMS + LIBERTHIA_ITEMS
