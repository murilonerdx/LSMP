package br.com.murilo.liberthia.network;

import br.com.murilo.liberthia.client.ClientMatterEnergyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CMatterEnergySyncPacket {
    private final int darkEnergy;
    private final int clearEnergy;
    private final int yellowEnergy;
    private final boolean stabilized;

    public S2CMatterEnergySyncPacket(int darkEnergy, int clearEnergy, int yellowEnergy, boolean stabilized) {
        this.darkEnergy = darkEnergy;
        this.clearEnergy = clearEnergy;
        this.yellowEnergy = yellowEnergy;
        this.stabilized = stabilized;
    }

    public static void encode(S2CMatterEnergySyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.darkEnergy);
        buf.writeInt(packet.clearEnergy);
        buf.writeInt(packet.yellowEnergy);
        buf.writeBoolean(packet.stabilized);
    }

    public static S2CMatterEnergySyncPacket decode(FriendlyByteBuf buf) {
        return new S2CMatterEnergySyncPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(S2CMatterEnergySyncPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ClientMatterEnergyState.set(packet.darkEnergy, packet.clearEnergy, packet.yellowEnergy, packet.stabilized);
        });
        ctx.setPacketHandled(true);
    }
}
