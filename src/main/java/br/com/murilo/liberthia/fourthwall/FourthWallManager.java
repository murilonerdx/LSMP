package br.com.murilo.liberthia.fourthwall;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.FourthWallTriggerS2CPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Cérebro server-side da quarta parede: agenda sustos raros por player, dispara
 * a tela de morte torta na morte, e reescreve placas de madrugada. Tudo gated
 * por {@link FourthWallData} (liga/desliga + intensidade) e com cooldown — nunca
 * vira spam infinito.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class FourthWallManager {

    private FourthWallManager() {}

    private static final Random RNG = new Random();
    /** player UUID → próximo gameTime em que pode rolar um susto. */
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    // ── Pools de texto ───────────────────────────────────────────────
    private static final String[] DEATH_LINES = {
            "%name% foi alcançado.",
            "%name% olhou por tempo demais.",
            "ele finalmente te pegou, %name%.",
            "%name% não devia ter cavado tão fundo.",
            "você voltou. ele também.",
            "%name% parou de respirar às escuras.",
            "isto não foi um acidente.",
            "%name% foi convidado a ficar.",
            "não foi a queda. foi o que esperava embaixo.",
            "%name% viu o rosto errado."
    };

    /** Cada linha = uma placa de 4 linhas. %name% %dia% são preenchidos. */
    private static final String[][] SIGN_LINES = {
            {"EU LI O QUE", "VOCÊ", "ESCREVEU", "ANTES"},
            {"você dorme", "de costas", "pra porta", "sempre"},
            {"NÃO É", "A SUA", "LETRA", "É?"},
            {"você contou", "os blocos?", "eu contei", "VOCÊ."},
            {"isto não", "estava", "aqui", "ontem"},
            {"quem é", "o OUTRO", "na sua", "base?"},
            {"obrigado", "pela", "madeira", "— não assinei"},
            {"ainda", "faltam", "%dia%", "dias"},
            {"volte", "pra cama", "ainda não", "terminamos"},
            {"a luz não", "chega aqui", "de", "propósito"},
            {"pare de", "olhar", "pra trás", "não adianta"},
            {"%name%", "%name%", "%name%", "%name%"},
            {"HELLO", "again", ":)", ""},
            {"você não", "devia", "ter lido", "isto"}
    };

    private static final String[] TAB_GHOST_NAMES = {
            "?????", "ele", "o_que_observa", "—", "void"
    };

    // ── Scheduler por player ─────────────────────────────────────────
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;
        FourthWallData data = FourthWallData.get(overworld);
        if (!data.isMaster()) return;

        long now = overworld.getGameTime();
        int cd = cooldownTicks(data.getIntensity());

        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            UUID id = sp.getUUID();
            Long next = NEXT.get(id);
            if (next == null) {                  // grace: 1 cooldown depois de visto
                NEXT.put(id, now + cd);
                continue;
            }
            if (now < next) continue;
            NEXT.put(id, now + cd + RNG.nextInt(Math.max(1, cd / 2)));

            List<FourthWallFeature> pool = new ArrayList<>();
            for (FourthWallFeature f : FourthWallFeature.values()) {
                if (f.scheduler && data.isOn(f)) pool.add(f);
            }
            if (pool.isEmpty()) continue;
            fire(server, sp, pool.get(RNG.nextInt(pool.size())));
        }
    }

    /** Executa um efeito agendado (client-trigger ou ação server-side). */
    public static void fire(MinecraftServer server, ServerPlayer sp, FourthWallFeature f) {
        switch (f) {
            case CROSSHAIR_EYE -> ModNetwork.sendToPlayer(sp, new FourthWallTriggerS2CPacket(f, "", 60));
            case REAL_CLOCK    -> ModNetwork.sendToPlayer(sp, new FourthWallTriggerS2CPacket(f, "", 0));
            case F3_LIE        -> ModNetwork.sendToPlayer(sp, new FourthWallTriggerS2CPacket(f, "", 600));
            case FAKE_CRASH    -> ModNetwork.sendToPlayer(sp, new FourthWallTriggerS2CPacket(f, "", 0));
            case TAB_GHOST     -> tabGhost(server, sp);
            default -> {}
        }
    }

    // ── Tela de morte torta ──────────────────────────────────────────
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        FourthWallData data = FourthWallData.get(sp.serverLevel());
        if (!data.isOn(FourthWallFeature.DEATH_SCREEN)) return;
        if (RNG.nextFloat() > deathChance(data.getIntensity())) return;
        String line = fill(DEATH_LINES[RNG.nextInt(DEATH_LINES.length)], sp);
        ModNetwork.sendToPlayer(sp, new FourthWallTriggerS2CPacket(FourthWallFeature.DEATH_SCREEN, line, 0));
    }

    // ── Reescrita de placas (de madrugada) ───────────────────────────
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % 200L != 0L) return;     // tenta a cada 10s
        if (!sl.isNight()) return;                       // só de noite
        FourthWallData data = FourthWallData.get(sl);
        if (!data.isOn(FourthWallFeature.SIGN_REWRITE)) return;
        if (RNG.nextFloat() > signChance(data.getIntensity())) return;
        List<ServerPlayer> players = sl.players();
        if (players.isEmpty()) return;
        rewriteNearbySign(sl, players.get(RNG.nextInt(players.size())));
    }

    /** Acha uma placa perto do player e reescreve com texto bizarro. Retorna true se trocou. */
    public static boolean rewriteNearbySign(ServerLevel sl, ServerPlayer sp) {
        int cx = sp.chunkPosition().x, cz = sp.chunkPosition().z;
        List<SignBlockEntity> signs = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!sl.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk ch = sl.getChunk(cx + dx, cz + dz);
                for (BlockEntity be : ch.getBlockEntities().values()) {
                    if (be instanceof SignBlockEntity s) signs.add(s);
                }
            }
        }
        if (signs.isEmpty()) return false;
        SignBlockEntity sign = signs.get(RNG.nextInt(signs.size()));
        String[] lines = SIGN_LINES[RNG.nextInt(SIGN_LINES.length)];
        SignText text = sign.getFrontText();
        for (int i = 0; i < 4; i++) {
            text = text.setMessage(i, Component.literal(fill(lines[i], sp)));
        }
        sign.setText(text, true);
        sign.setChanged();
        sl.sendBlockUpdated(sign.getBlockPos(), sign.getBlockState(), sign.getBlockState(), 3);
        return true;
    }

    // ── TAB ghost (footer da lista de players) ───────────────────────
    private static void tabGhost(MinecraftServer server, ServerPlayer sp) {
        String name = RNG.nextInt(3) == 0
                ? sp.getGameProfile().getName()                       // seu nome duplicado
                : TAB_GHOST_NAMES[RNG.nextInt(TAB_GHOST_NAMES.length)];
        Component footer = Component.literal("§8§o" + name + " está observando.");
        sp.connection.send(new ClientboundTabListPacket(Component.empty(), footer));
        // limpa depois de ~8s
        server.tell(new TickTask(server.getTickCount() + 160, () ->
                sp.connection.send(new ClientboundTabListPacket(Component.empty(), Component.empty()))));
    }

    // ── /test do comando ─────────────────────────────────────────────
    public static void test(MinecraftServer server, ServerPlayer sp, FourthWallFeature f) {
        switch (f) {
            case DEATH_SCREEN -> ModNetwork.sendToPlayer(sp,
                    new FourthWallTriggerS2CPacket(f, fill(DEATH_LINES[RNG.nextInt(DEATH_LINES.length)], sp), -1));
            case SIGN_REWRITE -> rewriteNearbySign(sp.serverLevel(), sp);
            default -> fire(server, sp, f);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────
    private static String fill(String s, ServerPlayer sp) {
        long day = sp.serverLevel().getDayTime() / 24000L;
        return s.replace("%name%", sp.getGameProfile().getName())
                .replace("%dia%", String.valueOf(day))
                .replace("%x%", String.valueOf(sp.getBlockX()))
                .replace("%y%", String.valueOf(sp.getBlockY()))
                .replace("%z%", String.valueOf(sp.getBlockZ()));
    }

    private static int cooldownTicks(int intensity) {
        return switch (intensity) {
            case 0 -> 7200;  // ~6min
            case 2 -> 1800;  // ~1.5min
            default -> 4200; // ~3.5min
        };
    }

    private static float deathChance(int intensity) {
        return switch (intensity) {
            case 0 -> 0.30f;
            case 2 -> 0.85f;
            default -> 0.55f;
        };
    }

    private static float signChance(int intensity) {
        return switch (intensity) {
            case 0 -> 0.07f;
            case 2 -> 0.30f;
            default -> 0.15f;
        };
    }
}
