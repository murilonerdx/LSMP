package br.com.murilo.liberthia;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import br.com.murilo.liberthia.event.InfectionEvents;
import br.com.murilo.liberthia.event.ModConfigEvents;
import br.com.murilo.liberthia.event.WorldSpawnerEvents;
import br.com.murilo.liberthia.network.KirikoBookNetworking;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.registry.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LiberthiaMod.MODID)
public class LiberthiaMod {
    public static final String MODID = "liberthia";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LiberthiaMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModFluids.register(modBus);
        ModSounds.register(modBus);
        ModEntities.register(modBus);
        ModCapabilities.register(modBus);
        ModEffects.register(modBus);
        ModPotions.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenuTypes.register(modBus);
        ModCreativeTabs.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.register(ModConfigEvents.class);

        context.registerConfig(ModConfig.Type.SERVER, LiberthiaConfig.SERVER_SPEC, "liberthia-server.toml");
        context.registerConfig(ModConfig.Type.CLIENT, LiberthiaConfig.CLIENT_SPEC, "liberthia-client.toml");
        KirikoBookNetworking.register();
        MinecraftForge.EVENT_BUS.register(new InfectionEvents());
        MinecraftForge.EVENT_BUS.register(new WorldSpawnerEvents());

        ModParticles.PARTICLE_TYPES.register(modBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}