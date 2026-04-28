package br.com.murilo.liberthia.effect;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.UUID;

/**
 * Blood Frenzy — buff granted by the Blood Syringe when injecting clean blood.
 * +2 ATK damage and +20% movement speed while active, but the carrier loses
 * 0.5 HP every 2s (the frenzy consumes its host). At amplifier 1: +4 ATK,
 * +30% speed, 1 HP/2s drain.
 */
public class BloodFrenzyEffect extends MobEffect {

    private static final UUID ATK_UUID  = UUID.fromString("12a1b8ae-45f9-4c5d-8dd2-4e7b9f0a1111");
    private static final UUID SPD_UUID  = UUID.fromString("12a1b8ae-45f9-4c5d-8dd2-4e7b9f0a2222");

    private final Map<Attribute, AttributeModifier> modifiers;

    public BloodFrenzyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xC20012);
        this.modifiers = ImmutableMap.of(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(ATK_UUID, "Blood Frenzy ATK", 2.0D,
                        AttributeModifier.Operation.ADDITION),
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(SPD_UUID, "Blood Frenzy SPD", 0.20D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public Map<Attribute, AttributeModifier> getAttributeModifiers() {
        return modifiers;
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return modifier.getAmount() * (amplifier + 1);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Drain 0.5 HP every 40 ticks (2s). Scaled by amplifier.
        return duration % 40 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            float dmg = 0.5F * (amplifier + 1);
            entity.hurt(entity.damageSources().magic(), dmg);
        }
    }
}
