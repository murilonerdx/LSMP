package br.com.murilo.liberthia.cosmic.living;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r47: <b>The Second Sky</b> — alguns players "veem" um segundo céu
 * impossível por cima do normal.
 *
 * <h2>Exposure</h2>
 * Player ganha exposure via:
 * <ul>
 *   <li>Curator command (admin override)</li>
 *   <li>Estar perto de outro player exposed por &gt;5 min</li>
 *   <li>Cosmic Influence ≥ 50 (contágio passivo)</li>
 *   <li>Usar artifacts amaldiçoados muitas vezes</li>
 * </ul>
 *
 * <h2>Sintomas (exposure ≥ 1)</h2>
 * <ul>
 *   <li>Periodically vê impossible_moon, dark masses na periferia</li>
 *   <li>Ouve distant ocean sounds em desertos</li>
 *   <li>Coordenadas ficam off por alguns segundos (fake_chat)</li>
 *   <li>Sanity drain passivo +1/min</li>
 * </ul>
 *
 * <h2>NBT key</h2>
 * {@code liberthia.second_sky_exposure} (int 0-100)
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SecondSkyManager {

    public static final String NBT_EXPOSURE = "liberthia.second_sky_exposure";
    public static final int CONTAGION_RANGE = 8; // blocos
    public static final int CONTAGION_TIME = 6000; // 5min ticks de proximidade

    private SecondSkyManager() {}

    public static int getExposure(ServerPlayer sp) {
        return sp.getPersistentData().getInt(NBT_EXPOSURE);
    }

    public static void setExposure(ServerPlayer sp, int v) {
        sp.getPersistentData().putInt(NBT_EXPOSURE, Math.max(0, Math.min(100, v)));
    }

    public static void addExposure(ServerPlayer sp, int delta) {
        setExposure(sp, getExposure(sp) + delta);
    }

    public static boolean isExposed(ServerPlayer sp) {
        return getExposure(sp) > 0;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // r177: gatear no kill-switch de spam cósmico (default OFF) — não aparece mais sozinho.
        if (!br.com.murilo.liberthia.config.LiberthiaConfig.SERVER.cosmicChatSpamEnabled.get()) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 200 != 0) return; // 10s

        int exp = getExposure(sp);

        // Contagion: ganha exposure se está perto de player exposed
        if (exp == 0) {
            for (ServerPlayer near : sp.serverLevel().getPlayers(
                    p -> p != sp && p.distanceToSqr(sp) < CONTAGION_RANGE * CONTAGION_RANGE)) {
                if (getExposure(near) > 0 && Math.random() < 0.15) {
                    setExposure(sp, 5);
                    sp.displayClientMessage(Component.literal(
                            "§8§o*você olha pra cima... e algo olha de volta*"), true);
                    LiberthiaMod.LOGGER.info("[SecondSky] {} infected via contagion from {}",
                            sp.getName().getString(), near.getName().getString());
                    break;
                }
            }
            // Cosmic Influence auto-expose
            if (InsanityData.getCosmicInfluence(sp) >= 50 && Math.random() < 0.01) {
                setExposure(sp, 10);
                sp.displayClientMessage(Component.literal(
                        "§5§o*o segundo céu se revela*"), false);
            }
        }

        // Sintomas escalonados por exposure
        if (exp > 0) {
            // Visual particles no céu
            if (sp.tickCount % 100 == 0) {
                ServerLevel sl = sp.serverLevel();
                int n = Math.min(8, exp / 10 + 1);
                for (int i = 0; i < n; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 20 + Math.random() * 40;
                    sl.sendParticles(sp, ModParticles.GLARING_EYE_PULSE.get(),
                            true,
                            sp.getX() + Math.cos(a) * r,
                            sp.getY() + 60 + Math.random() * 30,
                            sp.getZ() + Math.sin(a) * r,
                            1, 0, 0, 0, 0);
                }
            }
            // Sintomas
            if (sp.tickCount % 600 == 0) {
                // Distant ocean / impossible moon
                int rand = (int)(Math.random() * 4);
                switch (rand) {
                    case 0 -> HallucinationManager.force(sp, HallucinationType.IMPOSSIBLE_MOON, 0.6F, 80, "");
                    case 1 -> HallucinationManager.force(sp, HallucinationType.DISTORTED_AUDIO, 0.5F, 60, "");
                    case 2 -> HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                            "§7§o*você vê um céu acima do céu*");
                    case 3 -> {
                        // Fake coords memory
                        int fx = (int) (sp.getX() + (Math.random() - 0.5) * 2000);
                        int fz = (int) (sp.getZ() + (Math.random() - 0.5) * 2000);
                        HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                                "§8§o*você se lembra de ter estado em " + fx + ", ?, " + fz + "*");
                    }
                }
                // Sanity drain
                InsanityData.addInsanity(sp, 1);
                InsanityData.addCosmicInfluence(sp, 1);
                // Aumenta exposure muito devagar (50% chance/min)
                if (Math.random() < 0.5 && exp < 100) {
                    setExposure(sp, exp + 1);
                }
            }
        }
    }
}
