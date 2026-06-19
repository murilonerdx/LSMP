package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r183 — gate de spawn das criaturas cósmicas/horror: spawns NATURAIS só acontecem à
 * NOITE, no ESCURO, e perto de um player com sanidade < 40%. Caso contrário cancela —
 * nada de horror à luz do dia nem "do nada" com sanidade alta (pedido do user).
 * Magos NÃO são afetados (não são cósmicos) e spawnam normalmente.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class HorrorSpawnGateHandler {
    private HorrorSpawnGateHandler() {}

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn e) {
        if (!SanityDrainHandler.isCosmicCreature(e.getEntity())) return;
        MobSpawnType r = e.getSpawnType();
        if (r != MobSpawnType.NATURAL && r != MobSpawnType.CHUNK_GENERATION) return; // ovo/comando/spawner/estrutura livres
        if (!(e.getLevel() instanceof ServerLevel sl)) return;

        BlockPos pos = BlockPos.containing(e.getX(), e.getY(), e.getZ());
        boolean night = !sl.isDay();
        boolean dark = sl.getMaxLocalRawBrightness(pos) <= 7;
        boolean lowSanity = sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos).inflate(64))
                .stream().anyMatch(pl -> SpiritDimension.getSanity(pl) < 40);

        if (!(night && dark && lowSanity)) e.setSpawnCancelled(true);
    }
}
