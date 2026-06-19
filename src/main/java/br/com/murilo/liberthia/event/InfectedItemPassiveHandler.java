package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.matter.MatterProfileEvents;
import br.com.murilo.liberthia.matter.MatterProfileProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aplica penalidades passivas enquanto items com tag {@code MatterInfected}
 * estão no inventário do player.
 *
 * <p>Mecânica:
 * <ul>
 *   <li>A cada 40 ticks (2s) varre os 41 slots do inventário (main 0-35 +
 *       armor 4 + offhand 1).</li>
 *   <li>Cada item infectado encontrado: {@code profile.addDark(+0.5f)}.</li>
 *   <li>Item infectado na MAIN hand → aplica Wither I (3s) — feedback de
 *       "está doendo segurar isso".</li>
 *   <li>≥3 items infectados → partículas SCULK_SOUL ao redor pra reforço
 *       visual.</li>
 * </ul>
 *
 * <p><b>Proteção</b>: jogador com Refined Containment Pendant OU Refined
 * Containment Glove ATIVOS (durab > 0) está imune. Cada proteção drena 1
 * ponto de durabilidade por check com efeito.
 *
 * <p>Sync do profile via {@link MatterProfileEvents#syncTo} acontece só se
 * houve mudança (evita flood de pacotes).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class InfectedItemPassiveHandler {

    private static final Logger LOG = LoggerFactory.getLogger("Liberthia/InfectedItemPassive");

    /** Período do check em ticks. 40t = 2s. */
    private static final int CHECK_PERIOD = 40;
    /** DM acumulado por item infectado encontrado, por check. Lento por design. */
    private static final float DARK_PER_ITEM = 0.5f;
    /** Mínimo de items infectados pra disparar partículas SCULK ambient. */
    private static final int PARTICLE_THRESHOLD = 3;

    private InfectedItemPassiveHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % CHECK_PERIOD != 0) return;

        // Varre TODO o inventário (incluindo armor + offhand). getContainerSize()
        // em Inventory retorna 41 (36 main + 4 armor + 1 offhand) — não precisa
        // splitar manualmente.
        int infectedCount = 0;
        boolean mainHandInfected = isInfected(player.getMainHandItem());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isInfected(s)) infectedCount++;
        }
        if (infectedCount == 0) return;

        // ── Proteção via Refined Containment artifacts ──
        // Igual ao tratamento em DarkMatterRadiationHandler: ambos suprimem
        // totalmente o ganho e drenam 1 ponto de durabilidade por exposição.
        // Pendant tem prioridade (cobre todo o corpo); glove só se ativa
        // quando há contato direto (segurando item na mão).
        if (br.com.murilo.liberthia.compat.CuriosCompat.isRefinedPendantActive(player)) {
            br.com.murilo.liberthia.compat.CuriosCompat.damageRefinedPendant(player, 1);
            return;
        }
        if (mainHandInfected
                && br.com.murilo.liberthia.compat.CuriosCompat.isRefinedGloveActive(player)) {
            // Glove refinada cobre só o caso "segurando na mão" — items no
            // resto do inventário ainda passam reto. Decisão consciente:
            // a glove é o ítem de "proteção tátil", pendant é a proteção
            // completa de corpo.
            br.com.murilo.liberthia.compat.CuriosCompat.damageRefinedGlove(player, 1);
            return;
        }

        // ── Aplica matter dark proporcional ao count ──
        // v0.1.51: skip se player tem Dark Matter Resistance ativa (pílula).
        if (br.com.murilo.liberthia.matter.MatterResistance.blocked(
                player, br.com.murilo.liberthia.matter.MatterResistance.Type.DARK)) {
            return;
        }
        final int finalCount = infectedCount;
        LOG.debug("InfectedItemPassive: player {} has {} infected items, applying +{} DM",
                player.getGameProfile().getName(), finalCount, DARK_PER_ITEM * finalCount);
        player.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            profile.addDark(DARK_PER_ITEM * finalCount);
            MatterProfileEvents.syncTo(player);
        });

        // ── Wither leve enquanto segura na main hand ──
        // Duration 60t = 3s ≥ CHECK_PERIOD pra ficar permanente enquanto
        // carrega; amplifier 0 = Wither I (0.5 dmg/40t).
        if (mainHandInfected) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, true, false, true));
        }

        // ── Particles SCULK_SOUL quando há ≥3 items ──
        // Feedback visual que algo ruim está rolando mesmo se o player não tá
        // olhando o HUD. Evento ambient — não causa lag, só 6 partículas/check.
        if (infectedCount >= PARTICLE_THRESHOLD && player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SCULK_SOUL,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    6, 0.4, 0.6, 0.4, 0.02);
        }
    }

    /** Helper: checa se um stack tem a tag {@code MatterInfected}. */
    public static boolean isInfected(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var tag = stack.getTag();
        return tag != null && tag.getBoolean(MatterInfectionTooltipHandler.TAG_INFECTED);
    }
}
