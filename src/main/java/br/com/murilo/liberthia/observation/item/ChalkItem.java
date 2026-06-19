package br.com.murilo.liberthia.observation.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r72: <b>Chalk Item</b> — desenha runa no chão.
 *
 * <p>Pattern AN's chalk: right-click no chão → coloca bloco de Rune.
 * O rune é vazio (sem spell) — precisa ser imprintado com sneak+rclick + parchment.
 *
 * <p>Durabilidade 32 — cada uso consome 1.
 */
public class ChalkItem extends Item {

    public ChalkItem(Properties properties) {
        super(properties.stacksTo(1).durability(32).rarity(Rarity.UNCOMMON));
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) return InteractionResult.FAIL;
        // Place Rune block
        var runeBlock = br.com.murilo.liberthia.registry.ModBlocks.RUNE_BLOCK.get();
        level.setBlock(pos, runeBlock.defaultBlockState(), 3);
        ItemStack stack = ctx.getItemInHand();
        stack.hurtAndBreak(1, ctx.getPlayer(),
            p -> p.broadcastBreakEvent(ctx.getHand()));
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                "§5✦ Runa desenhada §7— shift+rclick com Pergaminho pra inscrever."), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§5§oGiz Encantado").withStyle(ChatFormatting.ITALIC));
        t.add(Component.literal("§7Right-click no chão: desenha §dRuna§7."));
        t.add(Component.literal("§7Depois, shift+rclick na runa com Pergaminho pra inscrever."));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"Marcas que esperam o pé passar.\""));
    }
}
