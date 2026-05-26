package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.world.RiftSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Bússola Dimensional — aponta para o RIFT dimensional mais próximo,
 * armazenado em {@link RiftSavedData} (persistente por mundo).
 *
 * <p>Right-click escaneia os rifts da dimensão atual e salva a posição
 * do mais próximo no NBT do item. Mostra coordenadas e distância no chat.
 */
public class DimensionalCompassItem extends Item {

    public static final String TAG_X = "TargetX";
    public static final String TAG_Y = "TargetY";
    public static final String TAG_Z = "TargetZ";
    public static final String TAG_DIST = "TargetDist";

    public DimensionalCompassItem(Properties props) { super(props); }

    /** v0.1.13: helper pra ler posição salva no NBT (ou null se não tem). */
    private static @Nullable BlockPos readSavedPos(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_X)) return null;
        CompoundTag tag = stack.getTag();
        return new BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(level instanceof ServerLevel sl))
            return InteractionResultHolder.success(stack);

        BlockPos origin = player.blockPosition();
        RiftSavedData data = RiftSavedData.get(sl);

        // v0.1.13: SHIFT+click cicla pro PRÓXIMO rift mais próximo (em vez de
        // sempre apontar pro mesmo). Sem shift = rift mais próximo (default).
        BlockPos previousTarget = readSavedPos(stack);
        BlockPos nearest;
        if (player.isShiftKeyDown() && previousTarget != null) {
            // Pula o rift atual e pega o próximo mais próximo
            nearest = data.findNearestExcluding(origin, previousTarget);
            if (nearest == null) {
                // Não tem outro — volta pro mais próximo
                nearest = data.findNearest(origin);
            } else {
                player.displayClientMessage(
                        Component.literal("⌖ Ciclando para o próximo rift...")
                                .withStyle(ChatFormatting.AQUA), true);
            }
        } else {
            nearest = data.findNearest(origin);
        }

        // Detecta troca de rift: se a posição salva no NBT não bate com o
        // rift mais próximo atual (provavelmente porque o anterior se esgotou),
        // notifica o player. Isso é importante porque o usuário pode estar
        // extraindo de um rift que SUMIU — agora aponta pra outro.
        boolean riftChanged = false;
        if (nearest != null && previousTarget != null && !player.isShiftKeyDown()) {
            riftChanged = !previousTarget.equals(nearest);
        }

        // Re-escaneia: se encontrou rift mais próximo, atualiza NBT.
        if (nearest != null) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt(TAG_X, nearest.getX());
            tag.putInt(TAG_Y, nearest.getY());
            tag.putInt(TAG_Z, nearest.getZ());
            int dist = (int) Math.sqrt(origin.distSqr(nearest));
            tag.putInt(TAG_DIST, dist);

            // Inclui também a capacidade restante do rift na mensagem
            tag.putInt("TargetCapacity", data.getCapacity(nearest));
        }

        if (riftChanged) {
            player.displayClientMessage(
                    Component.literal("⚠ Rift anterior se esgotou — apontando para o próximo")
                            .withStyle(ChatFormatting.GOLD), false);
        }

        // v0.1.13: mostra quantos rifts restam na dimensão (info útil pra
        // exploração — usuário sabe se vale procurar mais)
        int activeCount = data.activeCount();
        if (activeCount > 0) {
            player.displayClientMessage(
                    Component.literal("📡 " + activeCount + " rift" + (activeCount == 1 ? "" : "s") + " ativo" + (activeCount == 1 ? "" : "s") + " nesta dimensão")
                            .withStyle(ChatFormatting.DARK_AQUA), true);
        }

        // SEMPRE mostra a posição salva (se houver) — mesmo que o scan
        // não tenha achado nada nesta dimensão, mostra a última conhecida.
        // Antes mostrava só quando achava no scan; agora cada clique exibe
        // novamente as coordenadas salvas pra consulta rápida.
        if (stack.hasTag() && stack.getTag().contains(TAG_X)) {
            CompoundTag tag = stack.getTag();
            int x = tag.getInt(TAG_X);
            int y = tag.getInt(TAG_Y);
            int z = tag.getInt(TAG_Z);
            int dist = tag.contains(TAG_DIST) ? tag.getInt(TAG_DIST) : (int) Math.sqrt(origin.distSqr(new BlockPos(x, y, z)));

            String prefix = nearest != null ? "⌖ Rift Dimensional: " : "⌖ Última posição salva: ";
            player.displayClientMessage(
                    Component.literal(prefix + x + ", " + y + ", " + z + " (" + dist + "m)")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), false);

            // Mostra capacidade restante do rift (se ainda existe)
            if (nearest != null && tag.contains("TargetCapacity")) {
                int cap = tag.getInt("TargetCapacity");
                ChatFormatting color = cap > 50 ? ChatFormatting.GREEN
                        : cap > 20 ? ChatFormatting.YELLOW
                        : ChatFormatting.RED;
                player.displayClientMessage(
                        Component.literal("  Capacidade do rift: " + cap + " baldes restantes")
                                .withStyle(color), false);
            }

            if (nearest == null) {
                player.displayClientMessage(
                        Component.literal("(sem rift nesta dimensão — exibindo coords salvas)")
                                .withStyle(ChatFormatting.GRAY), false);
            }
            player.getCooldowns().addCooldown(this, 20);
        } else {
            player.displayClientMessage(
                    Component.literal("Nenhum rift detectado e nenhuma posição salva")
                            .withStyle(ChatFormatting.GRAY), true);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains(TAG_X)) {
            CompoundTag tag = stack.getTag();
            tooltip.add(Component.literal("Rift: " + tag.getInt(TAG_X) + ", "
                    + tag.getInt(TAG_Y) + ", " + tag.getInt(TAG_Z))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            if (tag.contains(TAG_DIST)) {
                tooltip.add(Component.literal("Distância: " + tag.getInt(TAG_DIST) + " blocos")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (tag.contains("TargetCapacity")) {
                int cap = tag.getInt("TargetCapacity");
                ChatFormatting color = cap > 50 ? ChatFormatting.GREEN
                        : cap > 20 ? ChatFormatting.YELLOW
                        : ChatFormatting.RED;
                tooltip.add(Component.literal("Capacidade: " + cap + " baldes")
                        .withStyle(color));
            }
        } else {
            tooltip.add(Component.literal("Right-click pra localizar rift").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_X);
    }
}
