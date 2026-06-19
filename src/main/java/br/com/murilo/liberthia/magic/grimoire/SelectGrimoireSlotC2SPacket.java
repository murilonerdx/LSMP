package br.com.murilo.liberthia.magic.grimoire;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r164: <b>SelectGrimoireSlotC2SPacket</b> — client manda quando player escolhe
 * um slot ativo no GrimoireWheelScreen.
 *
 * <p>Server resolve o grimoire na mão indicada e atualiza o NBT do active_slot.
 */
public class SelectGrimoireSlotC2SPacket {

    /** Slot ativo (0..8). */
    public final byte slot;
    /** {@code true} = main hand, {@code false} = off hand. */
    public final boolean mainHand;

    public SelectGrimoireSlotC2SPacket(byte slot, boolean mainHand) {
        this.slot = slot;
        this.mainHand = mainHand;
    }

    public static void encode(SelectGrimoireSlotC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.slot);
        buf.writeBoolean(pkt.mainHand);
    }

    public static SelectGrimoireSlotC2SPacket decode(FriendlyByteBuf buf) {
        return new SelectGrimoireSlotC2SPacket(buf.readByte(), buf.readBoolean());
    }

    public static void handle(SelectGrimoireSlotC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (pkt.slot < 0 || pkt.slot >= GrimoireBookItem.SCROLL_SLOTS) return;

            ItemStack stack = pkt.mainHand ? sp.getMainHandItem() : sp.getOffhandItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof GrimoireBookItem)) {
                // Fallback: procura em qualquer mão
                if (sp.getMainHandItem().getItem() instanceof GrimoireBookItem) {
                    stack = sp.getMainHandItem();
                } else if (sp.getOffhandItem().getItem() instanceof GrimoireBookItem) {
                    stack = sp.getOffhandItem();
                } else {
                    return; // sem grimoire na mão
                }
            }
            GrimoireBookItem.setActiveSlot(stack, pkt.slot);
        });
        ctx.get().setPacketHandled(true);
    }
}
