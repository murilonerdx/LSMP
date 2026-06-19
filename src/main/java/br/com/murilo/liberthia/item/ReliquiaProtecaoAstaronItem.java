package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Relíquia de Proteção de Astaron — equipável no slot Curios {@code belt}.
 *
 * <ul>
 *   <li><b>3 cargas</b>: recarrega 1 a cada 48000 ticks (40 minutos).</li>
 *   <li><b>Trigger</b>: se o player vai tomar mais de 50% do max HP num único
 *       hit, consome 1 carga, salva pos atual, teleporta o player pra um
 *       "lugar seguro" (spawn ou bed) e entrega uma {@code astaron_access_key}
 *       que pode ser usada pra voltar.</li>
 *   <li><b>Cooldown</b>: 1 minuto entre triggers.</li>
 * </ul>
 */
public class ReliquiaProtecaoAstaronItem extends Item {

    public static final int MAX_CHARGES = 3;
    public static final int CHARGE_REFILL_TICKS = 48000;  // 40 min
    public static final int TRIGGER_COOLDOWN_TICKS = 1200; // 1 min

    public static final String TAG_CHARGES = "AstaronCharges";
    public static final String TAG_LAST_REFILL = "LastRefillTick";
    public static final String TAG_LAST_TRIGGER = "LastTriggerTick";

    public ReliquiaProtecaoAstaronItem(Properties props) {
        super(props);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    public static int getCharges(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CHARGES)) return MAX_CHARGES;
        return tag.getInt(TAG_CHARGES);
    }

    public static void setCharges(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(TAG_CHARGES, Math.max(0, Math.min(MAX_CHARGES, value)));
    }

    public static long getLastRefill(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        return tag.getLong(TAG_LAST_REFILL);
    }

    public static void setLastRefill(ItemStack stack, long t) {
        stack.getOrCreateTag().putLong(TAG_LAST_REFILL, t);
    }

    public static long getLastTrigger(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return -100000L;
        return tag.getLong(TAG_LAST_TRIGGER);
    }

    public static void setLastTrigger(ItemStack stack, long t) {
        stack.getOrCreateTag().putLong(TAG_LAST_TRIGGER, t);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide()) return;
        // Recarrega cargas a cada CHARGE_REFILL_TICKS ticks de game time, se faltar carga
        long now = level.getGameTime();
        int charges = getCharges(stack);
        if (charges >= MAX_CHARGES) {
            setLastRefill(stack, now);
            return;
        }
        long last = getLastRefill(stack);
        if (last == 0) {
            setLastRefill(stack, now);
            return;
        }
        if (now - last >= CHARGE_REFILL_TICKS) {
            setCharges(stack, charges + 1);
            setLastRefill(stack, now);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Relíquia de Astaron")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Forjada por Astaron usando uma de suas fragmentações.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Protege contra dano fatal.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Slot Curios: belt")
                .withStyle(ChatFormatting.DARK_GRAY));
        int c = getCharges(stack);
        tooltip.add(Component.literal("Cargas: " + c + "/" + MAX_CHARGES)
                .withStyle(c > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}
