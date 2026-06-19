package br.com.murilo.liberthia.fog;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.PersonalFogS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NÉVOA PESSOAL — névoa de visão que afeta UM player específico (só ele vê) e o
 * acompanha pra onde for, por um tempo. Tipo um efeito de poção.
 *
 * <p>Estado guardado no NBT persistente do player (sobrevive relog; some na
 * morte, como efeito). Sincronizado só pra esse player via {@link PersonalFogS2CPacket}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PersonalFogManager {

    private static final String ACTIVE = "liberthia.pfog_active";
    private static final String COLOR = "liberthia.pfog_color";
    private static final String DENSITY = "liberthia.pfog_density";
    private static final String END = "liberthia.pfog_end";

    private PersonalFogManager() {}

    /** Aplica névoa pessoal por {@code seconds} segundos. */
    public static void apply(ServerPlayer sp, int color, float density, int seconds) {
        long end = sp.serverLevel().getGameTime() + (long) seconds * 20L;
        CompoundTag d = sp.getPersistentData();
        d.putBoolean(ACTIVE, true);
        d.putInt(COLOR, color & 0xFFFFFF);
        d.putFloat(DENSITY, Math.max(0.05f, Math.min(2f, density)));
        d.putLong(END, end);
        ModNetwork.sendToPlayer(sp, new PersonalFogS2CPacket(true, color & 0xFFFFFF,
                Math.max(0.05f, Math.min(2f, density))));
    }

    /** Remove a névoa pessoal. */
    public static void clear(ServerPlayer sp) {
        sp.getPersistentData().putBoolean(ACTIVE, false);
        ModNetwork.sendToPlayer(sp, new PersonalFogS2CPacket(false, 0, 0f));
    }

    public static boolean isActive(ServerPlayer sp) {
        return sp.getPersistentData().getBoolean(ACTIVE);
    }

    // ── expiração ────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if ((sp.tickCount & 15) != 0) return; // checa ~1x/s
        CompoundTag d = sp.getPersistentData();
        if (!d.getBoolean(ACTIVE)) return;
        if (sp.serverLevel().getGameTime() >= d.getLong(END)) {
            clear(sp);
        }
    }

    // ── re-sincroniza no login (se ainda válida) ─────────────────────
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        CompoundTag d = sp.getPersistentData();
        if (!d.getBoolean(ACTIVE)) return;
        if (sp.serverLevel().getGameTime() >= d.getLong(END)) {
            clear(sp);
        } else {
            ModNetwork.sendToPlayer(sp, new PersonalFogS2CPacket(true, d.getInt(COLOR), d.getFloat(DENSITY)));
        }
    }
}
