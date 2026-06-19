package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.network.packet.InvertedVisionS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * <b>Mapa Invertido</b> — um artefato dobrado em si mesmo. Ao usar, a realidade
 * "vira do avesso" por alguns segundos: você enxerga o mundo invertido, o lado
 * de lá que existe colado ao nosso. Apenas perceptivo — não teleporta, não
 * machuca. Só revela o que sempre esteve ali.
 *
 * <p>Server-side: dispara {@link InvertedVisionS2CPacket} pro player e aplica
 * cooldown. O efeito visual (post-shader {@code invert.json}) roda no cliente.
 */
public class MapaInvertidoItem extends Item {

    private static final int DURATION_TICKS = 200; // ~10s de visão invertida
    private static final int COOLDOWN_TICKS = 400; // ~20s de recarga

    public MapaInvertidoItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            InvertedVisionS2CPacket.send(sp, DURATION_TICKS);
            // r173: "Spirit Sight" — enquanto invertido, enxerga o mundo espiritual
            // (entidades brilham através das paredes + blocos espirituais revelados).
            SpiritSightHandler.activate(sp, DURATION_TICKS);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.6F, 0.55F);
            sp.displayClientMessage(Component.literal(
                    "§5§o O véu se dobra... você vê o lado de lá.").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        // sidedSuccess: no cliente faz o swing do braço; no server faz a lógica.
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.mapa_invertido.desc1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.liberthia.mapa_invertido.desc2")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // brilho de encantamento — parece "carregado"
    }
}
