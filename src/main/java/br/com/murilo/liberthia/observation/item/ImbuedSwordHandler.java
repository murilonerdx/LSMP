package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.api.ObservationPart;
import br.com.murilo.liberthia.observation.api.ObservationRegistry;
import br.com.murilo.liberthia.observation.api.ObservationResolver;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * v0.1.22 r71: <b>Imbued Sword Handler</b> — sistema de encantar QUALQUER espada
 * com um feitiço.
 *
 * <p>NBT layout em espadas encantadas:
 * <pre>
 * liberthia.imbued_spell: ListTag&lt;StringTag&gt; — recipe (parts ids)
 * liberthia.imbued_name: String — nome do feitiço
 * liberthia.imbued_color: Int — cor RGB
 * liberthia.imbued_class: String — classe elemental (FIRE/WATER/etc)
 * </pre>
 *
 * <h2>Mecânica</h2>
 * Quando o ImbuementBlock combina espada + parchment + soul fragment:
 * <ul>
 *   <li>NBT do parchment é gravado no NBT da espada</li>
 *   <li>Hit on entity: 25% chance de cast spell automaticamente (cooldown 60t)</li>
 *   <li>Right-click: cast spell sempre (custo Source)</li>
 *   <li>Tooltip mostra spell + classe + custo</li>
 * </ul>
 */
public final class ImbuedSwordHandler {

    public static final String NBT_RECIPE = "liberthia.imbued_spell";
    public static final String NBT_NAME = "liberthia.imbued_name";
    public static final String NBT_COLOR = "liberthia.imbued_color";
    public static final String NBT_CLASS = "liberthia.imbued_class";

    private ImbuedSwordHandler() {}

    public static boolean isImbued(ItemStack sword) {
        return sword.getOrCreateTag().contains(NBT_RECIPE);
    }

    /** Aplica recipe do parchment à espada. */
    public static void imbueSword(ItemStack sword, ItemStack parchment) {
        List<String> recipe = SpellParchmentItem.getRecipe(parchment);
        if (recipe.isEmpty()) return;
        CompoundTag tag = sword.getOrCreateTag();
        ListTag list = new ListTag();
        for (String id : recipe) list.add(StringTag.valueOf(id));
        tag.put(NBT_RECIPE, list);
        // Copia nome/cor do parchment
        CompoundTag parchTag = parchment.getOrCreateTag();
        if (parchTag.contains(SpellParchmentItem.NBT_NAME)) {
            tag.putString(NBT_NAME, parchTag.getString(SpellParchmentItem.NBT_NAME));
        } else {
            tag.putString(NBT_NAME, "Imbued");
        }
        if (parchTag.contains(SpellParchmentItem.NBT_COLOR)) {
            tag.putInt(NBT_COLOR, parchTag.getInt(SpellParchmentItem.NBT_COLOR));
        }
        // Determina classe elemental
        var spell = SpellParchmentItem.buildSpell(parchment);
        if (spell != null) {
            var dominantClass = findDominantClass(spell);
            tag.putString(NBT_CLASS, dominantClass.name());
        }
    }

    /** Encontra a classe elemental dominante na recipe. */
    public static br.com.murilo.liberthia.observation.api.SpellClass findDominantClass(ObservationSpell spell) {
        java.util.Map<br.com.murilo.liberthia.observation.api.SpellClass, Integer> counts = new java.util.HashMap<>();
        for (var part : spell.recipe()) {
            counts.merge(part.spellClass(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
            .filter(e -> e.getKey() != br.com.murilo.liberthia.observation.api.SpellClass.NONE)
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(br.com.murilo.liberthia.observation.api.SpellClass.NONE);
    }

    /** Constrói ObservationSpell do NBT da espada. */
    public static ObservationSpell buildSpellFromSword(ItemStack sword) {
        CompoundTag tag = sword.getOrCreateTag();
        if (!tag.contains(NBT_RECIPE)) return null;
        ListTag list = tag.getList(NBT_RECIPE, 8);
        String name = tag.contains(NBT_NAME) ? tag.getString(NBT_NAME) : "Imbued";
        int color = tag.contains(NBT_COLOR) ? tag.getInt(NBT_COLOR) : 0x9d4dd6;
        ObservationSpell.Builder b = ObservationSpell.builder(name, color);
        for (int i = 0; i < list.size(); i++) {
            ObservationPart p = ObservationRegistry.get(new ResourceLocation(list.getString(i)));
            if (p != null) b.add(p);
        }
        return b.build();
    }

    /**
     * Trigger on hit. Chamado em hooks de attack se a sword é imbued.
     * 25% chance de procar o spell quando bate em mob.
     */
    public static boolean tryProcOnHit(ItemStack sword, LivingEntity attacker, LivingEntity target) {
        if (!isImbued(sword)) return false;
        if (!(attacker instanceof ServerPlayer sp)) return false;
        if (sp.getCooldowns().isOnCooldown(sword.getItem())) return false;
        if (Math.random() >= 0.25) return false;
        ObservationSpell spell = buildSpellFromSword(sword);
        if (spell == null) return false;
        // Cast targetting the hit entity
        // Por simplicidade, casta no caster (effects que precisam de target serão null)
        ObservationResolver.cast(sp, spell);
        sp.getCooldowns().addCooldown(sword.getItem(), 60);
        return true;
    }

    public static void appendImbuedTooltip(ItemStack sword, List<Component> tooltip) {
        if (!isImbued(sword)) return;
        CompoundTag tag = sword.getOrCreateTag();
        ObservationSpell spell = buildSpellFromSword(sword);
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§5§l✦ Imbued §r§e" + tag.getString(NBT_NAME)));
        if (spell != null) {
            tooltip.add(Component.literal("§7Glyphs: §f" + spell.recipe().size()
                + " §7| Source: §c" + spell.totalSourceCost()));
        }
        if (tag.contains(NBT_CLASS)) {
            try {
                var clazz = br.com.murilo.liberthia.observation.api.SpellClass.valueOf(tag.getString(NBT_CLASS));
                tooltip.add(Component.literal("§7Classe: ").append(clazz.component()));
            } catch (Throwable ignored) {}
        }
        tooltip.add(Component.literal("§8§o25% proc on hit + right-click cast"));
    }
}
