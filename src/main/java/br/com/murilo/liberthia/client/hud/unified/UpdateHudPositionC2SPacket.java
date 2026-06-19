package br.com.murilo.liberthia.client.hud.unified;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r164: client → server quando player solta um HUD após drag no editor.
 * Salva a posição no NBT do player e re-syncs back.
 */
public class UpdateHudPositionC2SPacket {

    /** ID do HUD (string do {@link HudId#id}). */
    public final String hudId;
    public final int x;
    public final int y;
    /** Se {@code true}, ignora x/y e reseta esse HUD pro default. */
    public final boolean reset;

    public UpdateHudPositionC2SPacket(String hudId, int x, int y, boolean reset) {
        this.hudId = hudId;
        this.x = x;
        this.y = y;
        this.reset = reset;
    }

    public static void encode(UpdateHudPositionC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.hudId);
        buf.writeInt(pkt.x);
        buf.writeInt(pkt.y);
        buf.writeBoolean(pkt.reset);
    }

    public static UpdateHudPositionC2SPacket decode(FriendlyByteBuf buf) {
        return new UpdateHudPositionC2SPacket(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(UpdateHudPositionC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            HudId hud = HudId.byId(pkt.hudId);
            if (hud == null) return;

            if (pkt.reset) {
                HudPositionsData.reset(sp, hud);
            } else {
                // Sanitiza pra evitar valores absurdos (player malicioso)
                int x = Math.max(-2000, Math.min(4000, pkt.x));
                int y = Math.max(-2000, Math.min(4000, pkt.y));
                HudPositionsData.set(sp, hud, x, y);
            }

            // Re-sync de volta pra cliente confirmar
            br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                    HudPositionsSyncS2CPacket.forPlayer(sp));
        });
        ctx.get().setPacketHandled(true);
    }
}
