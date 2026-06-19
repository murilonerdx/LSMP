package br.com.murilo.liberthia.magic.accessory;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * r164: PlayerTickEvent handler que aplica regen passivo de Mana e Sanidade
 * pros players com os MagicAccessoryItem equipados/no inventário.
 *
 * <p>Roda a cada 1 segundo (tickCount % 20 == 0). Mantém um accumulator por
 * player pra suportar regen fracional (ex: 0.5/s → 1 ponto a cada 2s).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MagicAccessoryTicker {

    /** Acumulador fracional de mana regen por player (UUID → fração restante). */
    private static final Map<UUID, Float> MANA_ACC = new HashMap<>();
    /** Acumulador fracional de sanity regen por player. */
    private static final Map<UUID, Float> SANITY_ACC = new HashMap<>();

    private MagicAccessoryTicker() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        // Roda 1× por segundo
        if (sp.tickCount % 20 != 0) return;

        UUID uuid = sp.getUUID();

        // ── Mana regen ──
        float manaRate = MagicAccessoryEffects.manaRegenPerSec(sp);
        if (manaRate > 0F) {
            float acc = MANA_ACC.getOrDefault(uuid, 0F) + manaRate;
            int wholeAdd = (int) Math.floor(acc);
            if (wholeAdd > 0) {
                int cur = SourceData.get(sp);
                int max = SourceData.getMax(sp);
                if (cur < max) {
                    SourceData.set(sp, Math.min(max, cur + wholeAdd));
                    // Sync direto pro client pra HUD atualizar imediato
                    try {
                        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                                new br.com.murilo.liberthia.observation.source.SourceSyncS2CPacket(
                                        SourceData.get(sp), max));
                    } catch (Throwable t) {
                        LiberthiaMod.LOGGER.debug("[Accessory] mana sync failed: {}", t.toString());
                    }
                }
                acc -= wholeAdd;
            }
            MANA_ACC.put(uuid, acc);
        } else if (MANA_ACC.containsKey(uuid)) {
            MANA_ACC.put(uuid, 0F);
        }

        // ── Sanity regen ──
        float sanityRate = MagicAccessoryEffects.sanityRegenPerSec(sp);
        if (sanityRate > 0F) {
            float acc = SANITY_ACC.getOrDefault(uuid, 0F) + sanityRate;
            int wholeAdd = (int) Math.floor(acc);
            if (wholeAdd > 0) {
                int cur = SpiritDimension.getSanity(sp);
                if (cur < SpiritDimension.MAX_SANITY) {
                    SpiritDimension.setSanity(sp,
                            Math.min(SpiritDimension.MAX_SANITY, cur + wholeAdd));
                }
                acc -= wholeAdd;
            }
            SANITY_ACC.put(uuid, acc);
        } else if (SANITY_ACC.containsKey(uuid)) {
            SANITY_ACC.put(uuid, 0F);
        }
    }

    /** Logout cleanup. */
    @SubscribeEvent
    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        UUID uuid = e.getEntity().getUUID();
        MANA_ACC.remove(uuid);
        SANITY_ACC.remove(uuid);
    }
}
