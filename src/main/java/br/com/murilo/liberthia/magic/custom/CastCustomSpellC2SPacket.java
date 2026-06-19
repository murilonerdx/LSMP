package br.com.murilo.liberthia.magic.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * v0.1.22 r42: C2S — client manda o UUID do spell a castar.
 *
 * <p>Server valida que o player CONHECE esse spell (anti-cheat) e roda
 * CustomSpellExecutor.cast.
 */
public class CastCustomSpellC2SPacket {

    private final UUID spellId;

    public CastCustomSpellC2SPacket(UUID spellId) {
        this.spellId = spellId;
    }

    public static void encode(CastCustomSpellC2SPacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.spellId);
    }

    public static CastCustomSpellC2SPacket decode(FriendlyByteBuf buf) {
        return new CastCustomSpellC2SPacket(buf.readUUID());
    }

    public static void handle(CastCustomSpellC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            CustomSpell spell = CustomSpellStorage.find(sender, p.spellId);
            if (spell == null) {
                sender.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§cFeitiço desconhecido."), true);
                return;
            }
            CustomSpellExecutor.cast(sender, spell);
        });
        ctx.get().setPacketHandled(true);
    }
}
