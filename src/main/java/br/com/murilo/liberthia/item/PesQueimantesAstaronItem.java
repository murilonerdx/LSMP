package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pés Queimantes de Astaron — equipável no slot Curios {@code feet}.
 *
 * <ul>
 *   <li>Equipado: Speed III permanente.</li>
 *   <li>Coloca um bloco de FIRE no chão a cada bloco caminhado (se ar abaixo).</li>
 *   <li>Mobs/players no fogo: Weakness II + Slowness II + dano de fogo.</li>
 *   <li>On equip: spawna {@code flame_key} na mão (toggle do fogo).</li>
 * </ul>
 */
public class PesQueimantesAstaronItem extends Item {

    public PesQueimantesAstaronItem(Properties props) {
        super(props);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Pés Queimantes de Astaron")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Pés feitos de magma puro forjados por Astaron.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Slot Curios: feet")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§c• Speed III permanente"));
        tooltip.add(Component.literal("§c• Deixa rastro de fogo no chão"));
        tooltip.add(Component.literal("§c• Inimigos sobre o fogo: Weakness + Slowness"));
    }
}
