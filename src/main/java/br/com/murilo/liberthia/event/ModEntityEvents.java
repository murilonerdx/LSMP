package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entity.CorruptedZombieEntity;
import br.com.murilo.liberthia.entity.SporeSpitterEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CORRUPTED_ZOMBIE.get(), CorruptedZombieEntity.createAttributes().build());
        event.put(ModEntities.SPORE_SPITTER.get(), SporeSpitterEntity.createAttributes().build());
        event.put(ModEntities.CLONE_PLAYER.get(), br.com.murilo.liberthia.entity.ClonePlayerEntity.createAttributes().build());
        event.put(ModEntities.DARK_CONSCIOUSNESS.get(), br.com.murilo.liberthia.entity.DarkConsciousnessEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_WORM.get(), br.com.murilo.liberthia.entity.BloodWormEntity.createAttributes().build());
        event.put(ModEntities.FLESH_CRAWLER.get(), br.com.murilo.liberthia.entity.FleshCrawlerEntity.createAttributes().build());
        event.put(ModEntities.GORE_WORM.get(), br.com.murilo.liberthia.entity.GoreWormEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_CULTIST.get(), br.com.murilo.liberthia.entity.BloodCultistEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_PRIEST.get(), br.com.murilo.liberthia.entity.BloodPriestEntity.createAttributes().build());
        event.put(ModEntities.WOUNDED_PILGRIM.get(), br.com.murilo.liberthia.entity.WoundedPilgrimEntity.createAttributes().build());
        event.put(ModEntities.FLESH_MOTHER_BOSS.get(), br.com.murilo.liberthia.entity.FleshMotherBossEntity.createAttributes().build());
        event.put(ModEntities.ORDER_PALADIN.get(), br.com.murilo.liberthia.entity.OrderPaladinEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.BLOOD_CULTIST.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.BLOOD_PRIEST.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.WOUNDED_PILGRIM.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
    }
}
