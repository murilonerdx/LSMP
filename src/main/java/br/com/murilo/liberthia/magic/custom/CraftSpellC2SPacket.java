package br.com.murilo.liberthia.magic.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * v0.1.22 r42: C2S — client envia configuração de uma nova custom spell pra
 * server salvar no storage do player.
 */
public class CraftSpellC2SPacket {

    private final String name;
    private final byte sprite;
    private final byte shape;
    private final byte element;
    private final byte power;

    public CraftSpellC2SPacket(String name, SpellSprite sprite, SpellShape shape,
                                SpellElement element, int power) {
        this.name = name;
        this.sprite = (byte) sprite.ordinal();
        this.shape = (byte) shape.ordinal();
        this.element = (byte) element.ordinal();
        this.power = (byte) Math.max(1, Math.min(5, power));
    }

    public CraftSpellC2SPacket(String name, byte sprite, byte shape, byte element, byte power) {
        this.name = name;
        this.sprite = sprite;
        this.shape = shape;
        this.element = element;
        this.power = power;
    }

    public static void encode(CraftSpellC2SPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.name, 32);
        buf.writeByte(p.sprite);
        buf.writeByte(p.shape);
        buf.writeByte(p.element);
        buf.writeByte(p.power);
    }

    public static CraftSpellC2SPacket decode(FriendlyByteBuf buf) {
        return new CraftSpellC2SPacket(buf.readUtf(32), buf.readByte(),
                buf.readByte(), buf.readByte(), buf.readByte());
    }

    public static void handle(CraftSpellC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            CustomSpell spell = new CustomSpell(
                    UUID.randomUUID(),
                    p.name,
                    SpellSprite.byOrdinal(p.sprite),
                    SpellShape.byOrdinal(p.shape),
                    SpellElement.byOrdinal(p.element),
                    p.power);
            CustomSpellStorage.add(sender, spell);
            sender.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§a✦ Feitiço §6" + spell.name + "§a criado!"), false);
            // Sync the new list back to client
            br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sender,
                    new SyncCustomSpellsS2CPacket(CustomSpellStorage.getAll(sender),
                            CustomSpellStorage.getSelectedId(sender)));
        });
        ctx.get().setPacketHandled(true);
    }
}
