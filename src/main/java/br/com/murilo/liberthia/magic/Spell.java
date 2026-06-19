package br.com.murilo.liberthia.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;

/**
 * v0.1.22 r36: Data class de um feitiço aprendido.
 *
 * <p>Cada Spell tem:
 * <ul>
 *   <li><b>id</b>: chave única (ex: "void_bolt") — usada em NBT/save</li>
 *   <li><b>name</b>: display name traduzível</li>
 *   <li><b>type</b>: classificação (projectile/beam/aoe/buff/teleport/summon/heal/drain)</li>
 *   <li><b>school</b>: escola ocultista (goetia/kabbalah/necro/hauntologia/cosmic)</li>
 *   <li><b>manaCost</b>: custo em pontos de mana (player tem 100 max)</li>
 *   <li><b>cooldownTicks</b>: ticks entre casts</li>
 *   <li><b>damage</b>: dano (varia por tipo)</li>
 *   <li><b>range</b>: alcance em blocos</li>
 *   <li><b>color</b>: cor primária do feitiço (RGB hex) — usado em VFX</li>
 *   <li><b>rarity</b>: dropratabilidade (common/rare/epic/legendary)</li>
 *   <li><b>lore</b>: descrição mística (tooltip)</li>
 *   <li><b>sourceEntity</b>: qual entidade ensina este spell (Bael, Sandalphon...)</li>
 * </ul>
 */
public final class Spell {

    public final String id;
    public final String name;
    public final SpellType type;
    public final String school;
    public final int manaCost;
    public final int cooldownTicks;
    public final float damage;
    public final float range;
    public final int color;
    public final Rarity rarity;
    public final String lore;
    public final String sourceEntity;

    public Spell(String id, String name, SpellType type, String school,
                 int manaCost, int cooldownTicks, float damage, float range,
                 int color, Rarity rarity, String lore, String sourceEntity) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.school = school;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.damage = damage;
        this.range = range;
        this.color = color;
        this.rarity = rarity;
        this.lore = lore;
        this.sourceEntity = sourceEntity;
    }

    public Component displayName() {
        var fmt = switch (rarity) {
            case COMMON -> net.minecraft.ChatFormatting.WHITE;
            case UNCOMMON -> net.minecraft.ChatFormatting.GREEN;
            case RARE -> net.minecraft.ChatFormatting.AQUA;
            case EPIC -> net.minecraft.ChatFormatting.LIGHT_PURPLE;
            default -> net.minecraft.ChatFormatting.GOLD;
        };
        return Component.literal(name).withStyle(fmt);
    }
}
