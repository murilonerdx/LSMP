package br.com.murilo.liberthia.init;

import br.com.murilo.liberthia.item.SpiritualConnectionItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SpiritualTradeData {

    public static final String TAG_TRADES = "SpiritualTrades";
    public static final int TRADE_STACK_SIZE = 6;

    private SpiritualTradeData() {
    }

    public static void saveTrades(ItemStack connectionStack, List<ItemStack> stacks) {
        CompoundTag root = connectionStack.getOrCreateTag();
        ListTag list = new ListTag();

        for (int i = 0; i < TRADE_STACK_SIZE; i++) {
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;

            CompoundTag itemTag = new CompoundTag();

            if (!stack.isEmpty()) {
                stack.save(itemTag);
            }

            list.add(itemTag);
        }

        root.put(TAG_TRADES, list);
    }

    public static List<ItemStack> loadTrades(ItemStack connectionStack) {
        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < TRADE_STACK_SIZE; i++) {
            stacks.add(ItemStack.EMPTY);
        }

        if (connectionStack.isEmpty()) {
            return stacks;
        }

        if (!connectionStack.hasTag() || connectionStack.getTag() == null) {
            return stacks;
        }

        CompoundTag root = connectionStack.getTag();

        if (!root.contains(TAG_TRADES)) {
            return stacks;
        }

        ListTag list = root.getList(TAG_TRADES, Tag.TAG_COMPOUND);

        for (int i = 0; i < Math.min(list.size(), TRADE_STACK_SIZE); i++) {
            CompoundTag itemTag = list.getCompound(i);

            if (!itemTag.isEmpty()) {
                stacks.set(i, ItemStack.of(itemTag));
            }
        }

        return stacks;
    }

    public static List<SpiritualTradeOffer> loadOffers(ItemStack connectionStack) {
        List<ItemStack> stacks = loadTrades(connectionStack);
        List<SpiritualTradeOffer> offers = new ArrayList<>();

        for (int i = 0; i < TRADE_STACK_SIZE; i += 2) {
            ItemStack cost = stacks.get(i);
            ItemStack result = stacks.get(i + 1);

            if (!cost.isEmpty() && !result.isEmpty()) {
                offers.add(new SpiritualTradeOffer(cost.copy(), result.copy()));
            }
        }

        return offers;
    }

    public static ItemStack findOwnerConnectionStack(ServerPlayer viewer, ItemStack linkStack) {
        if (linkStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!linkStack.hasTag() || linkStack.getTag() == null) {
            return ItemStack.EMPTY;
        }

        String ownerUuidRaw = linkStack.getTag().getString(SpiritualConnectionItem.TAG_OWNER_UUID);

        if (ownerUuidRaw == null || ownerUuidRaw.isBlank()) {
            return ItemStack.EMPTY;
        }

        UUID ownerUuid;

        try {
            ownerUuid = UUID.fromString(ownerUuidRaw);
        } catch (IllegalArgumentException ex) {
            return ItemStack.EMPTY;
        }

        ServerPlayer owner = viewer.server.getPlayerList().getPlayer(ownerUuid);

        if (owner == null) {
            return ItemStack.EMPTY;
        }

        for (ItemStack stack : owner.getInventory().items) {
            if (isValidOwnerConnection(stack, ownerUuid)) {
                return stack;
            }
        }

        for (ItemStack stack : owner.getInventory().offhand) {
            if (isValidOwnerConnection(stack, ownerUuid)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean isValidOwnerConnection(ItemStack stack, UUID ownerUuid) {
        if (stack.isEmpty()) {
            return false;
        }

        if (!stack.is(ModItems.SPIRITUAL_CONNECTION.get())) {
            return false;
        }

        if (!stack.hasTag() || stack.getTag() == null) {
            return false;
        }

        String stackOwnerUuid = stack.getTag().getString(SpiritualConnectionItem.TAG_OWNER_UUID);

        return ownerUuid.toString().equals(stackOwnerUuid);
    }
}
