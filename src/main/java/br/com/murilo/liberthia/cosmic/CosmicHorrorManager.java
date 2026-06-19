package br.com.murilo.liberthia.cosmic;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22 r38: CORE state machine do Cosmic Horror System (server-side).
 *
 * <h2>r38 CHANGES (Tome rework)</h2>
 * <ul>
 *   <li><b>Duração total:</b> 4 minutos (era 2). Cada phase agora 60s (era 30s).</li>
 *   <li><b>End behavior:</b> quando MANIFESTATION termina (player atingiu
 *       240s do tome), automaticamente teleporta player pro Spirit World
 *       com flag {@code tome_cursed} — só sai com Prayer Book.</li>
 *   <li><b>Bug fix:</b> antes MANIFESTATION ficava pra sempre. Agora encerra
 *       e força exit ritual via spirit world.</li>
 * </ul>
 *
 * <h2>Arquitetura</h2>
 * <ul>
 *   <li>Por player: 1 sessão {@link Session} com phase + tempo + intensity</li>
 *   <li>Tick do server: avança automaticamente phases se {@link Session#autoEscalate}</li>
 *   <li>Sync a cada 20 ticks (1s) pro client via {@link CosmicSyncS2CPacket}</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CosmicHorrorManager {

    /** Duração de cada phase em ticks quando auto-escalate (r38: 60s). */
    public static final int PHASE_DURATION_TICKS = 1200; // 60s

    /** r38: duração TOTAL antes do spirit world TP (4 minutos). */
    public static final int SESSION_MAX_TICKS = 4800; // 4min

    /** Intervalo de sync client (em ticks). */
    public static final int SYNC_INTERVAL_TICKS = 20; // 1s

    /** Sessões ativas por player UUID. */
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static class Session {
        public CosmicHorrorPhase phase;
        public long startedAt;
        public long lastPhaseChange;
        public boolean autoEscalate;
        /** 0..1 intensity dentro da phase atual (pra fade in/out). */
        public float intensity = 0.0F;
        /** Source — pra debugging/sound positional. */
        public String source = "manual";
        /** r38: marca pra TP forçado pro spirit world ao expirar. */
        public boolean exilesToSpirit = true;

        public Session(CosmicHorrorPhase p, long now, boolean auto) {
            this.phase = p;
            this.startedAt = now;
            this.lastPhaseChange = now;
            this.autoEscalate = auto;
        }
    }

    private CosmicHorrorManager() {}

    // ─────────────────────── PUBLIC API ───────────────────────

    /** Inicia ou re-inicia evento com auto-escalation completa. */
    public static void trigger(ServerPlayer sp, String source) {
        long now = sp.level().getGameTime();
        Session s = new Session(CosmicHorrorPhase.SUBTLE_PRESENCE, now, true);
        s.source = source;
        s.intensity = 0.0F;
        // r38: tomes exilam pro spirit world ao final; outros sources não
        s.exilesToSpirit = "forbidden_tome".equals(source);
        SESSIONS.put(sp.getUUID(), s);
        LiberthiaMod.LOGGER.info("[Cosmic] {} triggered by '{}' (exiles={})",
                sp.getName().getString(), source, s.exilesToSpirit);
        sync(sp, s);
    }

    /** Força uma phase específica (sem auto-escalation). */
    public static void setPhase(ServerPlayer sp, CosmicHorrorPhase p) {
        Session s = SESSIONS.computeIfAbsent(sp.getUUID(),
                k -> new Session(p, sp.level().getGameTime(), false));
        s.phase = p;
        s.lastPhaseChange = sp.level().getGameTime();
        s.autoEscalate = false;
        s.intensity = 1.0F; // direct jump
        sync(sp, s);
    }

    /** Reseta — encerra evento. */
    public static void reset(ServerPlayer sp) {
        SESSIONS.remove(sp.getUUID());
        Session dormant = new Session(CosmicHorrorPhase.DORMANT,
                sp.level().getGameTime(), false);
        sync(sp, dormant);
        LiberthiaMod.LOGGER.info("[Cosmic] reset for {}", sp.getName().getString());
    }

    public static CosmicHorrorPhase getPhase(ServerPlayer sp) {
        Session s = SESSIONS.get(sp.getUUID());
        return s == null ? CosmicHorrorPhase.DORMANT : s.phase;
    }

    public static Session getSession(UUID uuid) {
        return SESSIONS.get(uuid);
    }

    /** r38: true se o player ainda está sob efeito ativo (não DORMANT). */
    public static boolean isActive(ServerPlayer sp) {
        Session s = SESSIONS.get(sp.getUUID());
        return s != null && s.phase != CosmicHorrorPhase.DORMANT;
    }

    // ─────────────────────── TICK + AUTO ESCALATION ───────────────────────

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();

        java.util.Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID uuid = entry.getKey();
            Session s = entry.getValue();
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp == null) continue;

            // Intensity ramp 0→1 nos primeiros 100 ticks (5s) de cada phase
            long phaseElapsed = now - s.lastPhaseChange;
            float targetIntensity = Math.min(1.0F, phaseElapsed / 100.0F);
            s.intensity = s.intensity * 0.9F + targetIntensity * 0.1F;

            // r38: SESSION TIMEOUT — força fim após 4 min
            long sessionElapsed = now - s.startedAt;
            if (sessionElapsed >= SESSION_MAX_TICKS) {
                handleSessionExpire(sp, s);
                it.remove();
                continue;
            }

            // Auto-escalation entre fases (cada phase = 60s)
            if (s.autoEscalate && phaseElapsed >= PHASE_DURATION_TICKS) {
                CosmicHorrorPhase next = advancePhase(s.phase);
                if (next != s.phase) {
                    s.phase = next;
                    s.lastPhaseChange = now;
                    s.intensity = 0.0F;
                    LiberthiaMod.LOGGER.info("[Cosmic] {} auto-advanced to {} (sessElapsed={})",
                            sp.getName().getString(), next, sessionElapsed);
                    // Trigger phase event
                    CosmicHorrorEvents.onPhaseEnter(sp, next);
                }
            }

            // Trigger procedural events durante phase
            if (s.phase != CosmicHorrorPhase.DORMANT) {
                CosmicHorrorEvents.tickPhase(sp, s);
            }

            // Sync periódico
            if (now % SYNC_INTERVAL_TICKS == 0) {
                sync(sp, s);
            }
        }
    }

    /**
     * r38: chamado quando a sessão expira (4min). Se {@code exilesToSpirit},
     * teleporta player pro Spirit World com flag tome_cursed. Sem exit
     * exceto via Prayer Book.
     */
    private static void handleSessionExpire(ServerPlayer sp, Session s) {
        LiberthiaMod.LOGGER.info("[Cosmic] {} session EXPIRED (exiles={})",
                sp.getName().getString(), s.exilesToSpirit);

        // Manda DORMANT pro client (limpa shaders/overlays)
        Session dormant = new Session(CosmicHorrorPhase.DORMANT,
                sp.level().getGameTime(), false);
        sync(sp, dormant);

        if (!s.exilesToSpirit) return;

        // Marca como tome_cursed ANTES de teleportar (pra returnToBody bloquear)
        sp.getPersistentData().putBoolean(SpiritDimension.NBT_TOME_CURSED, true);

        sp.displayClientMessage(Component.literal(
                "§4§l✦ §r§4§oO Tomo Proibido te puxa pra §lOUTRO LUGAR§r§4§o..."), false);
        sp.level().playSound(null, sp.blockPosition(),
                SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 2.0F, 0.4F);

        // Spawn particles de "ruptura" antes do TP
        if (sp.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 60; i++) {
                double a = Math.random() * Math.PI * 2;
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                        sp.getX() + Math.cos(a) * 2,
                        sp.getY() + Math.random() * 3,
                        sp.getZ() + Math.sin(a) * 2,
                        1, 0, 0, 0, 0.1);
            }
        }

        // TP pro Spirit World
        boolean teleported = SpiritDimension.enterSpiritWorld(sp);
        if (teleported) {
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5Você está no §dMundo Espiritual§r§5. Use um §6Livro do Êxodo§r§5"), true);
            sp.displayClientMessage(Component.literal(
                    "§5(ou outro item de oração) pra escapar e remover a maldição."), false);
        } else {
            LiberthiaMod.LOGGER.warn("[Cosmic] {} couldn't TP to spirit (already there?)",
                    sp.getName().getString());
            // Já estava no spirit — só marca curse, ele vai precisar de prayer pra sair
        }
    }

    private static CosmicHorrorPhase advancePhase(CosmicHorrorPhase current) {
        switch (current) {
            case DORMANT:            return CosmicHorrorPhase.SUBTLE_PRESENCE;
            case SUBTLE_PRESENCE:    return CosmicHorrorPhase.DIMENSIONAL_CORRUPTION;
            case DIMENSIONAL_CORRUPTION: return CosmicHorrorPhase.REALITY_RUPTURE;
            case REALITY_RUPTURE:    return CosmicHorrorPhase.ENTITY_MANIFESTATION;
            default: return current;
        }
    }

    private static void sync(ServerPlayer sp, Session s) {
        ModNetwork.sendToPlayer(sp, new CosmicSyncS2CPacket(
                s.phase.ordinal(), s.intensity));
    }
}
