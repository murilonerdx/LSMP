package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.event.PossessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: possessor pediu cancelamento (geralmente shift pressionado). Server
 * limpa estado e manda EndPossession pros dois clients via {@link PossessionManager#end}.
 */
public class EndPossessionC2SPacket {

    public EndPossessionC2SPacket() {}

    public static void encode(EndPossessionC2SPacket pkt, FriendlyByteBuf buf) {}

    public static EndPossessionC2SPacket decode(FriendlyByteBuf buf) {
        return new EndPossessionC2SPacket();
    }

    public static void handle(EndPossessionC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            PossessionManager.end(sender.server, sender.getUUID());
        });
        context.setPacketHandled(true);
    }
}
