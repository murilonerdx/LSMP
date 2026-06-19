package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * r185 — <b>Radiação Dimensional</b>. Emanada pela Adaga Corta-Fendas a quem está perto SEM
 * segurar uma adaga. Reduz velocidade e vida máxima e dá tontura (náusea) periódica.
 * Ícone em {@code textures/mob_effect/dimensional_radiation.png}.
 */
public class DimensionalRadiationEffect extends MobEffect {
    public DimensionalRadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x3D1C6B); // violeta profundo
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                "a3c7f921-bb01-4de1-9a22-00000000cafe",
                -0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.MAX_HEALTH,
                "a3c7f921-bb01-4de1-9a22-00000001cafe",
                -2.0D, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // a "tontura" da radiação — náusea curta sem partículas extras
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, false));
    }
}
