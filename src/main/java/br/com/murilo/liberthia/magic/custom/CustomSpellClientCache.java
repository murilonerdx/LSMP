package br.com.murilo.liberthia.magic.custom;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * v0.1.22 r42: <b>Client-side cache</b> dos custom spells do player local —
 * populado via {@link SyncCustomSpellsS2CPacket}.
 *
 * <p>Usado pela {@code SpellWheelScreen} pra desenhar os feitiços disponíveis
 * sem precisar consultar o player NBT a cada frame.
 */
@OnlyIn(Dist.CLIENT)
public final class CustomSpellClientCache {

    private static volatile List<CustomSpell> SPELLS = Collections.emptyList();
    private static volatile UUID SELECTED_ID = null;

    private CustomSpellClientCache() {}

    public static synchronized void update(List<CustomSpell> spells, UUID selectedId) {
        SPELLS = spells == null ? Collections.emptyList() : new ArrayList<>(spells);
        SELECTED_ID = selectedId;
    }

    public static List<CustomSpell> getSpells() {
        return SPELLS;
    }

    public static UUID getSelectedId() {
        return SELECTED_ID;
    }

    /** r140: optimistic local update (será sobrescrito pelo próximo sync server). */
    public static synchronized void setSelectedId(UUID id) {
        SELECTED_ID = id;
    }

    public static CustomSpell getSelected() {
        UUID id = SELECTED_ID;
        if (id == null) return null;
        for (CustomSpell s : SPELLS) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }

    public static void clear() {
        SPELLS = Collections.emptyList();
        SELECTED_ID = null;
    }
}
