package br.com.murilo.liberthia.magic.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * v0.1.22 r42: S2C — server manda lista completa de custom spells + UUID
 * selecionado pro client cachear pra exibir na spell wheel.
 */
public class SyncCustomSpellsS2CPacket {

    public final List<CustomSpell> spells;
    public final UUID selectedId;

    public SyncCustomSpellsS2CPacket(List<CustomSpell> spells, UUID selectedId) {
        this.spells = spells;
        this.selectedId = selectedId;
    }

    public static void encode(SyncCustomSpellsS2CPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.spells.size());
        for (CustomSpell s : p.spells) s.encode(buf);
        buf.writeBoolean(p.selectedId != null);
        if (p.selectedId != null) buf.writeUUID(p.selectedId);
    }

    public static SyncCustomSpellsS2CPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<CustomSpell> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(CustomSpell.decode(buf));
        UUID sel = buf.readBoolean() ? buf.readUUID() : null;
        return new SyncCustomSpellsS2CPacket(list, sel);
    }

    public static void handle(SyncCustomSpellsS2CPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                CustomSpellClientCache.update(p.spells, p.selectedId);
            } catch (Throwable t) {
                br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                        "[CustomSpell] sync error: {}", t.toString());
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
