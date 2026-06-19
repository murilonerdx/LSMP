package br.com.murilo.liberthia.magic.spell.necromancy;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * v0.1.162 r138: <b>SummonTicker</b> — gerencia minions summon de necromancy.
 *
 * <p>Cada minion tem 2 NBT keys:
 * <ul>
 *   <li>{@code liberthia.summon_owner} (UUID) — quem invocou</li>
 *   <li>{@code liberthia.summon_expires} (long gameTime) — quando despawna</li>
 * </ul>
 *
 * <p>Cada {@link #TICK_INTERVAL} ticks (1s), itera entities que tem essas tags e:
 * <ul>
 *   <li>Se gameTime > expires → discard + VFX dissipation</li>
 *   <li>Senão, se for Mob e não tem target, procura mob hostil próximo do owner pra atacar</li>
 *   <li>Spawna partícula sutil contínua pra indicar status "spectral"</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SummonTicker {

    public static final String NBT_OWNER = "liberthia.summon_owner";
    public static final String NBT_EXPIRES = "liberthia.summon_expires";
    public static final String NBT_SPECTRAL_WOLF = "liberthia.spectral_wolf";
    public static final int TICK_INTERVAL = 20; // 1s
    public static final double TARGET_RADIUS = 16.0;

    private SummonTicker() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (e.getServer().getTickCount() % TICK_INTERVAL != 0) return;

        for (ServerLevel sl : e.getServer().getAllLevels()) {
            long now = sl.getGameTime();
            sl.getEntities().getAll().forEach(entity -> {
                if (!(entity instanceof LivingEntity le)) return;
                var data = le.getPersistentData();
                if (!data.contains(NBT_OWNER) || !data.contains(NBT_EXPIRES)) return;

                long expires = data.getLong(NBT_EXPIRES);
                // r138: despawn na expiracao com VFX
                if (now >= expires) {
                    sl.sendParticles(ParticleTypes.SOUL,
                            le.getX(), le.getY() + 0.5, le.getZ(),
                            20, 0.4, 0.6, 0.4, 0.08);
                    le.discard();
                    return;
                }

                // Particula sutil de status spectral (1 por tick)
                if (data.getBoolean(NBT_SPECTRAL_WOLF)) {
                    if (le.getRandom().nextFloat() < 0.3F) {
                        sl.sendParticles(ParticleTypes.END_ROD,
                                le.getX(), le.getY() + 0.4, le.getZ(),
                                1, 0.2, 0.3, 0.2, 0.02);
                    }
                } else {
                    if (le.getRandom().nextFloat() < 0.15F) {
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                le.getX() + (le.getRandom().nextDouble() - 0.5) * 0.4,
                                le.getY() + le.getRandom().nextDouble() * 0.8,
                                le.getZ() + (le.getRandom().nextDouble() - 0.5) * 0.4,
                                1, 0, 0.01, 0, 0.005);
                    }
                }

                // Se for Mob sem target, procura hostil proximo
                if (le instanceof Mob mob && mob.getTarget() == null) {
                    UUID ownerUuid = data.getUUID(NBT_OWNER);
                    Player owner = sl.getPlayerByUUID(ownerUuid);
                    if (owner == null) return;

                    // Procura hostil em raio
                    var box = mob.getBoundingBox().inflate(TARGET_RADIUS);
                    var hostiles = sl.getEntitiesOfClass(LivingEntity.class, box,
                            ent -> ent != mob && ent != owner && ent.isAlive()
                                    && !(ent instanceof Player)
                                    && !isSameSummon(ent, ownerUuid)
                                    && ent instanceof net.minecraft.world.entity.monster.Enemy);
                    if (!hostiles.isEmpty()) {
                        // Pick closest
                        LivingEntity closest = hostiles.get(0);
                        double bestDist = closest.distanceToSqr(mob);
                        for (LivingEntity h : hostiles) {
                            double d = h.distanceToSqr(mob);
                            if (d < bestDist) { bestDist = d; closest = h; }
                        }
                        mob.setTarget(closest);
                    }
                }
            });
        }
    }

    private static boolean isSameSummon(LivingEntity ent, UUID ownerUuid) {
        var d = ent.getPersistentData();
        if (!d.contains(NBT_OWNER)) return false;
        return d.getUUID(NBT_OWNER).equals(ownerUuid);
    }

    /** Bloqueia damage entre minions do mesmo dono (friendly fire off). */
    @SubscribeEvent
    public static void onHurt(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        var src = e.getSource().getEntity();
        if (!(src instanceof LivingEntity attacker)) return;
        LivingEntity victim = e.getEntity();
        var aData = attacker.getPersistentData();
        var vData = victim.getPersistentData();
        if (!aData.contains(NBT_OWNER) || !vData.contains(NBT_OWNER)) return;
        // Same owner — cancel friendly fire
        if (aData.getUUID(NBT_OWNER).equals(vData.getUUID(NBT_OWNER))) {
            e.setCanceled(true);
            return;
        }
        // Atacar owner do attacker é proibido
        if (victim instanceof Player p && aData.getUUID(NBT_OWNER).equals(p.getUUID())) {
            e.setCanceled(true);
        }
    }
}
