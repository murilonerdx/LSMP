package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.init.SpiritualTradeData;
import br.com.murilo.liberthia.menu.SpiritualTradeMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class SpiritualLinkItem extends Item {

    public SpiritualLinkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!hasValidLink(stack)) {
            player.sendSystemMessage(
                    Component.literal("Este vínculo espiritual está corrompido.")
                            .withStyle(ChatFormatting.RED)
            );
            return InteractionResultHolder.fail(stack);
        }

        ItemStack ownerConnectionStack = SpiritualTradeData.findOwnerConnectionStack(serverPlayer, stack);

        if (ownerConnectionStack.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("A entidade espiritual não está disponível ou perdeu a Conexão Espiritual.")
                            .withStyle(ChatFormatting.RED)
            );
            return InteractionResultHolder.fail(stack);
        }

        String ownerName = stack.getTag().getString(SpiritualConnectionItem.TAG_OWNER_NAME);

        NetworkHooks.openScreen(
                serverPlayer,
                new SimpleMenuProvider(
                        (containerId, inventory, p) ->
                                new SpiritualTradeMenu(containerId, inventory, ownerConnectionStack),
                        Component.literal("Trocas espirituais: " + ownerName)
                                .withStyle(ChatFormatting.DARK_PURPLE)
                )
        );

        return InteractionResultHolder.consume(stack);
    }

    public static boolean hasValidLink(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (!(stack.getItem() instanceof SpiritualLinkItem)) {
            return false;
        }

        if (!stack.hasTag() || stack.getTag() == null) {
            return false;
        }

        CompoundTag tag = stack.getTag();

        return tag.contains(SpiritualConnectionItem.TAG_CHANNEL_ID)
                && tag.contains(SpiritualConnectionItem.TAG_OWNER_UUID)
                && tag.contains(SpiritualConnectionItem.TAG_OWNER_NAME)
                && !tag.getString(SpiritualConnectionItem.TAG_CHANNEL_ID).isBlank()
                && !tag.getString(SpiritualConnectionItem.TAG_OWNER_UUID).isBlank()
                && !tag.getString(SpiritualConnectionItem.TAG_OWNER_NAME).isBlank();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasValidLink(stack);
    }
}
