package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
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
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Liberthia Manual — abre a tela GUI custom multi-capítulo.
 *
 * <p>r153: SIMPLIFICADO — tirei toda a lógica de Patchouli que estava
 * falhando silenciosamente. Agora SEMPRE abre nossa screen no client.
 *
 * <p>Right-click client-side direto, sem ida-volta server-client.
 */
public class LiberthiaManualItem extends Item {

    public LiberthiaManualItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // r153: SÓ client-side. Server retorna sucesso e fica quieto.
        if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT) {
            LiberthiaMod.LOGGER.info("[LiberthiaManual] Opening custom screen...");
            try {
                ClientOpener.open();
                LiberthiaMod.LOGGER.info("[LiberthiaManual] Screen opened OK");
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.error("[LiberthiaManual] FAILED to open screen", t);
                player.displayClientMessage(
                        Component.literal("§c⚠ Erro abrindo manual: " + t.getClass().getSimpleName()
                                + " — veja log").withStyle(ChatFormatting.RED),
                        false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Manual completo do mod").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click pra abrir").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("39 capítulos · 363 páginas").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    /** Wrapper isolado — JVM só carrega Screen quando este método for chamado. */
    private static final class ClientOpener {
        static void open() {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new br.com.murilo.liberthia.client.screen.LiberthiaManualScreen());
        }
    }
}
