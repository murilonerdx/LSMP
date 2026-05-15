package br.com.murilo.liberthia;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import br.com.murilo.liberthia.event.ModConfigEvents;
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
        LOGGER.debug("[Liberthia] inicializando mod (versão 0.1.6)...");
        IEventBus modBus = context.getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModFluids.register(modBus);
        ModSounds.register(modBus);
        ModEntities.register(modBus);
        ModCapabilities.register(modBus);
        ModEffects.register(modBus);
        ModMobEffects.register(modBus);
        ModPotions.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenuTypes.register(modBus);
        ModCreativeTabs.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.register(ModConfigEvents.class);

        context.registerConfig(ModConfig.Type.SERVER, LiberthiaConfig.SERVER_SPEC, "liberthia-server.toml");
        context.registerConfig(ModConfig.Type.CLIENT, LiberthiaConfig.CLIENT_SPEC, "liberthia-client.toml");
        LOGGER.debug("[Liberthia] configs registrados: liberthia-server.toml (será gerado em world/serverconfig/), liberthia-client.toml (config/)");
        KirikoBookNetworking.register();
        // InfectionEvents + WorldSpawnerEvents REMOVIDOS — não modificam mais blocos.

        // Admin HTTP API — painel web externo de administração
        MinecraftForge.EVENT_BUS.register(new br.com.murilo.liberthia.admin.api.AdminHttpServer());
        MinecraftForge.EVENT_BUS.register(new br.com.murilo.liberthia.admin.api.AdminEventBus());

        // Voice: registra EXPLICITAMENTE os event handlers em vez de confiar
        // no @Mod.EventBusSubscriber scan (que às vezes falha em pegar a classe
        // se nada referencia ela em outro lugar). Os métodos são static,
        // EVENT_BUS aceita classes pra subscribers estáticos.
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.voice.VoiceTickWatcher.class);
        // Freeze events tem o mesmo padrão — garantir
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.freeze.FreezeEvents.class);

        // CommandEvents intercepta /liberthia admin via CommandEvent.
        // Sem registro manual, o @Mod.EventBusSubscriber às vezes não cola.
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.event.CommandEvents.class);

        // Hooks de comunidade — Backrooms tracking, Chat quotes, Memorial auto.
        // Alimenta /api/backrooms, /api/quotes, /api/memorials no backend.
        MinecraftForge.EVENT_BUS.register(new br.com.murilo.liberthia.admin.api.hooks.CommunityHooks());

        // Photo watcher — escaneia world/exposures/ e uploada PNGs novos pra galeria.
        MinecraftForge.EVENT_BUS.register(new br.com.murilo.liberthia.admin.api.hooks.ExposurePhotoWatcher());

        ModParticles.PARTICLE_TYPES.register(modBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}