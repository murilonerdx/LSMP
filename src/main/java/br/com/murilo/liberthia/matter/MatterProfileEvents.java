package br.com.murilo.liberthia.matter;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.MatterProfileSyncS2CPacket;
import br.com.murilo.liberthia.registry.ModMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hub central do sistema de matéria — anexa capability, ticka decay/sync,
 * aplica os 5 MobEffects conforme o perfil ativo.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MatterProfileEvents {

    public static final ResourceLocation CAP_KEY =
            new ResourceLocation(LiberthiaMod.MODID, "matter_profile");

    /** Decai 1 ponto a cada N ticks (1200 = 1 min). */
    private static final int DECAY_PERIOD = 1200;
    private static final float DECAY_AMOUNT = 1.0f;
    /** Período de sync mesmo sem mudança — protege contra perda de pacotes. */
    private static final int SYNC_PERIOD = 100; // 5s

    private MatterProfileEvents() {}

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(CAP_KEY, new MatterProfileProvider());
        }
    }

    /** Quando jogador respawna, copia valores antigos. */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(MatterProfileProvider.CAP).ifPresent(oldProfile -> {
            event.getEntity().getCapability(MatterProfileProvider.CAP).ifPresent(newProfile -> {
                newProfile.copyFrom(oldProfile);
            });
        });
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncTo(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        sp.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            // Decay passivo
            if (sp.tickCount % DECAY_PERIOD == 0) {
                profile.decayAll(DECAY_AMOUNT);
                syncTo(sp);
            }
            // Sync periódico
            if (sp.tickCount % SYNC_PERIOD == 0) {
                syncTo(sp);
            }
            // Aplica efeitos do perfil ativo
            applyProfileEffects(sp, profile);
        });
    }

    /** Aplica/refresca os MobEffects do perfil ativo a cada tick. */
    private static void applyProfileEffects(ServerPlayer sp, MatterProfile profile) {
        // Remove todos os efeitos de perfil antes de aplicar o atual.
        // Só re-aplicamos a cada 40 ticks pra não floodar pacotes de status.
        if (sp.tickCount % 40 != 0) return;

        var type = profile.getActiveType();
        // Limpa os outros (não interfere em efeitos vanilla/poções)
        for (var t : MatterProfileType.values()) {
            if (t == type) continue;
            var eff = effectFor(t);
            if (eff != null && sp.hasEffect(eff)) sp.removeEffect(eff);
        }
        var current = effectFor(type);
        if (current != null) {
            int amplifier = computeAmplifier(profile, type);
            // 100 ticks (5s) — refresca a cada 40t pra ficar permanente enquanto perfil está ativo
            sp.addEffect(new MobEffectInstance(current, 100, amplifier, true, false, true));
        }
    }

    private static @org.jetbrains.annotations.Nullable net.minecraft.world.effect.MobEffect effectFor(MatterProfileType t) {
        return switch (t) {
            case DARK         -> ModMobEffects.AGGRESSION.get();
            case DARK_WHITE   -> ModMobEffects.CONTAINED.get();
            case WHITE        -> ModMobEffects.FORGETFULNESS.get();
            case YELLOW       -> ModMobEffects.EMOTIONAL_CHAOS.get();
            case YELLOW_WHITE -> ModMobEffects.COLD_FOCUS.get();
            default           -> null;
        };
    }

    /**
     * Amplifier (potência) do efeito, baseado em quão alto está o valor relevante.
     * 0 → I, 1 → II, 2 → III.
     */
    private static int computeAmplifier(MatterProfile p, MatterProfileType t) {
        float v = switch (t) {
            case DARK         -> p.getDark();
            case WHITE        -> p.getWhite();
            case YELLOW       -> p.getYellow();
            case DARK_WHITE   -> Math.min(p.getDark(), p.getWhite());
            case YELLOW_WHITE -> Math.min(p.getYellow(), p.getWhite());
            default           -> 0;
        };
        if (v >= 75) return 2;
        if (v >= 50) return 1;
        return 0;
    }

    /** Empacota e envia pra o cliente. */
    public static void syncTo(ServerPlayer sp) {
        sp.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            ModNetwork.sendToPlayer(sp,
                    new MatterProfileSyncS2CPacket(profile.getDark(), profile.getWhite(), profile.getYellow()));
        });
    }
}
