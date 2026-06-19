package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * r180: <b>Antimagia</b> — o efeito-selo do sistema anti-magia. Quem está com ele
 * NÃO consegue voar, tem a magia que lança quase anulada (-75% de dano mágico),
 * e fica lento. Aplicado por seladores, pela Espada Anti-Magia e pelo Pilar Protetor.
 * Cor ciano = identidade visual de toda a linha anti-magia. Comportamento em
 * {@code event/AntimagiaHandler}.
 */
public class AntimagiaEffect extends MobEffect {
    /** Cor-tema da linha anti-magia (ciano). */
    public static final int THEME_COLOR = 0x2BD6D6;

    public AntimagiaEffect() {
        super(MobEffectCategory.HARMFUL, THEME_COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // a lógica roda no handler (PlayerTick/LivingHurt), não por tick do efeito
    }
}
