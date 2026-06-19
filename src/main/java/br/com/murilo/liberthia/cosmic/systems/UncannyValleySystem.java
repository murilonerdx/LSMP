package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;

import java.util.List;

/**
 * r81 #2/18 — <b>Uncanny Valley</b>.
 *
 * <p>Mobs/NPCs próximos do player começam a se comportar como humanos —
 * mas <i>levemente errados</i>. Aldeões viram a cabeça rápido pra olhar.
 * Mobs piscam fora de sincronia. Movimentos um frame atrasados.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>A cada 60 ticks (3s), pega LivingEntities num raio de 16</li>
 *   <li>Faz eles virarem a cabeça pro player súbitamente (180° instantaneo)</li>
 *   <li>Aplica MOVEMENT_SLOWDOWN brief — movimentação "delayed"</li>
 *   <li>Acumula exposição lenta (+0.5/tick) enquanto o player observa NPCs</li>
 * </ul>
 */
public final class UncannyValleySystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.UNCANNY;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        // Throttle pesado
        if (gameTime % 60 != 0) return;

        // Pega mobs/villagers próximos (16 raio)
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(16));
        if (nearby.isEmpty()) return;

        boolean anyVisible = false;
        for (LivingEntity e : nearby) {
            if (e == sp) continue;
            if (!sp.hasLineOfSight(e)) continue;
            anyVisible = true;

            // Vira a cabeça pro player INSTANTANEAMENTE (uncanny)
            double dx = sp.getX() - e.getX();
            double dz = sp.getZ() - e.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            // Stutter — rotaciona 30° por tick em vez de smooth (errado)
            e.setYHeadRot(yaw);

            // Villagers: aplica delay artificial nos movimentos
            if (e instanceof Villager v && Math.random() < 0.1) {
                v.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false));
            }
        }

        if (anyVisible) {
            state.addExposure(HorrorType.UNCANNY, 0.5F);
        }
    }
}
