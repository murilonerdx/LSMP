package br.com.murilo.liberthia.entry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AdminPlayerListS2CPacket {

    private final List<AdminPlayerEntry> players;

    public AdminPlayerListS2CPacket(List<AdminPlayerEntry> players) {
        this.players = players;
    }

    public List<AdminPlayerEntry> getPlayers() {
        return players;
    }

    public static void encode(AdminPlayerListS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.players.size());
        for (AdminPlayerEntry entry : msg.players) {
            entry.encode(buf);
        }
    }

    public static AdminPlayerListS2CPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<AdminPlayerEntry> players = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            players.add(AdminPlayerEntry.decode(buf));
        }
        return new AdminPlayerListS2CPacket(players);
    }

    public static void handle(AdminPlayerListS2CPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handlePlayerList(msg))
        );

        ctx.setPacketHandled(true);
    }
}
