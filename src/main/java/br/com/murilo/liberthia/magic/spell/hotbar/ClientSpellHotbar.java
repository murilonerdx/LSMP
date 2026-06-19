package br.com.murilo.liberthia.magic.spell.hotbar;

/**
 * v0.1.162 r138: <b>ClientSpellHotbar</b> — cache client-side dos 3 slots
 * de spells bindeded. Atualizado via {@link SpellHotbarSyncS2CPacket}.
 *
 * <p>HUD lê daqui pra renderizar.
 */
public final class ClientSpellHotbar {

    private static final String[] SPELL_IDS = new String[SpellHotbarData.SLOTS];

    private ClientSpellHotbar() {}

    public static void update(String[] ids) {
        for (int i = 0; i < SpellHotbarData.SLOTS && i < ids.length; i++) {
            SPELL_IDS[i] = (ids[i] == null || ids[i].isEmpty()) ? null : ids[i];
        }
    }

    public static String get(int slot) {
        if (slot < 0 || slot >= SPELL_IDS.length) return null;
        return SPELL_IDS[slot];
    }

    public static boolean hasAny() {
        for (String s : SPELL_IDS) if (s != null) return true;
        return false;
    }
}
