package br.com.murilo.liberthia.entry;

import br.com.murilo.liberthia.logic.TrackerManager;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class TrackerC2SPacket {
    private final UUID targetId;

    public TrackerC2SPacket(UUID targetId) {
        this.targetId = targetId;
    }

    public static void encode(TrackerC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetId);
    }

    public static TrackerC2SPacket decode(FriendlyByteBuf buf) {
        return new TrackerC2SPacket(buf.readUUID());
    }

    public static void handle(TrackerC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !sender.hasPermissions(2)) return;

            TrackerManager.TrackerData data = TrackerManager.getData(msg.targetId);
            if (data == null) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                        new TrackerDataS2CPacket(msg.targetId, "?", 0, 0, 0, "?", true));
                return;
            }

            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                    new TrackerDataS2CPacket(msg.targetId, data.name,
                            data.x, data.y, data.z, data.dimension, data.signalLost));
        });
        ctx.get().setPacketHandled(true);
    }
}
