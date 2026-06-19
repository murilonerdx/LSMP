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
 * v0.1.22 r23: Maddening Gaze — Olhar Enlouquecedor.
 *
 * <p>Item passivo. Quando holder OLHA DIRETAMENTE pra outro player a < 15
 * blocos, o alvo:
 * <ul>
 *   <li>Recebe Nausea II + Slowness II + Wither I por 8s</li>
 *   <li>Vê texto vermelho piscando: "VOCÊ ESTÁ ENLOUQUECENDO — FUJA"</li>
 *   <li>Perde 1 HP/s enquanto olhado</li>
 *   <li>Tela treme/flicka</li>
 * </ul>
 *
 * <p>Mecânica em {@link br.com.murilo.liberthia.event.MadnessEvents#onPlayerTickGaze}.
 */
public class MaddeningGazeItem extends Item {

    public static final double MAX_DISTANCE = 15.0;
    /** Cos do ângulo máximo entre olhar do holder e direção do target.
     *  0.93 ≈ 21° de cone — bem direto, exige mirar. */
    public static final double GAZE_DOT_THRESHOLD = 0.93;

    public MaddeningGazeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§4§oOlhar Enlouquecedor").withStyle(ChatFormatting.ITALIC));
        tip.add(Component.literal("§7Olhar diretamente pra outro player <§d15 blocos§r§7"));
        tip.add(Component.literal("§7faz a §4mente dele despedaçar§r§7."));
        tip.add(Component.empty());
        tip.add(Component.literal("§8§oNausea + Wither + 'FUJA' piscando na tela do alvo."));
    }
}
