package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Camera;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, LiberthiaMod.MODID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, LiberthiaMod.MODID);

    private static final ResourceLocation WATER_STILL =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
    private static final ResourceLocation WATER_FLOW =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");
    private static final ResourceLocation WATER_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_overlay");
    private static final ResourceLocation UNDERWATER =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/underwater.png");

    // ---------------- DARK MATTER ----------------
    public static final RegistryObject<FluidType> DARK_MATTER_TYPE = FLUID_TYPES.register("dark_matter_type", () ->
            new FluidType(FluidType.Properties.create()
                    .density(1000)
                    .viscosity(1000)
                    .lightLevel(1)
                    .canSwim(true)
                    .canDrown(true)
                    .supportsBoating(true)
                    .canHydrate(true)) {

                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(tintedExt(0xFF050505, new Vector3f(0.02F, 0.02F, 0.02F)));
                }
            });

    public static final RegistryObject<FlowingFluid> DARK_MATTER = FLUIDS.register("dark_matter",
            () -> new ForgeFlowingFluid.Source(darkMatterProperties()));

    public static final RegistryObject<FlowingFluid> FLOWING_DARK_MATTER = FLUIDS.register("flowing_dark_matter",
            () -> new ForgeFlowingFluid.Flowing(darkMatterProperties()));

    // ---------------- CLEAR MATTER ----------------
    public static final RegistryObject<FluidType> CLEAR_MATTER_TYPE = FLUID_TYPES.register("clear_matter_type", () ->
            new FluidType(FluidType.Properties.create()
                    .density(900)
                    .viscosity(800)
                    .lightLevel(6)
                    .canSwim(true)
                    .canDrown(true)
                    .supportsBoating(true)
                    .canHydrate(true)) {

                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(tintedExt(0xFFB0E8FF, new Vector3f(0.69F, 0.91F, 1.0F)));
                }
            });

    public static final RegistryObject<FlowingFluid> CLEAR_MATTER = FLUIDS.register("clear_matter",
            () -> new ForgeFlowingFluid.Source(clearMatterProperties()));

    public static final RegistryObject<FlowingFluid> FLOWING_CLEAR_MATTER = FLUIDS.register("flowing_clear_matter",
            () -> new ForgeFlowingFluid.Flowing(clearMatterProperties()));

    // ---------------- YELLOW MATTER ----------------
    public static final RegistryObject<FluidType> YELLOW_MATTER_TYPE = FLUID_TYPES.register("yellow_matter_type", () ->
            new FluidType(FluidType.Properties.create()
                    .density(1200)
                    .viscosity(1500)
                    .lightLevel(8)
                    .canSwim(true)
                    .canDrown(true)
                    .supportsBoating(true)
                    .canHydrate(true)) {

                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(tintedExt(0xFFFFD94A, new Vector3f(1.0F, 0.85F, 0.29F)));
                }
            });

    public static final RegistryObject<FlowingFluid> YELLOW_MATTER = FLUIDS.register("yellow_matter",
            () -> new ForgeFlowingFluid.Source(yellowMatterProperties()));

    public static final RegistryObject<FlowingFluid> FLOWING_YELLOW_MATTER = FLUIDS.register("flowing_yellow_matter",
            () -> new ForgeFlowingFluid.Flowing(yellowMatterProperties()));

    // ---------------- BLOOD ----------------
    private static final ResourceLocation BLOOD_STILL =
            ResourceLocation.fromNamespaceAndPath("liberthia", "block/blood_still");
    private static final ResourceLocation BLOOD_FLOW =
            ResourceLocation.fromNamespaceAndPath("liberthia", "block/blood_flow");

    public static final RegistryObject<FluidType> BLOOD_TYPE = FLUID_TYPES.register("blood_type", () ->
            new FluidType(FluidType.Properties.create()
                    .density(1500)
                    .viscosity(2500)
                    .lightLevel(2)
                    .canSwim(true)
                    .canDrown(true)
                    .supportsBoating(false)
                    .canHydrate(false)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture() { return BLOOD_STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return BLOOD_FLOW; }
                        @Override public ResourceLocation getOverlayTexture() { return BLOOD_STILL; }
                        @Override public ResourceLocation getRenderOverlayTexture(Minecraft mc) { return UNDERWATER; }
                        @Override public int getTintColor() { return 0xFFFFFFFF; }
                        @Override
                        public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                                       int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                            return new Vector3f(0.55F, 0.05F, 0.05F);
                        }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> BLOOD = FLUIDS.register("blood",
            () -> new ForgeFlowingFluid.Source(bloodProperties()));

    public static final RegistryObject<FlowingFluid> FLOWING_BLOOD = FLUIDS.register("flowing_blood",
            () -> new ForgeFlowingFluid.Flowing(bloodProperties()));

    private ModFluids() {
    }

    private static IClientFluidTypeExtensions tintedExt(int tint, Vector3f fog) {
        return new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return WATER_STILL; }
            @Override public ResourceLocation getFlowingTexture() { return WATER_FLOW; }
            @Override public ResourceLocation getOverlayTexture() { return WATER_OVERLAY; }
            @Override public ResourceLocation getRenderOverlayTexture(Minecraft mc) { return UNDERWATER; }
            @Override public int getTintColor() { return tint; }
            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                           int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(fog);
            }
        };
    }

    private static ForgeFlowingFluid.Properties darkMatterProperties() {
        return new ForgeFlowingFluid.Properties(DARK_MATTER_TYPE, DARK_MATTER, FLOWING_DARK_MATTER)
                .bucket(ModItems.DARK_MATTER_BUCKET)
                .block(ModBlocks.DARK_MATTER_FLUID_BLOCK)
                .levelDecreasePerBlock(1)
                .slopeFindDistance(4)
                .tickRate(5)
                .explosionResistance(100.0F);
    }

    private static ForgeFlowingFluid.Properties clearMatterProperties() {
        return new ForgeFlowingFluid.Properties(CLEAR_MATTER_TYPE, CLEAR_MATTER, FLOWING_CLEAR_MATTER)
                .bucket(ModItems.CLEAR_MATTER_BUCKET)
                .block(ModBlocks.CLEAR_MATTER_FLUID_BLOCK)
                .levelDecreasePerBlock(1)
                .slopeFindDistance(4)
                .tickRate(5)
                .explosionResistance(100.0F);
    }

    private static ForgeFlowingFluid.Properties yellowMatterProperties() {
        return new ForgeFlowingFluid.Properties(YELLOW_MATTER_TYPE, YELLOW_MATTER, FLOWING_YELLOW_MATTER)
                .bucket(ModItems.YELLOW_MATTER_BUCKET)
                .block(ModBlocks.YELLOW_MATTER_FLUID_BLOCK)
                .levelDecreasePerBlock(1)
                .slopeFindDistance(4)
                .tickRate(5)
                .explosionResistance(100.0F);
    }

    private static ForgeFlowingFluid.Properties bloodProperties() {
        return new ForgeFlowingFluid.Properties(BLOOD_TYPE, BLOOD, FLOWING_BLOOD)
                .bucket(ModItems.BLOOD_BUCKET)
                .block(ModBlocks.BLOOD_FLUID_BLOCK)
                .levelDecreasePerBlock(2)
                .slopeFindDistance(3)
                .tickRate(10)
                .explosionResistance(100.0F);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
