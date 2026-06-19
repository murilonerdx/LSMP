package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.client.AstaronClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r187 — <b>Escala de Astaron</b>: medidor da radiação dimensional do mundo e do nº de criaturas
 * cósmicas presentes. Lê o estado sincronizado ({@code AstaronClientState}) e mostra no tooltip + HUD.
 */
public class AstaronScaleItem extends Item {
    public AstaronScaleItem(Properties props) { super(props); }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (level == null || !level.isClientSide) return;
        int rad = AstaronClientState.radiation, count = AstaronClientState.cosmicCount;
        tip.add(Component.literal("✦ Escala de Astaron").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        tip.add(Component.literal("§7Radiação Dimensional: ")
                .append(Component.literal(rad + "/100").withStyle(
                        rad >= 80 ? ChatFormatting.RED : rad >= 50 ? ChatFormatting.GOLD : ChatFormatting.GREEN)));
        tip.add(Component.literal("§7Criaturas Cósmicas: §d" + count));
        String tier = rad >= 90 ? "§4COLAPSO IMINENTE" : rad >= 70 ? "§6CRÍTICO"
                : rad >= 40 ? "§eELEVADO" : rad >= 15 ? "§aESTÁVEL" : "§2DORMENTE";
        tip.add(Component.literal("§7Estado: " + tier));
    }
}
