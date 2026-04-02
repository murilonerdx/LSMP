package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class DarkInfectionEffect extends MobEffect {
    public DarkInfectionEffect() {
        super(MobEffectCategory.HARMFUL, 0x2A0845);
        addAttributeModifier(Attributes.MAX_HEALTH, "b3f25a1e-6c4a-4e5b-9d8f-1a2b3c4d5e6f",
                -2.0D, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (amplifier >= 2 && entity.tickCount % 40 == 0) {
            entity.hurt(entity.damageSources().magic(), 1.0F);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
