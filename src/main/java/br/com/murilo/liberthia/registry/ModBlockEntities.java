package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.block.entity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LiberthiaMod.MODID);

    public static final RegistryObject<BlockEntityType<PurificationBenchBlockEntity>> PURIFICATION_BENCH = BLOCK_ENTITIES.register("purification_bench",
            () -> BlockEntityType.Builder.of(PurificationBenchBlockEntity::new, ModBlocks.PURIFICATION_BENCH.get()).build(null));

    public static final RegistryObject<BlockEntityType<DarkMatterForgeBlockEntity>> DARK_MATTER_FORGE = BLOCK_ENTITIES.register("dark_matter_forge",
            () -> BlockEntityType.Builder.of(DarkMatterForgeBlockEntity::new, ModBlocks.DARK_MATTER_FORGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MatterInfuserBlockEntity>> MATTER_INFUSER = BLOCK_ENTITIES.register("matter_infuser",
            () -> BlockEntityType.Builder.of(MatterInfuserBlockEntity::new, ModBlocks.MATTER_INFUSER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE = BLOCK_ENTITIES.register("research_table",
            () -> BlockEntityType.Builder.of(ResearchTableBlockEntity::new, ModBlocks.RESEARCH_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ContainmentChamberBlockEntity>> CONTAINMENT_CHAMBER = BLOCK_ENTITIES.register("containment_chamber",
            () -> BlockEntityType.Builder.of(ContainmentChamberBlockEntity::new, ModBlocks.CONTAINMENT_CHAMBER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MatterTransmuterBlockEntity>> MATTER_TRANSMUTER = BLOCK_ENTITIES.register("matter_transmuter",
            () -> BlockEntityType.Builder.of(MatterTransmuterBlockEntity::new, ModBlocks.MATTER_TRANSMUTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.InfectionHeartBlockEntity>> INFECTION_HEART = BLOCK_ENTITIES.register("infection_heart",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.InfectionHeartBlockEntity::new, ModBlocks.INFECTION_HEART.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.BloodCauldronBlockEntity>> BLOOD_CAULDRON = BLOCK_ENTITIES.register("blood_cauldron",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.BloodCauldronBlockEntity::new, ModBlocks.BLOOD_CAULDRON.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
