package br.com.murilo.liberthia.magic.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * v0.1.24 r96: <b>Ascension</b> — empurra a entity verticalmente cada tick.
 *
 * <p>Effect duration short (5s default) — entity sobe ~0.4 blocos/tick com slow fall.
 * Combina knockup com controle mid-air.
 */
public class AscensionEffect extends MobEffect {

    public AscensionEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFEEAA);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amp) {
        if (entity.getDeltaMovement().y < 0.3) {
            entity.setDeltaMovement(
                    entity.getDeltaMovement().x,
                    Math.min(0.5, entity.getDeltaMovement().y + 0.2),
                    entity.getDeltaMovement().z);
        }
        entity.fallDistance = 0;
        entity.hasImpulse = true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // tick every tick
    }
}
