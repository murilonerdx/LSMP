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
 * Sanguine Vitality — +4 HP maximo (amplifier 0) ou +8 HP (amp 1) enquanto ativo.
 * Quando termina remove o HP bonus automaticamente via o AttributeModifier padrão do MobEffect.
 */
public class SanguineVitalityEffect extends MobEffect {

    private static final UUID VITALITY_UUID = UUID.fromString("b1f2c3d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d");

    private final Map<Attribute, AttributeModifier> modifiers;

    public SanguineVitalityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xA00020);
        this.modifiers = ImmutableMap.of(
                Attributes.MAX_HEALTH,
                new AttributeModifier(VITALITY_UUID, "Sanguine Vitality", 4.0D,
                        AttributeModifier.Operation.ADDITION));
    }

    @Override
    public Map<Attribute, AttributeModifier> getAttributeModifiers() {
        return modifiers;
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap map, int amplifier) {
        super.addAttributeModifiers(entity, map, amplifier);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return modifier.getAmount() * (amplifier + 1);
    }
}
