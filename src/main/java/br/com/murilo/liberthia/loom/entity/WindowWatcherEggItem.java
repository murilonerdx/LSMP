package br.com.murilo.liberthia.loom.entity;

import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r174: <b>Ovo do Observador</b> — clica num bloco e invoca um
 * {@link WindowWatcherEntity} preso ao player mais próximo. A entidade vai
 * sozinha achar uma janela perto do alvo e ficar do outro lado, observando.
 */
public class WindowWatcherEggItem extends Item {

    public WindowWatcherEggItem(Properties props) {
        super(props.rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel sl) {
            BlockPos pos = ctx.getClickedPos().above();
            Player user = ctx.getPlayer();
            WindowWatcherEntity e = ModEntities.LOOM_WINDOW_WATCHER.get().create(sl);
            if (e != null) {
                e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                // amarra a quem usou; sem player (dispenser) a entidade pega o mais
                // próximo sozinha no 1º tick.
                if (user != null) e.startWatch(user.getUUID());
                sl.addFreshEntity(e);
                if (user == null || !user.isCreative()) ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Algo vai te observar pela janela.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
