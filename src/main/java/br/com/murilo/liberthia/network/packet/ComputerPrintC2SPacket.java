package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.storage.ComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** C2S — imprime os dados num livro (precisa de Impressora adjacente + papel). */
public class ComputerPrintC2SPacket {

    private final BlockPos pos;
    private final int index; // -1 = todos

    public ComputerPrintC2SPacket(BlockPos pos, int index) {
        this.pos = pos;
        this.index = index;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(index);
    }

    public static ComputerPrintC2SPacket decode(FriendlyByteBuf buf) {
        return new ComputerPrintC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ComputerPrintC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (sp.distanceToSqr(msg.pos.getCenter()) > 64) return;
            if (!(sp.level().getBlockEntity(msg.pos) instanceof ComputerBlockEntity cbe)) return;
            if (cbe.isLocked(sp)) {
                sp.displayClientMessage(Component.literal("§cAcesso negado."), true);
                return;
            }

            boolean printer = false;
            for (Direction d : Direction.values()) {
                if (sp.level().getBlockState(msg.pos.relative(d)).is(ModBlocks.PRINTER.get())) {
                    printer = true;
                    break;
                }
            }
            if (!printer) {
                sp.displayClientMessage(Component.literal("§cColoque uma Impressora ao lado do Computador."), true);
                return;
            }

            int paperSlot = -1;
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                if (sp.getInventory().getItem(i).is(Items.PAPER)) { paperSlot = i; break; }
            }
            if (paperSlot < 0) {
                sp.displayClientMessage(Component.literal("§cVocê precisa de papel."), true);
                return;
            }

            List<ComputerData.Entry> files = ComputerData.fromList(cbe.getFiles());
            if (files.isEmpty()) {
                sp.displayClientMessage(Component.literal("§eNenhum dado pra imprimir."), true);
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

            sp.getInventory().getItem(paperSlot).shrink(1);
            if (!sp.getInventory().add(book)) sp.drop(book, false);
            sp.displayClientMessage(Component.literal("§a✔ Impresso: §f" + title), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
