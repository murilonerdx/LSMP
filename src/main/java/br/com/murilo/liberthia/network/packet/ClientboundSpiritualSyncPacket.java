package br.com.murilo.liberthia.network.packet;


import br.com.murilo.liberthia.init.ClientSpiritualState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundSpiritualSyncPacket {

    private final Map<UUID, String> activePlayers;

    public ClientboundSpiritualSyncPacket(Map<UUID, String> activePlayers) {
        this.activePlayers = new HashMap<>(activePlayers);
    }

    public static void encode(ClientboundSpiritualSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.activePlayers.size());

        for (Map.Entry<UUID, String> entry : packet.activePlayers.entrySet()) {
            buffer.writeUUID(entry.getKey());
            buffer.writeUtf(entry.getValue());
        }
    }

    public static ClientboundSpiritualSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<UUID, String> activePlayers = new HashMap<>();

        for (int i = 0; i < size; i++) {
            UUID playerUuid = buffer.readUUID();
            String channelId = buffer.readUtf();

            activePlayers.put(playerUuid, channelId);
        }

        return new ClientboundSpiritualSyncPacket(activePlayers);
    }

    public static void handle(
            ClientboundSpiritualSyncPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientSpiritualState.setActivePlayers(packet.activePlayers)
                )
        );

        context.setPacketHandled(true);
    }
}
