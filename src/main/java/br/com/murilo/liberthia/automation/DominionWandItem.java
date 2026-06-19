package br.com.murilo.liberthia.automation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
 * v0.1.24 r88: <b>Dominion Wand</b> — wand de linking universal.
 *
 * <h2>Uso</h2>
 * <ol>
 *   <li>Right-click no <b>source</b> block (ex.: Spell Sensor) → grava posição</li>
 *   <li>Right-click no <b>target</b> block (ex.: Spell Turret) → cria link</li>
 *   <li>Shift+Right-click vazio → limpa link armazenado</li>
 * </ol>
 *
 * <p>Link armazena Source → Target em NBT pareando blocks. Cada link cria
 * uma "conexão lógica" — quando Source dispara redstone, Target recebe
 * pulse equivalente (mesmo que não esteja adjacente).
 *
 * <p>NBT keys:
 * <ul>
 *   <li>{@code Source} (CompoundTag {x,y,z}) — posição gravada do source</li>
 *   <li>{@code Links} (ListTag) — link pairs em registry global</li>
 * </ul>
 */
public class DominionWandItem extends Item {

    public DominionWandItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        BlockPos clicked = ctx.getClickedPos();
        CompoundTag tag = stack.getOrCreateTag();

        if (sp.isShiftKeyDown()) {
            tag.remove("Source");
            sp.displayClientMessage(Component.literal("§5✦ Dominion Wand resetado."), true);
            return InteractionResult.CONSUME;
        }

        if (tag.contains("Source")) {
            // Segundo click → CRIA LINK
            CompoundTag sourceTag = tag.getCompound("Source");
            BlockPos source = new BlockPos(
                    sourceTag.getInt("x"), sourceTag.getInt("y"), sourceTag.getInt("z"));

            // Cria link em registry global
            DominionLinkRegistry.addLink(ctx.getLevel().dimension(), source, clicked);
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ Link criado §7(" + source.toShortString()
                    + " §8→§7 " + clicked.toShortString() + ")"), false);
            tag.remove("Source");
            return InteractionResult.CONSUME;
        } else {
            // Primeiro click → GRAVA SOURCE
            CompoundTag sourceTag = new CompoundTag();
            sourceTag.putInt("x", clicked.getX());
            sourceTag.putInt("y", clicked.getY());
            sourceTag.putInt("z", clicked.getZ());
            tag.put("Source", sourceTag);
            sp.displayClientMessage(Component.literal(
                    "§5✦ Source gravado: §7" + clicked.toShortString()
                    + " §5→ next-click pra linkar"), true);
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Source")) {
            CompoundTag s = tag.getCompound("Source");
            tooltip.add(Component.literal("§5Source: §b" + s.getInt("x") + ", "
                    + s.getInt("y") + ", " + s.getInt("z")));
            tooltip.add(Component.literal("§7§oClique no target pra linkar"));
        } else {
            tooltip.add(Component.literal("§7§oRight-click 2 blocos pra criar link"));
        }
        tooltip.add(Component.literal("§8§oShift+R-click vazio: reset"));
    }
}
