package br.com.murilo.liberthia.magic.spell.hotbar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * v0.1.162 r138: <b>SpellHotbarData</b> — 3 slots de quick-cast per player.
 *
 * <p>Storage: server-side via {@code Player.getPersistentData()}. Cada slot guarda:
 * <ul>
 *   <li>{@code spell_id} — string do SpellDef bound</li>
 *   <li>{@code composition_nbt} — opcional, copy do tag composition do scroll</li>
 * </ul>
 *
 * <p>Keybinds: Z=slot0, X=slot1, C=slot2 (configuráveis via Controls).
 */
public final class SpellHotbarData {

    public static final String NBT_HOTBAR = "liberthia.spell_hotbar";
    /** r155: expandido de 3 (Z/B/N) pra 8 (wheel radial). */
    public static final int SLOTS = 8;

    private SpellHotbarData() {}

    /** Lê spell_id do slot. Retorna null se vazio. */
    public static String getSpellId(Player player, int slot) {
        if (slot < 0 || slot >= SLOTS) return null;
        CompoundTag root = player.getPersistentData();
        if (!root.contains(NBT_HOTBAR)) return null;
        CompoundTag hb = root.getCompound(NBT_HOTBAR);
        String key = "slot_" + slot;
        if (!hb.contains(key)) return null;
        CompoundTag slotTag = hb.getCompound(key);
        if (!slotTag.contains("spell_id")) return null;
        String id = slotTag.getString("spell_id");
        return id.isEmpty() ? null : id;
    }

    /** Lê composition NBT do slot, ou null. */
    public static CompoundTag getCompositionTag(Player player, int slot) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(NBT_HOTBAR)) return null;
        CompoundTag hb = root.getCompound(NBT_HOTBAR);
        String key = "slot_" + slot;
        if (!hb.contains(key)) return null;
        CompoundTag slotTag = hb.getCompound(key);
        if (!slotTag.contains("composition")) return null;
        return slotTag.getCompound("composition");
    }

    /** Bind um spell em slot. */
    public static void bind(Player player, int slot, String spellId, CompoundTag composition) {
        if (slot < 0 || slot >= SLOTS) return;
        CompoundTag root = player.getPersistentData();
        CompoundTag hb = root.contains(NBT_HOTBAR) ? root.getCompound(NBT_HOTBAR) : new CompoundTag();
        CompoundTag slotTag = new CompoundTag();
        slotTag.putString("spell_id", spellId == null ? "" : spellId);
        if (composition != null) slotTag.put("composition", composition);
        hb.put("slot_" + slot, slotTag);
        root.put(NBT_HOTBAR, hb);
    }

    /** Limpa um slot. */
    public static void clear(Player player, int slot) {
        if (slot < 0 || slot >= SLOTS) return;
        CompoundTag root = player.getPersistentData();
        if (!root.contains(NBT_HOTBAR)) return;
        CompoundTag hb = root.getCompound(NBT_HOTBAR);
        hb.remove("slot_" + slot);
        root.put(NBT_HOTBAR, hb);
    }
}
