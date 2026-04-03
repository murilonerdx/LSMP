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

    public static final RegistryObject<EntityType<br.com.murilo.liberthia.entity.WhiteMatterExplosionEntity>> WHITE_MATTER_EXPLOSION =
            ENTITY_TYPES.register("white_matter_explosion",
                    () -> EntityType.Builder.<br.com.murilo.liberthia.entity.WhiteMatterExplosionEntity>of(br.com.murilo.liberthia.entity.WhiteMatterExplosionEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(10)
                            .build("white_matter_explosion"));

    private ModEntities() {}

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
