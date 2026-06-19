package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.compat.CuriosCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Astaron Access Key — chave temporária dropada pela
 * {@link ReliquiaProtecaoAstaronItem} quando o player é salvo de um hit fatal.
 *
 * <ul>
 *   <li>Right-click teleporta o player de volta pra posição salva no NBT.</li>
 *   <li>Vinculada ao player original (UUID em NBT) — outros não podem usar.</li>
 *   <li>Se o player tirar a Relíquia, a key some do inventário.</li>
 *   <li>Não pode ser dropada.</li>
 *   <li>Após uso, é consumida.</li>
 * </ul>
 */
public class AstaronAccessKeyItem extends Item {

    public static final String TAG_OWNER = "OwnerUUID";
    public static final String TAG_DIM = "ReturnDim";
    public static final String TAG_X = "ReturnX";
    public static final String TAG_Y = "ReturnY";
    public static final String TAG_Z = "ReturnZ";

    public AstaronAccessKeyItem(Properties props) {
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

    public static void setReturnPos(ItemStack stack, ResourceLocation dim, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_DIM, dim.toString());
        tag.putInt(TAG_X, pos.getX());
        tag.putInt(TAG_Y, pos.getY());
        tag.putInt(TAG_Z, pos.getZ());
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
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_DIM)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.fail(stack);
        }
        ResourceLocation dimRl = ResourceLocation.tryParse(tag.getString(TAG_DIM));
        if (dimRl == null) return InteractionResultHolder.fail(stack);
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl);
        ServerLevel targetLevel = sp.server.getLevel(key);
        if (targetLevel == null) {
            return InteractionResultHolder.fail(stack);
        }
        int x = tag.getInt(TAG_X);
        int y = tag.getInt(TAG_Y);
        int z = tag.getInt(TAG_Z);

        sp.teleportTo(targetLevel, x + 0.5, y, z + 0.5, sp.getYRot(), sp.getXRot());
        targetLevel.playSound(null, x, y, z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        // v0.1.35: NÃO consome mais ao usar — user pediu que a key persista
        // enquanto o cinto estiver equipado. Só remove a tag de return pos
        // (a key fica disponível pro próximo trigger).
        CompoundTag t = stack.getTag();
        if (t != null) {
            t.remove(TAG_DIM);
            t.remove(TAG_X);
            t.remove(TAG_Y);
            t.remove(TAG_Z);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // Não permite drop manual
        return false;
    }

    /**
     * v0.1.39: grace period antes de remover — protege contra race do Curios
     * sync ao trocar de dim, relog ou re-equipar o cinto.
     */
    private static final String TAG_GRACE = "GraceTicks";
    /** v0.1.43: aumentado de 100 (5s) → 600 (30s). */
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
        ItemStack belt = CuriosCompat.findEquippedReliquiaAstaron(player);
        CompoundTag tag = stack.getOrCreateTag();
        if (belt.isEmpty()) {
            int grace = tag.getInt(TAG_GRACE) + 1;
            tag.putInt(TAG_GRACE, grace);
            if (grace >= GRACE_MAX) {
                stack.shrink(1);
            }
        } else {
            tag.remove(TAG_GRACE);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Chave de Astaron")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Right-click: volta ao local salvo")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Vinculada — apenas o dono pode usar")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Persiste enquanto a Relíquia estiver equipada")
                .withStyle(ChatFormatting.DARK_GRAY));
        CompoundTag t = stack.getTag();
        boolean hasReturn = t != null && t.contains(TAG_DIM);
        tooltip.add(Component.literal(hasReturn
                        ? "✦ Destino salvo — pronta pra usar"
                        : "Sem destino — aguardando trigger da Relíquia")
                .withStyle(hasReturn ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
    }
}
