package br.com.murilo.liberthia.magic.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * v0.1.22 r42: C2S — deleta um custom spell salvo.
 */
public class DeleteSpellC2SPacket {

    private final UUID spellId;

    public DeleteSpellC2SPacket(UUID spellId) {
        this.spellId = spellId;
    }

    public static void encode(DeleteSpellC2SPacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.spellId);
    }

    public static DeleteSpellC2SPacket decode(FriendlyByteBuf buf) {
        return new DeleteSpellC2SPacket(buf.readUUID());
    }

    public static void handle(DeleteSpellC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            if (CustomSpellStorage.remove(sender, p.spellId)) {
                br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sender,
                        new SyncCustomSpellsS2CPacket(CustomSpellStorage.getAll(sender),
                                CustomSpellStorage.getSelectedId(sender)));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
