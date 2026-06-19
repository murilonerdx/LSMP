package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

/**
 * r81 #14/18 — <b>Perception Horror</b>.
 *
 * <p>Inspired by The Observer / SCP-173. Quando o player abre inventário,
 * mobs monstros próximos ganham um boost de movement por 2 segundos.
 * Mas se ele FECHAR o inventory enquanto algum monstro está se aproximando,
 * ele para imediatamente — "como se nunca tivesse se movido".
 *
 * <p>Adiciona exposição passiva quando o player tem inventário aberto por
 * tempo prolongado em áreas hostis.
 */
public final class PerceptionSystem implements HorrorSystem {

    private final java.util.Map<java.util.UUID, Integer> invOpenTicks = new java.util.HashMap<>();

    @Override
    public HorrorType type() {
        return HorrorType.PERCEPTION;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        boolean invOpen = sp.containerMenu != sp.inventoryMenu;
        java.util.UUID id = sp.getUUID();

        if (invOpen) {
            int t = invOpenTicks.getOrDefault(id, 0) + 1;
            invOpenTicks.put(id, t);

            // A cada 40 ticks de inv aberto, mob speed boost + exposure
            if (t % 40 == 0) {
                state.addExposure(HorrorType.PERCEPTION, 2.0F);
                boostNearbyMonsters(sp, level);
            }
        } else {
            invOpenTicks.remove(id);
        }

        // Decay
        if (gameTime % 100 == 0 && level.isDay()) {
            state.decayExposure(HorrorType.PERCEPTION, baseDecayRate());
        }
    }

    @Override
    public void onInventoryOpen(ServerPlayer sp, HorrorState state) {
        // Reset ticks ao abrir
        invOpenTicks.put(sp.getUUID(), 0);
    }

    private void boostNearbyMonsters(ServerPlayer sp, ServerLevel level) {
        var mobs = level.getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(20));
        for (Mob m : mobs) {
            if (m.getType().getCategory() != MobCategory.MONSTER) continue;
            m.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 1, true, false));
        }
    }
}
