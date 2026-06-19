package br.com.murilo.liberthia.magic.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * v0.1.22 r42: <b>CustomSpell</b> — feitiço criado pelo player.
 *
 * <h2>Atributos</h2>
 * <ul>
 *   <li><b>id (UUID):</b> chave única por spell</li>
 *   <li><b>name:</b> nome customizado escolhido pelo player</li>
 *   <li><b>sprite:</b> qual VFX sprite (escolhido dos 10)</li>
 *   <li><b>shape:</b> formato (projectile/beam/laser/aoe/self/touch)</li>
 *   <li><b>element:</b> elemento (fire/ice/void/light/blood/arcane/earth/lightning/shadow/nature)</li>
 *   <li><b>power:</b> 1-5 — escala dano e mana cost</li>
 * </ul>
 *
 * <h2>Mana cost calc</h2>
 * {@code manaCost = shape.baseManaCost × (0.5 + power × 0.3)}
 *
 * <h2>Damage calc</h2>
 * {@code damage = 4 × power × shape.damageMultiplier}
 *
 * <h2>Cooldown calc</h2>
 * {@code cooldownTicks = 40 + power × 30}
 */
public final class CustomSpell {

    public final UUID id;
    public final String name;
    public final SpellSprite sprite;
    public final SpellShape shape;
    public final SpellElement element;
    public final int power; // 1-5

    public CustomSpell(UUID id, String name, SpellSprite sprite,
                       SpellShape shape, SpellElement element, int power) {
        this.id = id;
        this.name = name == null || name.isBlank() ? "Untitled" : name;
        this.sprite = sprite;
        this.shape = shape;
        this.element = element;
        this.power = Math.max(1, Math.min(5, power));
    }

    public int manaCost() {
        return (int) (shape.baseManaCost * (0.5 + power * 0.3));
    }

    public float damage() {
        return 4.0F * power * shape.damageMultiplier;
    }

    public int cooldownTicks() {
        return 40 + power * 30;
    }

    public int range() {
        return switch (shape) {
            case PROJECTILE, LASER -> 32;
            case BEAM -> 24;
            case AOE -> 8;
            case SELF -> 0;
            case TOUCH -> 4;
        };
    }

    /** Color principal pra VFX = mix do element color com sprite color (50/50). */
    public int effectiveColor() {
        int sc = sprite.color;
        int ec = element.color;
        int r = (((sc >> 16) & 0xFF) + ((ec >> 16) & 0xFF)) / 2;
        int g = (((sc >> 8) & 0xFF) + ((ec >> 8) & 0xFF)) / 2;
        int b = ((sc & 0xFF) + (ec & 0xFF)) / 2;
        return (r << 16) | (g << 8) | b;
    }

    // ────────── Serialization ──────────

    public CompoundTag toNbt() {
        CompoundTag t = new CompoundTag();
        t.putUUID("Id", id);
        t.putString("Name", name);
        t.putByte("Sprite", (byte) sprite.ordinal());
        t.putByte("Shape", (byte) shape.ordinal());
        t.putByte("Element", (byte) element.ordinal());
        t.putByte("Power", (byte) power);
        return t;
    }

    public static CustomSpell fromNbt(CompoundTag t) {
        return new CustomSpell(
                t.getUUID("Id"),
                t.getString("Name"),
                SpellSprite.byOrdinal(t.getByte("Sprite")),
                SpellShape.byOrdinal(t.getByte("Shape")),
                SpellElement.byOrdinal(t.getByte("Element")),
                t.getByte("Power"));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 64);
        buf.writeByte(sprite.ordinal());
        buf.writeByte(shape.ordinal());
        buf.writeByte(element.ordinal());
        buf.writeByte(power);
    }

    public static CustomSpell decode(FriendlyByteBuf buf) {
        return new CustomSpell(
                buf.readUUID(),
                buf.readUtf(64),
                SpellSprite.byOrdinal(buf.readByte()),
                SpellShape.byOrdinal(buf.readByte()),
                SpellElement.byOrdinal(buf.readByte()),
                buf.readByte());
    }

    @Override
    public String toString() {
        return name + "(" + sprite.id + "/" + shape.name() + "/" + element.name() + "/p" + power + ")";
    }
}
