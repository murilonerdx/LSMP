package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.horror.entity.DimensionalWormEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * r184 — <b>Doenças Dimensionais</b> (permanentes até cura). Três males que o jogador contrai
 * (em zonas de entropia, radiação ou exposição cósmica) e que NÃO somem com leite nem com a
 * morte — só com o <i>Soro Dimensional</i>:
 * <ul>
 *   <li><b>Memória Dimensional</b> — partículas roxas; a cada ~10 min teletransporta para outra
 *       dimensão por 10s (com proteção de queda/fogo) e traz de volta.</li>
 *   <li><b>Infecção Dimensional</b> — partículas verdes; gera Vermes Dimensionais que dão Náusea+Fome.</li>
 *   <li><b>Paranoia Dimensional</b> — a cada ~30 min vê a visão de outro jogador por 5s.</li>
 * </ul>
 * A persistência usa FLAGS no {@code persistentData} (sobrevive a leite/morte/relog); o efeito é
 * re-aplicado pelo handler enquanto a flag estiver ligada. Cura = {@link #cureAll}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class DimensionalDiseaseHandler {
    private DimensionalDiseaseHandler() {}

    // flags de doença (persistentData)
    public static final String F_MEM = "liberthia.dis.mem";
    public static final String F_INF = "liberthia.dis.inf";
    public static final String F_PAR = "liberthia.dis.par";
    // timers/estado
    private static final String K_MEM_T   = "liberthia.dis.memT";
    private static final String K_MEM_BACK = "liberthia.dis.memBack";
    private static final String K_MEM_DIM = "liberthia.dis.memRetDim";
    private static final String K_MEM_X = "liberthia.dis.memRetX";
    private static final String K_MEM_Y = "liberthia.dis.memRetY";
    private static final String K_MEM_Z = "liberthia.dis.memRetZ";
    private static final String K_MEM_VALID = "liberthia.dis.memRetValid";
    private static final String K_INF_T = "liberthia.dis.infT";
    private static final String K_PAR_T = "liberthia.dis.parT";
    private static final String K_PAR_VIEW = "liberthia.dis.parView";

    private static final int MEM_PERIOD = 12000;  // 10 min entre viagens
    private static final int MEM_TRIP   = 200;    // 10s fora
    private static final int INF_PERIOD = 600;    // 30s entre tentativas de verme
    private static final int PAR_PERIOD = 36000;  // 30 min entre episódios
    private static final int PAR_VIEW   = 100;    // 5s vendo outro jogador

    // ───────────────────────── API pública (contrair / curar) ─────────────────────────

    public static void contract(Player p, int which) {
        CompoundTag pd = p.getPersistentData();
        switch (which) {
            case 0 -> apply(p, pd, F_MEM, ModEffects.DIMENSIONAL_MEMORY, "§5Você sente memórias de lugares onde nunca esteve…");
            case 1 -> apply(p, pd, F_INF, ModEffects.DIMENSIONAL_BLIGHT, "§2Algo se mexe sob sua pele…");
            case 2 -> apply(p, pd, F_PAR, ModEffects.DIMENSIONAL_PARANOIA, "§9Você não tem mais certeza de quem está olhando…");
        }
    }

    /** Contrai uma doença aleatória que o jogador ainda não tem. */
    public static void contractRandom(Player p) {
        CompoundTag pd = p.getPersistentData();
        List<Integer> avail = new ArrayList<>();
        if (!pd.getBoolean(F_MEM)) avail.add(0);
        if (!pd.getBoolean(F_INF)) avail.add(1);
        if (!pd.getBoolean(F_PAR)) avail.add(2);
        if (avail.isEmpty()) return;
        contract(p, avail.get(p.getRandom().nextInt(avail.size())));
    }

    private static void apply(Player p, CompoundTag pd, String flag, Supplier<MobEffect> eff, String msg) {
        if (pd.getBoolean(flag)) return; // já tem
        pd.putBoolean(flag, true);
        p.addEffect(new MobEffectInstance(eff.get(), 600, 0, false, true, true));
        if (p instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal(msg), true);
    }

    /** Cura TODAS as doenças dimensionais (Soro Dimensional). */
    public static void cureAll(Player p) {
        CompoundTag pd = p.getPersistentData();
        boolean had = pd.getBoolean(F_MEM) || pd.getBoolean(F_INF) || pd.getBoolean(F_PAR);
        // se estava NO MEIO de uma viagem dimensional, traz de volta antes de curar (não prende o player)
        if (p instanceof ServerPlayer sp2 && pd.getInt(K_MEM_BACK) > 0) {
            pd.putInt(K_MEM_BACK, 0);
            returnFromTrip(sp2, pd);
        }
        pd.putBoolean(F_MEM, false); pd.putBoolean(F_INF, false); pd.putBoolean(F_PAR, false);
        pd.putInt(K_MEM_BACK, 0); pd.putInt(K_PAR_VIEW, 0);
        p.removeEffect(ModEffects.DIMENSIONAL_MEMORY.get());
        p.removeEffect(ModEffects.DIMENSIONAL_BLIGHT.get());
        p.removeEffect(ModEffects.DIMENSIONAL_PARANOIA.get());
        if (p instanceof ServerPlayer sp) {
            sp.setCamera(null);
            if (had) sp.displayClientMessage(Component.literal("§aAs doenças dimensionais foram purgadas do seu corpo."), true);
        }
    }

    public static boolean hasAnyDisease(Player p) {
        CompoundTag pd = p.getPersistentData();
        return pd.getBoolean(F_MEM) || pd.getBoolean(F_INF) || pd.getBoolean(F_PAR);
    }

    // ───────────────────────── tick por jogador ─────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel sl)) return;
        CompoundTag pd = sp.getPersistentData();

        if (pd.getBoolean(F_MEM)) tickMemory(sp, sl, pd);
        if (pd.getBoolean(F_INF)) tickInfection(sp, sl, pd);
        if (pd.getBoolean(F_PAR)) tickParanoia(sp, sl, pd);
    }

    private static void keepEffect(ServerPlayer sp, Supplier<MobEffect> eff) {
        MobEffectInstance cur = sp.getEffect(eff.get());
        if (cur == null || cur.getDuration() < 200)
            sp.addEffect(new MobEffectInstance(eff.get(), 600, 0, false, true, true));
    }

    // ── Memória Dimensional ──
    private static void tickMemory(ServerPlayer sp, ServerLevel sl, CompoundTag pd) {
        keepEffect(sp, ModEffects.DIMENSIONAL_MEMORY);
        if (sp.tickCount % 8 == 0)
            sl.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1.0, sp.getZ(), 6, 0.4, 0.8, 0.4, 0.05);

        int back = pd.getInt(K_MEM_BACK);
        if (back > 0) {
            back--;
            pd.putInt(K_MEM_BACK, back);
            if (back <= 0) returnFromTrip(sp, pd);
            return;
        }
        if (sp.isCreative() || sp.isSpectator()) return;
        int t = pd.getInt(K_MEM_T) + 1;
        pd.putInt(K_MEM_T, t);
        if (t >= MEM_PERIOD) { pd.putInt(K_MEM_T, 0); startTrip(sp, sl, pd); }
    }

    private static void startTrip(ServerPlayer sp, ServerLevel from, CompoundTag pd) {
        MinecraftServer server = sp.getServer();
        if (server == null) return;
        List<ServerLevel> targets = new ArrayList<>();
        for (ServerLevel l : server.getAllLevels()) if (l != from) targets.add(l);
        if (targets.isEmpty()) return;
        ServerLevel to = targets.get(sp.getRandom().nextInt(targets.size()));

        // salva o ponto de retorno
        pd.putString(K_MEM_DIM, from.dimension().location().toString());
        pd.putDouble(K_MEM_X, sp.getX());
        pd.putDouble(K_MEM_Y, sp.getY());
        pd.putDouble(K_MEM_Z, sp.getZ());
        pd.putBoolean(K_MEM_VALID, true);
        pd.putInt(K_MEM_BACK, MEM_TRIP);

        // proteção pra sobreviver ao pouso
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, MEM_TRIP + 60, 0, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MEM_TRIP + 100, 0, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MEM_TRIP + 40, 2, false, false));

        BlockPos spawn = to.getSharedSpawnPos();
        int x = spawn.getX(), z = spawn.getZ();
        int y = to.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        y = Math.max(to.getMinBuildHeight() + 2, Math.min(y + 1, to.getMaxBuildHeight() - 2));
        from.playSound(null, sp.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6F, 1.4F);
        sp.teleportTo(to, x + 0.5, y, z + 0.5, EnumSet.noneOf(RelativeMovement.class), sp.getYRot(), sp.getXRot());
        sp.displayClientMessage(Component.literal("§5Sua mente o arrasta para outro mundo…"), true);
    }

    private static void returnFromTrip(ServerPlayer sp, CompoundTag pd) {
        MinecraftServer server = sp.getServer();
        if (server == null) return;
        ServerLevel back = null;
        try {
            ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    new ResourceLocation(pd.getString(K_MEM_DIM)));
            back = server.getLevel(key);
        } catch (Exception ignored) {}
        if (back == null) back = server.overworld();
        double x, y, z;
        if (pd.getBoolean(K_MEM_VALID)) {
            x = pd.getDouble(K_MEM_X); y = pd.getDouble(K_MEM_Y); z = pd.getDouble(K_MEM_Z);
        } else { // sem ponto de retorno válido → spawn seguro
            BlockPos s = back.getSharedSpawnPos();
            x = s.getX() + 0.5; z = s.getZ() + 0.5;
            y = back.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.getX(), s.getZ());
        }
        pd.putBoolean(K_MEM_VALID, false);
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
        sp.teleportTo(back, x, y, z, EnumSet.noneOf(RelativeMovement.class), sp.getYRot(), sp.getXRot());
        sp.displayClientMessage(Component.literal("§5…e o devolve, alterado."), true);
    }

    // ── Infecção Dimensional ──
    private static void tickInfection(ServerPlayer sp, ServerLevel sl, CompoundTag pd) {
        keepEffect(sp, ModEffects.DIMENSIONAL_BLIGHT);
        if (sp.tickCount % 10 == 0)
            sl.sendParticles(ParticleTypes.ITEM_SLIME, sp.getX(), sp.getY() + 1.0, sp.getZ(), 4, 0.4, 0.6, 0.4, 0.01);
        if (sp.isCreative() || sp.isSpectator()) return;
        int t = pd.getInt(K_INF_T) + 1;
        if (t >= INF_PERIOD) { pd.putInt(K_INF_T, 0); if (sl.random.nextInt(2) == 0) spawnWorms(sl, sp); }
        else pd.putInt(K_INF_T, t);
    }

    private static void spawnWorms(ServerLevel sl, ServerPlayer sp) {
        int near = sl.getEntitiesOfClass(DimensionalWormEntity.class, sp.getBoundingBox().inflate(16)).size();
        if (near >= 4) return;
        int n = 1 + sl.random.nextInt(2);
        for (int i = 0; i < n; i++) {
            DimensionalWormEntity w = ModEntities.DIMENSIONAL_WORM.get().create(sl);
            if (w == null) continue;
            double ang = sl.random.nextDouble() * Math.PI * 2, dist = 2 + sl.random.nextDouble() * 3;
            double x = sp.getX() + Math.cos(ang) * dist, z = sp.getZ() + Math.sin(ang) * dist;
            w.moveTo(x, sp.getY(), z, sl.random.nextFloat() * 360F, 0F);
            w.finalizeSpawn(sl, sl.getCurrentDifficultyAt(w.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            sl.addFreshEntity(w);
        }
    }

    // ── Paranoia Dimensional ──
    private static void tickParanoia(ServerPlayer sp, ServerLevel sl, CompoundTag pd) {
        keepEffect(sp, ModEffects.DIMENSIONAL_PARANOIA);
        int view = pd.getInt(K_PAR_VIEW);
        if (view > 0) {
            // se o alvo da câmera saiu/morreu/mudou de dimensão, reseta já
            net.minecraft.world.entity.Entity cam = sp.getCamera();
            boolean valid = cam != null && cam != sp && cam.isAlive()
                    && cam instanceof ServerPlayer eye && eye.level() == sp.level();
            if (!valid) { sp.setCamera(null); pd.putInt(K_PAR_VIEW, 0); return; }
            view--;
            pd.putInt(K_PAR_VIEW, view);
            if (view <= 0) sp.setCamera(null);
            return;
        }
        if (sp.isCreative() || sp.isSpectator()) return;
        int t = pd.getInt(K_PAR_T) + 1;
        if (t >= PAR_PERIOD) {
            pd.putInt(K_PAR_T, 0);
            List<ServerPlayer> others = new ArrayList<>();
            for (ServerPlayer o : sl.players()) if (o != sp && o.isAlive()) others.add(o);
            if (others.isEmpty()) { pd.putInt(K_PAR_T, PAR_PERIOD - 1200); return; } // tenta de novo em 1 min
            ServerPlayer eye = others.get(sp.getRandom().nextInt(others.size()));
            sp.setCamera(eye);
            pd.putInt(K_PAR_VIEW, PAR_VIEW);
            sp.displayClientMessage(Component.literal("§9Seus olhos não são mais seus…"), true);
        } else pd.putInt(K_PAR_T, t);
    }

    // ── persistência através da morte: copia as flags (doença permanente) ──
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath()) return;
        CompoundTag from = e.getOriginal().getPersistentData();
        CompoundTag to = e.getEntity().getPersistentData();
        for (String k : new String[]{F_MEM, F_INF, F_PAR, K_MEM_T, K_INF_T, K_PAR_T,
                K_MEM_DIM, K_MEM_X, K_MEM_Y, K_MEM_Z}) {
            if (from.contains(k)) to.put(k, from.get(k).copy());
        }
        // estados transitórios NÃO copiados (resetam ao renascer): trip e camera
        to.putInt(K_MEM_BACK, 0);
        to.putInt(K_PAR_VIEW, 0);
    }
}
