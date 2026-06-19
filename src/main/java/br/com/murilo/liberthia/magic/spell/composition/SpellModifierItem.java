package br.com.murilo.liberthia.magic.spell.composition;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.151 r119: <b>SpellModifierItem</b> — item físico que representa um
 * {@link SpellModifier}. Usado no SpellWeaver pra adicionar páginas a um
 * spell base.
 *
 * <p>Pattern: 1 ItemClass que delega a um enum-id (igual UniversalSpellScroll).
 */
public class SpellModifierItem extends Item {

    public final SpellModifier modifier;

    public SpellModifierItem(Properties props, SpellModifier modifier) {
        super(props.stacksTo(16));
        this.modifier = modifier;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Glifo: ").append(modifier.label());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return modifier == SpellModifier.APOLAO;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Modificador de Feitiço"));
        tooltip.add(modifier.descriptionTooltip());
        tooltip.add(modifier.statsTooltip());
        tooltip.add(Component.literal("§7Stacks máx: §e" + modifier.maxStacks));
        if (modifier == SpellModifier.APOLAO) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§4§l⚡ APOLÃO — exige Spell EPIC base"));
        }
    }
}
