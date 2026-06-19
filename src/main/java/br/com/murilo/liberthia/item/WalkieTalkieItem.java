package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenWalkieScreenS2CPacket;
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
 * <b>Walkie Talkie</b> — rádio portátil que usa o Simple Voice Chat.
 *
 * <ul>
 *   <li><b>Shift + botão direito</b>: abre a tela pra digitar o <i>código
 *       secreto</i> (frequência) e ligar/desligar.</li>
 *   <li><b>Botão direito</b>: liga/desliga o rádio.</li>
 *   <li>Quando ligado e segurado, sua fala (push-to-talk do SVC) vai pra
 *       todos os outros players com um walkie LIGADO no MESMO código —
 *       não importa a distância. Quem não tem o código não escuta nada.</li>
 * </ul>
 *
 * <p>O roteamento de voz é feito por {@code WalkieTalkieRelay} dentro do
 * plugin do Simple Voice Chat (precisa do SVC instalado no servidor).
 */
public class WalkieTalkieItem extends Item {

    public static final String NBT_FREQ = "WalkieFreq";
    public static final String NBT_ON = "WalkieOn";

    public WalkieTalkieItem(Properties props) {
        super(props);
    }

    // ---- NBT helpers ----
    public static String getFreq(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString(NBT_FREQ) : "";
    }

    public static boolean isOn(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_ON);
    }

    public static void setFreq(ItemStack stack, String freq) {
        stack.getOrCreateTag().putString(NBT_FREQ, freq == null ? "" : freq);
    }

    public static void setOn(ItemStack stack, boolean on) {
        stack.getOrCreateTag().putBoolean(NBT_ON, on);
    }

    /**
     * Frequência que esse player está TRANSMITINDO agora: precisa estar
     * segurando (mão principal ou secundária) um walkie LIGADO com código.
     * Retorna null se não está transmitindo.
     */
    public static String getActiveTransmitFrequency(Player p) {
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(h);
            if (s.getItem() instanceof WalkieTalkieItem && isOn(s)) {
                String f = getFreq(s);
                if (!f.isEmpty()) return f;
            }
        }
        return null;
    }

    /**
     * Esse player está OUVINDO essa frequência? Basta ter um walkie ligado e
     * tunado no código em qualquer lugar do inventário (mãos, hotbar, mochila).
     */
    public static boolean isListeningOn(Player p, String freq) {
        if (freq == null || freq.isEmpty()) return false;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof WalkieTalkieItem && isOn(s) && freq.equals(getFreq(s))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (player.isShiftKeyDown()) {
                // Abre a tela do código secreto no cliente desse player.
                ModNetwork.sendToPlayer(sp, new OpenWalkieScreenS2CPacket(getFreq(stack), isOn(stack)));
            } else {
                boolean newOn = !isOn(stack);
                setOn(stack, newOn);
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,
                        0.6F, newOn ? 1.4F : 0.8F);
                String freq = getFreq(stack);
                sp.displayClientMessage(Component.translatable(
                                newOn ? "item.liberthia.walkie_talkie.on"
                                        : "item.liberthia.walkie_talkie.off",
                                freq.isEmpty() ? "—" : freq)
                        .withStyle(newOn ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String freq = getFreq(stack);
        boolean on = isOn(stack);
        tooltip.add(Component.translatable("item.liberthia.walkie_talkie.channel",
                        freq.isEmpty() ? "—" : freq)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add((on ? Component.translatable("item.liberthia.walkie_talkie.state_on")
                        .withStyle(ChatFormatting.GREEN)
                : Component.translatable("item.liberthia.walkie_talkie.state_off")
                        .withStyle(ChatFormatting.DARK_GRAY)));
        tooltip.add(Component.translatable("item.liberthia.walkie_talkie.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isOn(stack); // brilha quando ligado
    }
}
