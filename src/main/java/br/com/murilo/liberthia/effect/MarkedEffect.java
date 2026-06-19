package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * <b>Marcado</b> (Tiro Marcado do Arco da Caçadora) — efeito marker sem tick
 * próprio. Enquanto ativo, o alvo recebe +30% de dano de QUALQUER fonte,
 * aplicado por {@code event/HuntressMarkHandler} no {@code LivingHurtEvent}.
 */
public class MarkedEffect extends MobEffect {

    public MarkedEffect() {
        super(MobEffectCategory.HARMFUL, 0xE0103A); // carmesim
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // marker — não faz nada por tick
    }
}
