package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r27: Registry estático de antenas dimensionais.
 *
 * <h2>Conceito</h2>
 * Cada antena ativa registra sua {@link GlobalPos} numa frequência (string).
 * Quando uma mensagem é broadcast pra uma freq, todos os players próximos
 * de qualquer antena tunada nessa freq recebem — mesmo em dimensões
 * diferentes (comunicação cross-dim).
 *
 * <h2>Concorrência</h2>
 * Operações via {@link ConcurrentHashMap} pra segurança em
 * multi-thread (chat events + tick handlers podem rolar em threads diferentes).
 *
 * <h2>Cleanup</h2>
 * Antenas que perdem fonte de energia/recursos chamam
 * {@link #unregister(GlobalPos)} no tick. Bloco quebrado também chama.
 * Cleanup completo em {@link #clearAll()} no server stop.
 */
public final class AntennaNetwork {

    /** Raio em blocos pro qual a mensagem broadcast é "audível". */
    public static final double BROADCAST_RADIUS = 12.0;

    /** Tamanho máximo de uma frequência (input do usuário). */
    public static final int MAX_FREQ_LENGTH = 16;

    /** Tamanho máximo da mensagem broadcast. */
    public static final int MAX_MSG_LENGTH = 200;

    /** Frequência (string) → set de GlobalPos das antenas tunadas. */
    private static final Map<String, Set<GlobalPos>> FREQUENCY_MAP = new ConcurrentHashMap<>();
    /** GlobalPos → frequência atual (lookup inverso). */
    private static final Map<GlobalPos, String> ANTENNA_FREQ = new ConcurrentHashMap<>();

    private AntennaNetwork() {}

    /**
     * Registra uma antena na frequência. Se já estava registrada noutra freq,
     * remove primeiro. Chamado quando antena fica ACTIVE no tick.
     */
    public static void register(GlobalPos pos, String frequency) {
        if (frequency == null || frequency.isEmpty()) {
            unregister(pos);
            return;
        }
        // Remove de freq antiga se existia
        String oldFreq = ANTENNA_FREQ.get(pos);
        if (oldFreq != null && !oldFreq.equals(frequency)) {
            Set<GlobalPos> oldSet = FREQUENCY_MAP.get(oldFreq);
            if (oldSet != null) {
                oldSet.remove(pos);
                if (oldSet.isEmpty()) FREQUENCY_MAP.remove(oldFreq);
            }
        }
        FREQUENCY_MAP.computeIfAbsent(frequency, k -> ConcurrentHashMap.newKeySet()).add(pos);
        ANTENNA_FREQ.put(pos, frequency);
    }

    /** Desregistra antena (quebrada, sem recursos, etc). */
    public static void unregister(GlobalPos pos) {
        String freq = ANTENNA_FREQ.remove(pos);
        if (freq != null) {
            Set<GlobalPos> set = FREQUENCY_MAP.get(freq);
            if (set != null) {
                set.remove(pos);
                if (set.isEmpty()) FREQUENCY_MAP.remove(freq);
            }
        }
        // v0.1.22 r28: limpa cache de canais SVC desta antena receptora
        try { AntennaVoiceRelay.cleanupReceiver(pos); }
        catch (Throwable ignored) {}
    }

    /** True se a antena nessa pos está registrada/ativa. */
    public static boolean isRegistered(GlobalPos pos) {
        return ANTENNA_FREQ.containsKey(pos);
    }

    /** Retorna a freq atual da antena nessa pos, ou null. */
    public static String getFrequency(GlobalPos pos) {
        return ANTENNA_FREQ.get(pos);
    }

    /** Retorna número de antenas tunadas na mesma freq (incluindo a passada). */
    public static int countTuned(String frequency) {
        if (frequency == null) return 0;
        Set<GlobalPos> set = FREQUENCY_MAP.get(frequency);
        return set == null ? 0 : set.size();
    }

    /** Retorna cópia imutável do set de antenas na frequência. */
    public static Set<GlobalPos> getTunedAntennas(String frequency) {
        Set<GlobalPos> set = FREQUENCY_MAP.get(frequency);
        return set == null ? Collections.emptySet() : new HashSet<>(set);
    }

    /**
     * v0.1.22 r28: Calcula tuning entre 2 antenas (0-100) baseado em direção.
     * Same dim: alinhamento direcional. Cross-dim: 50% base.
     */
    public static double calculateTuning(GlobalPos a, net.minecraft.core.Direction aFacing,
                                          GlobalPos b, net.minecraft.core.Direction bFacing) {
        if (a == null || b == null || aFacing == null || bFacing == null) return 0;
        if (!a.dimension().equals(b.dimension())) return 50.0;
        net.minecraft.world.phys.Vec3 aPos = net.minecraft.world.phys.Vec3.atCenterOf(a.pos());
        net.minecraft.world.phys.Vec3 bPos = net.minecraft.world.phys.Vec3.atCenterOf(b.pos());
        net.minecraft.world.phys.Vec3 aToB = bPos.subtract(aPos);
        if (aToB.lengthSqr() < 0.01) return 100.0;
        aToB = aToB.normalize();
        net.minecraft.world.phys.Vec3 bToA = aToB.scale(-1);
        net.minecraft.world.phys.Vec3 aFv = vecFromDir(aFacing);
        net.minecraft.world.phys.Vec3 bFv = vecFromDir(bFacing);
        double dotA = aFv.dot(aToB);
        double dotB = bFv.dot(bToA);
        double avg = (dotA + dotB) / 2.0;
        return Math.max(0, avg) * 100.0;
    }

    private static net.minecraft.world.phys.Vec3 vecFromDir(net.minecraft.core.Direction d) {
        var n = d.getNormal();
        return new net.minecraft.world.phys.Vec3(n.getX(), 0, n.getZ()).normalize();
    }

    /**
     * v0.1.22 r28: Listener interface pra Quantum Terminals que querem
     * receber mensagens de antenas tunadas. Terminal registra/desregistra
     * via {@link #addListener}/{@link #removeListener}.
     */
    public interface BroadcastListener {
        /** Chamado quando broadcast é entregue. msg é o conteúdo, freq é a freq. */
        void onBroadcast(String frequency, String senderName, String message);
        /** Frequência que o listener quer ouvir. Null = ouve TODAS. */
        String listeningFrequency();
    }

    private static final java.util.Set<BroadcastListener> LISTENERS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void addListener(BroadcastListener l) { LISTENERS.add(l); }
    public static void removeListener(BroadcastListener l) { LISTENERS.remove(l); }

    /** r39: snapshot dos listeners pra iteração externa segura. */
    public static java.util.Set<BroadcastListener> getListeners() {
        return new java.util.HashSet<>(LISTENERS);
    }

    /**
     * Broadcast uma mensagem em chat formatado pra todos os players num
     * raio de {@link #BROADCAST_RADIUS} blocos de qualquer antena tunada
     * em {@code frequency}, EXCETO os players próximos da antena fonte
     * (eles já ouviram local).
     *
     * @param server      server pra resolver dimensões
     * @param frequency   freq alvo
     * @param sender      player que falou
     * @param message     texto (trim + clamp)
     * @param sourcePos   pos da antena fonte (pra skip nearby)
     * @return número de players que receberam
     */
    public static int broadcastMessage(MinecraftServer server, String frequency,
                                       ServerPlayer sender, String message,
                                       GlobalPos sourcePos) {
        if (server == null || frequency == null || frequency.isEmpty()) return 0;
        if (message == null) return 0;
        message = message.trim();
        if (message.length() > MAX_MSG_LENGTH) {
            message = message.substring(0, MAX_MSG_LENGTH);
        }
        if (message.isEmpty()) return 0;

        String senderName = sender == null ? "???" : sender.getName().getString();

        // r39: NÃO MAIS broadcast pra chat ambiente — mensagens ficam APENAS
        // nos Quantum Terminals (storage server-side + display no GUI).
        // Notifica listeners (terminais) — eles é que salvam e mostram.
        int delivered = 0;
        for (BroadcastListener l : LISTENERS) {
            String lf = l.listeningFrequency();
            if (lf == null || lf.equals(frequency)) {
                try {
                    l.onBroadcast(frequency, senderName, message);
                    delivered++;
                } catch (Throwable t) {
                    LiberthiaMod.LOGGER.warn("[Antenna] listener threw: {}", t.getMessage());
                }
            }
        }

        // Feedback discreto pro sender (não é chat ambiente — actionbar pequeno)
        if (sender != null) {
            int channels = countTuned(frequency);
            sender.displayClientMessage(Component.literal(
                    "§7📡 §o" + delivered + " terminal(is) ouviram §7• §d"
                            + channels + "§7 antena(s) tunada(s)")
                    .withStyle(ChatFormatting.DARK_GRAY), true);
        }
        LiberthiaMod.LOGGER.debug("[Antenna] {} broadcast freq={} msg='{}' delivered={}",
                senderName, frequency, message, delivered);
        return delivered;
    }

    /** Encontra a antena ativa mais próxima do player num raio, ou null. */
    public static GlobalPos findNearestActive(ServerPlayer player, double radius) {
        if (player == null) return null;
        ResourceKey<Level> dim = player.level().dimension();
        BlockPos ppos = player.blockPosition();
        GlobalPos best = null;
        double bestDistSq = radius * radius;
        for (GlobalPos gp : ANTENNA_FREQ.keySet()) {
            if (!gp.dimension().equals(dim)) continue;
            double d = gp.pos().distSqr(ppos);
            if (d < bestDistSq) {
                bestDistSq = d;
                best = gp;
            }
        }
        return best;
    }

    /** Limpa tudo (server stopping). */
    public static void clearAll() {
        FREQUENCY_MAP.clear();
        ANTENNA_FREQ.clear();
        // v0.1.22 r28: limpa também cache de relay SVC
        try { AntennaVoiceRelay.clearAll(); }
        catch (Throwable ignored) {}
    }
}
