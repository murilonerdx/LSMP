package br.com.murilo.liberthia.network;

import br.com.murilo.liberthia.client.ClientInfectionState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record S2CInfectionSyncPacket(
        int infection,
        int permanentHealthPenalty,
        int stage,
        int rawExposure,
        int blockedExposure,
        int armorProtectionPercent,
        String mutations,
        float chunkDensity
) {
    public S2CInfectionSyncPacket(int infection, int permanentHealthPenalty, int stage,
                                   int rawExposure, int blockedExposure, int armorProtectionPercent,
                                   String mutations) {
        this(infection, permanentHealthPenalty, stage, rawExposure, blockedExposure,
                armorProtectionPercent, mutations, 0.0f);
    }

    public static void encode(S2CInfectionSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.infection);
        buffer.writeInt(packet.permanentHealthPenalty);
        buffer.writeInt(packet.stage);
        buffer.writeInt(packet.rawExposure);
        buffer.writeInt(packet.blockedExposure);
        buffer.writeInt(packet.armorProtectionPercent);
        buffer.writeUtf(packet.mutations != null ? packet.mutations : "");
        buffer.writeFloat(packet.chunkDensity);
    }

    public static S2CInfectionSyncPacket decode(FriendlyByteBuf buffer) {
        return new S2CInfectionSyncPacket(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(),
                buffer.readFloat()
        );
    }

    public static void handle(S2CInfectionSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientInfectionState.apply(packet)));
        context.setPacketHandled(true);
    }
}