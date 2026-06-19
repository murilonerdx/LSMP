package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.storage.ComputerBlockEntity;
import br.com.murilo.liberthia.storage.PrinterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** C2S — Impressora imprime um relatório (ou todos) do Computador vizinho num livro. */
public class PrinterPrintC2SPacket {

    private final BlockPos pos;
    private final int index; // -1 = todos

    public PrinterPrintC2SPacket(BlockPos pos, int index) {
        this.pos = pos;
        this.index = index;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(index);
    }

    public static PrinterPrintC2SPacket decode(FriendlyByteBuf buf) {
        return new PrinterPrintC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(PrinterPrintC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (sp.distanceToSqr(msg.pos.getCenter()) > 64) return;
            if (!(sp.level().getBlockEntity(msg.pos) instanceof PrinterBlockEntity printer)) return;

            ComputerBlockEntity comp = printer.findComputer();
            if (comp == null) {
                sp.displayClientMessage(Component.literal("§cColoque um Computador ao lado da Impressora."), true);
                return;
            }
            if (printer.getPaper().isEmpty()) {
                sp.displayClientMessage(Component.literal("§cColoque papel no slot da Impressora."), true);
                return;
            }
            if (!printer.hasEnergy(PrinterBlockEntity.PRINT_COST)) {
                sp.displayClientMessage(Component.literal("§cImpressora sem energia. Conecte um cabo/bateria."), true);
                return;
            }
            List<ComputerData.Entry> files = ComputerData.fromList(comp.getFiles());
            if (files.isEmpty()) {
                sp.displayClientMessage(Component.literal("§eO Computador não tem relatórios."), true);
                return;
            }

            String title;
            StringBuilder content = new StringBuilder();
            if (msg.index >= 0 && msg.index < files.size()) {
                ComputerData.Entry e = files.get(msg.index);
                title = e.name;
                content.append(e.body);
            } else {
                title = "Relatorios Liberthia";
                for (ComputerData.Entry e : files) {
                    content.append("=== ").append(e.name).append(" ===\n").append(e.body).append("\n\n");
                }
            }

            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            CompoundTag bt = book.getOrCreateTag();
            bt.putString("title", title.length() > 32 ? title.substring(0, 32) : title);
            bt.putString("author", sp.getGameProfile().getName());
            ListTag pages = new ListTag();
            String text = content.toString();
            for (int i = 0; i < text.length() && pages.size() < 100; i += 250) {
                String chunk = text.substring(i, Math.min(text.length(), i + 250));
                pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(chunk))));
            }
            if (pages.isEmpty()) pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
            bt.put("pages", pages);
            bt.putBoolean("resolved", true);

            printer.getPaper().shrink(1);
            printer.useEnergy(PrinterBlockEntity.PRINT_COST);
            printer.setChanged();
            if (!sp.getInventory().add(book)) sp.drop(book, false);
            sp.displayClientMessage(Component.literal("§a✔ Impresso: §f" + title), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
