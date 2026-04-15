package br.com.murilo.liberthia.entry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AdminInventoryS2CPacket {

    private final AdminInventorySnapshot snapshot;

    public AdminInventoryS2CPacket(AdminInventorySnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public AdminInventorySnapshot getSnapshot() {
        return snapshot;
    }

    public static void encode(AdminInventoryS2CPacket msg, FriendlyByteBuf buf) {
        msg.snapshot.encode(buf);
    }

    public static AdminInventoryS2CPacket decode(FriendlyByteBuf buf) {
        return new AdminInventoryS2CPacket(AdminInventorySnapshot.decode(buf));
    }

    public static void handle(AdminInventoryS2CPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleInventory(msg))
        );

        ctx.setPacketHandled(true);
    }
}
