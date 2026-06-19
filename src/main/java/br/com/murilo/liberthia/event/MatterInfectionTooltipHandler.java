package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Adiciona tooltip e marca de aviso em items com a tag NBT {@code MatterInfected}.
 *
 * <p>A tag é aplicada pelo {@code DarkMatterAlchemizerBlockEntity} ao gerar
 * recompensas — esses items carregam resíduo de matéria escura e causam
 * efeitos colaterais enquanto carregados no inventário (ver
 * {@link InfectedItemPassiveHandler}). A purificação acontece no
 * {@code MatterPurifierBlockEntity} ao custo de 100k FE.
 *
 * <p>Subscribed no FORGE bus pelo lado CLIENT — {@link ItemTooltipEvent} só
 * dispara no cliente quando o player passa o mouse sobre o item.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MatterInfectionTooltipHandler {

    /** Chave NBT que marca o item como infectado. */
    public static final String TAG_INFECTED = "MatterInfected";

    private MatterInfectionTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(TAG_INFECTED)) return;

        // ── Linhas de aviso ──
        // Vermelho-escuro com símbolo de risco biológico (☣) chama atenção
        // imediatamente. A segunda linha explica a consequência mecânica
        // ("infecção e radiação"). A terceira aponta o caminho de purificação
        // pra que o player saiba o que fazer com o item.
        event.getToolTip().add(Component.literal("☣ INFECTADO POR MATÉRIA ESCURA")
                .withStyle(ChatFormatting.DARK_RED));
        event.getToolTip().add(Component.literal("Causa infecção e radiação enquanto carregado")
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("Purifique no Matter Purifier (100k FE)")
                .withStyle(ChatFormatting.GRAY));
    }
}
