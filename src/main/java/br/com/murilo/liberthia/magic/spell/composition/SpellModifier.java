package br.com.murilo.liberthia.magic.spell.composition;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * v0.1.151 r119: <b>SpellModifier</b> — atoms que modificam um base spell.
 *
 * <p>Inspirado no padrão de "augments" do Ars Nouveau e nas "pages" do Iron's
 * Spells (cada página = mais poder mas custa mais mana). Cada modifier muda o
 * comportamento ou stats do spell base.
 *
 * <h2>Tipos de modifier</h2>
 * <ul>
 *   <li>{@code AMPLIFY} — +50% dano por stack (até 4 stacks)</li>
 *   <li>{@code AOE} — converte projétil em explosão; +50% mana</li>
 *   <li>{@code PIERCE} — projétil atravessa alvos; +30% mana</li>
 *   <li>{@code MULTISHOT} — dispara 3 projéteis; +80% mana</li>
 *   <li>{@code CHAIN} — pula entre alvos; +60% mana</li>
 *   <li>{@code IGNITE} — incendeia alvo 8s; +20% mana</li>
 *   <li>{@code FREEZE} — congela alvo 4s; +25% mana</li>
 *   <li>{@code KNOCKBACK} — empurra alvo; +15% mana</li>
 *   <li>{@code LIFESTEAL} — cura 25% do dano causado; +40% mana</li>
 *   <li>{@code RANGE} — +80% alcance; +20% mana</li>
 *   <li>{@code SUSTAIN} — efeito dura 5s extra; +35% mana</li>
 *   <li>{@code PENETRATE} — ignora 50% da armor; +30% mana</li>
 *   <li>{@code APOLAO} — modifier supremo: ×5 dano, ×3 mana — exige spell EPIC</li>
 * </ul>
 *
 * <h2>Stacking</h2>
 * Cada modifier pode ser stackado até {@link #maxStacks} vezes. Mana custa
 * crescentemente (1.5× por stack do MESMO modifier).
 */
public enum SpellModifier {
    AMPLIFY    ("amplify",    "Amplificar",  1.2F, 1.5F,   1.0F, 1.0F, 4, ChatFormatting.GOLD,
                "Aumenta o dano em 50%. Pode aplicar várias vezes."),
    AOE        ("aoe",        "Área",        1.5F, 1.0F,   1.0F, 1.0F, 2, ChatFormatting.RED,
                "Converte projétil em explosão de 4 blocos de raio."),
    PIERCE     ("pierce",     "Perfuração",  1.3F, 1.0F,   1.0F, 1.0F, 1, ChatFormatting.WHITE,
                "Projétil atravessa todos os alvos no caminho."),
    MULTISHOT  ("multishot",  "Multi-Tiro",  1.8F, 0.7F,   1.0F, 1.0F, 2, ChatFormatting.AQUA,
                "Dispara 3 projéteis em leque (cada com 70% dano)."),
    CHAIN      ("chain",      "Encadear",    1.6F, 0.6F,   1.0F, 1.0F, 1, ChatFormatting.YELLOW,
                "Após atingir um alvo, pula para até 5 alvos próximos."),
    IGNITE     ("ignite",     "Igniscência", 1.2F, 1.0F,   1.0F, 1.0F, 2, ChatFormatting.RED,
                "Alvo pega fogo por 8 segundos (4 dmg/s)."),
    FREEZE     ("freeze",     "Congelar",    1.25F,1.0F,   1.0F, 1.0F, 2, ChatFormatting.AQUA,
                "Alvo é congelado e fica imóvel por 4 segundos."),
    KNOCKBACK  ("knockback",  "Repulsão",    1.15F,1.0F,   1.0F, 1.0F, 3, ChatFormatting.GRAY,
                "Empurra o alvo (+1 bloco por stack)."),
    LIFESTEAL  ("lifesteal",  "Drenar Vida", 1.4F, 1.0F,   1.0F, 1.0F, 2, ChatFormatting.DARK_RED,
                "Recupera 25% do dano causado como HP."),
    RANGE      ("range",      "Alcance",     1.2F, 1.0F,   1.8F, 1.0F, 3, ChatFormatting.GREEN,
                "Aumenta o alcance em 80%."),
    SUSTAIN    ("sustain",    "Persistência",1.35F,1.0F,   1.0F, 1.0F, 2, ChatFormatting.LIGHT_PURPLE,
                "Efeitos duram +5 segundos. Cooldown +50%."),
    PENETRATE  ("penetrate",  "Penetração",  1.3F, 1.0F,   1.0F, 1.0F, 1, ChatFormatting.DARK_GRAY,
                "Ignora 50% da armor do alvo."),
    APOLAO     ("apolao",     "Apolão",      3.0F, 5.0F,   1.5F, 2.0F, 1, ChatFormatting.DARK_RED,
                "MODIFIER LENDÁRIO: ×5 dano, ×3 mana, ×2 cooldown. Exige spell EPIC."),
    // ── r173: 10 novas "Runas" (modifiers) com efeitos REAIS (vanilla MobEffects) ──
    POISON     ("poison",     "Veneno",      1.2F, 1.0F,   1.0F, 1.0F, 2, ChatFormatting.DARK_GREEN,
                "Envenena o alvo (Veneno II por 6s/stack)."),
    WITHER     ("wither",     "Definhar",    1.3F, 1.0F,   1.0F, 1.0F, 2, ChatFormatting.DARK_GRAY,
                "Aplica Wither II por 6s/stack — dano que ignora armadura."),
    SLOW       ("slow",       "Lentidão",    1.1F, 1.0F,   1.0F, 1.0F, 2, ChatFormatting.GRAY,
                "Lentidão III por 8s/stack."),
    WEAKEN     ("weaken",     "Enfraquecer", 1.15F,1.0F,   1.0F, 1.0F, 2, ChatFormatting.DARK_GRAY,
                "Fraqueza II por 8s/stack — reduz o dano corpo-a-corpo do alvo."),
    BLIND      ("blind",      "Cegar",       1.2F, 1.0F,   1.0F, 1.0F, 1, ChatFormatting.BLACK,
                "Cega o alvo por 6s."),
    GRAVITY    ("gravity",    "Gravidade",   1.25F,1.0F,   1.0F, 1.0F, 2, ChatFormatting.DARK_PURPLE,
                "Puxa o alvo na sua direção (efeito de gravidade)."),
    LEVITATE   ("levitate",   "Levitar",     1.25F,1.0F,   1.0F, 1.0F, 2, ChatFormatting.AQUA,
                "Levitação por 4s/stack — joga o alvo pro alto."),
    WARD       ("ward",       "Égide",       1.3F, 1.0F,   1.0F, 1.2F, 2, ChatFormatting.GOLD,
                "Pós-cast: você ganha Absorção II + Resistência II por 8s/stack."),
    HASTEN     ("hasten",     "Pressa",      1.2F, 1.0F,   1.0F, 1.0F, 2, ChatFormatting.YELLOW,
                "Pós-cast: você ganha Velocidade II por 10s/stack."),
    SMITE      ("smite",      "Julgamento",  1.3F, 1.2F,   1.0F, 1.0F, 3, ChatFormatting.WHITE,
                "Dano sagrado extra massivo contra mortos-vivos.");

    public final String id;
    public final String displayName;
    /** Multiplicador de custo de mana por stack desse modifier. */
    public final float manaMultiplier;
    /** Multiplicador de dano por stack. */
    public final float damageMultiplier;
    /** Multiplicador de range. */
    public final float rangeMultiplier;
    /** Multiplicador de cooldown. */
    public final float cooldownMultiplier;
    /** Quantos stacks são permitidos no mesmo spell. */
    public final int maxStacks;
    public final ChatFormatting color;
    public final String description;

    SpellModifier(String id, String displayName,
                  float mana, float damage, float range, float cooldown,
                  int maxStacks, ChatFormatting color, String description) {
        this.id = id;
        this.displayName = displayName;
        this.manaMultiplier = mana;
        this.damageMultiplier = damage;
        this.rangeMultiplier = range;
        this.cooldownMultiplier = cooldown;
        this.maxStacks = maxStacks;
        this.color = color;
        this.description = description;
    }

    public Component label() {
        return Component.literal(displayName).withStyle(color);
    }

    public Component descriptionTooltip() {
        return Component.literal("§7" + description);
    }

    public Component statsTooltip() {
        return Component.literal(String.format(
                "§7×Mana §b%.2f §7• ×Dano §c%.2f §7• ×Alcance §a%.2f §7• ×CD §e%.2f",
                manaMultiplier, damageMultiplier, rangeMultiplier, cooldownMultiplier));
    }

    private static final Map<String, SpellModifier> BY_ID = new LinkedHashMap<>();
    static {
        for (SpellModifier m : values()) BY_ID.put(m.id, m);
    }

    public static SpellModifier byId(String id) {
        return BY_ID.get(id);
    }

    public static java.util.Collection<SpellModifier> all() {
        return BY_ID.values();
    }
}
