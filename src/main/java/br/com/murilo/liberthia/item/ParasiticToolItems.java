package br.com.murilo.liberthia.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

/**
 * r194 — Ferramentas Parasíticas (do Coração da Colmeia): se regeneram sozinhas (cura 1 de
 * durabilidade a cada 60 ticks enquanto na mão/inventário). Tier = matéria escura.
 */
public final class ParasiticToolItems {
    private ParasiticToolItems() {}

    private static void regen(ItemStack stack, Level level, int slot) {
        if (level.isClientSide) return;
        if (stack.isDamaged() && level.getGameTime() % 60 == 0) stack.setDamageValue(stack.getDamageValue() - 1);
    }
    private static Item.Properties props() { return new Item.Properties().rarity(Rarity.EPIC); }

    public static class Pickaxe extends PickaxeItem {
        public Pickaxe() { super(DarkMatterToolMaterial.INSTANCE, 1, -2.8F, props()); }
        @Override public void inventoryTick(ItemStack s, Level l, Entity e, int slot, boolean sel) { regen(s, l, slot); }
        @Override public boolean isFoil(ItemStack s) { return true; }
    }
    public static class Sword extends SwordItem {
        public Sword() { super(DarkMatterToolMaterial.INSTANCE, 4, -2.4F, props()); }
        @Override public void inventoryTick(ItemStack s, Level l, Entity e, int slot, boolean sel) { regen(s, l, slot); }
        @Override public boolean isFoil(ItemStack s) { return true; }
    }
    public static class Axe extends AxeItem {
        public Axe() { super(DarkMatterToolMaterial.INSTANCE, 5, -3.0F, props()); }
        @Override public void inventoryTick(ItemStack s, Level l, Entity e, int slot, boolean sel) { regen(s, l, slot); }
        @Override public boolean isFoil(ItemStack s) { return true; }
    }
}
