package br.com.murilo.liberthia.magic.spell.hotbar;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.162 r138: <b>SpellHotbarSyncS2CPacket</b> — sync dos 3 slots do hotbar
 * de spells. Mandado quando muda (bind/clear) e no login.
 *
 * <p>Cliente armazena em {@link ClientSpellHotbar} pra HUD renderizar.
 */
public class SpellHotbarSyncS2CPacket {

    public final String[] spellIds; // tamanho 3

    public SpellHotbarSyncS2CPacket(String[] spellIds) {
        this.spellIds = spellIds;
    }

    public static SpellHotbarSyncS2CPacket snapshot(Player player) {
        String[] arr = new String[SpellHotbarData.SLOTS];
        for (int i = 0; i < SpellHotbarData.SLOTS; i++) {
            String id = SpellHotbarData.getSpellId(player, i);
            arr[i] = id == null ? "" : id;
        }
        return new SpellHotbarSyncS2CPacket(arr);
    }

    public static void encode(SpellHotbarSyncS2CPacket pkt, FriendlyByteBuf buf) {
        for (int i = 0; i < SpellHotbarData.SLOTS; i++) {
            buf.writeUtf(pkt.spellIds[i] == null ? "" : pkt.spellIds[i]);
        }
    }

    public static SpellHotbarSyncS2CPacket decode(FriendlyByteBuf buf) {
        String[] arr = new String[SpellHotbarData.SLOTS];
        for (int i = 0; i < SpellHotbarData.SLOTS; i++) {
            arr[i] = buf.readUtf(64);
        }
        return new SpellHotbarSyncS2CPacket(arr);
    }

    public static void handle(SpellHotbarSyncS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.apply(pkt)));
        ctx.get().setPacketHandled(true);
    }

    /** Client-only — classe SEPARADA (server-safe). */
    private static final class Client {
        static void apply(SpellHotbarSyncS2CPacket pkt) {
            if (Minecraft.getInstance().player != null) {
                ClientSpellHotbar.update(pkt.spellIds);
            }
        }
    }
}
