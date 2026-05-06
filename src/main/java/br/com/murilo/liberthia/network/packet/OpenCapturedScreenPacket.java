package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.client.screen.CapturedBySealScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenCapturedScreenPacket {

    public static void encode(OpenCapturedScreenPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenCapturedScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenCapturedScreenPacket();
    }

    public static void handle(OpenCapturedScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(new CapturedBySealScreen())
        ));

        context.setPacketHandled(true);
    }
}
