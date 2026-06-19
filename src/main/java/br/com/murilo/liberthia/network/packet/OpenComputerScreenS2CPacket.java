package br.com.murilo.liberthia.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C — abre a tela do Computador com os arquivos atuais. */
public class OpenComputerScreenS2CPacket {

    private final CompoundTag data;

    public OpenComputerScreenS2CPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static OpenComputerScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenComputerScreenS2CPacket(buf.readNbt());
    }

    public static void handle(OpenComputerScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.gui.ComputerScreen.openNow(msg.data)));
        ctx.get().setPacketHandled(true);
    }
}
