package br.com.murilo.liberthia.magic.factory;

import com.google.gson.JsonObject;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * r148: Spec de um efeito que o feitiço aplica em alvos (além do dano direto).
 *
 * <p>Cada {@link SpellRecipe} pode ter N effects. Suporta:
 * <ul>
 *   <li><b>Mob effects vanilla:</b> "poison", "wither", "regeneration", etc</li>
 *   <li><b>Ignite:</b> set on fire por X ticks</li>
 *   <li><b>Knockback:</b> empurra alvo na direção do impacto</li>
 *   <li><b>Freeze:</b> incrementa ticks frozen</li>
 *   <li><b>Heal:</b> cura o alvo (negativo dano)</li>
 *   <li><b>Lifesteal:</b> cura o caster com % do dano</li>
 *   <li><b>Levitate:</b> aplica levitation</li>
 * </ul>
 */
public final class SpellEffectSpec {

    public enum Type {
        IGNITE, FREEZE, KNOCKBACK, HEAL, LIFESTEAL, LEVITATE,
        POISON, WITHER, SLOWNESS, WEAKNESS, BLINDNESS, NAUSEA,
        REGENERATION, ABSORPTION, RESISTANCE, STRENGTH, SPEED,
        GLOWING, LUCK, FIRE_RESISTANCE, NIGHT_VISION
    }

    public final Type type;
    public final int duration;     // ticks
    public final int amplifier;    // level (0 = level 1)
    public final float magnitude;  // pra heal/lifesteal/knockback (% ou força)

    public SpellEffectSpec(Type type, int duration, int amplifier, float magnitude) {
        this.type = type;
        this.duration = Math.max(0, duration);
        this.amplifier = Math.max(0, amplifier);
        this.magnitude = magnitude;
    }

    /** Aplica esse efeito a um alvo. {@code damage} = dano original (pra lifesteal). */
    public void applyTo(LivingEntity target, LivingEntity caster, float damageDealt) {
        switch (type) {
            case IGNITE -> target.setSecondsOnFire(duration / 20);
            case FREEZE -> target.setTicksFrozen(target.getTicksFrozen() + duration);
            case KNOCKBACK -> {
                if (caster != null) {
                    double dx = target.getX() - caster.getX();
                    double dz = target.getZ() - caster.getZ();
                    double len = Math.sqrt(dx * dx + dz * dz);
                    if (len > 0.001) {
                        target.setDeltaMovement(target.getDeltaMovement().add(
                                dx / len * magnitude, 0.3, dz / len * magnitude));
                        target.hurtMarked = true;
                    }
                }
            }
            case HEAL -> target.heal(magnitude);
            case LIFESTEAL -> {
                if (caster != null && damageDealt > 0) {
                    caster.heal(damageDealt * magnitude);
                }
            }
            case LEVITATE -> target.addEffect(
                    new MobEffectInstance(MobEffects.LEVITATION, duration, amplifier));
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
            case WITHER -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amplifier));
            case SLOWNESS -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier));
            case WEAKNESS -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amplifier));
            case BLINDNESS -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, amplifier));
            case NAUSEA -> target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, amplifier));
            case REGENERATION -> target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier));
            case ABSORPTION -> target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier));
            case RESISTANCE -> target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier));
            case STRENGTH -> target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amplifier));
            case SPEED -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier));
            case GLOWING -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, amplifier));
            case LUCK -> target.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, amplifier));
            case FIRE_RESISTANCE -> target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, amplifier));
            case NIGHT_VISION -> target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, amplifier));
        }
    }

    /** Parse one effect from JSON. */
    public static SpellEffectSpec fromJson(JsonObject j) {
        String typeStr = j.get("type").getAsString().toUpperCase();
        Type type = Type.valueOf(typeStr);
        int duration = j.has("duration") ? j.get("duration").getAsInt() : 60;
        int amplifier = j.has("amplifier") ? j.get("amplifier").getAsInt() : 0;
        float magnitude = j.has("magnitude") ? j.get("magnitude").getAsFloat() : 1.0F;
        return new SpellEffectSpec(type, duration, amplifier, magnitude);
    }

    /** Parse a JSON array of effects. */
    public static List<SpellEffectSpec> fromJsonArray(com.google.gson.JsonArray arr) {
        List<SpellEffectSpec> list = new ArrayList<>();
        if (arr == null) return list;
        for (var elem : arr) {
            try {
                list.add(fromJson(elem.getAsJsonObject()));
            } catch (Exception ignored) {}
        }
        return list;
    }
}
