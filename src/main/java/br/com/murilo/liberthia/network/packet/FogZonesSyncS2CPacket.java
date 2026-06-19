package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.fog.FogZone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server → client: lista completa das zonas de neblina ativas. Enviado no login
 * e sempre que alguma zona é criada/removida. Resync total (são poucas zonas).
 */
public class FogZonesSyncS2CPacket {

    private final List<FogZone> zones;

    public FogZonesSyncS2CPacket(List<FogZone> zones) {
        this.zones = zones;
    }

    public static void encode(FogZonesSyncS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.zones.size());
        for (FogZone z : pkt.zones) z.write(buf);
    }

    public static FogZonesSyncS2CPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<FogZone> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(FogZone.read(buf));
        return new FogZonesSyncS2CPacket(list);
    }

    public static void handle(FogZonesSyncS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                br.com.murilo.liberthia.client.fog.ClientFogZones.set(pkt.zones))
        );
        ctx.get().setPacketHandled(true);
    }
}
