package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.CommandRunner;
import br.com.murilo.liberthia.item.CommandTabletItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * C2S: saves the editor's contents back to the held Command Tablet.
 * Server validates op permissions and total size limits.
 */
public class SaveCommandTabletC2SPacket {
    private static final int MAX_LINES = 32;
    private static final int MAX_LEN = 256;

    private final List<String> commands;
    private final String label;
    private final String targetMode;
    private final String targetName;
    private final boolean verbose;

    public SaveCommandTabletC2SPacket(List<String> commands, String label,
                                      String targetMode, String targetName,
                                      boolean verbose) {
        this.commands = commands;
        this.label = label == null ? "" : label;
        this.targetMode = targetMode == null ? "self" : targetMode;
        this.targetName = targetName == null ? "" : targetName;
        this.verbose = verbose;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(Math.min(commands.size(), MAX_LINES));
        int n = Math.min(commands.size(), MAX_LINES);
        for (int i = 0; i < n; i++) buf.writeUtf(commands.get(i), MAX_LEN);
        buf.writeUtf(label, 64);
        buf.writeUtf(targetMode, 8);
        buf.writeUtf(targetName, 32);
        buf.writeBoolean(verbose);
    }

    public static SaveCommandTabletC2SPacket decode(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), MAX_LINES);
        List<String> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(buf.readUtf(MAX_LEN));
        return new SaveCommandTabletC2SPacket(list,
                buf.readUtf(64), buf.readUtf(8), buf.readUtf(32),
                buf.readBoolean());
    }

    public static void handle(SaveCommandTabletC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (!CommandRunner.isOp(sp)) {
                sp.displayClientMessage(
                        Component.literal("Apenas OPs podem editar Tabletes de Comando.")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }
            ItemStack held = sp.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(held.getItem() instanceof CommandTabletItem)) {
                held = sp.getItemInHand(InteractionHand.OFF_HAND);
            }
            if (!(held.getItem() instanceof CommandTabletItem)) return;

            CommandTabletItem.writeCommands(held, msg.commands);
            CommandTabletItem.writeLabel(held, msg.label);
            CommandTabletItem.writeTarget(held, msg.targetMode, msg.targetName);
            CommandTabletItem.writeVerbose(held, msg.verbose);

            sp.displayClientMessage(
                    Component.literal("Tablete salvo (" + msg.commands.size() + " linhas, "
                                    + (msg.verbose ? "verboso" : "silencioso") + ").")
                            .withStyle(ChatFormatting.GREEN), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
