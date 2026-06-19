package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia os inimigos presos pelo {@code Chicote Celestial}: cada tick, segura
 * o alvo no lugar (zera movimento horizontal, trava navegação, snap de volta à
 * âncora), mantém Slowness extremo + Glowing, e solta sozinho quando o tempo
 * acaba. Correntes de luz (END_ROD) orbitam o alvo enquanto preso.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CelestialBindManager {

    private static final Map<UUID, Bind> BOUND = new ConcurrentHashMap<>();

    private CelestialBindManager() {}

    private static final class Bind {
        final LivingEntity target;
        final UUID binder;
        final long expiryTick;
        final double ax, ay, az;

        Bind(LivingEntity t, UUID binder, long expiry) {
            this.target = t;
            this.binder = binder;
            this.expiryTick = expiry;
            this.ax = t.getX();
            this.ay = t.getY();
            this.az = t.getZ();
        }
    }

    /** Prende {@code target} por {@code ticks} ticks. */
    public static void bind(LivingEntity target, ServerPlayer binder, int ticks) {
        if (target == null) return;
        long now = target.level().getGameTime();
        BOUND.put(target.getUUID(), new Bind(target, binder != null ? binder.getUUID() : null, now + ticks));
        applyHold(target, ticks + 20);
    }

    public static boolean isBound(LivingEntity e) {
        return e != null && BOUND.containsKey(e.getUUID());
    }

    /** r179: o alvo VIVO que {@code binder} laçou (ou null) — usado pelo feixe do chicote. */
    public static LivingEntity getBoundTargetOf(ServerPlayer binder) {
        if (binder == null) return null;
        UUID bid = binder.getUUID();
        for (Bind b : BOUND.values()) {
            if (bid.equals(b.binder) && b.target != null && b.target.isAlive() && !b.target.isRemoved()) {
                return b.target;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (BOUND.isEmpty()) return;

        Iterator<Map.Entry<UUID, Bind>> it = BOUND.entrySet().iterator();
        while (it.hasNext()) {
            Bind b = it.next().getValue();
            LivingEntity t = b.target;
            if (t == null || !t.isAlive() || t.isRemoved()) {
                it.remove();
                continue;
            }
            long now = t.level().getGameTime();
            if (now >= b.expiryTick) {
                t.removeEffect(MobEffects.GLOWING);
                it.remove();
                continue;
            }
            holdInPlace(b, now);

            if (t.level() instanceof ServerLevel sl && (now % 3 == 0)) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(),
                        6, t.getBbWidth() * 0.5, t.getBbHeight() * 0.4, t.getBbWidth() * 0.5, 0.01);
            }
        }
    }

    private static void applyHold(LivingEntity t, int durationTicks) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 250, false, false));
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0, false, false));
        if (t instanceof Mob mob) mob.getNavigation().stop();
    }

    private static void holdInPlace(Bind b, long now) {
        LivingEntity t = b.target;
        Vec3 dm = t.getDeltaMovement();
        t.setDeltaMovement(0, Math.min(dm.y, 0.0), 0); // mata horizontal, deixa gravidade
        t.hurtMarked = true; // força resync da velocidade pro cliente (sem "andar fantasma")
        double dx = b.ax - t.getX();
        double dz = b.az - t.getZ();
        if (dx * dx + dz * dz > 0.02) {
            t.setPos(b.ax, t.getY(), b.az); // puxa de volta pra âncora (horizontal)
        }
        if (t instanceof Mob mob) mob.getNavigation().stop();
        if (now % 20 == 0) applyHold(t, 60); // reforça caso outro efeito sobrescreva
    }
}
