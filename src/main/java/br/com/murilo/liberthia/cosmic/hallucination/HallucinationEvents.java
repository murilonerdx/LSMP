package br.com.murilo.liberthia.cosmic.hallucination;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r40: Hooks Forge pro {@link HallucinationManager}.
 *
 * <ul>
 *   <li><b>ServerTickEvent:</b> chama {@code HallucinationManager.onServerTick} a cada tick</li>
 *   <li><b>PlayerLoggedOut:</b> cleanup do tracking</li>
 *   <li><b>PlayerTickEvent:</b> regen passivo de insanity quando bem (light + saciado + overworld)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class HallucinationEvents {

    private HallucinationEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            HallucinationManager.onServerTick(event.getServer());
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[Hallucination] manager tick error: {}", t.toString());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HallucinationManager.cleanup(event.getEntity().getUUID());
    }

    /**
     * Tick por player — regen passivo de stats quando o player tá "bem":
     * <ul>
     *   <li>Light alta (luz solar)</li>
     *   <li>HP cheio</li>
     *   <li>Acima do nível 50 (Y)</li>
     *   <li>Overworld</li>
     * </ul>
     *
     * Regen: -1 paranoia / -1 obsession a cada 200t (10s). Insanity e
     * forbiddenKnowledge/cosmicInfluence regen mais lento (a cada 600t).
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        boolean overworld = sp.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD);
        boolean wellLit = sp.serverLevel().getMaxLocalRawBrightness(sp.blockPosition()) >= 10;
        boolean healthy = sp.getHealth() >= sp.getMaxHealth() - 1;
        boolean highUp = sp.blockPosition().getY() >= 50;

        boolean recovering = overworld && wellLit && healthy && highUp;
        if (!recovering) return;

        if (sp.tickCount % 200 == 0) {
            // Recovery rate: paranoia/obsession decay 1 a cada 10s
            if (InsanityData.getParanoia(sp) > 0) {
                InsanityData.addParanoia(sp, -1);
            }
            if (InsanityData.getObsession(sp) > 0) {
                InsanityData.addObsession(sp, -1);
            }
        }
        if (sp.tickCount % 600 == 0) {
            // Insanity / cosmic recovery — 1 a cada 30s
            if (InsanityData.getInsanity(sp) > 0) {
                InsanityData.addInsanity(sp, -1);
            }
            if (InsanityData.getCosmicInfluence(sp) > 0) {
                InsanityData.addCosmicInfluence(sp, -1);
            }
            // forbiddenKnowledge é PERMANENTE — não regen (você sempre sabe)
        }
    }
}
