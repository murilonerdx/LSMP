package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entity.BlackHoleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LiberthiaMod.MODID);

    public static final RegistryObject<EntityType<BlackHoleEntity>> BLACK_HOLE = 
            ENTITY_TYPES.register("black_hole",
                    () -> EntityType.Builder.<BlackHoleEntity>of(BlackHoleEntity::new, MobCategory.MISC)
                            .sized(2.0F, 2.0F)
                            .clientTrackingRange(10)
                            .build("black_hole"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.DarkMatterSporeEntity>> DARK_MATTER_SPORE = 
            ENTITY_TYPES.register("dark_matter_spore",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.DarkMatterSporeEntity>of(br.com.murilo.liberthia.entity.DarkMatterSporeEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(20)
                            .updateInterval(2)
                            .build("dark_matter_spore"));

    // REMOVIDO v0.1.13: CLEANSING_GRENADE entity

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.CorruptedZombieEntity>> CORRUPTED_ZOMBIE =
            ENTITY_TYPES.register("corrupted_zombie",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.CorruptedZombieEntity>of(br.com.murilo.liberthia.entity.CorruptedZombieEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .build("corrupted_zombie"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.SporeSpitterEntity>> SPORE_SPITTER =
            ENTITY_TYPES.register("spore_spitter",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.SporeSpitterEntity>of(br.com.murilo.liberthia.entity.SporeSpitterEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 0.5F)
                            .clientTrackingRange(8)
                            .build("spore_spitter"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.ClonePlayerEntity>> CLONE_PLAYER =
            ENTITY_TYPES.register("clone_player",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.ClonePlayerEntity>of(br.com.murilo.liberthia.entity.ClonePlayerEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(16)
                            .build("clone_player"));

    // v0.1.22 r24: SoulBody — corpo físico do player enquanto consciência
    // está no spirit world. Renderizado pelo ClonePlayerRenderer.
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.SoulBodyEntity>> SOUL_BODY =
            ENTITY_TYPES.register("soul_body",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.SoulBodyEntity>of(br.com.murilo.liberthia.entity.SoulBodyEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(32)
                            .build("soul_body"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.WhiteMatterExplosionEntity>> WHITE_MATTER_EXPLOSION =
            ENTITY_TYPES.register("white_matter_explosion",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.WhiteMatterExplosionEntity>of(br.com.murilo.liberthia.entity.WhiteMatterExplosionEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(10)
                            .build("white_matter_explosion"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.DarkConsciousnessEntity>> DARK_CONSCIOUSNESS =
            ENTITY_TYPES.register("dark_consciousness",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.DarkConsciousnessEntity>of(br.com.murilo.liberthia.entity.DarkConsciousnessEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 2.9F)
                            .clientTrackingRange(16)
                            .build("dark_consciousness"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.EyeOfHorusEntity>> EYE_OF_HORUS =
            ENTITY_TYPES.register("eye_of_horus",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.EyeOfHorusEntity>of(br.com.murilo.liberthia.entity.EyeOfHorusEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(24)
                            .build("eye_of_horus"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.BloodWormEntity>> BLOOD_WORM =
            ENTITY_TYPES.register("blood_worm",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.BloodWormEntity>of(br.com.murilo.liberthia.entity.BloodWormEntity::new, MobCategory.MONSTER)
                            .sized(0.5F, 0.3F)
                            .clientTrackingRange(8)
                            .build("blood_worm"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.BloodOrbEntity>> BLOOD_ORB =
            ENTITY_TYPES.register("blood_orb",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.BloodOrbEntity>of(br.com.murilo.liberthia.entity.BloodOrbEntity::new, MobCategory.MISC)
                            .sized(0.6F, 0.6F)
                            .clientTrackingRange(16)
                            .updateInterval(3)
                            .build("blood_orb"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.FleshCrawlerEntity>> FLESH_CRAWLER =
            ENTITY_TYPES.register("flesh_crawler",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.FleshCrawlerEntity>of(br.com.murilo.liberthia.entity.FleshCrawlerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 0.35F)
                            .clientTrackingRange(8)
                            .build("flesh_crawler"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.GoreWormEntity>> GORE_WORM =
            ENTITY_TYPES.register("gore_worm",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.GoreWormEntity>of(br.com.murilo.liberthia.entity.GoreWormEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 0.5F)
                            .clientTrackingRange(10)
                            .build("gore_worm"));

    // --- Culto do Sangue (Fase 1) ---
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.BloodCultistEntity>> BLOOD_CULTIST =
            ENTITY_TYPES.register("blood_cultist",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.BloodCultistEntity>of(
                                    br.com.murilo.liberthia.entity.BloodCultistEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build("blood_cultist"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.BloodPriestEntity>> BLOOD_PRIEST =
            ENTITY_TYPES.register("blood_priest",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.BloodPriestEntity>of(
                                    br.com.murilo.liberthia.entity.BloodPriestEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 2.0F)
                            .clientTrackingRange(12)
                            .build("blood_priest"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.WoundedPilgrimEntity>> WOUNDED_PILGRIM =
            ENTITY_TYPES.register("wounded_pilgrim",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.WoundedPilgrimEntity>of(
                                    br.com.murilo.liberthia.entity.WoundedPilgrimEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build("wounded_pilgrim"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.FleshMotherBossEntity>> FLESH_MOTHER_BOSS =
            ENTITY_TYPES.register("flesh_mother_boss",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.FleshMotherBossEntity>of(
                                    br.com.murilo.liberthia.entity.FleshMotherBossEntity::new, MobCategory.MONSTER)
                            .sized(2.4F, 3.2F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("flesh_mother_boss"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.projectile.BleedingArrowEntity>> BLEEDING_ARROW =
            ENTITY_TYPES.register("bleeding_arrow",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.projectile.BleedingArrowEntity>of(
                                    br.com.murilo.liberthia.entity.projectile.BleedingArrowEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(20)
                            .build("bleeding_arrow"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.OrderPaladinEntity>> ORDER_PALADIN =
            ENTITY_TYPES.register("order_paladin",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.OrderPaladinEntity>of(
                                    br.com.murilo.liberthia.entity.OrderPaladinEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 2.0F)
                            .clientTrackingRange(12)
                            .build("order_paladin"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.projectile.HemoBoltEntity>> HEMO_BOLT =
            ENTITY_TYPES.register("hemo_bolt",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.projectile.HemoBoltEntity>of(
                                    br.com.murilo.liberthia.entity.projectile.HemoBoltEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(8)
                            .updateInterval(4)
                            .build("hemo_bolt"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.BloodMageEntity>> BLOOD_MAGE =
            ENTITY_TYPES.register("blood_mage",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.BloodMageEntity>of(
                                    br.com.murilo.liberthia.entity.BloodMageEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 2.0F)
                            .clientTrackingRange(12)
                            .build("blood_mage"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.projectile.BloodPearlEntity>> BLOOD_PEARL =
            ENTITY_TYPES.register("blood_pearl",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.projectile.BloodPearlEntity>of(
                                    br.com.murilo.liberthia.entity.projectile.BloodPearlEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build("blood_pearl"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.projectile.VeilingOrbEntity>> VEILING_ORB =
            ENTITY_TYPES.register("veiling_orb",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.projectile.VeilingOrbEntity>of(
                                    br.com.murilo.liberthia.entity.projectile.VeilingOrbEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build("veiling_orb"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.projectile.MindSplinterDartEntity>> MIND_SPLINTER_DART =
            ENTITY_TYPES.register("mind_splinter_dart",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.projectile.MindSplinterDartEntity>of(
                                    br.com.murilo.liberthia.entity.projectile.MindSplinterDartEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build("mind_splinter_dart"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.BloodHoundEntity>> BLOOD_HOUND =
            ENTITY_TYPES.register("blood_hound",
                    () -> EntityType.Builder.of(
                                    br.com.murilo.liberthia.entity.BloodHoundEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 0.85F)
                            .clientTrackingRange(10)
                            .build("blood_hound"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.projectile.LightningGrenadeEntity>> LIGHTNING_GRENADE =
            ENTITY_TYPES.register("lightning_grenade",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.projectile.LightningGrenadeEntity>of(
                                    br.com.murilo.liberthia.entity.projectile.LightningGrenadeEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(10)
                            .build("lightning_grenade"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.projectile.BurningGemEntity>> BURNING_GEM =
            ENTITY_TYPES.register("burning_gem",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.projectile.BurningGemEntity>of(
                                    br.com.murilo.liberthia.entity.projectile.BurningGemEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(10)
                            .build("burning_gem"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.projectile.FrostFlaskEntity>> FROST_FLASK =
            ENTITY_TYPES.register("frost_flask",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.projectile.FrostFlaskEntity>of(
                                    br.com.murilo.liberthia.entity.projectile.FrostFlaskEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(10)
                            .build("frost_flask"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.projectile.PurifyingFlaskEntity>> PURIFYING_FLASK =
            ENTITY_TYPES.register("purifying_flask",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.projectile.PurifyingFlaskEntity>of(
                                    br.com.murilo.liberthia.entity.projectile.PurifyingFlaskEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(10)
                            .build("purifying_flask"));

    // --- Possessed mobs (Occultism-style) ---
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.PossessedZombieEntity>> POSSESSED_ZOMBIE =
            ENTITY_TYPES.register("possessed_zombie",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.PossessedZombieEntity>of(
                                    br.com.murilo.liberthia.entity.PossessedZombieEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).clientTrackingRange(8)
                            .build("possessed_zombie"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.PossessedSkeletonEntity>> POSSESSED_SKELETON =
            ENTITY_TYPES.register("possessed_skeleton",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.PossessedSkeletonEntity>of(
                                    br.com.murilo.liberthia.entity.PossessedSkeletonEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.99F).clientTrackingRange(8)
                            .build("possessed_skeleton"));

    // --- New blood-themed monsters ---
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.BloodWardenBossEntity>> BLOOD_WARDEN =
            ENTITY_TYPES.register("blood_warden",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.BloodWardenBossEntity>of(
                                    br.com.murilo.liberthia.entity.BloodWardenBossEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 2.9F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("blood_warden"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.WeavingShadeEntity>> WEAVING_SHADE =
            ENTITY_TYPES.register("weaving_shade",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.WeavingShadeEntity>of(
                                    br.com.murilo.liberthia.entity.WeavingShadeEntity::new, MobCategory.MONSTER)
                            .sized(0.4F, 0.8F)
                            .clientTrackingRange(8)
                            .build("weaving_shade"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.DisarmerEntity>> DISARMER =
            ENTITY_TYPES.register("disarmer",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.DisarmerEntity>of(
                                    br.com.murilo.liberthia.entity.DisarmerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .build("disarmer"));

    // ════════════════════════════════════════════════════════════════════════
    // v0.1.22 r33: LOOM DIMENSION ENTITIES
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.loom.entity.WatcherStalkerEntity>> LOOM_WATCHER =
            ENTITY_TYPES.register("loom_watcher",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.loom.entity.WatcherStalkerEntity>of(
                                    br.com.murilo.liberthia.loom.entity.WatcherStalkerEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 2.4F)
                            .clientTrackingRange(48)
                            .fireImmune()
                            .build("loom_watcher"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity>> LOOM_PERIPHERAL =
            ENTITY_TYPES.register("loom_peripheral",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity>of(
                                    br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 2.0F)
                            .clientTrackingRange(64)
                            .build("loom_peripheral"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.loom.entity.ScreamerTeleporterEntity>> LOOM_SCREAMER =
            ENTITY_TYPES.register("loom_screamer",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.loom.entity.ScreamerTeleporterEntity>of(
                                    br.com.murilo.liberthia.loom.entity.ScreamerTeleporterEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(32)
                            .build("loom_screamer"));

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.loom.entity.DimensionalWormEntity>> LOOM_WORM =
            ENTITY_TYPES.register("loom_worm",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.loom.entity.DimensionalWormEntity>of(
                                    br.com.murilo.liberthia.loom.entity.DimensionalWormEntity::new, MobCategory.MONSTER)
                            .sized(0.5F, 0.3F)
                            .clientTrackingRange(16)
                            .build("loom_worm"));

    // r48: REFLECTION ENTITY — clone player com AI natural (Reflection Seed)
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.cosmic.observatory.ReflectionEntity>> REFLECTION_ENTITY =
            ENTITY_TYPES.register("reflection_entity",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.cosmic.observatory.ReflectionEntity>of(
                                    br.com.murilo.liberthia.cosmic.observatory.ReflectionEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(48)
                            .build("reflection_entity"));

    // r74: Bookwyrm Familiar — pet que auto-casta spell em hostile mob
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.observation.entity.BookwyrmEntity>>
            BOOKWYRM = ENTITY_TYPES.register("bookwyrm",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.observation.entity.BookwyrmEntity>of(
                                    br.com.murilo.liberthia.observation.entity.BookwyrmEntity::new,
                                    MobCategory.CREATURE)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(16)
                            .build("bookwyrm"));

    // r64: Observation projectile — spell que voa com SynchedEntityData color, trail glow, hit detection
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.observation.entity.EntityObservationProjectile>>
            OBSERVATION_PROJECTILE = ENTITY_TYPES.register("observation_projectile",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.observation.entity.EntityObservationProjectile>of(
                                    br.com.murilo.liberthia.observation.entity.EntityObservationProjectile::new,
                                    MobCategory.MISC)
                            .sized(0.4F, 0.4F)
                            .clientTrackingRange(32)
                            .updateInterval(10)
                            .noSummon()
                            .fireImmune()
                            .build("observation_projectile"));

    // ════════════════════════════════════════════════════════════════════════
    // r81: HORROR FRAMEWORK ENTITIES — 4 iconic horror entities
    // ════════════════════════════════════════════════════════════════════════

    /** Empty Man — uncanny humanoid distante. Desaparece se aproximar. */
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity>> EMPTY_MAN =
            ENTITY_TYPES.register("empty_man",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity>of(
                                    br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity::new,
                                    MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(64)
                            .build("empty_man"));

    /** Observer — SCP-173 style. Move só sem line of sight. */
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity>> OBSERVER =
            ENTITY_TYPES.register("observer",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity>of(
                                    br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity::new,
                                    MobCategory.MONSTER)
                            .sized(0.7F, 2.0F)
                            .clientTrackingRange(48)
                            .build("observer"));

    /** Absence — invisible void creature. Detect by particles missing. */
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity>> ABSENCE =
            ENTITY_TYPES.register("absence",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity>of(
                                    br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity::new,
                                    MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(32)
                            .build("absence"));

    /** Remembered — memetic horror. Manifesta com menção em chat. */
    public static final RegistryObject<EntityType<br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity>> REMEMBERED =
            ENTITY_TYPES.register("remembered",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity>of(
                                    br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity::new,
                                    MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(32)
                            .build("remembered"));

    private ModEntities() {}

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
