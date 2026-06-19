package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.function.Supplier;

/**
 * r195 — injeta as Relíquias de Astaron (loot-only, SEM craft) em baús de fim de jogo, com chance baixa.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AstaronRelicLootHandler {
    private AstaronRelicLootHandler() {}

    private static final Set<ResourceLocation> TARGETS = Set.of(
            new ResourceLocation("minecraft", "chests/ancient_city"),
            new ResourceLocation("minecraft", "chests/end_city_treasure"),
            new ResourceLocation("minecraft", "chests/stronghold_library"),
            new ResourceLocation("minecraft", "chests/bastion_treasure"),
            new ResourceLocation("minecraft", "chests/woodland_mansion"),
            new ResourceLocation("minecraft", "chests/simple_dungeon"),
            new ResourceLocation("minecraft", "chests/buried_treasure"));

    @SuppressWarnings("unchecked")
    private static final Supplier<Item>[] RELICS = new Supplier[]{
            ModItems.ASTARON_EYE_RELIC, ModItems.ASTARON_MIND_AMULET, ModItems.ASTARON_HUNTER_GAUNTLET,
            ModItems.ASTARON_WARDEN_SASH, ModItems.ASTARON_VOID_TREADS, ModItems.ASTARON_MIRROR_RING,
            ModItems.ASTARON_FACELESS_CROWN, ModItems.ASTARON_ESSENCE_RELIC, ModItems.ASTARON_VOID_CORE,
            ModItems.ASTARON_STAR_PENDANT };

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent e) {
        if (!TARGETS.contains(e.getName())) return;
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(0.06F));
        for (Supplier<Item> r : RELICS) pool.add(LootItem.lootTableItem(r.get()).setWeight(1));
        e.getTable().addPool(pool.build());
    }
}
