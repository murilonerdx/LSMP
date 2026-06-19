package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.matter.MatterProfileEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S — cliente pede um re-sync do profile de matter ao abrir a Matter Analyzer
 * screen (tab Perfil). Sem payload — server responde com {@link MatterProfileSyncS2CPacket}.
 *
 * <p>Bug fix v1: quando o cliente abre a tela Perfil antes do sync periódico de
 * 5s, o cache ficava vazio e a tela mostrava "Profile não disponível". Agora a
 * Screen dispara este packet no init() pra forçar o sync imediato.
 */
public record RequestMatterProfileSyncC2SPacket() {

    public static void encode(RequestMatterProfileSyncC2SPacket pkt, FriendlyByteBuf buf) {
        // no payload
    }

    public static RequestMatterProfileSyncC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestMatterProfileSyncC2SPacket();
    }

    public static void handle(RequestMatterProfileSyncC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                MatterProfileEvents.syncTo(sender);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
