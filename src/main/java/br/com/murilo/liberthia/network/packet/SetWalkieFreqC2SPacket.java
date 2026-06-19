package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.WalkieTalkieItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S — player confirma código secreto + estado on/off do Walkie Talkie.
 * Server acha o walkie no inventário e grava o NBT.
 */
public class SetWalkieFreqC2SPacket {

    private final String freq;
    private final boolean on;

    public SetWalkieFreqC2SPacket(String freq, boolean on) {
        this.freq = freq == null ? "" : freq;
        this.on = on;
    }

    public static void encode(SetWalkieFreqC2SPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.freq, 32);
        buf.writeBoolean(p.on);
    }

    public static SetWalkieFreqC2SPacket decode(FriendlyByteBuf buf) {
        return new SetWalkieFreqC2SPacket(buf.readUtf(32), buf.readBoolean());
    }

    public static void handle(SetWalkieFreqC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            ItemStack stack = findWalkie(sender);
            if (stack.isEmpty()) {
                sender.displayClientMessage(Component.literal(
                        "§cVocê não tem um Walkie Talkie no inventário."), true);
                return;
            }

            String safe = msg.freq.trim();
            if (safe.length() > 24) safe = safe.substring(0, 24);
            WalkieTalkieItem.setFreq(stack, safe);
            WalkieTalkieItem.setOn(stack, msg.on);

            sender.displayClientMessage(Component.translatable(
                            msg.on ? "item.liberthia.walkie_talkie.on"
                                    : "item.liberthia.walkie_talkie.off",
                            safe.isEmpty() ? "—" : safe)
                    .withStyle(msg.on ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        });
        ctx.get().setPacketHandled(true);
    }

    /** Procura o primeiro Walkie Talkie no inventário inteiro. */
    private static ItemStack findWalkie(ServerPlayer p) {
        ItemStack main = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof WalkieTalkieItem) return main;
        ItemStack off = p.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof WalkieTalkieItem) return off;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof WalkieTalkieItem) return s;
        }
        return ItemStack.EMPTY;
    }
}
