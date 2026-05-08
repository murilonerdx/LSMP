package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.block.entity.DimensionalChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetDimensionalChannelC2SPacket {
    private final BlockPos pos;
    private final String channel;

    public SetDimensionalChannelC2SPacket(BlockPos pos, String channel) {
        this.pos = pos;
        this.channel = channel == null ? "" : channel;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(channel, 32);
    }

    public static SetDimensionalChannelC2SPacket decode(FriendlyByteBuf buf) {
        return new SetDimensionalChannelC2SPacket(buf.readBlockPos(), buf.readUtf(32));
    }

    public static void handle(SetDimensionalChannelC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            // Anti-cheese: precisa estar perto
            if (sp.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64) return;
            if (sp.level().getBlockEntity(msg.pos) instanceof DimensionalChestBlockEntity be) {
                be.setChannel(msg.channel);
                sp.displayClientMessage(
                        Component.literal("§9⌬ Canal alterado para: §f" + be.getChannel()), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
