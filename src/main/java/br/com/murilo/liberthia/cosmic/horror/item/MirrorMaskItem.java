package br.com.murilo.liberthia.cosmic.horror.item;

import br.com.murilo.liberthia.cosmic.framework.HorrorFramework;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r81 — <b>Mirror Mask</b> (Uncanny Valley artifact).
 *
 * <p>Right-click: apply GLOWING effect a outros players próximos pra dar
 * a impressão de "outro player te encontrou". Adiciona exposição UNCANNY
 * massiva neles. Você fica brevemente invisível pra eles também.
 *
 * <p>Cooldown: 1 minuto. Não é consumido.
 */
public class MirrorMaskItem extends Item {

    public MirrorMaskItem(Properties props) {
        super(props.stacksTo(1).durability(0));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            sp.displayClientMessage(Component.literal("§5✦ A máscara repousa..."), true);
            return InteractionResultHolder.fail(stack);
        }

        // Pega outros players num raio de 30
        List<ServerPlayer> nearby = sp.serverLevel().getEntitiesOfClass(
                ServerPlayer.class, sp.getBoundingBox().inflate(30));
        int affected = 0;
        for (ServerPlayer other : nearby) {
            if (other == sp) continue;
            // Boost de exposição uncanny — eles "viram outro player aparecer"
            HorrorFramework.getState(other).addExposure(HorrorType.UNCANNY, 25.0F);
            // Glowing + brief blindness na vítima
            other.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, 30, 0, true, false));
            other.displayClientMessage(Component.literal(
                    "§8§o✦ algo com o rosto de §r§7" + sp.getName().getString()
                    + "§r§8§o aparece..."), true);
            affected++;
        }

        // Usuário: brief invisibility
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.INVISIBILITY, 100, 0, true, false));
        sp.getCooldowns().addCooldown(this, 1200);

        if (affected > 0) {
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ A máscara espelha. §r§5§o" + affected + " §r§5§omentes vacilam."), false);
        } else {
            sp.displayClientMessage(Component.literal(
                    "§8§oNão há ninguém pra ver você."), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Right-click: imitar outro player."));
        tooltip.add(Component.literal("§5§oAplica horror UNCANNY massivo a quem te vê."));
        tooltip.add(Component.literal("§8§oCooldown: 60s"));
    }
}
