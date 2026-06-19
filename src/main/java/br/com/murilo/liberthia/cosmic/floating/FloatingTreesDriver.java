package br.com.murilo.liberthia.cosmic.floating;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22 r55: <b>Floating Trees Driver</b> — em baixa sanidade, árvores
 * "fantasma" começam a aparecer flutuando ao redor do player.
 *
 * <h2>Implementação</h2>
 * Não spawna entidades nem coloca blocos REAIS — usa BlockParticleOption
 * com {@link Blocks#OAK_LOG} / {@link Blocks#OAK_LEAVES} pra renderizar
 * uma "árvore" feita de particles em cluster suspenso. Por ser particle
 * client-only via packet pro player especifico, ninguém mais vê.
 *
 * <h2>Trigger</h2>
 * Sanity ≤ 30 OU spirit world, com chance 1% por tick (~3min interval).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class FloatingTreesDriver {

    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private FloatingTreesDriver() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (sp.tickCount % 40 != 0) return; // 2s rate

        int sanity = SpiritDimension.getSanity(sp);
        boolean inSpirit = SpiritDimension.isInSpiritWorld(sp);
        if (sanity > 30 && !inSpirit) return;

        UUID id = sp.getUUID();
        Long cd = COOLDOWNS.get(id);
        if (cd != null && sp.tickCount < cd) return;

        // 1% chance
        if (Math.random() > 0.01) return;

        spawnFloatingTree(sp, level);
        COOLDOWNS.put(id, (long)sp.tickCount + 3600); // 3 min cooldown
    }

    private static void spawnFloatingTree(ServerPlayer sp, ServerLevel level) {
        // Posição: lateral random a 6-15 blocos, altura 3-8 acima do player
        double a = Math.random() * Math.PI * 2;
        double r = 6 + Math.random() * 9;
        double dx = Math.cos(a) * r;
        double dz = Math.sin(a) * r;
        double dy = 3 + Math.random() * 5;
        double cx = sp.getX() + dx;
        double cy = sp.getY() + dy;
        double cz = sp.getZ() + dz;

        // Tronco — 4 particles verticais
        BlockParticleOption log = new BlockParticleOption(
                ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            level.sendParticles(log,
                    cx, cy + i * 0.6, cz, 4, 0.1, 0.1, 0.1, 0);
        }
        // Copa — 30 particles em volta do topo
        BlockParticleOption leaves = new BlockParticleOption(
                ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState());
        for (int i = 0; i < 30; i++) {
            double la = Math.random() * Math.PI * 2;
            double lr = 0.3 + Math.random() * 1.5;
            double lh = Math.random() * 1.5;
            level.sendParticles(leaves,
                    cx + Math.cos(la) * lr,
                    cy + 2.4 + lh,
                    cz + Math.sin(la) * lr,
                    1, 0.05, 0.05, 0.05, 0);
        }

        // Aura mágica
        level.sendParticles(ParticleTypes.PORTAL,
                cx, cy + 1.5, cz, 15, 0.5, 1.0, 0.5, 0.02);

        // Som
        CosmicSoundManager.playRealityDistortion(sp);
        // r80: removido chat spam "você vê algo flutuando..." — particles
        // visuais + som já comunicam o evento sem floodar o chat.

        LiberthiaMod.LOGGER.debug("[FloatingTree] spawned for {} at {} {} {}",
                sp.getName().getString(), cx, cy, cz);
    }
}
