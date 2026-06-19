package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * v0.1.51: Resistência temporária a uma matter específica.
 *
 * <p>Quando ativo, bloqueia:
 * <ul>
 *   <li>Ganho de matter do tipo no perfil (proximity blocks, blood tree, etc).</li>
 *   <li>Efeitos negativos derivados da matter (Wither do DM, Slowness do YM, etc).</li>
 * </ul>
 *
 * <p>Não reduz a matter já acumulada — só PROTEGE durante a duração. User
 * pediu: "a pílula não diminui infecção, só impede que ela aumente ou os
 * efeitos dela". Duração padrão 30 min (36000 ticks) aplicada ao tomar pílula.
 *
 * <p>Efeito é "marker" — não tem tick lógica própria. Os callsites de ganho
 * e de aplicação de efeitos checam {@code player.hasEffect(...)} e skip se
 * tiver. Ver {@link MatterResistance#blocked(net.minecraft.world.entity.LivingEntity,
 * br.com.murilo.liberthia.matter.MatterType)}.
 */
public class MatterResistanceEffect extends MobEffect {
    public MatterResistanceEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // marker only
    }
}
