package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Bug fix (#46): faz o <b>Filho de Lilith</b> (zumbi invocado pelo
 * {@code Berço de Lilith}) se comportar como ALIADO, não como hostil.
 *
 * <p>Problema: era um zumbi vanilla puro, então a IA padrão
 * ({@code NearestAttackableTargetGoal(Player)}) mirava no player mais próximo —
 * o próprio dono — e o atacava.
 *
 * <p>Correção (sem precisar de entidade custom):
 * <ul>
 *   <li>{@link LivingChangeTargetEvent}: cancela qualquer tentativa do aliado
 *       de mirar num player (incluindo o dono).</li>
 *   <li>{@link LivingHurtEvent}: quando o dono leva dano de uma criatura, os
 *       aliados próximos passam a mirar no atacante (proteção real).</li>
 *   <li>{@link TickEvent.LevelTickEvent}: some quando expira o tempo.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class LilithAllyEvents {

    private static final String OWNER = "liberthia.lilith_owner";
    private static final String EXPIRE = "liberthia.lilith_expire";

    private LilithAllyEvents() {}

    private static boolean isLilith(LivingEntity e) {
        return e.getPersistentData().contains(OWNER);
    }

    private static UUID ownerOf(LivingEntity e) {
        try {
            return UUID.fromString(e.getPersistentData().getString(OWNER));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Aliado nunca mira em players (guardião não bate em gente). */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!isLilith(event.getEntity())) return;
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget instanceof Player) {
            event.setNewTarget(null); // aliado nunca mira em player (nem no dono)
        }
    }

    /** Dono ferido → aliados próximos miram no atacante. */
    @SubscribeEvent
    public static void onOwnerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player owner)) return;
        if (!(owner.level() instanceof ServerLevel sl)) return;
        var src = event.getSource();
        if (src == null || !(src.getEntity() instanceof LivingEntity attacker)) return;
        if (attacker == owner) return;
        UUID ownerId = owner.getUUID();
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(24.0),
                m -> m.getPersistentData().contains(OWNER))) {
            UUID mo = ownerOf(mob);
            if (mo != null && mo.equals(ownerId) && mob != attacker) {
                mob.setTarget(attacker);
            }
        }
    }

    /** Despawn quando o tempo (lilith_expire) acaba. */
    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % 100 != 0) return;
        for (var ent : sl.getAllEntities()) {
            if (!(ent instanceof LivingEntity le) || !isLilith(le)) continue;
            long expire = le.getPersistentData().getLong(EXPIRE);
            if (expire > 0 && sl.getGameTime() >= expire) {
                sl.sendParticles(ParticleTypes.SOUL,
                        le.getX(), le.getY() + 1, le.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
                le.discard();
            }
        }
    }
}
