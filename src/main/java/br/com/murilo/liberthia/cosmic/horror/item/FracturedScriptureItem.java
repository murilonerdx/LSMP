package br.com.murilo.liberthia.cosmic.horror.item;

import br.com.murilo.liberthia.cosmic.framework.HorrorFramework;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r81 — <b>Fractured Scripture</b> (Cognitive Horror).
 *
 * <p>Quando "lido" (right-click) o player ganha CONFUSION + HUNGER + brief
 * NAUSEA, e ganha 30 exposição COGNITIVE. Em troca, ganha 1 nivel de XP
 * (insight cósmico).
 *
 * <p>Pode ser usado quantas vezes quiser, mas o ganho de exposição é
 * cumulativo — abrir muito acaba travando a mente.
 */
public class FracturedScriptureItem extends Item {

    private static final String[] CORRUPT_FRAGMENTS = {
            "§8§o\"a circunferência é proibida\"",
            "§8§o\"o numero verdadeiro é zero\"",
            "§8§o\"olhem por cima\"",
            "§8§o\"voce ja foi o outro\"",
            "§8§o\"o som está atrás de você\"",
            "§8§o\"as cores caem para baixo\"",
            "§8§o\"o tempo é um animal vermelho\""
    };

    public FracturedScriptureItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // Efeitos: confusion + hunger
        sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, true, false));
        sp.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1, true, false));
        sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false));

        // Exposure massive
        HorrorFramework.getState(sp).addExposure(HorrorType.COGNITIVE, 30.0F);

        // XP — insight cósmico
        sp.giveExperiencePoints(50);

        // Fragmento aleatório em chat
        String fragment = CORRUPT_FRAGMENTS[(int)(Math.random() * CORRUPT_FRAGMENTS.length)];
        sp.displayClientMessage(Component.literal(fragment), false);

        sp.getCooldowns().addCooldown(this, 200);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7§oTexto fraturado. Ler causa instabilidade mental."));
        tooltip.add(Component.literal("§5Ganha §d50 XP §5+ horror COGNITIVE."));
        tooltip.add(Component.literal("§8§oCada uso piora sua sanidade."));
    }
}
