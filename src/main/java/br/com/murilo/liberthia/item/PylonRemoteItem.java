package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.block.entity.CommandPylonBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pylon Remote — controle remoto pra ativar um Command Pylon à distância.
 *
 * <p>Uso:
 * <ul>
 *   <li><b>Sneak + right-click no Pylon</b> → linka o controle a este Pylon
 *       (salva posição + dimensão no NBT do item).</li>
 *   <li><b>Right-click no ar / bloco qualquer</b> → dispara o Pylon linkado
 *       (chama {@code pylon.fire(level, player)}). Cooldown 20 ticks.</li>
 *   <li><b>Sneak + right-click no ar</b> → desfaz o link.</li>
 * </ul>
 *
 * <p>Requer OP — só ops podem executar o Pylon, então só ops podem usar o
 * remoto. Funciona em qualquer distância DENTRO da mesma dimensão (chunk
 * precisa estar carregada — não força chunk-load).
 */
public class PylonRemoteItem extends Item {

    private static final String TAG_X = "PX";
    private static final String TAG_Y = "PY";
    private static final String TAG_Z = "PZ";
    private static final String TAG_DIM = "PDim";

    public PylonRemoteItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        // Sneak + click num Pylon = LINK
        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
            if (be instanceof CommandPylonBlockEntity) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                if (!CommandRunner.isOp((ServerPlayer) player)) {
                    player.displayClientMessage(
                            Component.literal("Apenas OPs podem linkar o controle.")
                                    .withStyle(ChatFormatting.RED), true);
                    return InteractionResult.FAIL;
                }
                ItemStack stack = ctx.getItemInHand();
                CompoundTag tag = stack.getOrCreateTag();
                BlockPos p = ctx.getClickedPos();
                tag.putInt(TAG_X, p.getX());
                tag.putInt(TAG_Y, p.getY());
                tag.putInt(TAG_Z, p.getZ());
                tag.putString(TAG_DIM, level.dimension().location().toString());
                player.displayClientMessage(
                        Component.literal("Controle linkado ao Pilar em "
                                        + p.getX() + ", " + p.getY() + ", " + p.getZ())
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
                return InteractionResult.CONSUME;
            }
        }
        // Não é um Pylon — fall through pra use() lidar.
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // Sneak + ar: desfaz link
        if (player.isShiftKeyDown()) {
            if (stack.hasTag() && stack.getTag().contains(TAG_X)) {
                stack.removeTagKey(TAG_X);
                stack.removeTagKey(TAG_Y);
                stack.removeTagKey(TAG_Z);
                stack.removeTagKey(TAG_DIM);
                player.displayClientMessage(
                        Component.literal("Link desfeito.").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.consume(stack);
        }

        // Não-sneak + ar: dispara o Pylon linkado
        if (!CommandRunner.isOp(sp)) {
            player.displayClientMessage(
                    Component.literal("Apenas OPs podem disparar.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!stack.hasTag() || !stack.getTag().contains(TAG_X)) {
            player.displayClientMessage(
                    Component.literal("Controle não linkado. Sneak+right-click num Pilar.")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        CompoundTag tag = stack.getTag();
        ResourceKey<Level> dimKey = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation(tag.getString(TAG_DIM)));
        ServerLevel target = sp.server.getLevel(dimKey);
        if (target == null) {
            player.displayClientMessage(
                    Component.literal("Dimensão do Pilar não encontrada.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        BlockPos pylonPos = new BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z));
        if (!target.hasChunkAt(pylonPos)) {
            player.displayClientMessage(
                    Component.literal("Pilar fora do chunk carregado — chegue mais perto.")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }

        BlockEntity be = target.getBlockEntity(pylonPos);
        if (!(be instanceof CommandPylonBlockEntity pylon)) {
            player.displayClientMessage(
                    Component.literal("Não há Pilar no destino linkado. Re-linke.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        pylon.fire(target, sp);
        player.displayClientMessage(
                Component.literal("⚡ Pilar disparado.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        sp.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains(TAG_X)) {
            CompoundTag tag = stack.getTag();
            tooltip.add(Component.literal("Linkado: "
                            + tag.getInt(TAG_X) + ", " + tag.getInt(TAG_Y) + ", " + tag.getInt(TAG_Z))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.literal("Dim: " + tag.getString(TAG_DIM))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Sneak+click num Pilar pra linkar")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("Right-click pra disparar à distância")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_X);
    }
}
