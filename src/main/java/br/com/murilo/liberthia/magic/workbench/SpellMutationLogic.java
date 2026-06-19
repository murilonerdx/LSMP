package br.com.murilo.liberthia.magic.workbench;

import br.com.murilo.liberthia.magic.spell.composition.SpellComposition;
import br.com.murilo.liberthia.magic.spell.composition.SpellModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * r155 Phase 2: Lógica que combina base scroll + modifier glyphs em scroll customizado.
 *
 * <p>Detecta se item modifier slot é um {@code ModifierGlyphItem}, lê o modifier type,
 * cria/atualiza {@link SpellComposition} no output stack.
 */
public final class SpellMutationLogic {

    private SpellMutationLogic() {}

    /**
     * Combina base + modifiers em output.
     * @return cópia do base com SpellComposition aplicado, ou EMPTY se base inválido.
     */
    public static ItemStack combine(ItemStack base, ItemStack[] modifiers) {
        if (base.isEmpty()) return ItemStack.EMPTY;

        // Detecta spell ID do base
        String spellId = extractSpellId(base);
        if (spellId == null) return ItemStack.EMPTY;

        // Coleta lista de modifiers detectados
        java.util.List<SpellModifier> mods = new java.util.ArrayList<>();
        for (ItemStack mod : modifiers) {
            if (mod.isEmpty()) continue;
            SpellModifier sm = detectModifier(mod.getItem());
            if (sm != null) mods.add(sm);
        }

        // Cria composition imutável
        SpellComposition comp = new SpellComposition(spellId, mods);

        // Output = copy do base com composition NBT
        ItemStack output = base.copy();
        output.setCount(1);
        comp.writeToStack(output);

        return output;
    }

    /** Extrai spell ID do scroll — funciona pra FactoryScroll e UniversalScroll. */
    private static String extractSpellId(ItemStack stack) {
        // Factory: NBT "liberthia.factory_spell_id"
        String factoryId = br.com.murilo.liberthia.magic.factory.DynamicSpellItem.spellId(stack);
        if (factoryId != null) return factoryId;
        // Universal: item name = spell ID (mapping in registry)
        if (stack.getItem() instanceof br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem uss) {
            return uss.spellId(stack);
        }
        return null;
    }

    /** Detecta tipo de modifier baseado no Item. */
    private static SpellModifier detectModifier(Item item) {
        // r173: detecção robusta — os modifier items SÃO SpellModifierItem, então
        // lemos o enum direto. (Antes só batia em "mod_*", mas os items são
        // registrados como "modifier_*", então nada era detectado.)
        if (item instanceof br.com.murilo.liberthia.magic.spell.composition.SpellModifierItem smi) {
            return smi.modifier;
        }
        // Fallback legado por nome ("mod_*" ou "modifier_*").
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (id == null || !id.getNamespace().equals("liberthia")) return null;
        String path = id.getPath();
        String name = null;
        if (path.startsWith("modifier_")) name = path.substring("modifier_".length());
        else if (path.startsWith("mod_")) name = path.substring("mod_".length());
        if (name == null) return null;
        try {
            return SpellModifier.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
