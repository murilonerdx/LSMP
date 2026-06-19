package br.com.murilo.liberthia.cosmic.curse;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * v0.1.22 r45: Tick que aplica hallucinations no victim do
 * {@link TargetedCurseStorage} pra cada carrier ativo.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>A cada 100 ticks (5s) per carrier player</li>
 *   <li>Resolve victim UUID → ServerPlayer</li>
 *   <li>Injeta hallucination escalonada pelo tempo de curse (intensifica)</li>
 *   <li>Aumenta insanity/paranoia do victim</li>
 * </ul>
 *
 * <p>O victim NÃO sabe que está sob curse — só vê coisas.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class TargetedCurseEvents {

    private TargetedCurseEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = event.getServer().overworld().getGameTime();
        if (now % 100 != 0) return; // 5s

        for (ServerPlayer carrier : event.getServer().getPlayerList().getPlayers()) {
            UUID targetId = TargetedCurseStorage.getTargetId(carrier);
            if (targetId == null) continue;

            ServerPlayer victim = event.getServer().getPlayerList().getPlayer(targetId);
            if (victim == null || victim == carrier) continue;

            long since = TargetedCurseStorage.getCurseStartTick(carrier);
            long elapsed = now - since;
            // Tier escala com tempo de curse: 0-2min tier 1, 2-5min tier 2, 5min+ tier 3
            int tier = elapsed < 2400 ? 1 : (elapsed < 6000 ? 2 : 3);

            HallucinationType[] pool;
            switch (tier) {
                case 1 -> pool = new HallucinationType[]{
                        HallucinationType.FAKE_WHISPER,
                        HallucinationType.SHADOW_MOVEMENT,
                        HallucinationType.FAKE_FOOTSTEP
                };
                case 2 -> pool = new HallucinationType[]{
                        HallucinationType.FAKE_ENTITY_PERIPHERAL,
                        HallucinationType.HEARTBEAT_PULSE,
                        HallucinationType.NAME_WHISPER,
                        HallucinationType.FAKE_BLOCK_FLASH,
                        HallucinationType.DISTORTED_AUDIO
                };
                default -> pool = new HallucinationType[]{
                        HallucinationType.FAKE_DEATH_FLASH,
                        HallucinationType.REALITY_SHAKE,
                        HallucinationType.SCREEN_GLITCH_BURST,
                        HallucinationType.FAKE_DAMAGE_INDICATOR,
                        HallucinationType.TEMPORAL_GHOST
                };
            }

            HallucinationType pick = pool[(int)(Math.random() * pool.length)];
            float intensity = 0.5F + tier * 0.2F;
            HallucinationManager.force(victim, pick, intensity, 60, "");

            InsanityData.addInsanity(victim, tier);
            InsanityData.addParanoia(victim, tier);
            if (tier >= 2) {
                InsanityData.addCosmicInfluence(victim, 1);
            }

            LiberthiaMod.LOGGER.debug("[TargetedCurse] {} → {} tier {} | {}",
                    carrier.getName().getString(),
                    victim.getName().getString(), tier, pick);
        }
    }
}
