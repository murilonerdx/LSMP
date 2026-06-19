package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * r184 — <b>Infecção Dimensional</b> (doença permanente). Partículas esverdeadas; gera
 * periodicamente <i>Vermes Dimensionais</i> que perseguem o portador e aplicam Náusea + Fome.
 * Permanece PARA SEMPRE até usar o Soro Dimensional. Lógica em {@code event/DimensionalDiseaseHandler}.
 */
public class DimensionalInfectionEffect extends MobEffect {
    public static final int COLOR = 0x4FA02A; // verde doentio
    public DimensionalInfectionEffect() { super(MobEffectCategory.HARMFUL, COLOR); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return false; }
}
