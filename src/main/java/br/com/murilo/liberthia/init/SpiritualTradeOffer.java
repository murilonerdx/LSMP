package br.com.murilo.liberthia.init;

import net.minecraft.world.item.ItemStack;

public record SpiritualTradeOffer(ItemStack cost, ItemStack result) {

    public boolean isValid() {
        return !cost.isEmpty() && !result.isEmpty();
    }
}
