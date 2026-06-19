package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.api.ObservationPart;
import br.com.murilo.liberthia.observation.api.ObservationRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r68: <b>GlyphItem</b> — item físico que representa um ObservationPart.
 *
 * <p>Pattern AN's {@code Glyph.java}: cada AbstractSpellPart tem um corresponding
 * Glyph item que existe no inventory do player. Sem glyph item → não pode usar
 * o part numa receita.
 *
 * <h2>Uso</h2>
 * <ul>
 *   <li>Hold no inventário pra "saber" o glyph</li>
 *   <li>Right-click com SpellParchment na offhand: adiciona o glyph à recipe</li>
 *   <li>Tooltip mostra: tipo (Method/Effect/Distortion), custo Source, lore</li>
 * </ul>
 */
public class GlyphItem extends Item {

    /** ResourceLocation do ObservationPart que esse glyph representa. */
    public final String partId;

    public GlyphItem(Properties properties, String partId) {
        super(properties.stacksTo(16).rarity(Rarity.UNCOMMON));
        this.partId = partId;
    }

    /** Lookup do ObservationPart no registry. Pode retornar null se part não init. */
    @Nullable
    public ObservationPart getPart() {
        return ObservationRegistry.get(new ResourceLocation(partId));
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        ObservationPart part = getPart();
        if (part == null) {
            tooltip.add(Component.literal("§c⚠ Part não registrado: " + partId));
            return;
        }

        // Tipo
        String typeLabel = switch (part.typeIndex()) {
            case 1 -> "§b★ Método de Observação";
            case 5 -> "§d✦ Manifestação";
            case 10 -> "§e◆ Distorção";
            default -> "§7? Desconhecido";
        };
        tooltip.add(Component.literal(typeLabel));

        // Custos
        tooltip.add(Component.literal("§7Source: §e" + part.sourceCost()
            + " §7| Sanidade: §c" + part.sanityCost()));
        tooltip.add(Component.empty());

        // Lore do part
        for (Component lore : part.lore()) {
            tooltip.add(lore);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§7§oRight-click com Pergaminho na offhand")
            .withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.literal("§7§opra adicionar ao feitiço.")
            .withStyle(ChatFormatting.ITALIC));
    }

    @Override
    public Component getName(ItemStack stack) {
        ObservationPart part = getPart();
        if (part != null) {
            return Component.literal("Glifo: ")
                .append(part.displayComponent());
        }
        return super.getName(stack);
    }
}
