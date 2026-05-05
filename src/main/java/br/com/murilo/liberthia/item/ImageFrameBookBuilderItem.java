package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.client.ClientBookHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.DistExecutor;

public class ImageFrameBookBuilderItem extends Item {

    public ImageFrameBookBuilderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> ClientBookHooks.openEditor()
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
