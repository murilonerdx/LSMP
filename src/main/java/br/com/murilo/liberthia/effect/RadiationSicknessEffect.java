package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class RadiationSicknessEffect extends MobEffect {
    public RadiationSicknessEffect() {
        super(MobEffectCategory.HARMFUL, 0x3D5C1E);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                -0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, "f0e1d2c3-b4a5-6789-0fed-cba987654321",
                -1.0D, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
