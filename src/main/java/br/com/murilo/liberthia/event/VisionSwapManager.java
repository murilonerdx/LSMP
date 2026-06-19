package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.EndVisionSwapS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador server-side da Feature "Vision Swap" (vidência via White Matter).
 *
 * <h2>Como funciona</h2>
 * Quando um player usa o {@code vision_swap_lens}, este manager registra um
 * {@link VisionSwapState} para ele. Durante {@link #DURATION_TICKS} (8s), a
 * câmera do user é redirecionada (no cliente) para a entidade do alvo via
 * {@code Minecraft.setCameraEntity(target)}. O corpo físico do user continua
 * exatamente onde estava, visível para todos os outros players.
 *
 * <p>Importante: o estado aqui registrado vive apenas no server; clients
 * recebem packets dedicados ({@code StartVisionSwapS2CPacket} e
 * {@code EndVisionSwapS2CPacket}) para aplicar/desfazer o swap visual.
 *
 * <p>Cancelamento automático:
 * <ul>
 *   <li>Expiração do timer (8s)</li>
 *   <li>Player target sai do servidor (handled em
 *       {@link #onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent)})</li>
 *   <li>User sai do servidor (idem)</li>
 *   <li>Death do user — não cancelamos explicitamente: ao reaparecer o player
 *       perde a câmera porque já não está mais conectado ao mesmo entity tick.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class VisionSwapManager {

    /** Duração padrão do swap em ticks (8 segundos × 20 ticks/s). */
    public static final int DURATION_TICKS = 160;

    /** Limite mínimo de White Matter para um player ser elegível como alvo. */
    public static final float MIN_WHITE_FOR_TARGET = 50.0f;

    /** user UUID → estado ativo. ConcurrentHashMap: writes vêm de main thread server. */
    private static final Map<UUID, VisionSwapState> ACTIVE = new ConcurrentHashMap<>();

    private VisionSwapManager() {}

    /** Estado imutável-ish de uma sessão de vision swap. */
    public static final class VisionSwapState {
        public final UUID userUuid;
        public final UUID targetUuid;
        public final long expiresAtTick;

        public VisionSwapState(UUID user, UUID target, long expires) {
            this.userUuid = user;
            this.targetUuid = target;
            this.expiresAtTick = expires;
        }
    }

    /**
     * Cria um novo swap. Retorna {@code true} se iniciou com sucesso (não havia
     * swap ativo desse user). Caller é responsável por mandar
     * {@code StartVisionSwapS2CPacket}.
     */
    public static boolean start(ServerPlayer user, ServerPlayer target) {
        if (ACTIVE.containsKey(user.getUUID())) return false;
        long now = user.serverLevel().getGameTime();
        ACTIVE.put(user.getUUID(),
                new VisionSwapState(user.getUUID(), target.getUUID(), now + DURATION_TICKS));
        // Action bar subtle pro alvo. Sem broadcast, sem nome do user.
        Component msg = Component.literal("✦ Voce sente que alguem esta te observando")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        target.connection.send(new ClientboundSetActionBarTextPacket(msg));
        return true;
    }

    /** Termina o swap manualmente (também manda packet). */
    public static void end(ServerPlayer user) {
        if (ACTIVE.remove(user.getUUID()) != null) {
            ModNetwork.sendToPlayer(user, new EndVisionSwapS2CPacket());
        }
    }

    /** True se o user está em swap (camera redirecionada). Usado pra bloquear inputs. */
    public static boolean isUserSwapping(UUID userUuid) {
        return ACTIVE.containsKey(userUuid);
    }

    /**
     * Tick periódico: expira swaps cuja {@code expiresAtTick} já passou e
     * cancela se o alvo desapareceu.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        long now = 0L;
        // Pega game time de qualquer level válido — todos os levels avançam juntos no server
        var level = server.overworld();
        if (level != null) now = level.getGameTime();

        Iterator<Map.Entry<UUID, VisionSwapState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, VisionSwapState> e = it.next();
            VisionSwapState st = e.getValue();
            ServerPlayer user = server.getPlayerList().getPlayer(st.userUuid);
            if (user == null) {
                // user offline — só remove silenciosamente
                it.remove();
                continue;
            }
            ServerPlayer target = server.getPlayerList().getPlayer(st.targetUuid);
            if (target == null || now >= st.expiresAtTick) {
                // expirou OU alvo desconectou — termina
                it.remove();
                ModNetwork.sendToPlayer(user, new EndVisionSwapS2CPacket());
            }
        }
    }

    /** Limpa estado do user / cancela se alvo sair. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        // Se o player saindo era user, só remove.
        if (ACTIVE.remove(id) != null) return;
        // Se era target de algum swap, encerra todos os swaps com ele.
        for (Iterator<Map.Entry<UUID, VisionSwapState>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, VisionSwapState> e = it.next();
            if (e.getValue().targetUuid.equals(id)) {
                ServerPlayer user = sp.server.getPlayerList().getPlayer(e.getKey());
                it.remove();
                if (user != null) ModNetwork.sendToPlayer(user, new EndVisionSwapS2CPacket());
            }
        }
    }

    /** Bloqueia ataques do user enquanto está em swap (server-side defesa em profundidade). */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() == null) return;
        if (isUserSwapping(event.getEntity().getUUID())) event.setCanceled(true);
    }
}
