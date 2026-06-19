package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * <b>Medo</b> — efeito marker aplicado a quem fica na rua durante a Lua do Medo.
 * O efeito em si é só o ícone/nome; os debuffs reais (Fraqueza V + Velocidade IV)
 * são aplicados junto por {@code event/FearMoonEvents}.
 */
public class MedoEffect extends MobEffect {

    public MedoEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A0A2A); // roxo-sangue
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
