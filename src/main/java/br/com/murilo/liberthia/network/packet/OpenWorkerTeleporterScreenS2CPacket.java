package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.util.MarkedPlayerEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * S2C packet opening the Worker Teleporter picker screen with the list of online
 * players the caller can teleport to. Mirrors the design of
 * {@link OpenTeleportExecutorScreenS2CPacket} — the actual screen-opening call
 * is routed through a client-only dispatch class so this packet is safe to load
 * on the dedicated server.
 */
public class OpenWorkerTeleporterScreenS2CPacket {
    private final List<MarkedPlayerEntry> players;

    public OpenWorkerTeleporterScreenS2CPacket(List<MarkedPlayerEntry> players) {
        this.players = players;
    }

    public List<MarkedPlayerEntry> getPlayers() {
        return players;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(players.size());
        for (MarkedPlayerEntry entry : players) {
            buf.writeUUID(entry.uuid());
            buf.writeUtf(entry.name());
        }
    }

    public static OpenWorkerTeleporterScreenS2CPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<MarkedPlayerEntry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf();
            list.add(new MarkedPlayerEntry(uuid, name));
        }
        return new OpenWorkerTeleporterScreenS2CPacket(list);
    }

    public static void handle(OpenWorkerTeleporterScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.ClientWorkerTeleporterDispatch.open(msg)));
        ctx.get().setPacketHandled(true);
    }
}
