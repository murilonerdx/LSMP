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

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.CleansingGrenadeEntity>> CLEANSING_GRENADE = 
            ENTITY_TYPES.register("cleansing_grenade",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.CleansingGrenadeEntity>of(br.com.murilo.liberthia.entity.CleansingGrenadeEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("cleansing_grenade"));

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

    private ModEntities() {}

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
