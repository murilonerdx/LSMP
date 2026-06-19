package br.com.murilo.liberthia.client.hud.unified;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r164: server → client pedindo pra abrir o {@link UnifiedHudEditorScreen}.
 * Usado pelo comando {@code /liberthia hud editor} pra trigger a UI no client
 * que executou o comando.
 */
public class OpenHudEditorS2CPacket {

    public OpenHudEditorS2CPacket() {}

    public static void encode(OpenHudEditorS2CPacket pkt, FriendlyByteBuf buf) {}
    public static OpenHudEditorS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenHudEditorS2CPacket();
    }

    public static void handle(OpenHudEditorS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.open()));
        ctx.get().setPacketHandled(true);
    }

    /** Client-only — classe SEPARADA (server-safe). */
    private static final class Client {
        static void open() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new UnifiedHudEditorScreen());
            }
        }
    }
}
