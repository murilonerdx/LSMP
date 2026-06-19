package br.com.murilo.liberthia.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.24 r97: <b>Jar of Light</b> — armazena posições onde Magelight Torch
 * foi colocado. Right-click no chão re-deploya o último torch armazenado.
 *
 * <p>Capacidade: 64 posições. Stack via NBT ListTag.
 */
public class JarOfLightItem extends Item {

    public static final String NBT_POSITIONS = "Positions";

    public JarOfLightItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        BlockPos clicked = ctx.getClickedPos().relative(ctx.getClickedFace());
        Level level = ctx.getLevel();

        if (!level.getBlockState(clicked).isAir()) {
            sp.displayClientMessage(Component.literal("§7§o✦ Posição não está vazia"), true);
            return InteractionResult.FAIL;
        }

        // Coloca Magelight Torch + grava posição
        var torch = br.com.murilo.liberthia.registry.ModBlocks.MAGELIGHT_TORCH.get();
        level.setBlock(clicked, torch.defaultBlockState(), 3);

        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.getList(NBT_POSITIONS, Tag.TAG_COMPOUND);
        if (list.size() < 64) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", clicked.getX());
            posTag.putInt("y", clicked.getY());
            posTag.putInt("z", clicked.getZ());
            list.add(posTag);
            tag.put(NBT_POSITIONS, list);
        }

        sp.displayClientMessage(Component.literal(
                "§5✦ Magelight colocado §7(" + list.size() + "/64)"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_POSITIONS)) {
            ListTag list = tag.getList(NBT_POSITIONS, Tag.TAG_COMPOUND);
            tooltip.add(Component.literal("§7Magelights: §b" + list.size() + "/64"));
        }
        tooltip.add(Component.literal("§7§oRight-click pra colocar Magelight"));
    }
}
