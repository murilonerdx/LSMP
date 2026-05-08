package br.com.murilo.liberthia.matter;

import net.minecraft.ChatFormatting;

/**
 * Conteúdo de matéria de um item — quanto de DM/WM/YM ele carrega + a
 * "mutação" (tag composta) derivada das proporções.
 *
 * <p>Cada uma das 6 mutações da lore é classificada por proporção:
 * <ul>
 *   <li><b>Wild</b> (DM puro) — agressivo, transforma hospedeiros</li>
 *   <li><b>Cognitive</b> (WM puro) — inteligente, teleporta, esquece</li>
 *   <li><b>Erratic</b> (YM puro) — caótico emocional, alucinações</li>
 *   <li><b>Symbiotic</b> (DM+WM) — controle, motivações próprias</li>
 *   <li><b>Strategist</b> (YM+WM) — calculista, frio, intensifica vontades</li>
 *   <li><b>Unstable</b> (DM+YM) — REPULSÃO, instável, explosivo</li>
 *   <li><b>Inert</b> — nada relevante</li>
 * </ul>
 *
 * <p>Energia é derivada da quantidade total: {@code (dm+wm+ym) × 100} FE-equiv.
 */
public record MatterContent(float dark, float white, float yellow) {

    public static final MatterContent EMPTY = new MatterContent(0, 0, 0);

    /** Proporção total — usado pra "energia" e "potência". */
    public float total() { return dark + white + yellow; }

    /** Energia derivada em FE-equivalente. */
    public int energyEquivalent() { return (int) (total() * 100); }

    /** Tag de mutação dominante. */
    public Mutation dominantMutation() {
        // Casos especiais primeiro
        if (dark > 20 && yellow > 20) return Mutation.UNSTABLE;
        if (dark > 30 && white < 15 && yellow < 15) return Mutation.WILD;
        if (dark >= 20 && white >= 20 && yellow < 15) return Mutation.SYMBIOTIC;
        if (yellow >= 20 && white >= 20 && dark < 15) return Mutation.STRATEGIST;
        if (white > 30 && dark < 15 && yellow < 15) return Mutation.COGNITIVE;
        if (yellow > 30 && white < 15 && dark < 15) return Mutation.ERRATIC;
        if (total() > 5) return Mutation.INERT;
        return Mutation.NONE;
    }

    public enum Mutation {
        NONE       ("Nenhuma",      ChatFormatting.DARK_GRAY,    "Sem traços de matéria detectados"),
        INERT      ("Inerte",       ChatFormatting.GRAY,         "Matéria detectada mas em equilíbrio estável"),
        WILD       ("Selvagem",     ChatFormatting.DARK_PURPLE,  "Matéria escura pura — agressivo, infectante"),
        COGNITIVE  ("Cognitiva",    ChatFormatting.WHITE,        "Matéria branca pura — apaga memórias, teletransporte"),
        ERRATIC    ("Errática",     ChatFormatting.YELLOW,       "Matéria amarela pura — alucinações, descontrole"),
        SYMBIOTIC  ("Simbiótica",   ChatFormatting.LIGHT_PURPLE, "DM + WM — consciência maligna, motivações próprias"),
        STRATEGIST ("Estrategista", ChatFormatting.GOLD,         "YM + WM — frio, calculista, intensifica vontades"),
        UNSTABLE   ("Instável",     ChatFormatting.RED,          "DM + YM — REPULSÃO. Pode explodir.");

        public final String displayName;
        public final ChatFormatting color;
        public final String description;

        Mutation(String dn, ChatFormatting c, String d) {
            this.displayName = dn; this.color = c; this.description = d;
        }
    }
}
