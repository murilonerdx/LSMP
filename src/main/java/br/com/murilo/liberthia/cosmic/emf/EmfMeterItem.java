package br.com.murilo.liberthia.cosmic.emf;

import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r174: <b>Medidor EMF</b> — equipamento 3D de caça-fantasmas.
 *
 * <p>Na mão, as luzes acendem do verde→vermelho conforme se aproxima de uma
 * <b>irregularidade dimensional</b> ({@link EmfSource}). Quanto mais forte:
 * <ul>
 *   <li>o modelo 3D na mão pisca mais pro vermelho (via ItemProperty {@code emf});</li>
 *   <li>apita mais rápido e mais agudo;</li>
 *   <li>no máximo, <b>drena a sanidade</b> do portador (HUD de sanidade desce).</li>
 * </ul>
 *
 * <p>A leitura (0..100) fica no NBT {@code emf} e é lida pelo modelo no cliente.
 */
public class EmfMeterItem extends Item {

    public static final String NBT_EMF = "emf"; // 0..100 (intensidade suavizada)

    public EmfMeterItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** Lido pelo ItemProperty no cliente (0..1). */
    public static float readEmf(ItemStack stack) {
        if (!stack.hasTag()) return 0F;
        return stack.getTag().getInt(NBT_EMF) / 100F;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp)) return;
        long gt = level.getGameTime();
        if (gt % 4 != 0) return; // amostra a cada 4 ticks (barato + suave)

        double intensity = EmfSource.intensityAt(level, sp.position(), sp);
        int target = (int) Math.round(intensity * 100);

        CompoundTag tag = stack.getOrCreateTag();
        int prev = tag.getInt(NBT_EMF);
        // suaviza a "agulha" rumo ao alvo
        int diff = target - prev;
        int next = prev + Integer.signum(diff) * Math.min(15, Math.abs(diff));
        if (next != prev) tag.putInt(NBT_EMF, next);

        double cur = next / 100.0;
        if (!selected) return; // só apita / drena na mão

        // bip: cadência acelera com a intensidade (em passos de 4 ticks)
        if (cur > 0.05) {
            int steps = Math.max(1, (int) Math.round((40 - cur * 36) / 4)); // 1..10 (×4t)
            if ((gt / 4) % steps == 0) {
                float pitch = 0.7F + (float) cur * 1.2F;
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, pitch);
            }
        }

        // no vermelho máximo: drena sanidade (HUD desce visivelmente)
        if (cur >= 0.85 && gt % 20 == 0) {
            SpiritDimension.addSanity(sp, -1);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Medidor de irregularidade dimensional.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Verde → calmo. Vermelho piscando → perigo.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("No máximo, sua sanidade se desfaz.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        if (stack.hasTag() && stack.getTag().contains(NBT_EMF)) {
            int v = stack.getTag().getInt(NBT_EMF);
            ChatFormatting c = v >= 85 ? ChatFormatting.RED
                    : v >= 50 ? ChatFormatting.GOLD
                    : v >= 20 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
            tooltip.add(Component.literal("Leitura: " + v + "%").withStyle(c));
        }
    }
}
