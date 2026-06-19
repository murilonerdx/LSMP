package br.com.murilo.liberthia.magic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * v0.1.22 r36: Conhecimento de feitiços do player. Armazenado no
 * persistentData do player (sync via packet quando aprende).
 *
 * <h2>Persistência</h2>
 * NBT key {@code liberthia.grimoire.spells} = ListTag de string IDs.
 *
 * <h2>API</h2>
 * <ul>
 *   <li>{@link #learn(Player, String)} — adiciona spell, broadcast pro client</li>
 *   <li>{@link #knows(Player, String)} — checa se aprendeu</li>
 *   <li>{@link #known(Player)} — set imutável de IDs aprendidos</li>
 *   <li>{@link #setSelected(Player, String)} — qual feitiço está "equipado" no grimoire</li>
 *   <li>{@link #getSelected(Player)} — feitiço atual selecionado</li>
 * </ul>
 */
public final class PlayerSpellKnowledge {

    public static final String NBT_KEY = "liberthia.grimoire.spells";
    public static final String NBT_SELECTED = "liberthia.grimoire.selected";
    public static final String NBT_MANA = "liberthia.grimoire.mana";
    public static final int MAX_MANA = 100;
    public static final int MANA_REGEN_INTERVAL = 20; // 1s
    public static final int MANA_PER_TICK = 2;

    private PlayerSpellKnowledge() {}

    public static Set<String> known(Player p) {
        CompoundTag tag = p.getPersistentData();
        if (!tag.contains(NBT_KEY)) return new HashSet<>();
        ListTag list = tag.getList(NBT_KEY, 8);
        Set<String> out = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    public static boolean knows(Player p, String spellId) {
        return known(p).contains(spellId);
    }

    /** Adiciona spell. Retorna true se NOVO (false se já tinha). */
    public static boolean learn(Player p, String spellId) {
        if (knows(p, spellId)) return false;
        Set<String> set = known(p);
        set.add(spellId);
        saveSet(p, set);
        if (p instanceof ServerPlayer sp) {
            Spell s = SpellRegistry.get(spellId);
            String name = s == null ? spellId : s.name;
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§d§l✦ FEITIÇO APRENDIDO: §r§5" + name), false);
            sp.level().playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
        }
        return true;
    }

    public static void forget(Player p, String spellId) {
        Set<String> set = known(p);
        if (set.remove(spellId)) saveSet(p, set);
    }

    private static void saveSet(Player p, Set<String> set) {
        ListTag list = new ListTag();
        for (String id : set) list.add(StringTag.valueOf(id));
        p.getPersistentData().put(NBT_KEY, list);
    }

    public static String getSelected(Player p) {
        return p.getPersistentData().getString(NBT_SELECTED);
    }

    public static void setSelected(Player p, String spellId) {
        if (spellId == null || spellId.isEmpty()) {
            p.getPersistentData().remove(NBT_SELECTED);
        } else {
            p.getPersistentData().putString(NBT_SELECTED, spellId);
        }
    }

    // ─── MANA SYSTEM ───

    public static int getMana(Player p) {
        var tag = p.getPersistentData();
        return tag.contains(NBT_MANA) ? tag.getInt(NBT_MANA) : MAX_MANA;
    }

    public static void setMana(Player p, int mana) {
        p.getPersistentData().putInt(NBT_MANA, Math.max(0, Math.min(MAX_MANA, mana)));
    }

    public static boolean consumeMana(Player p, int amount) {
        if (br.com.murilo.liberthia.magic.antimagic.AntiMagic.isSuppressed(p)) {
            br.com.murilo.liberthia.magic.antimagic.AntiMagic.notifySuppressed(p);
            return false;
        }
        int cur = getMana(p);
        if (cur < amount) return false;
        setMana(p, cur - amount);
        br.com.murilo.liberthia.magic.antimagic.AntiMagic.markCast(p);
        return true;
    }
}
