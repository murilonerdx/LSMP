package br.com.murilo.liberthia.entry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record AdminInventorySnapshot(
        UUID targetId,
        String targetName,
        List<ItemStack> inventory,
        List<ItemStack> armor,
        ItemStack offhand
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetId);
        buf.writeUtf(targetName);

        buf.writeVarInt(inventory.size());
        for (ItemStack stack : inventory) {
            buf.writeItem(stack);
        }

        buf.writeVarInt(armor.size());
        for (ItemStack stack : armor) {
            buf.writeItem(stack);
        }

        buf.writeItem(offhand);
    }

    public static AdminInventorySnapshot decode(FriendlyByteBuf buf) {
        UUID targetId = buf.readUUID();
        String targetName = buf.readUtf();

        int invSize = buf.readVarInt();
        List<ItemStack> inventory = new ArrayList<>(invSize);
        for (int i = 0; i < invSize; i++) {
            inventory.add(buf.readItem());
        }

        int armorSize = buf.readVarInt();
        List<ItemStack> armor = new ArrayList<>(armorSize);
        for (int i = 0; i < armorSize; i++) {
            armor.add(buf.readItem());
        }

        ItemStack offhand = buf.readItem();

        return new AdminInventorySnapshot(targetId, targetName, inventory, armor, offhand);
    }
}
