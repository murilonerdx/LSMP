import json, os
D = "src/main/resources/data/liberthia/recipes"
os.makedirs(D, exist_ok=True)
def sl(out, items, count=1):
    json.dump({"type":"minecraft:crafting_shapeless",
               "ingredients":[({"item":i} if isinstance(i,str) else i) for i in items],
               "result":{"item":f"liberthia:{out}","count":count}},
              open(f"{D}/{out}.json","w"))
# #74 — itens que estavam sem craft
sl("lighter", ["minecraft:flint","minecraft:iron_ingot","minecraft:blaze_powder"])
sl("source_gem", ["minecraft:amethyst_shard","minecraft:diamond","minecraft:ender_pearl"])
sl("dark_matter_laser", ["liberthia:dark_matter_ingot","minecraft:redstone_block","minecraft:glass","minecraft:iron_ingot"])
sl("white_matter_syringe", ["minecraft:glass_bottle","minecraft:iron_ingot","liberthia:clear_matter_block"])
sl("dark_matter_syringe", ["minecraft:glass_bottle","minecraft:iron_ingot","liberthia:dark_matter_block"])
sl("yellow_matter_syringe", ["minecraft:glass_bottle","minecraft:iron_ingot","liberthia:yellow_matter_block"])
sl("matter_tester", ["minecraft:iron_ingot","minecraft:redstone","minecraft:glass","minecraft:comparator"])
sl("matter_sample", ["minecraft:glass_bottle","minecraft:paper"])
sl("cobaia_analyzer", ["minecraft:iron_ingot","minecraft:redstone","minecraft:glass","minecraft:ender_pearl"])
sl("codex_cosmic", ["minecraft:book","minecraft:ender_eye"])
sl("codex_magic", ["minecraft:book","minecraft:amethyst_shard"])
sl("codex_matter", ["minecraft:book","liberthia:dark_matter_shard"])
sl("candle_occult_black", ["minecraft:black_candle","minecraft:gunpowder"])
sl("candle_occult_golden", ["minecraft:yellow_candle","minecraft:glowstone_dust"])
sl("candle_occult_purple", ["minecraft:purple_candle","minecraft:amethyst_shard"])
sl("candle_occult_red", ["minecraft:red_candle","minecraft:redstone"])
sl("candle_occult_white", ["minecraft:white_candle","minecraft:bone_meal"])
print("recipes #74 gerados (17)")
