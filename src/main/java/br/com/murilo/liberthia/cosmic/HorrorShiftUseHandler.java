package br.com.murilo.liberthia.cosmic;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r179: <b>SHIFT + interagir com outro player</b> segurando um item de horror que
 * implementa {@link HorrorUsableOnOther} → aplica o efeito do item NO ALVO (como se
 * ele tivesse usado). Cooldown compartilhado com o uso normal do item.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class HorrorShiftUseHandler {

    private HorrorShiftUseHandler() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        Player user = e.getEntity();
        if (user.level().isClientSide) return;
        if (!user.isShiftKeyDown()) return;
        if (!(user instanceof ServerPlayer suser)) return;
        if (!(e.getTarget() instanceof ServerPlayer target)) return;

        ItemStack stack = e.getItemStack();
        if (!(stack.getItem() instanceof HorrorUsableOnOther horror)) return;

        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (suser.getCooldowns().isOnCooldown(stack.getItem())) return;
        if (target == suser) return;

        horror.useOnOther(suser, target);
        suser.getCooldowns().addCooldown(stack.getItem(), 40);
        suser.swing(e.getHand(), true);
        suser.displayClientMessage(Component.literal("§5✦ Você usa em §d"
                + target.getName().getString() + "§5..."), true);
    }
}
