package br.com.murilo.liberthia.cosmic.living;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r47: <b>The Walkers</b> — silhuetas distantes na render distance.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Spawn aleatório a ~48-80 blocos do player (próximo à render distance)</li>
 *   <li>NUNCA se aproximam — só ficam lá</li>
 *   <li>Quando player OLHA pra eles (dot &gt; 0.7), desaparecem em 1s</li>
 *   <li>Quando player olha pra OUTRO lado, podem se mover lateralmente</li>
 *   <li>Aparição é PURAMENTE visual — particles, sem entidade real</li>
 * </ul>
 *
 * <h2>Trigger conditions</h2>
 * Frequência aumenta com:
 * <ul>
 *   <li>Player com Insanity ≥ 30</li>
 *   <li>Player em Second Sky exposure ≥ 5</li>
 *   <li>Chunk com emotional index alto</li>
 *   <li>Observation Pressure alto</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class WalkersManager {

    private WalkersManager() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // r177: gatear no kill-switch de spam cósmico (default OFF) — não aparece mais sozinho.
        if (!br.com.murilo.liberthia.config.LiberthiaConfig.SERVER.cosmicChatSpamEnabled.get()) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 100 != 0) return; // 5s

        // Base chance: 0
        float chance = 0;
        if (InsanityData.getInsanity(sp) >= 30) chance += 0.05F;
        if (InsanityData.getInsanity(sp) >= 60) chance += 0.10F;
        if (SecondSkyManager.isExposed(sp)) chance += 0.10F;
        if (sp.serverLevel().dimension().location().getPath().contains("loom")) {
            chance += 0.20F;
        }
        // Night time = mais chance
        if (sp.level().isNight()) chance += 0.05F;
        if (Math.random() > chance) return;

        spawnWalker(sp);
    }

    /** Spawn 1-3 walker silhouettes em direções aleatórias da render distance. */
    private static void spawnWalker(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        int count = 1 + (int)(Math.random() * 2);

        for (int i = 0; i < count; i++) {
            // Distance 48-80b
            double angle = Math.random() * Math.PI * 2;
            double dist = 48 + Math.random() * 32;
            double wx = sp.getX() + Math.cos(angle) * dist;
            double wz = sp.getZ() + Math.sin(angle) * dist;
            double wy = sp.getY() + (Math.random() - 0.5) * 6;

            // Check direction — só spawn FORA do cone de visão
            Vec3 lookDir = sp.getLookAngle();
            Vec3 toWalker = new Vec3(wx - sp.getX(), 0, wz - sp.getZ()).normalize();
            if (lookDir.dot(toWalker) > 0.6) {
                // Player tá olhando pra essa direção — skip
                continue;
            }

            // Spawn vulto_shadow particles formando silhueta humanoide
            // Cabeça
            level.sendParticles(sp, ModParticles.VULTO_SHADOW.get(), true,
                    wx, wy + 1.8, wz, 4, 0.1, 0.1, 0.1, 0);
            // Torso
            for (int t = 0; t < 3; t++) {
                level.sendParticles(sp, ModParticles.VULTO_SHADOW.get(), true,
                        wx + (Math.random() - 0.5) * 0.3,
                        wy + 0.8 + t * 0.3,
                        wz + (Math.random() - 0.5) * 0.3,
                        1, 0, 0, 0, 0);
            }
            // Olhos vermelhos
            level.sendParticles(sp, net.minecraft.core.particles.ParticleTypes.FLAME, true,
                    wx, wy + 1.9, wz, 1, 0, 0, 0, 0);
        }

        // 30% chance de chat sussurro
        if (Math.random() < 0.3) {
            HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                    "§8§o*você vê algo no horizonte*");
        }
        LiberthiaMod.LOGGER.debug("[Walkers] {} saw {} walker(s)",
                sp.getName().getString(), count);
    }
}
