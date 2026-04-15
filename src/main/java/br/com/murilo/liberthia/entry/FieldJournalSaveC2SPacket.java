package br.com.murilo.liberthia.entry;

import br.com.murilo.liberthia.item.FieldJournalItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FieldJournalSaveC2SPacket {
    private final InteractionHand hand;
    private final String title;
    private final List<String> pages;

    public FieldJournalSaveC2SPacket(InteractionHand hand, String title, List<String> pages) {
        this.hand = hand;
        this.title = title;
        this.pages = pages;
    }

    public static void encode(FieldJournalSaveC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeUtf(msg.title, 60);
        buf.writeVarInt(Math.min(msg.pages.size(), FieldJournalItem.MAX_PAGES));
        for (int i = 0; i < Math.min(msg.pages.size(), FieldJournalItem.MAX_PAGES); i++) {
            buf.writeUtf(msg.pages.get(i), 512);
        }
    }

    public static FieldJournalSaveC2SPacket decode(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        String title = buf.readUtf(60);
        int count = buf.readVarInt();
        List<String> pages = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pages.add(buf.readUtf(512));
        }
        return new FieldJournalSaveC2SPacket(hand, title, pages);
    }

    public static void handle(FieldJournalSaveC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            // qualquer portador pode editar

            ItemStack stack = sender.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof FieldJournalItem)) return;

            CompoundTag tag = stack.getOrCreateTag();
            FieldJournalItem.setPages(tag, msg.pages, msg.title);
        });
        ctx.get().setPacketHandled(true);
    }
}
