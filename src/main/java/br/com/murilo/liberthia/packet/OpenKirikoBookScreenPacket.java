package br.com.murilo.liberthia.packet;


import br.com.murilo.liberthia.client.screen.ClientKirikoBookScreenHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenKirikoBookScreenPacket {

    private final List<DimensionEntry> dimensions;
    private final List<PlayerEntry> players;

    public OpenKirikoBookScreenPacket(List<DimensionEntry> dimensions, List<PlayerEntry> players) {
        this.dimensions = dimensions;
        this.players = players;
    }

    public static void encode(OpenKirikoBookScreenPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.dimensions.size());

        for (DimensionEntry entry : packet.dimensions) {
            buf.writeUtf(entry.dimensionId());
            buf.writeDouble(entry.x());
            buf.writeDouble(entry.y());
            buf.writeDouble(entry.z());
        }

        buf.writeInt(packet.players.size());

        for (PlayerEntry entry : packet.players) {
            buf.writeUtf(entry.name());
        }
    }

    public static OpenKirikoBookScreenPacket decode(FriendlyByteBuf buf) {
        int dimensionSize = buf.readInt();
        List<DimensionEntry> dimensions = new ArrayList<>();

        for (int i = 0; i < dimensionSize; i++) {
            String id = buf.readUtf();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();

            dimensions.add(new DimensionEntry(id, x, y, z));
        }

        int playerSize = buf.readInt();
        List<PlayerEntry> players = new ArrayList<>();

        for (int i = 0; i < playerSize; i++) {
            players.add(new PlayerEntry(buf.readUtf()));
        }

        return new OpenKirikoBookScreenPacket(dimensions, players);
    }

    public static void handle(OpenKirikoBookScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientKirikoBookScreenHandler.open(packet.dimensions, packet.players)
        ));

        context.setPacketHandled(true);
    }

    public record DimensionEntry(String dimensionId, double x, double y, double z) {
    }

    public record PlayerEntry(String name) {
    }

    public record TeleportPos(double x, double y, double z) {
    }
}
