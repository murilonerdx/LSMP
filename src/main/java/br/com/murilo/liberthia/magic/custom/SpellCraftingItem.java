package br.com.murilo.liberthia.magic.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r42: <b>Mesa de Feitiços (item portátil)</b> — right-click abre a
 * Spell Crafting GUI pra criar novos custom spells.
 */
public class SpellCraftingItem extends Item {

    public SpellCraftingItem(Properties p) { super(p.stacksTo(1)); }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) {
            openCraftingScreen();
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openCraftingScreen() {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new br.com.murilo.liberthia.magic.custom.client.SpellCraftingScreen());
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§5§oMesa de Feitiços Portátil").withStyle(ChatFormatting.ITALIC));
        t.add(Component.literal("§7Right-click pra abrir a §dinterface de criação§r§7."));
        t.add(Component.empty());
        t.add(Component.literal("§7Combine: §6sprite VFX§r§7 + §6forma§r§7 + §6elemento§r§7 + §6poder§r§7"));
        t.add(Component.literal("§7Custos de mana/dano/cd calculados automaticamente."));
        t.add(Component.empty());
        t.add(Component.literal("§a§lController:"));
        t.add(Component.literal("§7  X = abrir §dSpell Wheel§r§7 (radial)"));
        t.add(Component.literal("§7  V = quick-cast do selecionado"));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"Pequenos truques, grandes consequências.\""));
    }
}
