package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * r184 — <b>Memória Dimensional</b> (doença permanente). Partículas roxas constantes; a cada
 * ~10 min teletransporta o portador para outra dimensão por 10s e o traz de volta. Permanece
 * PARA SEMPRE (sobrevive a morte/leite) até usar o Soro Dimensional. Lógica em
 * {@code event/DimensionalDiseaseHandler}; o efeito é apenas o marcador/ícone.
 */
public class DimensionalMemoryEffect extends MobEffect {
    public static final int COLOR = 0x9B30D0; // roxo
    public DimensionalMemoryEffect() { super(MobEffectCategory.HARMFUL, COLOR); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return false; }
}
