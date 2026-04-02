package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ClearShieldEffect extends MobEffect {
    public ClearShieldEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xAADDFF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
