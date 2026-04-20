package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.client.TeleportExecutorScreen;
import br.com.murilo.liberthia.util.MarkedPlayerEntry;
import br.com.murilo.liberthia.util.TeleportAnchor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenTeleportExecutorScreenS2CPacket {
    private final TeleportAnchor anchor;
    private final List<MarkedPlayerEntry> entries;

    public OpenTeleportExecutorScreenS2CPacket(TeleportAnchor anchor, List<MarkedPlayerEntry> entries) {
        this.anchor = anchor;
        this.entries = entries;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(anchor.dimension().location());
        buf.writeDouble(anchor.x());
        buf.writeDouble(anchor.y());
        buf.writeDouble(anchor.z());
        buf.writeVarInt(entries.size());
        for (MarkedPlayerEntry entry : entries) {
            buf.writeUUID(entry.uuid());
            buf.writeUtf(entry.name());
        }
    }

    public static OpenTeleportExecutorScreenS2CPacket decode(FriendlyByteBuf buf) {
        ResourceLocation dimensionLocation = buf.readResourceLocation();
        TeleportAnchor anchor = new TeleportAnchor(
                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionLocation),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
        int size = buf.readVarInt();
        List<MarkedPlayerEntry> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf();
            entries.add(new MarkedPlayerEntry(uuid, name));
        }
        return new OpenTeleportExecutorScreenS2CPacket(anchor, entries);
    }

    public static void handle(OpenTeleportExecutorScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> Minecraft.getInstance().setScreen(new TeleportExecutorScreen(msg.anchor, msg.entries)));
        ctx.get().setPacketHandled(true);
    }
}
