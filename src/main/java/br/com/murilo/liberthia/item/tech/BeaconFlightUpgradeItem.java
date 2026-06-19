package br.com.murilo.liberthia.item.tech;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r186 — <b>Núcleo de Voo Global</b>: upgrade da Forja/Farol de Voo. Colocado no slot do Farol de
 * Voo, faz o farol conceder voo no MUNDO INTEIRO (sem limite de região) por FE, drenando muito
 * mais energia. Craft difícil.
 */
public class BeaconFlightUpgradeItem extends Item {
    public BeaconFlightUpgradeItem() {
        super(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.literal("§dColoque no Farol de Voo para voar no MUNDO INTEIRO."));
        tips.add(Component.literal("§7Drena muita energia (500 FE/tick)."));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
}
