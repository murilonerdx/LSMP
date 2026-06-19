package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * BloodTreeProximityHandler — efeito ambiental quando jogadores ficam perto
 * de aglomerados de blood_log/blood_leaves.
 *
 * <p>A cada 60 ticks (3 s) por jogador, faz um scan numa caixa 8×8×8 ao
 * redor procurando blocos da árvore de sangue. Se encontrar ≥3 blocos:
 * <ul>
 *   <li>aplica Wither I por 5 s (efeito leve, marca o "olhar" da árvore);</li>
 *   <li>1× por minuto, manda mensagem na action bar.</li>
 * </ul>
 *
 * <p>Custo: o scan é 17³ = 4913 blocos, mas só rola a cada 3 s e early-out
 * quando atinge o threshold (3 blocos) — então em prática é barato. Não roda
 * em creative/spectator.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BloodTreeProximityHandler {
    private BloodTreeProximityHandler() {}

    private static final int SCAN_INTERVAL_TICKS = 60;
    private static final int MESSAGE_COOLDOWN_TICKS = 1200; // 1 min
    private static final int SCAN_RADIUS = 8;
    /** v0.1.44: reduzido de 3 → 1 — sapling sozinho já infecta. User pediu:
     *  "sapling segue sem dar infecção". Threshold 3 exigia árvore completa. */
    private static final int THRESHOLD_BLOCKS = 1;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        if (!(ev.player instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % SCAN_INTERVAL_TICKS != 0) return;

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int count = countBloodTreeBlocksNear(level, center, SCAN_RADIUS, THRESHOLD_BLOCKS);
        if (count < THRESHOLD_BLOCKS) return;

        // v0.1.51: se player tomou Dark Matter Pill, está resistente a DM —
        // pula TANTO o Wither (efeito derivado) QUANTO o ganho de matter.
        if (br.com.murilo.liberthia.matter.MatterResistance.blocked(
                player, br.com.murilo.liberthia.matter.MatterResistance.Type.DARK)) {
            return;
        }
        // Wither I por 5s — efeito moderado, não letal.
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, false, true, true));
        // v0.1.44: também aplica infecção REAL via MatterProfile (DM aumenta).
        // 0.3 ponto a cada 3s = ~6 pontos/min, escalável com nº de blocos próximos.
        float gain = 0.3f * Math.min(count, 8); // até 2.4/check pra clusters grandes
        player.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP)
                .ifPresent(profile -> {
                    profile.addDark(gain);
                    br.com.murilo.liberthia.matter.MatterProfileEvents.syncTo(player);
                });

        // Action bar message com cooldown próprio.
        long worldTime = level.getGameTime();
        long lastMsg = player.getPersistentData().getLong("liberthia.bloodtree.last_msg");
        if (worldTime - lastMsg >= MESSAGE_COOLDOWN_TICKS || lastMsg == 0L) {
            player.displayClientMessage(
                    Component.literal("⚠ Você sente a árvore de sangue te observando..."),
                    true);
            player.getPersistentData().putLong("liberthia.bloodtree.last_msg", worldTime);
        }
    }

    /**
     * Conta blocos de árvore de sangue numa caixa cúbica em torno do centro.
     * Early-out assim que atinge {@code earlyOutAt} pra economizar CPU.
     */
    private static int countBloodTreeBlocksNear(ServerLevel level, BlockPos center,
                                                 int radius, int earlyOutAt) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int x0 = center.getX(), y0 = center.getY(), z0 = center.getZ();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(x0 + dx, y0 + dy, z0 + dz);
                    // Skip chunks not loaded — getBlockState força load, então check explícito.
                    if (!level.isLoaded(cursor)) continue;
                    BlockState st = level.getBlockState(cursor);
                    // v0.1.40: ampliada lista pra incluir saplings (sanguine + blood)
                    // e ambas as famílias (sanguine_* original + blood_* nova).
                    // User pediu: "sanguine sapling deve dar infecção a todos
                    // que ficam perto o mesmo serve para as arvores já adultas".
                    if (st.is(ModBlocks.BLOOD_LOG.get())
                            || st.is(ModBlocks.BLOOD_LEAVES.get())
                            || st.is(ModBlocks.BLOOD_SAPLING.get())
                            || st.is(ModBlocks.STRIPPED_BLOOD_LOG.get())
                            || st.is(ModBlocks.SANGUINE_LOG.get())
                            || st.is(ModBlocks.SANGUINE_LEAVES.get())
                            || st.is(ModBlocks.SANGUINE_SAPLING.get())
                            || st.is(ModBlocks.STRIPPED_SANGUINE_LOG.get())) {
                        if (++count >= earlyOutAt) return count;
                    }
                }
            }
        }
        return count;
    }
}
