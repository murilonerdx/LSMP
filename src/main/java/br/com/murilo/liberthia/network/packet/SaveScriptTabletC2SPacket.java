package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.CommandRunner;
import br.com.murilo.liberthia.item.script.ScriptTabletItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveScriptTabletC2SPacket {
    private final String source;
    private final String label;
    private final boolean verbose;

    public SaveScriptTabletC2SPacket(String source, String label, boolean verbose) {
        this.source = source == null ? "" : source;
        this.label = label == null ? "" : label;
        this.verbose = verbose;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(source, 16_000);
        buf.writeUtf(label, 64);
        buf.writeBoolean(verbose);
    }

    public static SaveScriptTabletC2SPacket decode(FriendlyByteBuf buf) {
        return new SaveScriptTabletC2SPacket(
                buf.readUtf(16_000), buf.readUtf(64), buf.readBoolean());
    }

    public static void handle(SaveScriptTabletC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (!CommandRunner.isOp(sp)) {
                sp.displayClientMessage(
                        Component.literal("Apenas OPs podem editar Tabletes de Script.")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }
            ItemStack held = sp.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(held.getItem() instanceof ScriptTabletItem)) {
                held = sp.getItemInHand(InteractionHand.OFF_HAND);
            }
            if (!(held.getItem() instanceof ScriptTabletItem)) return;

            ScriptTabletItem.writeSource(held, msg.source);
            ScriptTabletItem.writeLabel(held, msg.label);
            ScriptTabletItem.writeVerbose(held, msg.verbose);
            sp.displayClientMessage(
                    Component.literal("Script salvo (" + msg.source.length() + " chars, "
                                    + (msg.verbose ? "verboso" : "silencioso") + ").")
                            .withStyle(ChatFormatting.GREEN), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
