package br.com.murilo.liberthia.entry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class TrackerDataS2CPacket {
    private final UUID targetId;
    private final String name;
    private final double x, y, z;
    private final String dimension;
    private final boolean signalLost;

    public TrackerDataS2CPacket(UUID targetId, String name, double x, double y, double z, String dimension, boolean signalLost) {
        this.targetId = targetId;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.signalLost = signalLost;
    }

    public static void encode(TrackerDataS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetId);
        buf.writeUtf(msg.name, 64);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeUtf(msg.dimension, 128);
        buf.writeBoolean(msg.signalLost);
    }

    public static TrackerDataS2CPacket decode(FriendlyByteBuf buf) {
        return new TrackerDataS2CPacket(
                buf.readUUID(), buf.readUtf(64),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readUtf(128), buf.readBoolean()
        );
    }

    public static void handle(TrackerDataS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleClient(TrackerDataS2CPacket msg) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof TrackerScreen screen) {
            screen.updateData(msg.name, msg.x, msg.y, msg.z, msg.dimension, msg.signalLost);
        }
    }
}
