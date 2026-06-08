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
        // r180: novos mobs cósmicos
        event.put(ModEntities.O_OBSERVADO.get(), br.com.murilo.liberthia.cosmic.horror.entity.ObservadoEntity.createAttributes().build());
        event.put(ModEntities.SILENCE_SHEPHERD.get(), br.com.murilo.liberthia.cosmic.horror.entity.SilenceShepherdEntity.createAttributes().build());
        event.put(ModEntities.END_ARCHIVIST.get(), br.com.murilo.liberthia.cosmic.horror.entity.EndArchivistEntity.createAttributes().build());
        event.put(ModEntities.BLIND_AMALGAM.get(), br.com.murilo.liberthia.cosmic.horror.entity.BlindAmalgamEntity.createAttributes().build());
        event.put(ModEntities.PARASITIC_EYE.get(), br.com.murilo.liberthia.cosmic.horror.entity.ParasiticEyeEntity.createAttributes().build());
        event.put(ModEntities.CINDER_PARASITE.get(), br.com.murilo.liberthia.cosmic.horror.entity.CinderParasiteEntity.createAttributes().build());
        event.put(ModEntities.DIMENSIONAL_WORM.get(), br.com.murilo.liberthia.cosmic.horror.entity.DimensionalWormEntity.createAttributes().build());
        event.put(ModEntities.GAZE_LEECH.get(), br.com.murilo.liberthia.cosmic.horror.entity.GazeLeechEntity.createAttributes().build());
        event.put(ModEntities.BLIND_WEAVER.get(), br.com.murilo.liberthia.cosmic.horror.entity.BlindWeaverEntity.createAttributes().build());
        event.put(ModEntities.MAW_CRAWLER.get(), br.com.murilo.liberthia.cosmic.horror.entity.MawCrawlerEntity.createAttributes().build());
        event.put(ModEntities.WHISPER_MITE.get(), br.com.murilo.liberthia.cosmic.horror.entity.WhisperMiteEntity.createAttributes().build());
        event.put(ModEntities.DREAD_ORB.get(), br.com.murilo.liberthia.cosmic.horror.entity.DreadOrbEntity.createAttributes().build());
        event.put(ModEntities.FLESH_WATCHER.get(), br.com.murilo.liberthia.cosmic.horror.entity.FleshWatcherEntity.createAttributes().build());
        event.put(ModEntities.VOID_TICK.get(), br.com.murilo.liberthia.cosmic.horror.entity.VoidTickEntity.createAttributes().build());
        event.put(ModEntities.GLOOM_MOTH.get(), br.com.murilo.liberthia.cosmic.horror.entity.GloomMothEntity.createAttributes().build());
        event.put(ModEntities.ROT_EYE.get(), br.com.murilo.liberthia.cosmic.horror.entity.RotEyeEntity.createAttributes().build());
        event.put(ModEntities.SCREAM_LARVA.get(), br.com.murilo.liberthia.cosmic.horror.entity.ScreamLarvaEntity.createAttributes().build());
        event.put(ModEntities.MIRROR_SPAWN.get(), br.com.murilo.liberthia.cosmic.horror.entity.MirrorSpawnEntity.createAttributes().build());
        event.put(ModEntities.PARASITE_HOST.get(), br.com.murilo.liberthia.cosmic.horror.entity.ParasiteHostEntity.createAttributes().build());
        event.put(ModEntities.COLOSSAL_EYE.get(), br.com.murilo.liberthia.cosmic.horror.entity.ColossalEyeEntity.createAttributes().build());
        event.put(ModEntities.BLIND_ASTRONOMER.get(), br.com.murilo.liberthia.entity.BlindAstronomerBossEntity.createAttributes().build());
        event.put(ModEntities.HIVE_QUEEN.get(), br.com.murilo.liberthia.entity.HiveQueenBossEntity.createAttributes().build());
        event.put(ModEntities.COBAIA.get(), br.com.murilo.liberthia.entity.CobaiaEntity.createAttributes().build());
        event.put(ModEntities.CORRUPTED_ZOMBIE.get(), CorruptedZombieEntity.createAttributes().build());
        event.put(ModEntities.SPORE_SPITTER.get(), SporeSpitterEntity.createAttributes().build());
        event.put(ModEntities.CLONE_PLAYER.get(), br.com.murilo.liberthia.entity.ClonePlayerEntity.createAttributes().build());
        // r24: SoulBody usa as mesmas attributes do ClonePlayer (extends)
        event.put(ModEntities.SOUL_BODY.get(), br.com.murilo.liberthia.entity.ClonePlayerEntity.createAttributes().build());
        event.put(ModEntities.DARK_CONSCIOUSNESS.get(), br.com.murilo.liberthia.entity.DarkConsciousnessEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_WORM.get(), br.com.murilo.liberthia.entity.BloodWormEntity.createAttributes().build());
        // r120: Void Larva attributes
        event.put(ModEntities.VOID_LARVA.get(),
                br.com.murilo.liberthia.magic.spell.voidspell.VoidLarvaEntity.createAttributes().build());
        // r135: Drygmy familiar
        event.put(ModEntities.DRYGMY.get(),
                br.com.murilo.liberthia.magic.familiar.DrygmyEntity.createAttributes().build());
        event.put(ModEntities.FLESH_CRAWLER.get(), br.com.murilo.liberthia.entity.FleshCrawlerEntity.createAttributes().build());
        event.put(ModEntities.GORE_WORM.get(), br.com.murilo.liberthia.entity.GoreWormEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_CULTIST.get(), br.com.murilo.liberthia.entity.BloodCultistEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_PRIEST.get(), br.com.murilo.liberthia.entity.BloodPriestEntity.createAttributes().build());
        event.put(ModEntities.WOUNDED_PILGRIM.get(), br.com.murilo.liberthia.entity.WoundedPilgrimEntity.createAttributes().build());
        event.put(ModEntities.FLESH_MOTHER_BOSS.get(), br.com.murilo.liberthia.entity.FleshMotherBossEntity.createAttributes().build());
        event.put(ModEntities.ORDER_PALADIN.get(), br.com.murilo.liberthia.entity.OrderPaladinEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_MAGE.get(), br.com.murilo.liberthia.entity.BloodMageEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_HOUND.get(), br.com.murilo.liberthia.entity.BloodHoundEntity.createAttributes().build());
        event.put(ModEntities.POSSESSED_ZOMBIE.get(), br.com.murilo.liberthia.entity.PossessedZombieEntity.createAttributes().build());
        event.put(ModEntities.POSSESSED_SKELETON.get(), br.com.murilo.liberthia.entity.PossessedSkeletonEntity.createAttributes().build());
        event.put(ModEntities.BLOOD_WARDEN.get(), br.com.murilo.liberthia.entity.BloodWardenBossEntity.createAttributes().build());
        event.put(ModEntities.WEAVING_SHADE.get(), br.com.murilo.liberthia.entity.WeavingShadeEntity.createAttributes().build());
        event.put(ModEntities.DISARMER.get(), br.com.murilo.liberthia.entity.DisarmerEntity.createAttributes().build());
        // r33: Loom dimension entities
        event.put(ModEntities.LOOM_WATCHER.get(),
                br.com.murilo.liberthia.loom.entity.WatcherStalkerEntity.createAttributes().build());
        event.put(ModEntities.LOOM_PERIPHERAL.get(),
                br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity.createAttributes().build());
        event.put(ModEntities.LOOM_WINDOW_WATCHER.get(),
                br.com.murilo.liberthia.loom.entity.WindowWatcherEntity.createAttributes().build());
        event.put(ModEntities.LOOM_SCREAMER.get(),
                br.com.murilo.liberthia.loom.entity.ScreamerTeleporterEntity.createAttributes().build());
        event.put(ModEntities.LOOM_WORM.get(),
                br.com.murilo.liberthia.loom.entity.DimensionalWormEntity.createAttributes().build());
        // r48: Reflection Entity
        event.put(ModEntities.REFLECTION_ENTITY.get(),
                br.com.murilo.liberthia.cosmic.observatory.ReflectionEntity.createAttributes().build());
        // r74: Bookwyrm Familiar
        event.put(ModEntities.BOOKWYRM.get(),
                br.com.murilo.liberthia.observation.entity.BookwyrmEntity.createAttributes().build());
        // r81: Horror Framework entities
        event.put(ModEntities.EMPTY_MAN.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity.createAttributes().build());
        event.put(ModEntities.OBSERVER.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity.createAttributes().build());
        event.put(ModEntities.ABSENCE.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity.createAttributes().build());
        event.put(ModEntities.REMEMBERED.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity.createAttributes().build());
        event.put(ModEntities.VISITANTE.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.VisitanteEntity.createAttributes().build());
        event.put(ModEntities.MULHER_HORIZONTE.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.MulherDoHorizonteEntity.createAttributes().build());
        event.put(ModEntities.IDOL.get(),
                br.com.murilo.liberthia.cosmic.idol.IdolEntity.createAttributes().build());
        event.put(ModEntities.FACELESS.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.FacelessEntity.createAttributes().build());
        event.put(ModEntities.CEILING_LURKER.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.CeilingLurkerEntity.createAttributes().build());
        event.put(ModEntities.EYE_COLLECTOR.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.EyeCollectorEntity.createAttributes().build());
        event.put(ModEntities.NEIGHBOR.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.NeighborEntity.createAttributes().build());
        event.put(ModEntities.CACADOR.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.CacadorEntity.createAttributes().build());
        event.put(ModEntities.ESPREITADOR.get(),
                br.com.murilo.liberthia.cosmic.horror.entity.EspreitadorEntity.createAttributes().build());
        // r150: 8 Wooden Horror variants — todos compartilham createAttributes()
        var woodenAttrs = br.com.murilo.liberthia.cosmic.horror.entity.WoodenHorrorEntity.createAttributes();
        event.put(ModEntities.WOODEN_CHARCOAL.get(), woodenAttrs.build());
        event.put(ModEntities.WOODEN_PALE_OAK.get(), woodenAttrs.build());
        event.put(ModEntities.WOODEN_ROTTED_BIRCH.get(), woodenAttrs.build());
        event.put(ModEntities.WOODEN_BLEEDING_MAPLE.get(), woodenAttrs.build());
        event.put(ModEntities.WOODEN_MOSSY.get(), woodenAttrs.build());
        event.put(ModEntities.WOODEN_FROZEN_PINE.get(), woodenAttrs.build());
        event.put(ModEntities.WOODEN_BURNING_ACACIA.get(), woodenAttrs.build());
        event.put(ModEntities.WOODEN_CURSED_MAHOGANY.get(), woodenAttrs.build());
        // r87: Wizard mobs
        event.put(ModEntities.PYROMANCER.get(),
                br.com.murilo.liberthia.magic.wizard.AbstractWizardEntity.createWizardAttributes().build());
        event.put(ModEntities.CRYOMANCER.get(),
                br.com.murilo.liberthia.magic.wizard.AbstractWizardEntity.createWizardAttributes().build());
        event.put(ModEntities.ELECTROMANCER.get(),
                br.com.murilo.liberthia.magic.wizard.AbstractWizardEntity.createWizardAttributes().build());
        event.put(ModEntities.NECROMANCER.get(),
                br.com.murilo.liberthia.magic.wizard.AbstractWizardEntity.createWizardAttributes().build());
        event.put(ModEntities.ELDRITCH_CULTIST.get(),
                br.com.murilo.liberthia.magic.wizard.AbstractWizardEntity.createWizardAttributes().build());
        // r95: Familiars
        event.put(ModEntities.WISP_PICKER.get(),
                br.com.murilo.liberthia.magic.familiar.WispPickerEntity.createAttributes().build());
        event.put(ModEntities.GROVE_SPRITE.get(),
                br.com.murilo.liberthia.magic.familiar.GroveSpriteEntity.createAttributes().build());
        event.put(ModEntities.SOUL_REAPER.get(),
                br.com.murilo.liberthia.magic.familiar.SoulReaperEntity.createAttributes().build());
        // r106: Wizards
        event.put(ModEntities.APOTHECARIST.get(),
                br.com.murilo.liberthia.magic.wizard.AbstractWizardEntity.createWizardAttributes().build());
        event.put(ModEntities.KEEPER.get(),
                br.com.murilo.liberthia.magic.wizard.AbstractWizardEntity.createWizardAttributes().build());
        event.put(ModEntities.ARCHEVOKER.get(),
                br.com.murilo.liberthia.magic.wizard.ArchevokerEntity.createArchevokerAttributes().build());
        // r178 HOTFIX: Lich boss + adds — tinham createAttributes() mas NÃO estavam
        // registrados aqui → NPE (AttributeSupplier null) ao spawnar via ovo = crash.
        event.put(ModEntities.ABYSSAL_LICH.get(),
                br.com.murilo.liberthia.magic.boss.AbyssalLichEntity.createAttributes().build());
        event.put(ModEntities.LICH_STALKER.get(),
                br.com.murilo.liberthia.magic.boss.LichStalkerEntity.createAttributes().build());
        event.put(ModEntities.LICH_HUNTER.get(),
                br.com.murilo.liberthia.magic.boss.LichHunterEntity.createAttributes().build());
        // r178 HOTFIX: Frozen Humanoid (Ice Tomb spell) — Monster sem atributos → mesmo crash.
        event.put(ModEntities.FROZEN_HUMANOID.get(),
                br.com.murilo.liberthia.magic.spells.FrozenHumanoidEntity.createAttributes().build());
        // r109: Familiars
        event.put(ModEntities.WHELP.get(),
                br.com.murilo.liberthia.magic.familiar.WhelpEntity.createAttributes().build());
        event.put(ModEntities.CARBUNCLE.get(),
                br.com.murilo.liberthia.magic.familiar.CarbuncleEntity.createAttributes().build());
        event.put(ModEntities.AMETHYST_GOLEM.get(),
                br.com.murilo.liberthia.magic.familiar.AmethystGolemEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        // r34: LOOM entities — spawn em ON_GROUND com rules sem light-level (Loom é dark)
        event.register(ModEntities.LOOM_WATCHER.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getDifficulty()
                        != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.LOOM_PERIPHERAL.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getDifficulty()
                        != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.LOOM_SCREAMER.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getDifficulty()
                        != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.BLOOD_CULTIST.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.BLOOD_PRIEST.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.WOUNDED_PILGRIM.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.BLOOD_MAGE.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.BLOOD_HOUND.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);

        // ════════════════════════════════════════════════════════════════════
        // r68: SPIRIT WORLD entities — placements pra biome spawner funcionar
        // Sem isso, o mob aparece em ModEntities mas a biome NUNCA spawna eles.
        // ════════════════════════════════════════════════════════════════════
        event.register(ModEntities.WEAVING_SHADE.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                // Spirit world não tem luz solar normal — sem checagem de light level
                (type, level, spawnType, pos, rand) -> level.getDifficulty()
                        != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.DARK_CONSCIOUSNESS.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getDifficulty()
                        != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.DISARMER.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);

        // r81: Horror Framework entities — spawn placements (mostly summon-only)
        event.register(ModEntities.EMPTY_MAN.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.OBSERVER.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.ABSENCE.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.REMEMBERED.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);

        // r87: Wizard spawn placements
        event.register(ModEntities.PYROMANCER.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.CRYOMANCER.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.ELECTROMANCER.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.NECROMANCER.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ModEntities.ELDRITCH_CULTIST.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL,
                SpawnPlacementRegisterEvent.Operation.AND);
    }
}
