package br.com.murilo.liberthia.cosmic.haunt;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.idol.IdolManager;
import br.com.murilo.liberthia.cosmic.scare.ScareS2CPacket;
import br.com.murilo.liberthia.cosmic.scare.ScareType;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModSounds;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * r178: <b>Diretor de Assombração</b> — {@code /liberthia haunt <alvos> <min>}.
 * Orquestra TODO o arsenal de terror num crescendo: sussurros e vultos no começo,
 * sustos e observadores no meio, flashes/grito/Ídolo no clímax. Escala com o tempo.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class HauntDirector {

    /** uuid → [startTick, endTick, idolFiredFlag]. */
    private static final Map<UUID, long[]> HAUNTS = new ConcurrentHashMap<>();

    private HauntDirector() {}

    public static void start(ServerPlayer sp, int minutes) {
        long now = sp.serverLevel().getGameTime();
        HAUNTS.put(sp.getUUID(), new long[]{now, now + (long) Math.max(1, minutes) * 1200L, 0});
    }

    public static void stop(UUID id) { HAUNTS.remove(id); }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("haunt")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("stop")
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(c -> {
                                            for (ServerPlayer p : EntityArgument.getPlayers(c, "alvos")) stop(p.getUUID());
                                            c.getSource().sendSuccess(() -> Component.literal("§7✦ Assombração encerrada."), false);
                                            return 1;
                                        })))
                        .then(Commands.argument("alvos", EntityArgument.players())
                                .executes(c -> haunt(c, 15))
                                .then(Commands.argument("minutos", IntegerArgumentType.integer(1, 240))
                                        .executes(c -> haunt(c, IntegerArgumentType.getInteger(c, "minutos")))))));
    }

    private static int haunt(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c, int min)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var targets = EntityArgument.getPlayers(c, "alvos");
        for (ServerPlayer p : targets) start(p, min);
        final int n = targets.size();
        c.getSource().sendSuccess(() -> Component.literal(
                "§5✦ Assombração iniciada em §f" + n + " §7player(s) por §f" + min + " §7min."), false);
        return n;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 40 != 0) return; // ~2s
        long[] h = HAUNTS.get(sp.getUUID());
        if (h == null) return;
        long now = sp.serverLevel().getGameTime();
        if (now >= h[1]) { HAUNTS.remove(sp.getUUID()); return; }

        float prog = (float) (now - h[0]) / (float) Math.max(1, h[1] - h[0]); // 0..1
        // chance de evento sobe com a intensidade: ~12% (início) → ~45% (clímax)
        float chance = 0.12F + prog * 0.33F;
        if (sp.getRandom().nextFloat() > chance) return;
        fireEvent(sp, prog, h);
    }

    private static void fireEvent(ServerPlayer sp, float prog, long[] h) {
        var r = sp.getRandom();
        ServerLevel sl = sp.serverLevel();
        float roll = r.nextFloat();

        // CLÍMAX (prog alto): eventos pesados
        if (prog > 0.66F) {
            if (roll < 0.18F && h[2] == 0L) {        // Ídolo, uma vez só
                h[2] = 1L; IdolManager.forceSpawn(sp); return;
            }
            if (roll < 0.32F) { spawnCreature(sl, sp); return; }  // O Sem-Rosto / O do Teto
            if (roll < 0.46F) { scare(sp, ScareType.FLASH, 20, r.nextInt(11), ""); return; }
            if (roll < 0.55F) { scare(sp, ScareType.BLACKOUT, 30, 0, ""); return; }
            if (roll < 0.70F) { scare(sp, ScareType.HEARTBEAT, 120, 0, ""); return; }
            if (roll < 0.85F) { drainSanity(sp, 5); return; }
            // cai pro pool médio também
        }
        // MÉDIO (prog 0.33+): sustos e observadores
        if (prog > 0.33F) {
            if (roll < 0.20F) { spawnGlimpse(sl, sp); return; }
            if (roll < 0.35F) { scare(sp, ScareType.EYES, 110, 0, ""); return; }
            if (roll < 0.48F) { scare(sp, ScareType.STATIC, 50, 0, ""); return; }
            if (roll < 0.60F) { scare(sp, ScareType.BLINK, 14, 0, ""); return; }
            if (roll < 0.72F) { scare(sp, ScareType.WHISPER, 90, 0, ""); return; }
            if (roll < 0.82F) { fakeChat(sp); return; }
            if (roll < 0.92F) { drainSanity(sp, 2); return; }
        }
        // SUTIL (sempre disponível): sons e vultos
        if (roll < 0.55F) whisperBehind(sl, sp);
        else vultoSwarm(sl, sp);
    }

    private static void scare(ServerPlayer sp, ScareType t, int dur, int variant, String text) {
        ModNetwork.sendToPlayer(sp, new ScareS2CPacket(t, dur, variant, text));
    }

    private static void drainSanity(ServerPlayer sp, int amt) {
        SpiritDimension.addSanity(sp, -amt);
        ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
    }

    private static void whisperBehind(ServerLevel sl, ServerPlayer sp) {
        Vec3 behind = sp.position().subtract(sp.getLookAngle().scale(3)).add(0, 1, 0);
        sl.playSound(null, behind.x, behind.y, behind.z, ModSounds.PERIPHERAL_WHISPER.get(),
                SoundSource.HOSTILE, 0.7F, 0.45F + sl.random.nextFloat() * 0.3F);
    }

    private static void vultoSwarm(ServerLevel sl, ServerPlayer sp) {
        // silhuetas de fumaça correndo na periferia (visual + sopro)
        for (int i = 0; i < 5; i++) {
            double ang = sl.random.nextDouble() * Math.PI * 2;
            double d = 6 + sl.random.nextDouble() * 8;
            double x = sp.getX() + Math.cos(ang) * d, z = sp.getZ() + Math.sin(ang) * d;
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, x, sp.getY() + 0.5, z, 6, 0.2, 0.8, 0.2, 0.01);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x, sp.getY() + 1.2, z, 3, 0.1, 0.4, 0.1, 0.01);
        }
        sl.playSound(null, sp.blockPosition(), ModSounds.WATCHER_STEP.get(), SoundSource.HOSTILE, 0.5F, 0.6F);
    }

    private static void fakeChat(ServerPlayer sp) {
        String[] names = {"Steve", "?????", "Herobrine", "null", "void"};
        String n = names[sp.getRandom().nextInt(names.length)];
        int v = sp.getRandom().nextInt(3);
        scare(sp, ScareType.FAKEJOIN, 1, v, n);
    }

    /** Invoca O do Teto (se há teto acima) ou O Sem-Rosto (num canto), conforme o lugar. */
    private static void spawnCreature(ServerLevel sl, ServerPlayer sp) {
        var bp = sp.blockPosition();
        // procura teto até 6 blocos acima
        int ceil = 0;
        for (int dy = 2; dy <= 6; dy++) {
            if (sl.getBlockState(bp.above(dy)).blocksMotion()) { ceil = dy; break; }
        }
        if (ceil >= 2 && sl.random.nextBoolean()) {
            var lurk = ModEntities.CEILING_LURKER.get().create(sl);
            if (lurk != null) {
                lurk.moveTo(sp.getX(), bp.getY() + ceil - 1.0, sp.getZ(), 0, 0);
                sl.addFreshEntity(lurk);
                return;
            }
        }
        // senão, uma criatura terrestre num ponto a ~10 blocos (varia: Sem-Rosto / Coletor / Vizinho)
        int pick = sl.random.nextInt(3);
        net.minecraft.world.entity.Mob ground =
                  pick == 0 ? ModEntities.FACELESS.get().create(sl)
                : pick == 1 ? ModEntities.EYE_COLLECTOR.get().create(sl)
                :             ModEntities.NEIGHBOR.get().create(sl);
        if (ground == null) return;
        double ang = sl.random.nextDouble() * Math.PI * 2, d = 8 + sl.random.nextDouble() * 6;
        ground.moveTo(sp.getX() + Math.cos(ang) * d, sp.getY(), sp.getZ() + Math.sin(ang) * d, 0, 0);
        sl.addFreshEntity(ground);
    }

    private static void spawnGlimpse(ServerLevel sl, ServerPlayer sp) {
        PeripheralObserverEntity obs = ModEntities.LOOM_PERIPHERAL.get().create(sl);
        if (obs == null) return;
        Vec3 look = sp.getLookAngle();
        double bx = sp.getX() - look.x * 9, bz = sp.getZ() - look.z * 9;
        obs.moveTo(bx, sp.getY(), bz, sl.random.nextFloat() * 360, 0);
        obs.startHaunt(sp.getUUID(), 2); // some sozinho em ~2 min
        sl.addFreshEntity(obs);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        HAUNTS.remove(e.getEntity().getUUID());
    }
}
