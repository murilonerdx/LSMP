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
 * v0.1.22: Selo de Passagem (Passage Sigil).
 *
 * <p>Quando o jogador tem este item em <b>qualquer slot do inventário</b>
 * (hotbar, main, offhand ou armor), as seguintes ameaças <b>ignoram-no</b>:
 * <ul>
 *   <li>Blocos atacantes: withering_eye, venom_geyser, lightning_coil,
 *       lightning_node, screaming_soul, magnetic_pylon.</li>
 *   <li>Criaturas de sangue: flesh_mother_boss, blood_warden_boss,
 *       disarmer, blood_cultist, blood_priest, flesh_crawler, blood_mage,
 *       blood_hound, gore_worm, wounded_pilgrim, weaving_shade,
 *       possessed_zombie, possessed_skeleton.</li>
 * </ul>
 *
 * <p>Não tem cooldown nem consome durabilidade — só precisa estar no inventário.
 * Stack único, rareza ÉPICA. Pra obter, deve ser via comando admin ou loot
 * raro (drop de bosses).
 *
 * <p>Helper de checagem: {@link br.com.murilo.liberthia.logic.BloodKinPassage#hasSigil}.
 */
public class PassageSigilItem extends Item {

    public PassageSigilItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // brilho enchanted permanente
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("⊕ Selo de Passagem")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Criaturas de sangue e blocos hostis")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("ignoram você enquanto estiver no inventário.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Não tem cooldown nem consome — só estar carregando basta.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
