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
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL =
                                ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
                        private static final ResourceLocation FLOW =

                                ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");
                        private static final ResourceLocation OVERLAY =
                                ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_overlay");
                        private static final ResourceLocation UNDERWATER =
                                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/underwater.png");

                        @Override
                        public ResourceLocation getStillTexture() {
                            return STILL;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return FLOW;
                        }

                        @Override
                        public ResourceLocation getOverlayTexture() {
                            return OVERLAY;
                        }

                        @Override
                        public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
                            return UNDERWATER;
                        }

                        @Override
                        public int getTintColor() {
                            return 0xFF050505;
                        }

                        @Override
                        public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                                       int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                            return new Vector3f(0.02F, 0.02F, 0.02F);
                        }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> DARK_MATTER = FLUIDS.register("dark_matter",
            () -> new ForgeFlowingFluid.Source(darkMatterProperties()));

    public static final RegistryObject<FlowingFluid> FLOWING_DARK_MATTER = FLUIDS.register("flowing_dark_matter",
            () -> new ForgeFlowingFluid.Flowing(darkMatterProperties()));

    private ModFluids() {
    }

    private static ForgeFlowingFluid.Properties darkMatterProperties() {
        return new ForgeFlowingFluid.Properties(
                DARK_MATTER_TYPE,
                DARK_MATTER,
                FLOWING_DARK_MATTER
        )
                .bucket(ModItems.DARK_MATTER_BUCKET)
                .block(ModBlocks.DARK_MATTER_FLUID_BLOCK)
                .levelDecreasePerBlock(1)
                .slopeFindDistance(4)
                .tickRate(5)
                .explosionResistance(100.0F);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}