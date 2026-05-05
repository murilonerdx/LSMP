package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.menu.SpiritualTradeConfigMenu;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class SpiritualConnectionItem extends Item {
    public static final String TAG_CHANNEL_ID = "SpiritualChannelId";
    public static final String TAG_OWNER_UUID = "SpiritualOwnerUUID";
    public static final String TAG_OWNER_NAME = "SpiritualOwnerName";
    public static final String TAG_LINK_CREATED = "SpiritualLinkCreated";

    public SpiritualConnectionItem(Properties properties) {
        super(properties);
    }

    /**
     * Clique direito no ar:
     * abre a tela de configuração de trocas.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        ensureConnectionData(stack, serverPlayer);

        NetworkHooks.openScreen(
                serverPlayer,
                new SimpleMenuProvider(
                        (containerId, inventory, p) ->
                                new SpiritualTradeConfigMenu(containerId, inventory, stack),
                        Component.literal("Configurar Trocas Espirituais")
                                .withStyle(ChatFormatting.DARK_PURPLE)
                )
        );

        return InteractionResultHolder.consume(stack);
    }

    /**
     * Clique direito em bloco:
     * SHIFT + clique gera o vínculo espiritual.
     * Clique normal abre a tela também.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack connectionStack = context.getItemInHand();
        ensureConnectionData(connectionStack, serverPlayer);

        if (serverPlayer.isShiftKeyDown()) {
            CompoundTag tag = connectionStack.getOrCreateTag();

            if (tag.getBoolean(TAG_LINK_CREATED)) {
                serverPlayer.sendSystemMessage(
                        Component.literal("Esta conexão espiritual já criou um vínculo.")
                                .withStyle(ChatFormatting.RED)
                );
                return InteractionResult.CONSUME;
            }

            createSpiritualLink(
                    (ServerLevel) level,
                    serverPlayer,
                    connectionStack,
                    context.getClickedPos().getX() + 0.5D,
                    context.getClickedPos().getY() + 1.0D,
                    context.getClickedPos().getZ() + 0.5D
            );

            tag.putBoolean(TAG_LINK_CREATED, true);

            return InteractionResult.CONSUME;
        }

        NetworkHooks.openScreen(
                serverPlayer,
                new SimpleMenuProvider(
                        (containerId, inventory, p) ->
                                new SpiritualTradeConfigMenu(containerId, inventory, connectionStack),
                        Component.literal("Configurar Trocas Espirituais")
                                .withStyle(ChatFormatting.DARK_PURPLE)
                )
        );

        return InteractionResult.CONSUME;
    }

    private void ensureConnectionData(ItemStack stack, ServerPlayer player) {
        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.contains(TAG_CHANNEL_ID)) {
            tag.putString(TAG_CHANNEL_ID, UUID.randomUUID().toString());
        }

        tag.putString(TAG_OWNER_UUID, player.getUUID().toString());
        tag.putString(TAG_OWNER_NAME, player.getName().getString());
    }

    private void createSpiritualLink(
            ServerLevel level,
            ServerPlayer player,
            ItemStack connectionStack,
            double x,
            double y,
            double z
    ) {
        CompoundTag connectionTag = connectionStack.getOrCreateTag();

        String channelId = connectionTag.getString(TAG_CHANNEL_ID);
        String ownerUuid = connectionTag.getString(TAG_OWNER_UUID);
        String ownerName = connectionTag.getString(TAG_OWNER_NAME);

        ItemStack linkStack = new ItemStack(ModItems.SPIRITUAL_LINK.get());
        CompoundTag linkTag = linkStack.getOrCreateTag();

        linkTag.putString(TAG_CHANNEL_ID, channelId);
        linkTag.putString(TAG_OWNER_UUID, ownerUuid);
        linkTag.putString(TAG_OWNER_NAME, ownerName);

        ItemEntity entity = new ItemEntity(level, x, y, z, linkStack);
        entity.setNoPickUpDelay();

        level.addFreshEntity(entity);

        player.sendSystemMessage(
                Component.literal("Vínculo espiritual criado.")
                        .withStyle(ChatFormatting.DARK_PURPLE)
        );
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag()
                && stack.getTag() != null
                && stack.getTag().contains(TAG_CHANNEL_ID);
    }
}