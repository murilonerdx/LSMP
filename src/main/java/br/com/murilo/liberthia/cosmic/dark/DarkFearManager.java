package br.com.murilo.liberthia.cosmic.dark;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r178: <b>Medo do escuro</b> — no escuro (luz &le; 7) a sanidade cai mais rápido, e
 * com sanidade baixa surgem sussurros e, raramente, uma <b>tocha apaga sozinha</b>
 * perto de você. Quanto mais escuro e instável, pior. Gateado no kill-switch de
 * horror cósmico ({@code cosmic_horror_enabled}, default OFF) pra não incomodar
 * quem não quer.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class DarkFearManager {

    private DarkFearManager() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;
        if (sp.tickCount % 40 != 0) return; // ~2s
        if (!br.com.murilo.liberthia.config.LiberthiaConfig.SERVER.cosmicHorrorEnabled.get()) return;

        ServerLevel sl = sp.serverLevel();
        if (sl.getMaxLocalRawBrightness(sp.blockPosition()) > 7) return; // não está no escuro

        // sanidade cai mais rápido no escuro
        if (sp.tickCount % 200 == 0) {
            SpiritDimension.addSanity(sp, -1);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
        }

        int sanity = SpiritDimension.getSanity(sp);
        if (sanity >= 40) return; // r183: alinhado ao gate global de horror (<40%)

        var r = sp.getRandom();
        if (r.nextFloat() < 0.10F) {
            net.minecraft.world.phys.Vec3 b = sp.position().subtract(sp.getLookAngle().scale(2.5)).add(0, 1, 0);
            sl.playSound(null, BlockPos.containing(b), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.6F, 0.5F);
        }
        // Vela da Sanidade (SanityCandleItem) protege as tochas enquanto durar
        boolean warded = sp.getPersistentData().getLong("liberthia.warded_until") > sl.getGameTime();
        if (!warded && r.nextFloat() < 0.05F) killNearbyTorch(sl, sp);
    }

    /** Apaga UMA tocha (vira ar) num raio pequeno — "a luz não dura perto de você". */
    private static void killNearbyTorch(ServerLevel sl, ServerPlayer sp) {
        BlockPos c = sp.blockPosition();
        for (int i = 0; i < 30; i++) {
            BlockPos p = c.offset(sl.random.nextInt(11) - 5, sl.random.nextInt(7) - 3, sl.random.nextInt(11) - 5);
            BlockState st = sl.getBlockState(p);
            if (st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH)) {
                sl.removeBlock(p, false);
                sl.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 8, 0.1, 0.1, 0.1, 0.01);
                sl.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
                return;
            }
        }
    }
}
