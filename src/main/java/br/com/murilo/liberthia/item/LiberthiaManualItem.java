package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Liberthia Manual — abre uma tela GUI completa (multi-capítulo) com toda a
 * documentação do mod, lore, mutações e mecânicas.
 *
 * <p>Right-click. Cliente apenas.
 */
public class LiberthiaManualItem extends Item {

    public LiberthiaManualItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientOpener::open);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Manual completo do mod").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click pra abrir").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    /** Wrapper pra evitar carregar Screen no servidor dedicado. */
    private static final class ClientOpener {
        static void open() {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new br.com.murilo.liberthia.client.screen.LiberthiaManualScreen());
        }
    }
}
