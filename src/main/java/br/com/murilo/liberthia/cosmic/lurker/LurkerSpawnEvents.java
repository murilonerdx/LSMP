package br.com.murilo.liberthia.cosmic.lurker;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * r173: spawna o {@link LurkerEntity} enquanto o player MINERA, no escuro,
 * raramente e com cooldown por player. Aparece ~10 blocos, encara, some em 2s.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class LurkerSpawnEvents {

    private LurkerSpawnEvents() {}

    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static final int COOLDOWN = 1800; // 90s entre sustos
    private static final int CHANCE = 110;     // 1/110 por bloco quebrado no escuro

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;
        if (!(sp.level() instanceof ServerLevel level)) return;

        // só no escuro (mineração subterrânea / cavernas)
        if (level.getMaxLocalRawBrightness(e.getPos()) > 7) return;

        long now = level.getGameTime();
        Long last = LAST.get(sp.getUUID());
        if (last != null && now - last < COOLDOWN) return;
        if (level.random.nextInt(CHANCE) != 0) return;

        double ang = level.random.nextDouble() * Math.PI * 2;
        double dist = 9 + level.random.nextDouble() * 3;
        double x = sp.getX() + Math.cos(ang) * dist;
        double z = sp.getZ() + Math.sin(ang) * dist;
        double y = sp.getEyeY() - 0.5;

        BlockPos bp = BlockPos.containing(x, y, z);
        if (!level.getBlockState(bp).isAir()) return;          // precisa de ar pra "caber" no escuro
        if (level.getMaxLocalRawBrightness(bp) > 7) return;     // e estar escuro lá também

        int face = level.random.nextInt(6);
        LurkerEntity.spawn(level, x, y, z, face);
        LAST.put(sp.getUUID(), now);

        // barulho bem sutil perto do rosto
        level.playSound(null, bp, SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 0.45F, 0.5F);
    }
}
