package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.compat.CuriosCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Flame Key — toggle do fogo dos Pés Queimantes.
 *
 * <ul>
 *   <li>Right-click alterna estado fire_enabled (NBT).</li>
 *   <li>Vinculada via UUID — só o dono usa.</li>
 *   <li>Some do inventário se os Pés Queimantes não estiverem equipados.</li>
 *   <li>Não dropa manualmente.</li>
 * </ul>
 */
public class FlameKeyItem extends Item {

    public static final String TAG_OWNER = "OwnerUUID";
    public static final String TAG_ENABLED = "FireEnabled";

    public FlameKeyItem(Properties props) {
        super(props);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    public static void setOwner(ItemStack stack, UUID uuid) {
        stack.getOrCreateTag().putUUID(TAG_OWNER, uuid);
    }

    @Nullable
    public static UUID getOwner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_OWNER)) return null;
        return tag.getUUID(TAG_OWNER);
    }

    public static boolean isFireEnabled(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ENABLED)) return true; // default ON
        return tag.getBoolean(TAG_ENABLED);
    }

    public static void setFireEnabled(ItemStack stack, boolean enabled) {
        stack.getOrCreateTag().putBoolean(TAG_ENABLED, enabled);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.consume(stack);
        }
        UUID owner = getOwner(stack);
        if (owner != null && !owner.equals(player.getUUID())) {
            return InteractionResultHolder.fail(stack);
        }
        boolean newState = !isFireEnabled(stack);
        setFireEnabled(stack, newState);
        level.playSound(null, player.blockPosition(),
                newState ? SoundEvents.FIRECHARGE_USE : SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.7F, 1.0F);
        player.displayClientMessage(
                Component.literal("Fogo: " + (newState ? "ATIVADO" : "DESATIVADO"))
                        .withStyle(newState ? ChatFormatting.RED : ChatFormatting.GRAY),
                true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        return false;
    }

    /**
     * Tolerância: só remove a key após N ticks SEGUIDOS sem detectar botas equipadas.
     * Antes removia no PRIMEIRO tick que findEquippedPesQueimantes voltava empty —
     * mas isso podia falhar em race condition (Curios slot sync, mudança de dim,
     * relog). Resultado: user com botas equipadas via key sumir do inv.
     * 100 ticks (5s) é margem ampla.
     */
    private static final String TAG_GRACE = "GraceTicks";
    /** v0.1.43: aumentado de 100 (5s) → 600 (30s). User reportou key sumindo
     *  após ativar/desativar Curios slot várias vezes. 30s dá margem ampla
     *  pra qualquer race condition do Curios sync. */
    private static final int GRACE_MAX = 600;

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) {
            stack.shrink(1);
            return;
        }
        UUID owner = getOwner(stack);
        if (owner != null && !owner.equals(player.getUUID())) {
            stack.shrink(1);
            return;
        }
        ItemStack pes = CuriosCompat.findEquippedPesQueimantes(player);
        CompoundTag tag = stack.getOrCreateTag();
        if (pes.isEmpty()) {
            // Incrementa grace counter. Só remove após GRACE_MAX ticks consecutivos
            // sem botas equipadas — protege contra race no Curios sync.
            int grace = tag.getInt(TAG_GRACE) + 1;
            tag.putInt(TAG_GRACE, grace);
            if (grace >= GRACE_MAX) {
                stack.shrink(1);
            }
        } else {
            // Botas equipadas — reseta grace.
            tag.remove(TAG_GRACE);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Chave do Fogo")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Right-click: alterna o fogo dos Pés")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Estado: " + (isFireEnabled(stack) ? "ATIVO" : "INATIVO"))
                .withStyle(isFireEnabled(stack) ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Vinculada ao portador")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
