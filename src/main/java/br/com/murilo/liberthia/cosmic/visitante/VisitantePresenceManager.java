package br.com.murilo.liberthia.cosmic.visitante;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.horror.entity.VisitanteEntity;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * r180b — <b>Presença do Visitante</b>. Orquestra o terror psicológico do
 * {@link VisitanteEntity} <b>quando o jogador está completamente sozinho</b>
 * (ninguém num raio de 200b, via {@link AfkObserverManager#isAlone}).
 *
 * <p>Quanto mais tempo sozinho no escuro, mais a <b>presença</b> sobe e os
 * eventos escalam — a voz (sons posicionais + texto com glitch §k) chega cada
 * vez mais perto até "sussurrar no ouvido", a Figura passa ao fundo / espreita,
 * a luz "respira" (DARKNESS) e, raramente, o clímax "CORRA!!" (cego + perseguição
 * vermelha que NUNCA causa dano). Reaproveita {@link CosmicSoundManager} e
 * {@link InsanityData}.
 */
public final class VisitantePresenceManager {

    private VisitantePresenceManager() {}

    private static final Map<UUID, State> STATE = new ConcurrentHashMap<>();
    private static final int RUN_EVERY = 20;          // roda a lógica a cada 1s
    private static final int CLIMAX_COOLDOWN = 12000; // 10 min entre clímax por player

    static final class State {
        int presence = 0;
        long aloneSince = -1;
        long lastEvent = 0;
        long lastClimax = -100000;
    }

    /** Falas ambiente (a Voz que Aprende — imitação com pausas erradas). PT-PT. */
    private static final String[] AMBIENT = {
            "...estás aí?", "fica.", "não vás.", "eu ouço-te.", "tão sozinho...",
            "olha para trás.", "mais perto agora.", "ainda aqui.", "shhh..."
    };
    /** Falas raras (eventos fortes). */
    private static final String[] RARE = {
            "Estou mesmo aqui.", "Não lembras de mim?",
            "Eu sempre estive cá... na outra dimensão.",
            "Vira-te.", "Estou exatamente atrás de ti."
    };

    // ──────────── tick ────────────
    public static void onServerTick(MinecraftServer server) {
        if (server == null || server.getTickCount() % RUN_EVERY != 0) return;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            try { tickPlayer(sp); }
            catch (Throwable t) { LiberthiaMod.LOGGER.warn("[Visitante] tick error: {}", t.toString()); }
        }
    }

    private static void tickPlayer(ServerPlayer sp) {
        if (sp.isCreative() || sp.isSpectator()) return;
        if (sp.level().dimension().equals(SpiritDimension.SPIRIT_WORLD)) return; // spirit tem horror próprio
        if (!(sp.level() instanceof ServerLevel sl)) return;
        // r183: O Visitante só se manifesta com sanidade < 40%
        if (SpiritDimension.getSanity(sp) >= 40) {
            State s2 = STATE.get(sp.getUUID());
            if (s2 != null) { s2.presence = Math.max(0, s2.presence - 2); s2.aloneSince = -1; }
            return;
        }

        long now = sl.getGameTime();
        State st = STATE.computeIfAbsent(sp.getUUID(), k -> new State());

        // "Só aparece quando o jogador está completamente sozinho."
        if (!isAlone(sl, sp)) {
            st.aloneSince = -1;
            st.presence = Math.max(0, st.presence - 3);
            return;
        }
        if (st.aloneSince < 0) st.aloneSince = now;

        // escuro/noite/subterrâneo intensifica; dia claro acalma
        int light = sl.getMaxLocalRawBrightness(sp.blockPosition());
        boolean dim = light < 7 || !sl.isDay() || sp.blockPosition().getY() < 50;
        st.presence = dim ? Math.min(100, st.presence + 2) : Math.max(0, st.presence - 1);
        if (st.presence < 12) return;

        int interval = Mth.clamp(300 - st.presence * 2, 80, 300);
        if (now - st.lastEvent < interval) return;
        st.lastEvent = now;
        fireEvent(sp, sl, st, now);
    }

    private static void fireEvent(ServerPlayer sp, ServerLevel sl, State st, long now) {
        RandomSource r = sp.getRandom();
        int pres = st.presence;

        if (pres >= 85 && now - st.lastClimax > CLIMAX_COOLDOWN && r.nextFloat() < 0.18F) {
            doClimax(sp, sl, st, now); return;
        }
        if (pres >= 70 && r.nextFloat() < 0.28F) {
            if (r.nextBoolean()) lightsOutCloser(sp, sl, r);
            else spawnFigure(sp, sl, VisitanteEntity.Mode.PEEK, r);
            bumpParanoia(sp, 2); return;
        }
        if (pres >= 55) {
            float roll = r.nextFloat();
            if (roll < 0.30F) { earWhisper(sp, rareOrName(sp, r), r); bumpParanoia(sp, 2); return; }
            if (roll < 0.45F) { lightBreath(sp); return; }
            if (roll < 0.62F) { spawnFigure(sp, sl, VisitanteEntity.Mode.PASSING, r); return; }
        }
        if (pres >= 40 && r.nextFloat() < 0.25F) { spawnFigure(sp, sl, VisitanteEntity.Mode.PASSING, r); return; }
        if (pres >= 25 && r.nextFloat() < 0.35F) { voiceLine(sp, AMBIENT[r.nextInt(AMBIENT.length)]); bumpParanoia(sp, 1); return; }
        behindWhisper(sp, pres, r);
    }

    // ──────────── behaviors ────────────
    /** A voz atrás do player — fica mais perto/alta conforme a presença sobe. */
    private static void behindWhisper(ServerPlayer sp, int pres, RandomSource r) {
        double dist = Math.max(0.9, 4.0 - pres / 30.0);
        Vec3 behind = sp.position().subtract(sp.getLookAngle().scale(dist));
        float vol = 0.4F + pres / 200.0F;
        CosmicSoundManager.playPositional(sp, ModSounds.COSMIC_DISTANT_WHISPERS.get(),
                behind.x, behind.y + 1.4, behind.z, vol, 0.7F + r.nextFloat() * 0.2F);
    }

    /** Sussurro "no ouvido" + fala. */
    private static void earWhisper(ServerPlayer sp, String line, RandomSource r) {
        Vec3 ear = sp.position().subtract(sp.getLookAngle().scale(0.6));
        CosmicSoundManager.playPositional(sp, whisperSound(r),
                ear.x, ear.y + 1.5, ear.z, 0.95F, 0.85F);
        voiceLine(sp, line);
    }

    private static void voiceLine(ServerPlayer sp, String text) {
        sp.displayClientMessage(Component.literal(glitch(text, sp.getRandom())), false);
    }

    /** "A Luz que Pisca" — DARKNESS curto pulsa e volta = respiração da luz. */
    private static void lightBreath(ServerPlayer sp) {
        sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false));
    }

    /** Luz apaga ~1s → quando volta, a figura está mais perto. */
    private static void lightsOutCloser(ServerPlayer sp, ServerLevel sl, RandomSource r) {
        sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 22, 0, false, false));
        spawnFigure(sp, sl, VisitanteEntity.Mode.PEEK, r);
        CosmicSoundManager.playPositional(sp, ModSounds.COSMIC_VOID_BREATHING.get(),
                sp.getX(), sp.getY() + 1, sp.getZ(), 0.6F, 0.6F);
    }

    /** Clímax "CORRA!!": cego + grito + perseguição vermelha (sem dano), depois some. */
    private static void doClimax(ServerPlayer sp, ServerLevel sl, State st, long now) {
        sendTitle(sp, "§4§lCORRA", "§cele está mesmo atrás de ti");
        sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
        CosmicSoundManager.playPositional(sp, ModSounds.COSMIC_DISTANT_SCREAM.get(),
                sp.getX(), sp.getY() + 1, sp.getZ(), 2.4F, 0.85F);
        Vec3 behind = sp.position().subtract(sp.getLookAngle().scale(6.0));
        VisitanteEntity v = spawnAt(sl, behind.x, sp.getY(), behind.z, VisitanteEntity.Mode.CHASE);
        if (v != null) v.setGlowingTag(true);
        bumpParanoia(sp, 18);
        try { InsanityData.addInsanity(sp, 8); } catch (Throwable ignored) {}
        st.presence = 0;
        st.lastClimax = now;
    }

    // ──────────── figura ────────────
    private static void spawnFigure(ServerPlayer sp, ServerLevel sl, VisitanteEntity.Mode mode, RandomSource r) {
        Vec3 look = horizontalLook(sp);
        double rx = -look.z, rz = look.x; // perpendicular horizontal (à direita)
        if (mode == VisitanteEntity.Mode.PASSING) {
            double front = 16 + r.nextDouble() * 8;
            double side = 8 + r.nextDouble() * 3;
            double cx = sp.getX() + look.x * front, cz = sp.getZ() + look.z * front;
            VisitanteEntity v = spawnAt(sl, cx + rx * side, sp.getY(), cz + rz * side, mode);
            if (v != null) v.setCrossTarget(cx - rx * side, cz - rz * side); // atravessa pro outro lado
        } else { // PEEK — perto, à frente
            double front = 9 + r.nextDouble() * 5;
            double side = (r.nextBoolean() ? 1 : -1) * (r.nextDouble() * 3);
            spawnAt(sl, sp.getX() + look.x * front + rx * side, sp.getY(),
                    sp.getZ() + look.z * front + rz * side, mode);
        }
    }

    private static VisitanteEntity spawnAt(ServerLevel sl, double x, double y, double z, VisitanteEntity.Mode mode) {
        VisitanteEntity v = ModEntities.VISITANTE.get().create(sl);
        if (v == null) return null;
        v.moveTo(x, y, z, 0.0F, 0.0F);
        v.setMode(mode);
        sl.addFreshEntity(v);
        return v;
    }

    // ──────────── util ────────────
    /** Sozinho = nenhum outro player (não-spectator) num raio de 200b no mesmo level. */
    private static boolean isAlone(ServerLevel sl, ServerPlayer self) {
        for (ServerPlayer other : sl.players()) {
            if (other == self || other.isSpectator()) continue;
            if (other.distanceToSqr(self) < 200.0 * 200.0) return false;
        }
        return true;
    }

    private static Vec3 horizontalLook(ServerPlayer sp) {
        Vec3 l = sp.getLookAngle();
        Vec3 h = new Vec3(l.x, 0, l.z);
        return h.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : h.normalize();
    }

    private static String rareOrName(ServerPlayer sp, RandomSource r) {
        if (r.nextFloat() < 0.22F) return sp.getName().getString(); // diz o teu nome
        return RARE[r.nextInt(RARE.length)];
    }

    private static SoundEvent whisperSound(RandomSource r) {
        return switch (r.nextInt(4)) {
            case 0 -> ModSounds.COSMIC_WHISPER_1.get();
            case 1 -> ModSounds.COSMIC_WHISPER_2.get();
            case 2 -> ModSounds.COSMIC_WHISPER_3.get();
            default -> ModSounds.COSMIC_DISTANT_WHISPERS.get();
        };
    }

    /** Corrompe parte do texto com §k (obfuscated) = glitch na voz / palavra trocada. */
    private static String glitch(String s, RandomSource r) {
        if (s.length() < 4 || r.nextFloat() < 0.25F) return "§8§o" + s;
        int a = r.nextInt(s.length() - 2);
        int b = Math.min(s.length(), a + 1 + r.nextInt(3));
        return "§8§o" + s.substring(0, a) + "§k" + s.substring(a, b) + "§r§8§o" + s.substring(b);
    }

    private static void bumpParanoia(ServerPlayer sp, int n) {
        try { InsanityData.addParanoia(sp, n); } catch (Throwable ignored) {}
    }

    private static void sendTitle(ServerPlayer sp, String title, String sub) {
        sp.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
        sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
        sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
    }

    public static void cleanup(UUID id) { STATE.remove(id); }
}
