package br.com.murilo.liberthia.magic.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * v0.1.22 r42: C2S — client seleciona um spell pra ser o "ativo" via wheel.
 */
public class SelectSpellC2SPacket {

    private final UUID spellId;
    private final boolean clear;

    public SelectSpellC2SPacket(UUID spellId) {
        this.spellId = spellId;
        this.clear = spellId == null;
    }

    public static void encode(SelectSpellC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.clear);
        if (!p.clear) buf.writeUUID(p.spellId);
    }

    public static SelectSpellC2SPacket decode(FriendlyByteBuf buf) {
        boolean clear = buf.readBoolean();
        return new SelectSpellC2SPacket(clear ? null : buf.readUUID());
    }

    public static void handle(SelectSpellC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            CustomSpellStorage.setSelected(sender, p.spellId);
        });
        ctx.get().setPacketHandled(true);
    }
}
