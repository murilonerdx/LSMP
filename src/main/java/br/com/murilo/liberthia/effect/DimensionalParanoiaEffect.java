package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * r184 — <b>Paranoia Dimensional</b> (doença permanente). A cada ~30 min, o portador passa a
 * enxergar pela visão de OUTRO jogador por 5s (câmera trocada). Permanece PARA SEMPRE até usar
 * o Soro Dimensional. Lógica em {@code event/DimensionalDiseaseHandler}.
 */
public class DimensionalParanoiaEffect extends MobEffect {
    public static final int COLOR = 0x6A4FB5; // roxo-azulado inquietante
    public DimensionalParanoiaEffect() { super(MobEffectCategory.HARMFUL, COLOR); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return false; }
}
