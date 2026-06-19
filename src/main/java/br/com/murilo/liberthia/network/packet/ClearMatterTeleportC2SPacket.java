package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.event.ClearMatterPowersHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Cliente pede ao server "execute teleport da Clear Matter armor".
 *
 * <p>Disparado pelo cliente quando o player pressiona shift+right-click no AR
 * (sem mirar em bloco/entidade). Esse evento só existe no cliente
 * ({@code PlayerInteractEvent.RightClickEmpty}), por isso o packet — sem
 * ele a feature só funcionaria quando o player segura um item na mão
 * (RightClickItem dispara no server).
 *
 * <p>Sem payload — toda a validação acontece no handler do server:
 * checagem de set completo, cooldown, e shift segurado.
 */
public class ClearMatterTeleportC2SPacket {

    public ClearMatterTeleportC2SPacket() {}

    public static void encode(ClearMatterTeleportC2SPacket pkt, FriendlyByteBuf buf) {}

    public static ClearMatterTeleportC2SPacket decode(FriendlyByteBuf buf) {
        return new ClearMatterTeleportC2SPacket();
    }

    public static void handle(ClearMatterTeleportC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            ClearMatterPowersHandler.tryTeleport(sp);
        });
        ctx.get().setPacketHandled(true);
    }
}
