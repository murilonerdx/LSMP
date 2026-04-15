package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.HorusEffectManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Olho de Horus — clique direito em qualquer entidade viva para aplicar efeito de perseguição.
 * Shift+clique = remove efeito. Funciona em players E mobs.
 * Se clicar no ar olhando pra entidade, usa raycast.
 */
public class EyeOfHorusItem extends Item {

    public EyeOfHorusItem(Properties properties) {
        super(properties);
    }

    /* Clique direto numa entidade */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        if (target instanceof ServerPlayer targetPlayer) {
            if (player.isShiftKeyDown()) {
                HorusEffectManager.remove(targetPlayer.getUUID());
            } else {
                HorusEffectManager.apply(targetPlayer, 20 * 30);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /* Clique no ar — raycast pra achar entidade */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer admin)) {
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        ServerLevel sLevel = admin.serverLevel();
        Vec3 eye = admin.getEyePosition();
        Vec3 look = admin.getLookAngle();

        // Procura qualquer ServerPlayer no cone de visão (50 blocos, 25 graus)
        ServerPlayer closest = null;
        double closestDist = Double.MAX_VALUE;
        for (ServerPlayer sp : sLevel.players()) {
            if (sp == admin) continue;
            Vec3 toTarget = sp.position().add(0, sp.getEyeHeight() * 0.5, 0).subtract(eye);
            double dist = toTarget.length();
            if (dist > 50) continue;
            double dot = toTarget.normalize().dot(look.normalize());
            if (dot > 0.85 && dist < closestDist) {
                closestDist = dist;
                closest = sp;
            }
        }

        if (closest != null) {
            if (admin.isShiftKeyDown()) {
                HorusEffectManager.remove(closest.getUUID());
            } else {
                HorusEffectManager.apply(closest, 20 * 30);
            }
        } else if (!admin.isShiftKeyDown()) {
            // Sem alvo? aplica em si mesmo pra teste
            HorusEffectManager.apply(admin, 20 * 30);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("O Olho observa...").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Clique: perseguir player 30s").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sem alvo: aplica em si mesmo").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift+Clique: remover efeito").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
