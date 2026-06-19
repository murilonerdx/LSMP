package br.com.murilo.liberthia.magic.armor;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.149 r117: <b>ManaArmorEffects</b> — escaneia a armadura do player a cada
 * tick e aplica bônus de Source max + regen multiplier baseado em quantas
 * peças de Spirit Robes estão equipadas.
 *
 * <h2>Bonus stacking</h2>
 * <ul>
 *   <li>+20 Source max por peça</li>
 *   <li>+1 regen por chamada do tick (cada peça aumenta a frequência base)</li>
 *   <li>Set bonus 4/4: cost multiplier 0.75 (consultado por UniversalSpellScrollItem)</li>
 * </ul>
 *
 * <p>NBT key {@code liberthia.armor_source_bonus} é setado quando equipa e
 * limpo quando desequipa — evita conflito com outras fontes de Source max.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ManaArmorEffects {

    public static final String NBT_BONUS = "liberthia.armor_source_bonus";
    public static final int BONUS_PER_PIECE = 20;

    private ManaArmorEffects() {}

    public static int countSpiritRobes(Player p) {
        int n = 0;
        for (ItemStack s : p.getArmorSlots()) {
            if (s.getItem() instanceof SpiritRobesItem) n++;
        }
        return n;
    }

    /**
     * Multiplicador de custo aplicado ao spell. Set 4/4 = 0.75 (25% off).
     * Sem armadura = 1.0F (sem desconto).
     */
    public static float costMultiplierFor(Player p) {
        int pieces = countSpiritRobes(p);
        if (pieces >= 4) return 0.75F;        // full set
        if (pieces == 3) return 0.85F;        // 3/4
        if (pieces == 2) return 0.92F;        // 2/4
        if (pieces == 1) return 0.97F;        // 1/4
        return 1.0F;
    }

    /** Multiplicador de regen: cada peça +50%, set bonus +50% extra. */
    public static float regenMultiplierFor(Player p) {
        int pieces = countSpiritRobes(p);
        float base = 1.0F + pieces * 0.5F;
        if (pieces >= 4) base += 0.5F;
        return base;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;  // check 1×/s

        // r164 BUG FIX: usa SourceData.recomputeMax (que soma level+armor+base
        // de forma idempotente) em vez de diff math. NBT_BONUS é mantido só
        // como marcador "última quantidade de robes vista" pra evitar
        // recompute desnecessário todo segundo.
        int pieces = countSpiritRobes(sp);
        int currentBonus = sp.getPersistentData().getInt(NBT_BONUS);
        int targetBonus = pieces * BONUS_PER_PIECE;

        if (currentBonus != targetBonus) {
            sp.getPersistentData().putInt(NBT_BONUS, targetBonus);
            SourceData.recomputeMax(sp);
        }
    }
}
