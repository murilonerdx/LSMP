package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;

/**
 * v0.1.143 r111: <b>SpellDef</b> — definição metadata + behavior de um feitiço.
 *
 * <p>Inspirado no Iron's Spells N Spellbooks ({@code AbstractSpell}). Cada spell
 * tem:
 * <ul>
 *   <li>{@code id} — chave única (ex: "fireball")</li>
 *   <li>{@code name} — display name PT-BR</li>
 *   <li>{@code school} — escola de magia (FIRE, ICE, ...)</li>
 *   <li>{@code rarity} — common/uncommon/rare/epic</li>
 *   <li>{@code manaCost} — custo em pontos de Source/Mana (0–100)</li>
 *   <li>{@code cooldownTicks} — ticks até poder castar de novo</li>
 *   <li>{@code damage} — dano base (varia por spell)</li>
 *   <li>{@code range} — alcance em blocos</li>
 *   <li>{@code lore} — descrição mística (tooltip 2)</li>
 *   <li>{@code cast} — lambda que executa o feitiço</li>
 * </ul>
 *
 * <p>Builder fluente pra montar registros legíveis na {@link SpellLibrary}.
 */
public final class SpellDef {

    public final String id;
    public final String name;
    public final SpellSchool school;
    public final Rarity rarity;
    public final int manaCost;
    public final int cooldownTicks;
    public final float damage;
    public final float range;
    public final String lore;
    public final SpellCast cast;

    private SpellDef(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.school = b.school;
        this.rarity = b.rarity;
        this.manaCost = b.manaCost;
        this.cooldownTicks = b.cooldownTicks;
        this.damage = b.damage;
        this.range = b.range;
        this.lore = b.lore;
        this.cast = b.cast;
    }

    /** Display tinted by school color + rarity. */
    public Component displayName() {
        return Component.literal(name).withStyle(school.color());
    }

    /** Tooltip line 1 — stats. */
    public Component statsTooltip() {
        return Component.literal("§7Mana §b" + manaCost + " §7• CD §c" + (cooldownTicks / 20) + "s"
                + (damage > 0 ? " §7• Dano §c" + damage : "")
                + (range > 0 ? " §7• Alcance §a" + range + "b" : ""));
    }

    public Component loreTooltip() {
        return Component.literal("§o§8" + lore);
    }

    public Component schoolTooltip() {
        return Component.literal("§7Escola: ").append(school.label()).append("  §7Raridade: ")
                .append(Component.literal(rarityLabel()).withStyle(rarityColor()));
    }

    private String rarityLabel() {
        return switch (rarity) {
            case COMMON -> "Comum";
            case UNCOMMON -> "Incomum";
            case RARE -> "Raro";
            case EPIC -> "Épico";
        };
    }

    private ChatFormatting rarityColor() {
        return switch (rarity) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.AQUA;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String name = "";
        private SpellSchool school = SpellSchool.FIRE;
        private Rarity rarity = Rarity.COMMON;
        private int manaCost = 10;
        private int cooldownTicks = 40;
        private float damage = 0;
        private float range = 0;
        private String lore = "";
        private SpellCast cast = ctx -> false;

        public Builder(String id) {
            this.id = id;
        }

        public Builder name(String name) { this.name = name; return this; }
        public Builder school(SpellSchool s) { this.school = s; return this; }
        public Builder rarity(Rarity r) { this.rarity = r; return this; }
        public Builder mana(int m) { this.manaCost = m; return this; }
        public Builder cooldown(int t) { this.cooldownTicks = t; return this; }
        public Builder damage(float d) { this.damage = d; return this; }
        public Builder range(float r) { this.range = r; return this; }
        public Builder lore(String l) { this.lore = l; return this; }
        public Builder cast(SpellCast c) { this.cast = c; return this; }

        public SpellDef build() { return new SpellDef(this); }
    }
}
