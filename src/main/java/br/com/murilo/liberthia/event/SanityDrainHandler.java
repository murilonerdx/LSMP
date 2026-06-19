package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.ICosmicHorror;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r183 — <b>Dreno de sanidade</b> (faz a sanidade cair → habilita os eventos de horror <40%).
 * Fontes: ficar no ESCURO, MINERAR muito tempo (blocos quebrados no escuro/subsolo), e VER
 * uma criatura cósmica/horror de perto (com linha de visão). Em luz/segurança a sanidade
 * regenera (SpiritWorldEvents). Spirit World tem seu próprio dreno — aqui pulamos.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SanityDrainHandler {
    private SanityDrainHandler() {}

    private static final String NBT_MINED = "liberthia.sanityMineCount";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;
        if (SpiritDimension.isInSpiritWorld(sp)) return; // tem dreno próprio
        int t = sp.tickCount;
        int sanity = SpiritDimension.getSanity(sp);
        if (sanity <= 0) return;

        int drain = 0;
        int light = sp.level().getMaxLocalRawBrightness(sp.blockPosition());
        // escuro: dreno lento; escuridão total: mais rápido
        if (light <= 7 && t % 200 == 0) drain += 1;
        if (light <= 4 && t % 120 == 0) drain += 1;

        // ver criatura cósmica/horror de perto (a cada 2s) — Coroa Sem Face anula esse dreno
        if (t % 40 == 0 && seesCosmicCreature(sp)
                && !br.com.murilo.liberthia.compat.CuriosCompat.isWearing(sp, br.com.murilo.liberthia.registry.ModItems.ASTARON_FACELESS_CROWN.get()))
            drain += 1;

        if (drain > 0) {
            SpiritDimension.addSanity(sp, -drain);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
        }
    }

    /** Minerar muito tempo: cada 56 blocos quebrados no escuro/subsolo → -1 sanidade. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;
        if (SpiritDimension.isInSpiritWorld(sp)) return;
        int light = sp.level().getMaxLocalRawBrightness(sp.blockPosition());
        boolean underground = sp.getY() < 50 || light <= 7;
        if (!underground) return;
        var d = sp.getPersistentData();
        int c = d.getInt(NBT_MINED) + 1;
        if (c >= 56) {
            c = 0;
            if (SpiritDimension.getSanity(sp) > 0) {
                SpiritDimension.addSanity(sp, -1);
                ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
            }
        }
        d.putInt(NBT_MINED, c);
    }

    private static boolean seesCosmicCreature(ServerPlayer sp) {
        var box = sp.getBoundingBox().inflate(20.0);
        for (LivingEntity le : sp.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == sp) continue;
            if (isCosmic(le) && sp.hasLineOfSight(le)
                    && sp.distanceToSqr(le) <= 20.0 * 20.0) return true;
        }
        return false;
    }

    /** Cósmico/horror: marcador ICosmicHorror (entidades novas) OU classe no pacote cosmic. */
    private static boolean isCosmic(LivingEntity le) {
        if (le instanceof ICosmicHorror) return true;
        String cn = le.getClass().getName();
        return cn.startsWith("br.com.murilo.liberthia.cosmic.") || cn.contains(".horror.");
    }

    public static boolean isCosmicCreature(LivingEntity le) { return isCosmic(le); }
}
