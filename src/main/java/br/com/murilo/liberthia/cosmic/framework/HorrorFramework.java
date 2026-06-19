package br.com.murilo.liberthia.cosmic.framework;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.systems.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.24 r81: <b>Horror Framework</b> — engine central que orquestra todos
 * os 18 sistemas de horror em paralelo.
 *
 * <h2>Como funciona</h2>
 * <ol>
 *   <li>{@link #init()} registra todos os 18 sistemas no {@link HorrorRegistry}</li>
 *   <li>Cada {@link TickEvent.PlayerTickEvent} END phase, percorre todos sistemas
 *       e chama {@code tick(player, level, gameTime, state)}</li>
 *   <li>Sistemas modificam o {@link HorrorState} do player (adicionam exposição,
 *       disparam efeitos, packets, etc.)</li>
 *   <li>Estado é mantido em memória — {@link #STATES} ConcurrentHashMap por UUID</li>
 * </ol>
 *
 * <h2>Filosofia de design</h2>
 * <ul>
 *   <li>Sistemas são <b>independentes</b> e <b>auto-rate-limited</b></li>
 *   <li>Throws em sistemas são capturados — nenhum sistema bloqueia outros</li>
 *   <li>Estado é leve (memória) — não persiste pra disk por padrão</li>
 *   <li>Players novos começam com 0 exposição em todos os tipos</li>
 * </ul>
 *
 * <h2>Integração com sistemas existentes</h2>
 * Este framework <b>NÃO substitui</b> o {@link br.com.murilo.liberthia.cosmic.CosmicHorrorManager}
 * antigo — coexiste, focando em mecânicas <b>passivas</b> e <b>ambientais</b>
 * que ticam sem trigger explícito. O Manager antigo continua sendo a base de
 * phases narrativas e item Forbidden Tome.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class HorrorFramework {

    private static final java.util.Map<UUID, HorrorState> STATES = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    private HorrorFramework() {}

    /**
     * Inicialização — chamada uma vez em {@code FMLCommonSetupEvent}. Idempotente
     * (chamar 2x não duplica).
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // Registra todos os 18 sistemas em ordem
        HorrorRegistry.register(new CosmicGodSystem());
        HorrorRegistry.register(new UncannyValleySystem());
        HorrorRegistry.register(new ExistentialSystem());
        HorrorRegistry.register(new OntologicalSystem());
        HorrorRegistry.register(new CognitiveSystem());
        HorrorRegistry.register(new MemeticSystem());
        HorrorRegistry.register(new TemporalSystem());
        HorrorRegistry.register(new DimensionalSystem());
        HorrorRegistry.register(new FleshSystem());
        HorrorRegistry.register(new ReligiousSystem());
        HorrorRegistry.register(new AnalogSystem());
        HorrorRegistry.register(new LiminalSystem());
        HorrorRegistry.register(new AstronomicalSystem());
        HorrorRegistry.register(new PerceptionSystem());
        HorrorRegistry.register(new GeometricSystem());
        HorrorRegistry.register(new VoidHorrorSystem());
        HorrorRegistry.register(new ParasiticSystem());
        HorrorRegistry.register(new PsychospiritualSystem());

        LiberthiaMod.LOGGER.info("[HorrorFramework] inicializado com {} sistemas",
                HorrorRegistry.size());
    }

    /** Pega ou cria o {@link HorrorState} pro player. */
    public static HorrorState getState(ServerPlayer sp) {
        return STATES.computeIfAbsent(sp.getUUID(), u -> new HorrorState());
    }

    /** Remove estado em logout (libera memória). */
    public static void clearState(UUID id) {
        STATES.remove(id);
    }

    /** Total de players com estado ativo (debug). */
    public static int activeStates() {
        return STATES.size();
    }

    // ─────────────────────── EVENT HOOKS ───────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // r112: kill switch — Horror auto-tick OFF por default
        if (!br.com.murilo.liberthia.config.LiberthiaConfig.SERVER.cosmicHorrorAutoTick.get()) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        // r183: GATE GLOBAL — eventos de horror só disparam com sanidade < 40%
        if (br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(sp) >= 40) return;

        HorrorState state = getState(sp);
        long now = level.getGameTime();

        for (HorrorSystem system : HorrorRegistry.all()) {
            try {
                system.tick(sp, level, now, state);
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.error(
                        "[HorrorFramework] erro em sistema {}: {}",
                        system.type(), t.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearState(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        HorrorState state = getState(sp);
        state.resetAnchor(sp.level().getGameTime());
        for (HorrorSystem system : HorrorRegistry.all()) {
            try { system.onDimensionChange(sp, state); }
            catch (Throwable ignored) {}
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        HorrorState state = getState(sp);
        for (HorrorSystem system : HorrorRegistry.all()) {
            try { system.onDeath(sp, state); }
            catch (Throwable ignored) {}
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sp = event.getPlayer();
        if (sp == null) return;
        HorrorState state = getState(sp);
        String msg = event.getMessage().getString();
        for (HorrorSystem system : HorrorRegistry.all()) {
            try { system.onChat(sp, msg, state); }
            catch (Throwable ignored) {}
        }
    }
}
