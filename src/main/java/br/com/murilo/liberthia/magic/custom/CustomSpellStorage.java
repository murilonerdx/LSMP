package br.com.murilo.liberthia.magic.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * v0.1.22 r42: Storage de custom spells por player.
 *
 * <p>Persistido em {@code player.getPersistentData()} sob a chave
 * {@code liberthia.custom_spells} (ListTag de CompoundTag).
 *
 * <p>Máximo de {@link #MAX_SPELLS_PER_PLAYER} feitiços salvos por player —
 * passa do limite, o mais antigo é removido.
 */
public final class CustomSpellStorage {

    public static final String NBT_KEY = "liberthia.custom_spells";
    public static final String NBT_SELECTED = "liberthia.custom_spell_selected";
    public static final int MAX_SPELLS_PER_PLAYER = 24;

    private CustomSpellStorage() {}

    /** Lê lista completa do player (newest last). */
    public static List<CustomSpell> getAll(Player p) {
        var data = p.getPersistentData();
        List<CustomSpell> out = new ArrayList<>();
        if (!data.contains(NBT_KEY)) return out;
        ListTag list = data.getList(NBT_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try { out.add(CustomSpell.fromNbt(list.getCompound(i))); }
            catch (Throwable ignored) {}
        }
        return out;
    }

    /** Salva lista completa sobrescrevendo. */
    public static void saveAll(Player p, List<CustomSpell> spells) {
        ListTag list = new ListTag();
        for (CustomSpell s : spells) list.add(s.toNbt());
        p.getPersistentData().put(NBT_KEY, list);
    }

    /** Adiciona novo spell. Retorna true se aceitou (limit cap). */
    public static boolean add(Player p, CustomSpell s) {
        List<CustomSpell> list = getAll(p);
        if (list.size() >= MAX_SPELLS_PER_PLAYER) {
            list.remove(0); // remove oldest
        }
        list.add(s);
        saveAll(p, list);
        return true;
    }

    /** Remove um spell pelo UUID. */
    public static boolean remove(Player p, UUID spellId) {
        List<CustomSpell> list = getAll(p);
        boolean removed = list.removeIf(s -> s.id.equals(spellId));
        if (removed) saveAll(p, list);
        return removed;
    }

    /** Acha um spell pelo UUID. */
    public static CustomSpell find(Player p, UUID spellId) {
        for (CustomSpell s : getAll(p)) {
            if (s.id.equals(spellId)) return s;
        }
        return null;
    }

    /** UUID do spell selecionado pela wheel (o que vai castar com keybind). */
    public static UUID getSelectedId(Player p) {
        var data = p.getPersistentData();
        if (!data.hasUUID(NBT_SELECTED)) return null;
        return data.getUUID(NBT_SELECTED);
    }

    public static void setSelected(Player p, UUID id) {
        if (id == null) p.getPersistentData().remove(NBT_SELECTED);
        else p.getPersistentData().putUUID(NBT_SELECTED, id);
    }

    public static CustomSpell getSelected(Player p) {
        UUID id = getSelectedId(p);
        if (id == null) return null;
        return find(p, id);
    }

    /** Limpa tudo (admin). */
    public static void clearAll(Player p) {
        p.getPersistentData().remove(NBT_KEY);
        p.getPersistentData().remove(NBT_SELECTED);
    }
}
