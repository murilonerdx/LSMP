package br.com.murilo.liberthia.client.hud.unified;

/**
 * r164: Identifiers + default positions for every draggable HUD in the mod.
 *
 * <p>Each enum stores:
 * <ul>
 *   <li>{@link #id} — string id used in NBT/packets</li>
 *   <li>{@link #displayName} — pt-BR label shown in the editor</li>
 *   <li>{@link #defaultX}/{@link #defaultY} — fallback position (px from top-left)</li>
 *   <li>{@link #width}/{@link #height} — drag-handle bbox for hit-test in editor</li>
 *   <li>{@link #anchorRight}/{@link #anchorBottom} — if true, X/Y are interpreted
 *       relative to the right/bottom edge of the screen (useful pra HUDs que
 *       devem ficar no canto direito independente da resolução)</li>
 * </ul>
 *
 * <p>Default coords escolhidos pra <b>não sobrepor</b>: sanity bottom-right
 * canto, mana bar no centro acima da hotbar, matter HUDs top-left, source HUD
 * bottom-left.
 */
public enum HudId {
    SANITY_HUD     ("sanity_hud",     "Sanidade",          true,  true,  88,  50,  100, 18),
    MANA_BAR       ("mana_bar",       "Mana / Level",      false, true,  60,  50,  158, 16),
    SOURCE_HUD     ("source_hud",     "Source (detalhe)",  false, true,  4,   65,  140, 44),
    MATTER_ENERGY  ("matter_energy",  "Matter Energy",     false, false, 6,   6,   140, 20),
    MATTER_PROFILE ("matter_profile", "Matter Profile",    false, false, 6,   30,  110, 38),
    INFECTION_HUD  ("infection_hud",  "Infecção",          false, false, 6,   72,  110, 38),
    DNA_MUTATION   ("dna_mutation",   "DNA Mutação",       false, false, 6,   115, 105, 38),
    RADIATION      ("radiation",      "Radiação / Defesa", false, false, 6,   158, 140, 30),
    SPELL_BOOK_HUD ("spell_book_hud", "Spell Book / Hotbar", true, false, 80,  6,   180, 26),
    CAST_BAR       ("cast_bar",       "Cast Bar (Spell)",  false, true,  240, 38,  180, 22),
    RECAST_COUNTER ("recast_counter", "Recast Counter",    false, true,  80,  120, 60,  18);

    public final String id;
    public final String displayName;
    public final boolean anchorRight;
    public final boolean anchorBottom;
    public final int defaultX;
    public final int defaultY;
    public final int width;
    public final int height;

    HudId(String id, String displayName, boolean anchorRight, boolean anchorBottom,
          int defaultX, int defaultY, int width, int height) {
        this.id = id;
        this.displayName = displayName;
        this.anchorRight = anchorRight;
        this.anchorBottom = anchorBottom;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.width = width;
        this.height = height;
    }

    public static HudId byId(String id) {
        for (HudId h : values()) if (h.id.equals(id)) return h;
        return null;
    }
}
