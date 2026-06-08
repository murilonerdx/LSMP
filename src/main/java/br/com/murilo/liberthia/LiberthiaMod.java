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
        br.com.murilo.liberthia.registry.ModTech.register(modBus);
        ModFluids.register(modBus);
        // r138: Worldgen features (Wizard Tower)
        br.com.murilo.liberthia.registry.ModFeatures.register(modBus);
        // r83: Synergy mob effects (Chilled/Frostbite/Burning/Immolate/Bleed/Hemorrhage/Static/StormMark/Heartstop)
        br.com.murilo.liberthia.magic.effect.SynergyEffects.register(modBus);
        ModSounds.register(modBus);
        ModEntities.register(modBus);
        ModCapabilities.register(modBus);
        ModEffects.register(modBus);
        br.com.murilo.liberthia.registry.ModEnchantments.register(modBus);
        ModMobEffects.register(modBus);
        ModPotions.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenuTypes.register(modBus);
        ModCreativeTabs.register(modBus);
        ModRecipes.register(modBus);

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

        // r54: Spirit World chat block — bloqueia /tell, /msg, /w, /say, /me e chat
        // normal pra players em spirit world. Player precisa achar saída física.
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.dimension.SpiritWorldChatBlocker.class);

        // r55: Spirit World — preserva inventário em caso de death
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.dimension.SpiritWorldDeathHandler.class);

        // r55: Shadow Stalker spawning tick — entidade que se aproxima quando você olha
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.stalker.ShadowStalkerManager.class);

        // r55: Player Silhouette spawner — silhuetas que aparecem e somem
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.silhouette.PlayerSilhouetteManager.class);

        // r55: Hallucination expansion — vultos, footsteps, particles em low sanity
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.hallucination.LowSanityHallucinationDriver.class);

        // r55: Red-eye mob stare — player com baixa sanidade vê mobs encarando red
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.staredown.RedEyeStareManager.class);

        // r55: Chunk Copy Illusion — mineração random teleporta pra spirit world
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.illusion.ChunkIllusionManager.class);

        // r55: Floating Trees — árvores flutuantes random perto do player
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.floating.FloatingTreesDriver.class);

        // r55: Pale Watch Events — drivers passivos dos pendants
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.palewatch.PaleWatchEvents.class);

        // r56: 15 cosmic artifacts passive handlers
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56Events.class);

        // r56: Wood Dimension Event — temporário "lugar de madeira" pra player paranóico
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.wooddim.WoodDimensionEvent.class);

        // r57: LIMINAL DIMENSIONS — 3 dimensões psychological horror
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.dimension.LiminalEffectsManager.class);

        // r58: Clone Army AI — clones reais que atacam aggressores do master
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.cosmic.palewatch.CloneArmyAI.class);

        // r58: Procedural Strangeness Engine — gera rooms liminais on-the-fly
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.dimension.proc.ProceduralStrangeness.class);

        // r61: Observation Source tick handler — regen mana cosmic ao observar
        MinecraftForge.EVENT_BUS.register(br.com.murilo.liberthia.observation.source.SourceTickHandler.class);
        // r61: Force-init observation parts singleton (registry warmup)
        br.com.murilo.liberthia.observation.ObservationParts.init();

        // Hooks de comunidade — Backrooms tracking, Chat quotes, Memorial auto.
        // Alimenta /api/backrooms, /api/quotes, /api/memorials no backend.
        MinecraftForge.EVENT_BUS.register(new br.com.murilo.liberthia.admin.api.hooks.CommunityHooks());

        // Photo watcher — escaneia world/exposures/ e uploada PNGs novos pra galeria.
        MinecraftForge.EVENT_BUS.register(new br.com.murilo.liberthia.admin.api.hooks.ExposurePhotoWatcher());

        // Sistema Nervoso (telemetria + análise comportamental).
        // EventCollector subscreve eventos Forge e alimenta TelemetryManager.
        // O Manager é iniciado/parado automaticamente via ServerStartedEvent /
        // ServerStoppingEvent (cobertos pelo próprio EventCollector).
        MinecraftForge.EVENT_BUS.register(new br.com.murilo.liberthia.telemetry.events.EventCollector());

        // Matter economy: drops em mineração (DarkMatterShard com chance baseada em profundidade)
        // + conversão TNT (DarkMatterShard → YellowMatterIngot via explosão).
        // Permite progression sem depender só de minérios raros.
        MinecraftForge.EVENT_BUS.register(new br.com.murilo.liberthia.event.MatterDropEvents());

        // r177/r178: AfkObserverManager, BlinkInDarkManager, IdolManager, IdolCommand,
        // HauntDirector, DarkFearManager, SoundMimicManager, NightmareSleepManager —
        // todos @Mod.EventBusSubscriber (auto-registram no FORGE bus). NÃO registrar
        // manualmente aqui também (causava registro DUPLO = timers contando em dobro).

        ModParticles.PARTICLE_TYPES.register(modBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        // r81: inicializa o Horror Framework (registra os 18 sistemas)
        event.enqueueWork(br.com.murilo.liberthia.cosmic.framework.HorrorFramework::init);
        // r116: inicializa receitas de Spirit Glyphs (Spirit Reagent → Spell Scroll)
        event.enqueueWork(br.com.murilo.liberthia.magic.glyph.SpiritGlyphRecipeInit::registerAll);
    }
}