package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: pede ao cliente para abrir a tela "capturado pelo selo".
 *
 * <p>{@link #handle} roda nos dois lados, então qualquer referência a classes
 * de cliente ({@code Screen}, {@code Minecraft}, …) tem que estar isolada num
 * tipo separado — caso contrário o servidor dedicado tenta carregar
 * {@code net/minecraft/client/gui/screens/Screen} e crasha o boot.
 */
public class OpenCapturedScreenPacket {

    public static void encode(OpenCapturedScreenPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenCapturedScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenCapturedScreenPacket();
    }

    public static void handle(OpenCapturedScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        // safeRunWhenOn só carrega ClientHandler no lado correto. No servidor
        // dedicado o lambda nunca é executado e o classloader nunca toca em Screen.
        context.enqueueWork(() ->
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientHandler::new));

        context.setPacketHandled(true);
    }

    /**
     * Carregada apenas no cliente. Implementa {@link DistExecutor.SafeRunnable}
     * para que o método {@code run} seja invocado pelo DistExecutor.
     */
    private static final class ClientHandler implements DistExecutor.SafeRunnable {
        @Override
        public void run() {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new br.com.murilo.liberthia.client.screen.CapturedBySealScreen());
        }
    }
}
