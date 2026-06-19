package br.com.murilo.liberthia.matter;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

/**
 * v0.1.51: helper centralizado pra checar se um player tá "resistente" a um
 * tipo de matter (tomou a pílula correspondente).
 *
 * <p>Tipos: {@link Type#DARK}, {@link Type#CLEAR}, {@link Type#YELLOW}.
 *
 * <p>Uso típico nos callsites de ganho de matter:
 * <pre>{@code
 * if (MatterResistance.blocked(player, MatterResistance.Type.DARK)) return;
 * profile.addDark(gain);
 * }</pre>
 *
 * <p>E nos handlers de efeitos negativos (ex: WitherTick do BloodTreeProximityHandler):
 * <pre>{@code
 * if (MatterResistance.blocked(player, MatterResistance.Type.DARK)) {
 *     // não aplica Wither — player tomou DarkMatterPill
 *     return;
 * }
 * }</pre>
 */
public final class MatterResistance {

    public enum Type { DARK, CLEAR, YELLOW }

    /** Duração padrão da resistência (30 min = 36000 ticks). */
    public static final int DURATION_TICKS = 30 * 60 * 20;

    private MatterResistance() {}

    /**
     * True se o player está protegido contra a matter do tipo. Verificar no
     * INÍCIO de qualquer chamada que daria ganho/efeito de matter.
     */
    public static boolean blocked(LivingEntity entity, Type type) {
        if (entity == null) return false;
        MobEffect effect = switch (type) {
            case DARK -> ModEffects.DARK_MATTER_RESISTANCE.get();
            case CLEAR -> ModEffects.CLEAR_MATTER_RESISTANCE.get();
            case YELLOW -> ModEffects.YELLOW_MATTER_RESISTANCE.get();
        };
        return entity.hasEffect(effect);
    }
}
