package br.com.murilo.liberthia.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenDimensionalChannelScreenS2CPacket {
    private final BlockPos pos;
    private final String channel;

    public OpenDimensionalChannelScreenS2CPacket(BlockPos pos, String channel) {
        this.pos = pos;
        this.channel = channel == null ? "" : channel;
    }

    public BlockPos getPos() { return pos; }
    public String getChannel() { return channel; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(channel, 32);
    }

    public static OpenDimensionalChannelScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenDimensionalChannelScreenS2CPacket(buf.readBlockPos(), buf.readUtf(32));
    }

    public static void handle(OpenDimensionalChannelScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.ClientDimensionalChestDispatch.openChannelScreen(msg)));
        ctx.get().setPacketHandled(true);
    }
}
