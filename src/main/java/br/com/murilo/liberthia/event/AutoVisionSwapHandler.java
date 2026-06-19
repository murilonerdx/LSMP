package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.matter.MatterProfile;
import br.com.murilo.liberthia.matter.MatterProfileProvider;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.StartVisionSwapS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * v0.1.38: WM Vision automática — quem está infectado por matéria branca
 * (profile.getWhite() ≥ {@link #MIN_WHITE}) "ve" automaticamente através de
 * outros infectados, sem precisar do Vision Swap Lens.
 *
 * <h3>Mecânica</h3>
 * <ul>
 *   <li>A cada {@link #CHECK_INTERVAL_TICKS} (~1 min), pra cada player elegível
 *       (WM ≥ 50), tenta disparar um vision swap automático.</li>
 *   <li>Cooldown por player: {@link #COOLDOWN_TICKS} entre swaps. Sem isso
 *       todo player infectado ficaria em swap permanente.</li>
 *   <li>Quanto MAIS WM o player tem, maior a chance e mais rápido o cooldown
 *       — escalonamento que faz a "infecção sentir worsening".</li>
 *   <li>Mantém compatibilidade com Vision Swap Lens (item) — esse handler só
 *       dispara se o player NÃO está em swap ativo via item.</li>
 * </ul>
 *
 * <p>O swap usa o mesmo {@link VisionSwapManager} infrastructure — mesma
 * duração, mesmo packet flow, mesma cancelação.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AutoVisionSwapHandler {

    /** Threshold mínimo de WM pra elegibilidade. Mesma const do VisionSwapManager. */
    private static final float MIN_WHITE = 50.0f;

    /** Frequência do check (1 min). Cada player rola dado nessa frequência. */
    private static final int CHECK_INTERVAL_TICKS = 1200;

    /** Cooldown base entre auto-swaps por player (3 min). */
    private static final int COOLDOWN_TICKS = 3600;

    /** Player UUID → last auto swap tick. */
    private static final Map<UUID, Long> LAST_AUTO_SWAP = new HashMap<>();

    private static final Random RNG = new Random();

    private AutoVisionSwapHandler() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        if (server.overworld() == null) return;
        long now = server.overworld().getGameTime();
        if (now % CHECK_INTERVAL_TICKS != 0) return;

        // Coleta todos os players elegíveis (WM ≥ MIN_WHITE)
        List<ServerPlayer> eligible = new ArrayList<>();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            MatterProfile profile = sp.getCapability(MatterProfileProvider.CAP).orElse(null);
            if (profile == null) continue;
            if (profile.getWhite() < MIN_WHITE) continue;
            eligible.add(sp);
        }
        if (eligible.size() < 2) return; // precisa de ≥ 2 pra ter user + target

        // Pra cada elegível, tenta disparar (probabilístico baseado em WM)
        for (ServerPlayer user : eligible) {
            // Cooldown check
            Long last = LAST_AUTO_SWAP.get(user.getUUID());
            int cooldown = effectiveCooldown(user);
            if (last != null && now - last < cooldown) continue;

            // Não roda se já tem swap ativo (via item ou auto anterior)
            if (VisionSwapManager.isUserSwapping(user.getUUID())) continue;

            // Probabilidade escala com WM: 50 WM = 30% chance, 100 WM = 80%
            float wm = user.getCapability(MatterProfileProvider.CAP)
                    .map(MatterProfile::getWhite).orElse(0f);
            float chance = 0.30f + (wm - MIN_WHITE) / 50.0f * 0.50f;
            if (RNG.nextFloat() > chance) continue;

            // Escolhe target random ≠ user
            ServerPlayer target = pickRandomDifferent(eligible, user);
            if (target == null) continue;

            // Cross-dim: pula targets em dim diferente (client não resolve)
            if (target.serverLevel() != user.serverLevel()) continue;

            // Dispara swap
            if (VisionSwapManager.start(user, target)) {
                ModNetwork.sendToPlayer(user, new StartVisionSwapS2CPacket(target.getUUID()));
                LAST_AUTO_SWAP.put(user.getUUID(), now);
                user.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "✦ A matéria branca te puxa para outro olhar...")
                                .withStyle(net.minecraft.ChatFormatting.AQUA,
                                        net.minecraft.ChatFormatting.ITALIC),
                        true);
            }
        }
    }

    /**
     * Cooldown efetivo escala INVERSO com WM: mais matter = mais frequente.
     * 50 WM → 3 min, 100 WM → 1.5 min.
     */
    private static int effectiveCooldown(ServerPlayer sp) {
        float wm = sp.getCapability(MatterProfileProvider.CAP)
                .map(MatterProfile::getWhite).orElse(50f);
        // wm 50 → 1.0 multiplier, wm 100 → 0.5 multiplier
        float mult = Math.max(0.5f, 1.5f - wm / 100f);
        return (int) (COOLDOWN_TICKS * mult);
    }

    private static ServerPlayer pickRandomDifferent(List<ServerPlayer> list, ServerPlayer exclude) {
        if (list.size() < 2) return null;
        // Filtra exclude
        List<ServerPlayer> copy = new ArrayList<>(list);
        copy.removeIf(p -> p.getUUID().equals(exclude.getUUID()));
        if (copy.isEmpty()) return null;
        return copy.get(RNG.nextInt(copy.size()));
    }
}
