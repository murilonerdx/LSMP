package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.BloodSyringeItem;
import br.com.murilo.liberthia.matter.MatterContent;
import br.com.murilo.liberthia.menu.MatterAnalyzerMenu;
import br.com.murilo.liberthia.storage.ComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S — exporta a leitura atual do Matter Analyzer (sangue ou matéria) pra um
 * <b>Computador colocado ao lado do Analyzer</b>, como um novo arquivo de
 * relatório. Sem computador adjacente → avisa pra colocar um.
 */
public class AnalyzerToComputerC2SPacket {

    public AnalyzerToComputerC2SPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static AnalyzerToComputerC2SPacket decode(FriendlyByteBuf buf) {
        return new AnalyzerToComputerC2SPacket();
    }

    public static void handle(AnalyzerToComputerC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;

            if (!(sp.containerMenu instanceof MatterAnalyzerMenu menu)) {
                sp.displayClientMessage(Component.literal("§cAbra o Matter Analyzer primeiro."), true);
                return;
            }
            ItemStack input = menu.getInputStack();
            if (input.isEmpty()) {
                sp.displayClientMessage(Component.literal("§eSlot do analyzer vazio."), true);
                return;
            }

            // Procura um Computador adjacente ao Analyzer (6 vizinhos).
            BlockPos analyzerPos = menu.getAnalyzerPos();
            ComputerBlockEntity comp = null;
            for (Direction d : Direction.values()) {
                BlockEntity be = sp.level().getBlockEntity(analyzerPos.relative(d));
                if (be instanceof ComputerBlockEntity c) { comp = c; break; }
            }
            if (comp == null) {
                sp.displayClientMessage(Component.literal("§cColoque um Computador ao lado do Analyzer."), true);
                return;
            }

            String name;
            String body;
            if (input.getItem() instanceof BloodSyringeItem && BloodSyringeItem.isFilled(input)) {
                String src = BloodSyringeItem.getSource(input);
                if (src.isEmpty()) src = "?";
                int inf = BloodSyringeItem.getStoredInfection(input);
                if (inf < 0) inf = 0;
                var uuid = BloodSyringeItem.getSourceUuid(input);
                String dna = uuid != null ? uuid.toString().substring(0, 8).toUpperCase() : "????????";
                name = "sangue_" + src;
                body = "§4§l=== RELATORIO DE SANGUE ===§r\n"
                        + "§7Fonte: §f" + src + "\n"
                        + "§7DNA: §c" + dna + "\n"
                        + "§7Infeccao: §c" + inf + "%\n"
                        + "§7--- Perfil de Materia ---\n"
                        + "§7DM: §d" + fmt(BloodSyringeItem.getDark(input)) + "\n"
                        + "§7WM: §f" + fmt(BloodSyringeItem.getWhite(input)) + "\n"
                        + "§7YM: §e" + fmt(BloodSyringeItem.getYellow(input));
            } else {
                MatterContent c = menu.currentContent();
                String item = input.getHoverName().getString();
                name = "materia_" + item;
                body = "§5§l=== RELATORIO DE MATERIA ===§r\n"
                        + "§7Amostra: §f" + item + "\n"
                        + "§7--- Composicao ---\n"
                        + "§7DM: §d" + fmt(c.dark()) + "\n"
                        + "§7WM: §f" + fmt(c.white()) + "\n"
                        + "§7YM: §e" + fmt(c.yellow()) + "\n"
                        + "§7Mutacao: §d" + c.dominantMutation().displayName + "\n"
                        + "§7Energia-eq: §6" + c.energyEquivalent() + " FE";
            }

            if (!comp.appendReport(name, body)) {
                String why = !comp.hasHd() ? "§cInsira um HD no Computador pra salvar relatórios."
                        : !comp.hasEnergy(ComputerBlockEntity.SAVE_COST) ? "§cComputador sem energia."
                        : "§cHD cheio.";
                sp.displayClientMessage(Component.literal(why), true);
                return;
            }
            sp.displayClientMessage(Component.literal("§a✔ Relatório salvo no Computador: §f" + name), true);
        });
        ctx.get().setPacketHandled(true);
    }

    private static String fmt(float v) {
        return String.format("%.0f", v);
    }
}
