package br.com.murilo.liberthia.cosmic.idol;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * r178: spawn de SLOW-BURN do {@link IdolEntity}. Cada player acumula tempo de jogo;
 * quando passa de {@link #spawnIntervalTicks} (default 25 min), O Ídolo aparece longe
 * e o ciclo começa. Quando o encontro termina (você o encara / 20 min), há um cooldown
 * antes dele voltar. Configurável por {@code /liberthia idol ...}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class IdolManager {

    private static volatile boolean enabled = true;
    private static volatile boolean aggressive = false;       // psicológico (default) vs agressivo
    private static volatile int spawnIntervalTicks = 30000;   // ~25 min de jogo
    private static final int COOLDOWN_AFTER = 12000;          // ~10 min após um encontro
    private static final double SPAWN_DIST = 46.0;

    private static final Map<UUID, Integer> TIMER = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> ACTIVE = new ConcurrentHashMap<>();

    private IdolManager() {}

    // ── config (comando) ──
    public static void setEnabled(boolean on) { enabled = on; }
    public static boolean isEnabled() { return enabled; }
    public static void setAggressive(boolean a) { aggressive = a; }
    public static boolean isAggressive() { return aggressive; }
    public static void setIntervalMinutes(int min) { spawnIntervalTicks = Math.max(1, min) * 60 * 20; }
    public static int getIntervalMinutes() { return spawnIntervalTicks / (60 * 20); }
    public static boolean hasActive(UUID id) { return ACTIVE.containsKey(id); }

    /** Chamado pelo IdolEntity quando some (encara/expira/atacado). */
    public static void onIdolGone(UUID player) {
        ACTIVE.remove(player);
        TIMER.put(player, -COOLDOWN_AFTER); // cooldown antes de re-armar
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!enabled) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.isSpectator() || sp.isCreative()) return;
        if (sp.tickCount % 20 != 0) return; // 1x/s
        UUID id = sp.getUUID();

        // tem um Ídolo ativo? mantém-no perto; se sumiu/foi longe, limpa.
        UUID oid = ACTIVE.get(id);
        if (oid != null) {
            if (sp.level() instanceof ServerLevel sl) {
                Entity ent = sl.getEntity(oid);
                if (ent == null || ent.distanceToSqr(sp) > 200 * 200) {
                    if (ent != null) ent.discard();
                    onIdolGone(id);
                }
            }
            return; // não acumula timer enquanto ativo
        }

        int t = TIMER.merge(id, 20, Integer::sum);
        if (t >= spawnIntervalTicks && sp.level() instanceof ServerLevel sl) {
            // multiplayer: prefere caçar quem está SOZINHO/afastado do grupo.
            if (!isIsolated(sp)) return; // não reseta o timer → aparece assim que ficar só
            if (spawn(sl, sp, id)) TIMER.put(id, 0);
        }
    }

    /** True se não há outro player (não-espectador) num raio de 40 blocos. */
    private static boolean isIsolated(ServerPlayer sp) {
        for (var p : sp.level().players()) {
            if (p != sp && !p.isSpectator() && p.distanceToSqr(sp) < 40 * 40) return false;
        }
        return true;
    }

    /** Força um spawn imediato (comando). */
    public static boolean forceSpawn(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel sl)) return false;
        UUID id = sp.getUUID();
        UUID oid = ACTIVE.get(id);
        if (oid != null) { Entity ex = sl.getEntity(oid); if (ex != null) ex.discard(); }
        return spawn(sl, sp, id);
    }

    public static void stop(ServerPlayer sp) {
        UUID oid = ACTIVE.remove(sp.getUUID());
        if (oid != null && sp.level() instanceof ServerLevel sl) {
            Entity ent = sl.getEntity(oid);
            if (ent != null) ent.discard();
        }
        TIMER.put(sp.getUUID(), 0);
    }

    private static boolean spawn(ServerLevel sl, ServerPlayer sp, UUID id) {
        IdolEntity idol = ModEntities.IDOL.get().create(sl);
        if (idol == null) return false;
        // longe, de preferência ATRÁS do player
        Vec3 look = sp.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z);
        if (back.lengthSqr() < 1.0e-4) back = new Vec3(0, 0, 1);
        back = back.normalize();
        BlockPos spot = null;
        for (int i = 0; i < 20; i++) {
            double ang = Math.atan2(back.z, back.x) + (sl.random.nextDouble() - 0.5) * Math.PI; // arco atrás
            int x = (int) (sp.getX() + Math.cos(ang) * SPAWN_DIST);
            int z = (int) (sp.getZ() + Math.sin(ang) * SPAWN_DIST);
            spot = findStand(sl, x, sp.getBlockY(), z);
            if (spot != null) break;
        }
        if (spot == null) return false;
        idol.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, sl.random.nextFloat() * 360, 0);
        idol.bind(id);
        idol.setAggressive(aggressive);
        sl.addFreshEntity(idol);
        ACTIVE.put(id, idol.getUUID());
        LiberthiaMod.LOGGER.info("[Idol] O Ídolo apareceu pra {} em {}", sp.getName().getString(), spot);
        return true;
    }

    private static BlockPos findStand(ServerLevel sl, int x, int y0, int z) {
        for (int dy = 5; dy >= -6; dy--) {
            BlockPos feet = new BlockPos(x, y0 + dy, z);
            BlockState floor = sl.getBlockState(feet.below());
            if (floor.blocksMotion() && sl.getBlockState(feet).isAir()
                    && sl.getBlockState(feet.above()).isAir()) {
                return feet;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        UUID oid = ACTIVE.remove(id);
        if (oid != null && e.getEntity().level() instanceof ServerLevel sl) {
            Entity ent = sl.getEntity(oid);
            if (ent != null) ent.discard();
        }
        TIMER.remove(id);
    }
}
