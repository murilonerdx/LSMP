package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r54: S2C — pede pro cliente abrir o {@code SpellWheelScreen}.
 *
 * <p>Disparado pelo {@link br.com.murilo.liberthia.magic.GrimoireItem} quando
 * o player faz shift+rclick. Como o GrimoireItem é server-side e o Screen
 * é client-only, precisamos do packet pra abrir a tela via DistExecutor.
 *
 * <p>O packet é vazio — não há payload. O cliente apenas abre a wheel já
 * sincronizada pelo {@code SyncCustomSpellsS2CPacket}.
 */
public class OpenSpellWheelS2CPacket {

    public OpenSpellWheelS2CPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static OpenSpellWheelS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenSpellWheelS2CPacket();
    }

    public static void handle(OpenSpellWheelS2CPacket msg,
                               Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.open()));
        ctx.get().setPacketHandled(true);
    }

    /** Client-only — classe SEPARADA: o servidor nunca carrega Screen ao verificar o pacote. */
    private static final class Client {
        static void open() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen == null) {
                mc.setScreen(new br.com.murilo.liberthia.magic.custom.client.SpellWheelScreen());
            }
        }
    }
}
