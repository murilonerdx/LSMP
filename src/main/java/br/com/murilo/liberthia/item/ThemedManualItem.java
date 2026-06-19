package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.manual.ManualContent;
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
 * r180: <b>Livro temático</b> — abre o manual filtrado por categoria (Horror Cósmico /
 * Matérias & Máquinas / Magia). Divide o manual gigante em 3 tomos focados (pedido #?
 * do gabisousa02). Capítulos gerais (intro, referência, receitas) aparecem nos 3.
 */
public class ThemedManualItem extends Item {

    private final ManualContent.Category category;
    private final String subtitle;

    public ThemedManualItem(Properties props, ManualContent.Category category, String subtitle) {
        super(props);
        this.category = category;
        this.subtitle = subtitle;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT) {
            try {
                ClientOpener.open(category);
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.error("[ThemedManual] FAILED to open", t);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(subtitle).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Right-click pra abrir").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    private static final class ClientOpener {
        static void open(ManualContent.Category cat) {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new br.com.murilo.liberthia.client.screen.LiberthiaManualScreen(cat));
        }
    }
}
