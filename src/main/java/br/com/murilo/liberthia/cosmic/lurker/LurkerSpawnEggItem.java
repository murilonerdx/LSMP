package br.com.murilo.liberthia.cosmic.lurker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * r173: "Ovo" customizado do {@link LurkerEntity}.
 *
 * <p>{@code ForgeSpawnEggItem} exige um {@code EntityType<? extends Mob>}, mas o
 * Lurker é uma {@code Entity} pura (billboard que some sozinho), então usamos um
 * item próprio: clica num bloco e spawna um Lurker com uma cara aleatória ali —
 * útil pra testar/posicionar manualmente o susto.
 */
public class LurkerSpawnEggItem extends Item {

    public LurkerSpawnEggItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel sl) {
            var pos = ctx.getClickedPos().relative(ctx.getClickedFace());
            int face = sl.random.nextInt(6);
            LurkerEntity.spawn(sl,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, face);
            if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }
}
