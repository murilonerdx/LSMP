package br.com.murilo.liberthia.magic.glyph;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.148 r116: <b>SourceRegenTicker</b> — regenera Source/Mana do player
 * passivamente ao longo do tempo. Resolve a queixa de "sistema de source
 * muito cru" — antes Source só subia via items/blocos específicos.
 *
 * <h2>Regras de regeneração</h2>
 * <ul>
 *   <li><b>Base</b>: +1 Source a cada 60 ticks (3 segundos)</li>
 *   <li><b>Spirit World</b>: +1 a cada 20 ticks (3x mais rápido — dimensão é fonte de mana)</li>
 *   <li><b>Combat</b>: regen pausada 5s após dano (encoraja kiting / posicionamento)</li>
 *   <li><b>Cap por nível</b>: max scaling com {@link MagicLevelData#getMaxSourceForLevel}</li>
 * </ul>
 *
 * <p>Adicionalmente, recompensa o player que se aventura no Spirit World
 * com regen massivamente maior — alinha gameplay loop (vai pra Spirit
 * pra coletar reagents → também recupera mana lá).
 *
 * <p>Original code, Forge PlayerTickEvent boilerplate + nossa lógica.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SourceRegenTicker {

    /** NBT key: tick em que o player levou dano por último. */
    private static final String NBT_LAST_HURT_TICK = "liberthia.last_hurt_tick";
    /** Cooldown após dano em que regen fica pausada. */
    private static final int COMBAT_PAUSE_TICKS = 100; // 5s

    private SourceRegenTicker() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return; // check 1× por segundo

        int cur = SourceData.get(sp);
        int max = SourceData.getMax(sp);
        if (cur >= max) return; // já no max

        // Combat pause check
        long lastHurt = sp.getPersistentData().getLong(NBT_LAST_HURT_TICK);
        if (sp.tickCount - lastHurt < COMBAT_PAUSE_TICKS) return;

        // Determine regen rate by location
        boolean inSpirit = SpiritDimension.isInSpiritWorld(sp);
        // r117: armor bonus de regen (até 3.5× com set 4/4)
        float armorMult = br.com.murilo.liberthia.magic.armor.ManaArmorEffects.regenMultiplierFor(sp);
        int regen;
        if (inSpirit) {
            // Spirit baseline: 1/s
            regen = Math.max(1, (int)(1 * armorMult));
            SourceData.add(sp, regen);
        } else {
            // Overworld baseline: 1 a cada 3s (60t)
            if (sp.tickCount % 60 == 0) {
                regen = Math.max(1, (int)(1 * armorMult));
                SourceData.add(sp, regen);
            }
        }
    }

    /** Hook pra LivingHurt resetar combat pause. */
    @SubscribeEvent
    public static void onPlayerHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.getPersistentData().putLong(NBT_LAST_HURT_TICK, sp.tickCount);
        }
    }
}
