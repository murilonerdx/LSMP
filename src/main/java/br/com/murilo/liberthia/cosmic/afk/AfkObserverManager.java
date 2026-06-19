package br.com.murilo.liberthia.cosmic.afk;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * r177: se um player fica <b>AFK</b> (parado) por ~60s, um Observador aparece
 * <b>parado de frente pra ele, encarando</b>. No instante em que o player se
 * mexe, o observador <b>some</b> (fumaça). Per-player, server-side.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AfkObserverManager {

    private static final int AFK_TICKS = 1200;            // 60s parado → aparece
    private static final double MOVE_EPS_SQ = 0.04 * 0.04; // tolerância de "parado"
    private static final int CHECK_EVERY = 10;            // amostra a cada 0.5s
    // r179: nenhum outro player neste raio = "sozinho". Alinhado ao SOLO_RANGE (200)
    // do PeripheralObserverEntity — mesma definição de "sozinho" do resto do mod.
    // (O modo afkStare pula o tick normal da entidade, então o gate precisa vir aqui.)
    private static final double ALONE_RADIUS = 200.0;

    private static final Map<UUID, Vec3> LAST_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> AFK = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> OBSERVER = new ConcurrentHashMap<>();

    private AfkObserverManager() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.isSpectator()) return;
        if (sp.tickCount % CHECK_EVERY != 0) return;

        UUID id = sp.getUUID();
        Vec3 now = sp.position();
        Vec3 prev = LAST_POS.put(id, now);

        boolean moved = prev == null || prev.distanceToSqr(now) > MOVE_EPS_SQ;
        if (moved) {
            AFK.put(id, 0);
            despawn(sp.level() instanceof ServerLevel sl ? sl : null, id);
            return;
        }
        int afk = AFK.merge(id, CHECK_EVERY, Integer::sum);
        if (afk >= AFK_TICKS && sp.level() instanceof ServerLevel sl) {
            // r179: só aparece se o player estiver SOZINHO (mais imersivo).
            // Se outro player chega perto, o visitante some.
            if (!isAlone(sl, sp)) {
                despawn(sl, id);
                return;
            }
            if (!OBSERVER.containsKey(id)) {
                spawn(sl, sp, id);
            }
        }
    }

    /** r179: true se NENHUM outro player (não-spectator) está dentro de {@link #ALONE_RADIUS}. */
    private static boolean isAlone(ServerLevel sl, ServerPlayer self) {
        double r2 = ALONE_RADIUS * ALONE_RADIUS;
        for (ServerPlayer other : sl.players()) {
            if (other == self || other.isSpectator()) continue;
            if (other.distanceToSqr(self) <= r2) return false;
        }
        return true;
    }

    private static void spawn(ServerLevel sl, ServerPlayer sp, UUID id) {
        PeripheralObserverEntity obs = ModEntities.LOOM_PERIPHERAL.get().create(sl);
        if (obs == null) return;
        Vec3 look = sp.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0e-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        double d = 3.5;
        int bx = (int) Math.floor(sp.getX() + flat.x * d);
        int bz = (int) Math.floor(sp.getZ() + flat.z * d);
        BlockPos foot = findStand(sl, bx, sp.getBlockY(), bz);
        double y = foot != null ? foot.getY() : sp.getY();
        obs.moveTo(bx + 0.5, y, bz + 0.5, 0, 0);
        obs.startAfkStare(id);
        sl.addFreshEntity(obs);
        sl.sendParticles(ParticleTypes.SMOKE, bx + 0.5, y + 1, bz + 0.5, 8, 0.2, 0.4, 0.2, 0.01);
        OBSERVER.put(id, obs.getUUID());
    }

    private static void despawn(ServerLevel sl, UUID id) {
        UUID oid = OBSERVER.remove(id);
        if (oid == null || sl == null) return;
        Entity ent = sl.getEntity(oid);
        if (ent != null) {
            sl.sendParticles(ParticleTypes.SMOKE, ent.getX(), ent.getY() + 1, ent.getZ(), 16, 0.3, 0.6, 0.3, 0.04);
            ent.discard();
        }
    }

    private static BlockPos findStand(ServerLevel sl, int x, int y0, int z) {
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos feet = new BlockPos(x, y0 + dy, z);
            if (sl.getBlockState(feet.below()).blocksMotion()
                    && sl.getBlockState(feet).isAir()
                    && sl.getBlockState(feet.above()).isAir()) {
                return feet;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        LAST_POS.remove(id);
        AFK.remove(id);
        despawn(e.getEntity().level() instanceof ServerLevel sl ? sl : null, id);
    }
}
