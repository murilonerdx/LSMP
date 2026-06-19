package br.com.murilo.liberthia.cosmic.observatory;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r48: <b>Observation Injection</b> — Caretaker secretly marca
 * um player. Ao longo do tempo, o server reage MAIS a esse player com
 * anomalias intensificadas.
 *
 * <h2>NBT keys</h2>
 * <ul>
 *   <li>{@code liberthia.injected_observation} (boolean): marca ativa?</li>
 *   <li>{@code liberthia.injection_since} (long): tick em que foi marcado</li>
 *   <li>{@code liberthia.injection_intensity} (int 1-5): nível de escalation</li>
 * </ul>
 *
 * <h2>Escalation curve</h2>
 * Cada 5 min de "marcação", intensity++ (cap 5). Em cada tier:
 * <ul>
 *   <li>1: whispers ocasionais</li>
 *   <li>2: shadow movement + footsteps</li>
 *   <li>3: fake entities periféricos + heartbeat</li>
 *   <li>4: name whispers + reverse audio + temporal ghosts</li>
 *   <li>5: full glitch bursts + reality shake + death flash</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ObservationInjection {

    public static final String NBT_MARKED = "liberthia.injected_observation";
    public static final String NBT_SINCE = "liberthia.injection_since";
    public static final String NBT_INTENSITY = "liberthia.injection_intensity";

    /** Tier escalation interval: 5 min. */
    public static final int TIER_INTERVAL = 6000;

    private ObservationInjection() {}

    public static boolean isMarked(Player p) {
        return p.getPersistentData().getBoolean(NBT_MARKED);
    }

    public static void mark(Player p) {
        p.getPersistentData().putBoolean(NBT_MARKED, true);
        p.getPersistentData().putLong(NBT_SINCE, p.level().getGameTime());
        p.getPersistentData().putInt(NBT_INTENSITY, 1);
        LiberthiaMod.LOGGER.info("[ObservationInjection] {} MARKED",
                p.getName().getString());
    }

    public static void unmark(Player p) {
        p.getPersistentData().remove(NBT_MARKED);
        p.getPersistentData().remove(NBT_SINCE);
        p.getPersistentData().remove(NBT_INTENSITY);
    }

    public static int getIntensity(Player p) {
        return p.getPersistentData().getInt(NBT_INTENSITY);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!isMarked(sp)) return;
        if (sp.tickCount % 100 != 0) return; // checa 5s

        long now = sp.level().getGameTime();
        long since = sp.getPersistentData().getLong(NBT_SINCE);
        long elapsed = now - since;

        // Atualiza intensity por tempo decorrido
        int expectedTier = Math.min(5, (int)(elapsed / TIER_INTERVAL) + 1);
        int currentTier = sp.getPersistentData().getInt(NBT_INTENSITY);
        if (expectedTier > currentTier) {
            sp.getPersistentData().putInt(NBT_INTENSITY, expectedTier);
            currentTier = expectedTier;
            LiberthiaMod.LOGGER.info("[ObservationInjection] {} escalated to tier {}",
                    sp.getName().getString(), currentTier);
        }

        // Roll dado pra trigger anomaly
        float chance = 0.10F + currentTier * 0.06F;
        if (Math.random() > chance) return;

        HallucinationType pick;
        switch (currentTier) {
            case 1 -> pick = pickFromTier(new HallucinationType[]{
                    HallucinationType.FAKE_WHISPER,
                    HallucinationType.SHADOW_MOVEMENT
            });
            case 2 -> pick = pickFromTier(new HallucinationType[]{
                    HallucinationType.FAKE_FOOTSTEP,
                    HallucinationType.SHADOW_MOVEMENT,
                    HallucinationType.FAKE_BLOCK_FLASH
            });
            case 3 -> pick = pickFromTier(new HallucinationType[]{
                    HallucinationType.FAKE_ENTITY_PERIPHERAL,
                    HallucinationType.HEARTBEAT_PULSE,
                    HallucinationType.DISTORTED_AUDIO,
                    HallucinationType.FALSE_LIGHT
            });
            case 4 -> pick = pickFromTier(new HallucinationType[]{
                    HallucinationType.NAME_WHISPER,
                    HallucinationType.REVERSE_AUDIO_PULSE,
                    HallucinationType.TEMPORAL_GHOST,
                    HallucinationType.FAKE_INVENTORY_ITEM
            });
            default -> pick = pickFromTier(new HallucinationType[]{
                    HallucinationType.SCREEN_GLITCH_BURST,
                    HallucinationType.REALITY_SHAKE,
                    HallucinationType.FAKE_DEATH_FLASH,
                    HallucinationType.FAKE_DAMAGE_INDICATOR,
                    HallucinationType.IMPOSSIBLE_MOON
            });
        }
        float intensity = 0.4F + currentTier * 0.12F;
        HallucinationManager.force(sp, pick, intensity, 60, "");

        // Sanity drain proporcional
        InsanityData.addInsanity(sp, currentTier);
        if (currentTier >= 3) InsanityData.addParanoia(sp, 1);
        if (currentTier >= 4) InsanityData.addCosmicInfluence(sp, 1);
    }

    private static HallucinationType pickFromTier(HallucinationType[] pool) {
        return pool[(int)(Math.random() * pool.length)];
    }
}
